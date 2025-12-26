package com.example.dropmultiplier;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.function.ConditionalLootFunction;
import net.minecraft.loot.function.LootFunctionType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;

import java.util.List;

public class MultiplyCountLootFunction extends ConditionalLootFunction {
    public static final MapCodec<MultiplyCountLootFunction> CODEC = RecordCodecBuilder.mapCodec(
            instance -> addConditionsField(instance).apply(instance, MultiplyCountLootFunction::new)
    );

    protected MultiplyCountLootFunction(List<LootCondition> conditions) {
        super(conditions);
    }

    public static ConditionalLootFunction.Builder<?> builder() {
        return ConditionalLootFunction.builder(MultiplyCountLootFunction::new);
    }

    @Override
    protected ItemStack process(ItemStack stack, LootContext context) {
        if (stack.isEmpty()) {
            return stack;
        }

        BlockState blockState = context.get(LootContextParameters.BLOCK_STATE);
        if (blockState != null) {
            if (context.getWorld() instanceof ServerWorld serverWorld) {
                Vec3d origin = context.get(LootContextParameters.ORIGIN);
                if (origin != null) {
                    BlockPos pos = BlockPos.ofFloored(origin);
                    PlacedBlockTracker tracker = PlacedBlockTracker.get(serverWorld);
                    if (tracker.isPlaced(serverWorld, pos)) {
                        tracker.clearPlaced(serverWorld, pos);
                        return stack;
                    }
                    tracker.clearPlaced(serverWorld, pos);
                }
            }
            Identifier blockId = Registries.BLOCK.getId(blockState.getBlock());
            return applyMultiplier(stack, DropMultiplierMod.CONFIG.getBlockMultiplier(blockId));
        }

        Entity entity = context.get(LootContextParameters.THIS_ENTITY);
        if (entity != null) {
            if (entity instanceof PlayerEntity) {
                return stack;
            }
            Identifier entityId = Registries.ENTITY_TYPE.getId(entity.getType());
            return applyMultiplier(stack, DropMultiplierMod.CONFIG.getEntityMultiplier(entityId));
        }

        return stack;
    }

    private ItemStack applyMultiplier(ItemStack stack, int multiplier) {
        if (multiplier <= 0) {
            stack.setCount(0);
            return stack;
        }
        if (multiplier == 1) {
            return stack;
        }

        long multiplied = (long) stack.getCount() * (long) multiplier;
        int max = stack.getMaxCount();

        if (multiplied > max) {
            stack.setCount(max);
        } else {
            stack.setCount((int) multiplied);
        }

        return stack;
    }

    @Override
    public LootFunctionType<? extends ConditionalLootFunction> getType() {
        return DropMultiplierMod.MULTIPLY_COUNT_FUNCTION_TYPE;
    }
}
