package com.example.dropmultiplier;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.function.ConditionalLootFunction;
import net.minecraft.loot.function.LootFunctionType;

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

        int multiplier = DropMultiplierMod.CONFIG.getMultiplierForLootContext(context);
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
