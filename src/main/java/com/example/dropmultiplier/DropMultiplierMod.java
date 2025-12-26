package com.example.dropmultiplier;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.fabricmc.fabric.api.loot.v3.FabricLootTableBuilder;
import net.minecraft.loot.function.LootFunctionType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class DropMultiplierMod implements ModInitializer {
    public static final String MOD_ID = "dropmultiplier";

    public static DropMultiplierConfig CONFIG;
    public static LootFunctionType<MultiplyCountLootFunction> MULTIPLY_COUNT_FUNCTION_TYPE;

    @Override
    public void onInitialize() {
        CONFIG = DropMultiplierConfig.load();

        MULTIPLY_COUNT_FUNCTION_TYPE = Registry.register(
                Registries.LOOT_FUNCTION_TYPE,
                Identifier.of(MOD_ID, "multiply_count"),
                new LootFunctionType<>(MultiplyCountLootFunction.CODEC)
        );

        LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> {
            Identifier id = key.getValue();
            String path = id.getPath();

            if (!path.startsWith("blocks/") && !path.startsWith("entities/")) {
                return;
            }

            ((FabricLootTableBuilder) tableBuilder).modifyPools(poolBuilder -> {
                poolBuilder.apply(MultiplyCountLootFunction.builder());
            });
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            DropMultiplierCommands.register(dispatcher);
        });
    }
}
