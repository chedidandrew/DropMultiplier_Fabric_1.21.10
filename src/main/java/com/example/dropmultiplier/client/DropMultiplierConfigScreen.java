package com.example.dropmultiplier.client;

import com.example.dropmultiplier.DropMultiplierConfig;
import com.example.dropmultiplier.DropMultiplierMod;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Environment(EnvType.CLIENT)
public class DropMultiplierConfigScreen {
    public static Screen create(Screen parent) {
        DropMultiplierConfig cfg = DropMultiplierMod.CONFIG;

        final int[] defaultMultiplier = new int[]{cfg.defaultMultiplier};
        final List<String> blockRules = new ArrayList<>();
        final List<String> entityRules = new ArrayList<>();

        cfg.blockMultipliers.entrySet().stream()
                .sorted(Comparator.comparing(Map.Entry::getKey))
                .forEach(e -> blockRules.add(e.getKey() + "=" + e.getValue()));

        cfg.entityMultipliers.entrySet().stream()
                .sorted(Comparator.comparing(Map.Entry::getKey))
                .forEach(e -> entityRules.add(e.getKey() + "=" + e.getValue()));

        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Text.literal("DropMultiplier"));

        ConfigEntryBuilder eb = builder.entryBuilder();

        ConfigCategory general = builder.getOrCreateCategory(Text.literal("General"));
        general.addEntry(
                eb.startIntField(Text.literal("Default multiplier"), cfg.defaultMultiplier)
                        .setMin(0)
                        .setMax(1_000_000)
                        .setSaveConsumer(v -> defaultMultiplier[0] = v)
                        .build()
        );

        ConfigCategory blocks = builder.getOrCreateCategory(Text.literal("Blocks"));
        blocks.addEntry(
                eb.startStrList(Text.literal("Block overrides (id=multiplier)"), blockRules)
                        .setExpanded(true)
                        .setSaveConsumer(list -> {
                            blockRules.clear();
                            blockRules.addAll(list);
                        })
                        .build()
        );

        ConfigCategory entities = builder.getOrCreateCategory(Text.literal("Entities"));
        entities.addEntry(
                eb.startStrList(Text.literal("Entity overrides (id=multiplier)"), entityRules)
                        .setExpanded(true)
                        .setSaveConsumer(list -> {
                            entityRules.clear();
                            entityRules.addAll(list);
                        })
                        .build()
        );

        builder.setSavingRunnable(() -> {
            DropMultiplierConfig newCfg = DropMultiplierMod.CONFIG;
            newCfg.setDefaultMultiplier(defaultMultiplier[0]);

            newCfg.blockMultipliers.clear();
            newCfg.entityMultipliers.clear();

            for (String s : blockRules) {
                applyRuleToBlocks(newCfg, s);
            }
            for (String s : entityRules) {
                applyRuleToEntities(newCfg, s);
            }

            newCfg.save();
        });

        return builder.build();
    }

    private static void applyRuleToBlocks(DropMultiplierConfig cfg, String rule) {
        ParsedRule pr = ParsedRule.parse(rule);
        if (pr == null) return;

        Identifier id = Identifier.tryParse(pr.id);
        if (id == null) return;
        if (!Registries.BLOCK.containsId(id)) return;

        cfg.setBlockMultiplier(id, pr.multiplier);
    }

    private static void applyRuleToEntities(DropMultiplierConfig cfg, String rule) {
        ParsedRule pr = ParsedRule.parse(rule);
        if (pr == null) return;

        Identifier id = Identifier.tryParse(pr.id);
        if (id == null) return;
        if (!Registries.ENTITY_TYPE.containsId(id)) return;

        cfg.setEntityMultiplier(id, pr.multiplier);
    }

    private static final class ParsedRule {
        final String id;
        final int multiplier;

        private ParsedRule(String id, int multiplier) {
            this.id = id;
            this.multiplier = multiplier;
        }

        static ParsedRule parse(String s) {
            if (s == null) return null;
            int eq = s.indexOf('=');
            if (eq <= 0 || eq == s.length() - 1) return null;

            String id = s.substring(0, eq).trim();
            String multStr = s.substring(eq + 1).trim();
            if (id.isEmpty() || multStr.isEmpty()) return null;

            try {
                int m = Integer.parseInt(multStr);
                if (m < 0) m = 0;
                return new ParsedRule(id, m);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
    }
}
