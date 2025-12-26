package com.example.dropmultiplier.client;

import com.example.dropmultiplier.DropMultiplierConfig;
import com.example.dropmultiplier.DropMultiplierMod;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Environment(EnvType.CLIENT)
public class DropMultiplierConfigScreen {
    public static Screen create(Screen parent) {
        return new ConfigScreen(parent);
    }

    private enum Tab {
        BLOCKS,
        ENTITIES
    }

    private static final class ConfigScreen extends Screen {
        private static final int ROW_HEIGHT = 36;
        private static final int ICON_SIZE = 16;
        private static final int MIN_MULTIPLIER = DropMultiplierConfig.MIN_MULTIPLIER;
        private static final int MAX_MULTIPLIER = DropMultiplierConfig.MAX_MULTIPLIER;

        private final Screen parent;
        private final Map<Identifier, Integer> stagedBlockOverrides = new HashMap<>();
        private final Map<Identifier, Integer> stagedEntityOverrides = new HashMap<>();

        private Tab activeTab = Tab.BLOCKS;

        private TextFieldWidget searchField;
        private ButtonWidget blocksTabButton;
        private ButtonWidget entitiesTabButton;
        private ButtonWidget resetAllButton;
        private ButtonWidget doneButton;
        private ButtonWidget cancelButton;
        private RegistryListWidget listWidget;

        private List<EntryData> blockEntries;
        private List<EntryData> entityEntries;

        ConfigScreen(Screen parent) {
            super(Text.literal("DropMultiplier"));
            this.parent = parent;
            loadStagedOverrides();
        }

        private void loadStagedOverrides() {
            stagedBlockOverrides.clear();
            stagedEntityOverrides.clear();
            DropMultiplierConfig cfg = DropMultiplierMod.CONFIG;
            for (Map.Entry<String, Integer> entry : cfg.blockMultipliers.entrySet()) {
                Identifier id = Identifier.tryParse(entry.getKey());
                if (id != null) {
                    stagedBlockOverrides.put(id, entry.getValue());
                }
            }
            for (Map.Entry<String, Integer> entry : cfg.entityMultipliers.entrySet()) {
                Identifier id = Identifier.tryParse(entry.getKey());
                if (id != null) {
                    stagedEntityOverrides.put(id, entry.getValue());
                }
            }
        }

        @Override
        protected void init() {
            int margin = 16;
            int contentWidth = Math.max(240, this.width - margin * 2);
            int contentX = (this.width - contentWidth) / 2;

            int topY = 18;
            int tabWidth = Math.min(140, (contentWidth - 8) / 2);
            int tabHeight = 20;

            this.blocksTabButton = this.addDrawableChild(ButtonWidget.builder(Text.literal("Blocks"), button -> {
                activeTab = Tab.BLOCKS;
                updateTabs();
                refreshList();
            }).dimensions(contentX, topY, tabWidth, tabHeight).build());

            this.entitiesTabButton = this.addDrawableChild(ButtonWidget.builder(Text.literal("Entities"), button -> {
                activeTab = Tab.ENTITIES;
                updateTabs();
                refreshList();
            }).dimensions(contentX + tabWidth + 8, topY, tabWidth, tabHeight).build());

            int resetWidth = 90;
            int resetX = contentX + contentWidth - resetWidth;
            this.resetAllButton = this.addDrawableChild(ButtonWidget.builder(Text.literal("Reset all"), button -> {
                this.client.setScreen(new ConfirmScreen(confirmed -> {
                    if (confirmed) {
                        stagedBlockOverrides.clear();
                        stagedEntityOverrides.clear();
                        refreshList();
                    }
                    this.client.setScreen(this);
                }, Text.literal("Reset all overrides?"), Text.literal("All multipliers will be set back to 1.")));
            }).dimensions(resetX, topY, resetWidth, tabHeight).build());

            int searchY = topY + tabHeight + 8;
            this.searchField = new TextFieldWidget(this.textRenderer, contentX, searchY, contentWidth, 20, Text.literal(""));
            this.searchField.setPlaceholder(Text.literal("Search by id..."));
            this.searchField.setChangedListener(s -> refreshList());
            this.addDrawableChild(this.searchField);

            int bottomBarHeight = 32;
            int bottomY = this.height - bottomBarHeight;
            int listY = searchY + 26;
            int listHeight = Math.max(40, bottomY - listY - 8);

            this.listWidget = new RegistryListWidget(this.client, contentX, contentWidth, listHeight, listY, ROW_HEIGHT, this);
            this.addDrawableChild(this.listWidget);

            int buttonWidth = 120;
            int buttonsY = bottomY + 6;
            this.doneButton = this.addDrawableChild(ButtonWidget.builder(Text.literal("Done"), button -> {
                applyStagedOverrides();
                this.client.setScreen(this.parent);
            }).dimensions(contentX, buttonsY, buttonWidth, 20).build());

            this.cancelButton = this.addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"), button -> {
                this.client.setScreen(this.parent);
            }).dimensions(contentX + buttonWidth + 8, buttonsY, buttonWidth, 20).build());

            updateTabs();
            refreshList();
        }

        @Override
        public void close() {
            this.client.setScreen(this.parent);
        }

        private void updateTabs() {
            if (blocksTabButton != null) {
                blocksTabButton.active = activeTab != Tab.BLOCKS;
            }
            if (entitiesTabButton != null) {
                entitiesTabButton.active = activeTab != Tab.ENTITIES;
            }
        }

        private void refreshList() {
            if (listWidget == null) {
                return;
            }
            List<EntryData> entries = getEntriesForActiveTab();
            String query = searchField == null ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);
            List<EntryData> filtered = new ArrayList<>();
            if (query.isEmpty()) {
                filtered.addAll(entries);
            } else {
                for (EntryData data : entries) {
                    if (data.idString.contains(query)) {
                        filtered.add(data);
                    }
                }
            }
            listWidget.setEntries(filtered, activeTab);
        }

        private List<EntryData> getEntriesForActiveTab() {
            if (activeTab == Tab.BLOCKS) {
                if (blockEntries == null) {
                    blockEntries = buildBlockEntries();
                }
                return blockEntries;
            }
            if (entityEntries == null) {
                entityEntries = buildEntityEntries();
            }
            return entityEntries;
        }

        private List<EntryData> buildBlockEntries() {
            List<EntryData> entries = new ArrayList<>();
            for (Identifier id : Registries.BLOCK.getIds()) {
                Block block = Registries.BLOCK.get(id);
                Item item = block.asItem();
                ItemStack stack = item == Items.AIR ? new ItemStack(Items.BARRIER) : new ItemStack(item);
                entries.add(new EntryData(id, block.getName(), stack));
            }
            entries.sort(Comparator.comparing(entry -> entry.idString));
            return entries;
        }

        private List<EntryData> buildEntityEntries() {
            List<EntryData> entries = new ArrayList<>();
            for (Identifier id : Registries.ENTITY_TYPE.getIds()) {
                Identifier spawnEggId = Identifier.of(id.getNamespace(), id.getPath() + "_spawn_egg");
                Item spawnEgg = Registries.ITEM.get(spawnEggId);
                if (spawnEgg == Items.AIR) {
                    spawnEgg = Items.SPAWNER;
                }
                ItemStack stack = new ItemStack(spawnEgg);
                entries.add(new EntryData(id, Registries.ENTITY_TYPE.get(id).getName(), stack));
            }
            entries.sort(Comparator.comparing(entry -> entry.idString));
            return entries;
        }

        private void applyStagedOverrides() {
            DropMultiplierConfig cfg = DropMultiplierMod.CONFIG;
            cfg.blockMultipliers.clear();
            cfg.entityMultipliers.clear();
            for (Map.Entry<Identifier, Integer> entry : stagedBlockOverrides.entrySet()) {
                cfg.blockMultipliers.put(entry.getKey().toString(), entry.getValue());
            }
            for (Map.Entry<Identifier, Integer> entry : stagedEntityOverrides.entrySet()) {
                cfg.entityMultipliers.put(entry.getKey().toString(), entry.getValue());
            }
            cfg.save();
        }

        private int getMultiplier(Tab tab, Identifier id) {
            Integer value = tab == Tab.BLOCKS ? stagedBlockOverrides.get(id) : stagedEntityOverrides.get(id);
            return value != null ? value : MIN_MULTIPLIER;
        }

        private void setMultiplier(Tab tab, Identifier id, int value) {
            int clamped = clampMultiplier(value);
            Map<Identifier, Integer> overrides = tab == Tab.BLOCKS ? stagedBlockOverrides : stagedEntityOverrides;
            if (clamped == MIN_MULTIPLIER) {
                overrides.remove(id);
            } else {
                overrides.put(id, clamped);
            }
        }

        private int getOverridesCount() {
            return stagedBlockOverrides.size() + stagedEntityOverrides.size();
        }

        private int clampMultiplier(int value) {
            if (value < MIN_MULTIPLIER) {
                return MIN_MULTIPLIER;
            }
            return Math.min(value, MAX_MULTIPLIER);
        }

        @Override
        public void render(DrawContext context, int mouseX, int mouseY, float delta) {
            this.renderBackground(context, mouseX, mouseY, delta);
            super.render(context, mouseX, mouseY, delta);

            int margin = 16;
            int contentWidth = Math.max(240, this.width - margin * 2);
            int contentX = (this.width - contentWidth) / 2;
            int topY = 18;

            context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 6, 0xFFFFFF);

            String overridesText = "Overrides: " + getOverridesCount();
            int overridesWidth = this.textRenderer.getWidth(overridesText);
            int resetX = contentX + contentWidth - (resetAllButton == null ? 0 : resetAllButton.getWidth());
            int textX = Math.max(contentX, resetX - 8 - overridesWidth);
            context.drawTextWithShadow(this.textRenderer, Text.literal(overridesText), textX, topY + 6, 0xA0A0A0);
        }

        private final class RegistryListWidget extends AlwaysSelectedEntryListWidget<MultiplierEntry> {
            private final int x;

            RegistryListWidget(MinecraftClient client, int x, int width, int height, int y, int itemHeight, ConfigScreen owner) {
                super(client, width, height, y, itemHeight);
                this.x = x;
            }

            void setEntries(List<EntryData> entries, Tab tab) {
                this.clearEntries();
                for (EntryData data : entries) {
                    this.addEntry(new MultiplierEntry(data, tab));
                }
                this.setScrollY(0);
            }

            @Override
            protected int getScrollbarX() {
                return this.x + this.getWidth() - 6;
            }

            @Override
            public int getRowWidth() {
                return this.getWidth() - 12;
            }

            @Override
            public int getRowLeft() {
                return this.x + 6;
            }

            @Override
            public int getRowRight() {
                return this.getRowLeft() + getRowWidth();
            }
        }

        private final class MultiplierEntry extends AlwaysSelectedEntryListWidget.Entry<MultiplierEntry> {
            private final EntryData data;
            private final Tab tab;
            private final ButtonWidget minusButton;
            private final ButtonWidget plusButton;
            private final ButtonWidget resetButton;
            private final TextFieldWidget field;

            private MultiplierEntry(EntryData data, Tab tab) {
                this.data = data;
                this.tab = tab;
                this.minusButton = ButtonWidget.builder(Text.literal("-"), button -> {
                    int current = getMultiplier(tab, data.id);
                    setMultiplier(tab, data.id, current - 1);
                    syncField();
                }).dimensions(0, 0, 20, 18).build();

                this.plusButton = ButtonWidget.builder(Text.literal("+"), button -> {
                    int current = getMultiplier(tab, data.id);
                    setMultiplier(tab, data.id, current + 1);
                    syncField();
                }).dimensions(0, 0, 20, 18).build();

                this.resetButton = ButtonWidget.builder(Text.literal("↺"), button -> {
                    setMultiplier(tab, data.id, MIN_MULTIPLIER);
                    syncField();
                }).dimensions(0, 0, 18, 18).build();

                this.field = new TextFieldWidget(textRenderer, 0, 0, 34, 18, Text.literal(""));
                this.field.setTextPredicate(text -> text.matches("d*"));
                this.field.setChangedListener(text -> {
                    if (text == null || text.isEmpty()) {
                        return;
                    }
                    int parsed;
                    try {
                        parsed = Integer.parseInt(text);
                    } catch (NumberFormatException e) {
                        return;
                    }
                    setMultiplier(tab, data.id, parsed);
                    syncField();
                });
                syncField();
            }

            private void syncField() {
                int value = getMultiplier(tab, data.id);
                String text = Integer.toString(value);
                if (!text.equals(field.getText())) {
                    field.setText(text);
                }
            }

            @Override
            public Text getNarration() {
                return Text.literal(data.id.toString());
            }

            @Override
            public void render(DrawContext context, int index, int y, int x, int rowWidth, int rowHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
                int rowLeft = listWidget.getRowLeft();
                int rowRight = listWidget.getRowRight();
                int iconX = rowLeft + 4;
                int iconY = y + (rowHeight - ICON_SIZE) / 2;

                context.drawItem(data.icon, iconX, iconY);

                int textX = iconX + ICON_SIZE + 6;
                int nameY = y + 6;
                int idY = y + 18;
                context.drawTextWithShadow(textRenderer, data.displayName, textX, nameY, 0xFFFFFF);
                context.drawTextWithShadow(textRenderer, Text.literal(data.idString), textX, idY, 0xA0A0A0);

                int editorWidth = 96;
                int editorX = rowRight - editorWidth;
                int editorY = y + (rowHeight - 18) / 2;

                minusButton.setX(editorX);
                minusButton.setY(editorY);
                field.setX(editorX + 22);
                field.setY(editorY);
                plusButton.setX(editorX + 58);
                plusButton.setY(editorY);
                resetButton.setX(editorX + 80);
                resetButton.setY(editorY);

                minusButton.render(context, mouseX, mouseY, tickDelta);
                field.render(context, mouseX, mouseY, tickDelta);
                plusButton.render(context, mouseX, mouseY, tickDelta);
                resetButton.render(context, mouseX, mouseY, tickDelta);
            }

            @Override
            public boolean mouseClicked(Click click, boolean doubled) {
                if (minusButton.mouseClicked(click, doubled)) {
                    return true;
                }
                if (field.mouseClicked(click, doubled)) {
                    return true;
                }
                if (plusButton.mouseClicked(click, doubled)) {
                    return true;
                }
                return resetButton.mouseClicked(click, doubled);
            }

            @Override
            public boolean mouseReleased(Click click) {
                minusButton.mouseReleased(click);
                field.mouseReleased(click);
                plusButton.mouseReleased(click);
                resetButton.mouseReleased(click);
                return false;
            }

            @Override
            public boolean keyPressed(KeyInput input) {
                return field.keyPressed(input);
            }

            @Override
            public boolean charTyped(CharInput input) {
                return field.charTyped(input);
            }
        }
    }

    private static final class EntryData {
        private final Identifier id;
        private final Text displayName;
        private final ItemStack icon;
        private final String idString;

        private EntryData(Identifier id, Text displayName, ItemStack icon) {
            this.id = id;
            this.displayName = displayName;
            this.icon = icon;
            this.idString = id.toString().toLowerCase(Locale.ROOT);
        }
    }
}