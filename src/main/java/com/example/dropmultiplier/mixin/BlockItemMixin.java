package com.example.dropmultiplier.mixin;

import com.example.dropmultiplier.PlacedBlockTracker;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public class BlockItemMixin {
    @Inject(method = "place", at = @At("RETURN"))
    private void dropmultiplier$markPlaced(ItemPlacementContext context, CallbackInfoReturnable<ActionResult> cir) {
        if (context.getWorld().isClient()) {
            return;
        }
        if (context.getPlayer() == null) {
            return;
        }
        ActionResult result = cir.getReturnValue();
        if (!result.isAccepted()) {
            return;
        }
        if (context.getWorld() instanceof ServerWorld serverWorld) {
            PlacedBlockTracker.get(serverWorld).markPlaced(serverWorld, context.getBlockPos());
        }
    }
}
