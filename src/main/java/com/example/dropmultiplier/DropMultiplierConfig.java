package com.example.dropmultiplier;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class DropMultiplierConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "dropmultiplier.json";

    public Map<String, Integer> blockMultipliers = new HashMap<>();
    public Map<String, Integer> entityMultipliers = new HashMap<>();

    public static final int MIN_MULTIPLIER = 1;
    public static final int MAX_MULTIPLIER = 64;

    public static DropMultiplierConfig load() {
        Path path = getPath();
        if (Files.exists(path)) {
            try {
                String json = Files.readString(path, StandardCharsets.UTF_8);
                DropMultiplierConfig cfg = GSON.fromJson(json, DropMultiplierConfig.class);
                if (cfg == null) {
                    cfg = new DropMultiplierConfig();
                }
                cfg.sanitize();
                return cfg;
            } catch (Exception e) {
                DropMultiplierConfig cfg = new DropMultiplierConfig();
                cfg.save();
                return cfg;
            }
        }

        DropMultiplierConfig cfg = new DropMultiplierConfig();
        cfg.save();
        return cfg;
    }

    public void save() {
        Path path = getPath();
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(this), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }
    }

    public int getMultiplierForLootContext(LootContext context) {
        BlockState state = null;
        state = context.get(LootContextParameters.BLOCK_STATE);
        if (state != null) {
            Identifier blockId = Registries.BLOCK.getId(state.getBlock());
            return getBlockMultiplier(blockId);
        }

        Entity entity = null;
        entity = context.get(LootContextParameters.THIS_ENTITY);
        if (entity != null) {
            Identifier entityId = Registries.ENTITY_TYPE.getId(entity.getType());
            return getEntityMultiplier(entityId);
        }

        return MIN_MULTIPLIER;
    }

    public int getBlockMultiplier(Identifier blockId) {
        Integer v = blockMultipliers.get(blockId.toString());
        return v != null ? v : MIN_MULTIPLIER;
    }

    public int getEntityMultiplier(Identifier entityId) {
        Integer v = entityMultipliers.get(entityId.toString());
        return v != null ? v : MIN_MULTIPLIER;
    }

    public void setBlockMultiplier(Identifier blockId, int multiplier) {
        int clamped = clampMultiplier(multiplier);
        if (clamped == MIN_MULTIPLIER) {
            blockMultipliers.remove(blockId.toString());
        } else {
            blockMultipliers.put(blockId.toString(), clamped);
        }
    }

    public void setEntityMultiplier(Identifier entityId, int multiplier) {
        int clamped = clampMultiplier(multiplier);
        if (clamped == MIN_MULTIPLIER) {
            entityMultipliers.remove(entityId.toString());
        } else {
            entityMultipliers.put(entityId.toString(), clamped);
        }
    }

    public void removeBlockMultiplier(Identifier blockId) {
        blockMultipliers.remove(blockId.toString());
    }

    public void removeEntityMultiplier(Identifier entityId) {
        entityMultipliers.remove(entityId.toString());
    }

    private void sanitize() {
        if (blockMultipliers == null) blockMultipliers = new HashMap<>();
        if (entityMultipliers == null) entityMultipliers = new HashMap<>();
        sanitizeMap(blockMultipliers);
        sanitizeMap(entityMultipliers);
    }

    private static Path getPath() {
        return FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
    }

    private static int clampMultiplier(int value) {
        if (value < MIN_MULTIPLIER) {
            return MIN_MULTIPLIER;
        }
        return Math.min(value, MAX_MULTIPLIER);
    }

    private static void sanitizeMap(Map<String, Integer> map) {
        map.entrySet().removeIf(entry -> {
            Integer value = entry.getValue();
            if (value == null) {
                return true;
            }
            int clamped = clampMultiplier(value);
            if (clamped == MIN_MULTIPLIER) {
                return true;
            }
            if (clamped != value) {
                entry.setValue(clamped);
            }
            return false;
        });
    }
}
