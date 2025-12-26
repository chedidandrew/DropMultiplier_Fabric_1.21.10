package com.example.dropmultiplier.client;

import com.example.dropmultiplier.DropMultiplierConfig;
import com.example.dropmultiplier.DropMultiplierMod;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Environment(EnvType.CLIENT)
public class DropMultiplierConfigScreen {
    public static Screen create(Screen parent) {
        return new MainScreen(parent);
    }

    private static final class MainScreen extends Screen {
        private final Screen parent;
        private TextFieldWidget defaultMultiplierField;

        MainScreen(Screen parent) {
            super(Text.literal("DropMultiplier"));
            this.parent = parent;
        }

        @Override
        protected void init() {
            int contentWidth = Math.min(420, this.width - 40);
            int x = (this.width - contentWidth) / 2;
            int y = 56;

            DropMultiplierConfig cfg = DropMultiplierMod.CONFIG;

            this.defaultMultiplierField = new TextFieldWidget(this.textRenderer, x + 160, y, 80, 20, Text.literal(""));
            this.defaultMultiplierField.setText(Integer.toString(cfg.defaultMultiplier));
            this.defaultMultiplierField.setChangedListener(s -> {
                if (!s.isEmpty() && !s.matches("\\d{0,9}")) {
                    this.defaultMultiplierField.setText(s.replaceAll("[^0-9]", ""));
                }
            });
            this.addDrawableChild(this.defaultMultiplierField);

            this.addDrawableChild(ButtonWidget.builder(Text.literal("Blocks"), b -> {
                this.client.setScreen(new RegistryOverrideScreen(this, RegistryOverrideScreen.Mode.BLOCKS));
            }).dimensions(x, y + 40, 120, 20).build());

            this.addDrawableChild(ButtonWidget.builder(Text.literal("Entities"), b -> {
                this.client.setScreen(new RegistryOverrideScreen(this, RegistryOverrideScreen.Mode.ENTITIES));
            }).dimensions(x + 130, y + 40, 120, 20).build());

            this.addDrawableChild(ButtonWidget.builder(Text.literal("Done"), b -> {
                applyAndSaveDefaultMultiplier();
                this.client.setScreen(this.parent);
            }).dimensions(x, this.height - 28, 120, 20).build());

            this.addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"), b -> {
                this.client.setScreen(this.parent);
            }).dimensions(x + 130, this.height - 28, 120, 20).build());
        }

        private void applyAndSaveDefaultMultiplier() {
            DropMultiplierConfig cfg = DropMultiplierMod.CONFIG;
            int parsed = parseIntSafe(this.defaultMultiplierField.getText(), cfg.defaultMultiplier);
            if (parsed < 0) parsed = 0;
            if (parsed > 1_000_000) parsed = 1_000_000;
            cfg.defaultMultiplier = parsed;
            cfg.save();
        }

        @Override
        public void render(DrawContext context, int mouseX, int mouseY, float delta) {
            
            int contentWidth = Math.min(420, this.width - 40);
            int x = (this.width - contentWidth) / 2;
            int y = 56;

            context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 20, 0xFFFFFF);

            context.drawTextWithShadow(this.textRenderer, Text.literal("Default multiplier"), x, y + 6, 0xFFFFFF);

            DropMultiplierConfig cfg = DropMultiplierMod.CONFIG;
            context.drawTextWithShadow(
                    this.textRenderer,
                    Text.literal("Block overrides: " + cfg.blockMultipliers.size() + "    Entity overrides: " + cfg.entityMultipliers.size()),
                    x,
                    y + 86,
                    0xA0A0A0
            );

            super.render(context, mouseX, mouseY, delta);
        }

        @Override
        public boolean keyPressed(KeyInput input) {
            if (this.defaultMultiplierField != null && this.defaultMultiplierField.isFocused()) {
                return this.defaultMultiplierField.keyPressed(input) || super.keyPressed(input);
            }
            return super.keyPressed(input);
        }

        @Override
        public boolean charTyped(CharInput input) {
            if (this.defaultMultiplierField != null && this.defaultMultiplierField.isFocused()) {
                return this.defaultMultiplierField.charTyped(input);
            }
            return super.charTyped(input);
        }
    }

    private static final class RegistryOverrideScreen extends Screen {
        enum Mode { BLOCKS, ENTITIES }

        private final Screen parent;
        private final Mode mode;

        private TextFieldWidget searchField;
        private TextFieldWidget multiplierField;
        private ButtonWidget setButton;
        private ButtonWidget removeButton;

        private RegistryListWidget listWidget;

        RegistryOverrideScreen(Screen parent, Mode mode) {
            super(Text.literal(mode == Mode.BLOCKS ? "DropMultiplier - Blocks" : "DropMultiplier - Entities"));
            this.parent = parent;
            this.mode = mode;
        }

        @Override
        protected void init() {
            int contentWidth = Math.min(520, this.width - 40);
            int x = (this.width - contentWidth) / 2;

            int topY = 26;
            int searchY = topY + 18;

            this.searchField = new TextFieldWidget(this.textRenderer, x, searchY, contentWidth, 20, Text.literal(""));
            this.searchField.setPlaceholder(Text.literal("Search by id..."));
            this.searchField.setChangedListener(s -> refreshList());
            this.addDrawableChild(this.searchField);

            int listY = searchY + 28;
            int listHeight = this.height - listY - 74;

            this.listWidget = new RegistryListWidget(this.client, contentWidth, listHeight, listY, 20, this);
            this.addDrawableChild(this.listWidget);

            int panelY = listY + listHeight + 8;

            this.multiplierField = new TextFieldWidget(this.textRenderer, x + 160, panelY, 80, 20, Text.literal(""));
            this.multiplierField.setChangedListener(s -> {
                if (!s.isEmpty() && !s.matches("\\d{0,9}")) {
                    this.multiplierField.setText(s.replaceAll("[^0-9]", ""));
                }
            });
            this.addDrawableChild(this.multiplierField);

            this.setButton = this.addDrawableChild(ButtonWidget.builder(Text.literal("Set override"), b -> {
                Identifier sel = getSelectedId();
                if (sel == null) return;

                DropMultiplierConfig cfg = DropMultiplierMod.CONFIG;
                int m = parseIntSafe(this.multiplierField.getText(), cfg.defaultMultiplier);
                if (m < 0) m = 0;
                if (m > 1_000_000) m = 1_000_000;

                if (this.mode == Mode.BLOCKS) {
                    cfg.blockMultipliers.put(sel.toString(), m);
                } else {
                    cfg.entityMultipliers.put(sel.toString(), m);
                }
                cfg.save();
                updateButtonsAndFields();
            }).dimensions(x, panelY + 28, 120, 20).build());

            this.removeButton = this.addDrawableChild(ButtonWidget.builder(Text.literal("Remove override"), b -> {
                Identifier sel = getSelectedId();
                if (sel == null) return;

                DropMultiplierConfig cfg = DropMultiplierMod.CONFIG;
                if (this.mode == Mode.BLOCKS) {
                    cfg.blockMultipliers.remove(sel.toString());
                } else {
                    cfg.entityMultipliers.remove(sel.toString());
                }
                cfg.save();
                updateButtonsAndFields();
            }).dimensions(x + 130, panelY + 28, 120, 20).build());

            this.addDrawableChild(ButtonWidget.builder(Text.literal("Back"), b -> {
                this.client.setScreen(this.parent);
            }).dimensions(x, this.height - 28, 120, 20).build());

            refreshList();
            updateButtonsAndFields();
        }

        private void refreshList() {
            List<Identifier> ids = new ArrayList<>();
            if (this.mode == Mode.BLOCKS) {
                ids.addAll(Registries.BLOCK.getIds());
            } else {
                ids.addAll(Registries.ENTITY_TYPE.getIds());
            }
            ids.sort(Comparator.comparing(Identifier::toString));

            String q = this.searchField == null ? "" : this.searchField.getText().trim().toLowerCase(Locale.ROOT);

            List<Identifier> filtered = new ArrayList<>();
            if (q.isEmpty()) {
                filtered = ids;
            } else {
                for (Identifier id : ids) {
                    if (id.toString().toLowerCase(Locale.ROOT).contains(q)) {
                        filtered.add(id);
                    }
                }
            }

            this.listWidget.setIds(filtered);
            updateButtonsAndFields();
        }

        private Identifier getSelectedId() {
            RegistryListWidget.Entry e = this.listWidget.getSelectedOrNull();
            return e == null ? null : e.id;
        }

        private void updateButtonsAndFields() {
            Identifier sel = getSelectedId();
            boolean hasSelection = sel != null;

            DropMultiplierConfig cfg = DropMultiplierMod.CONFIG;

            Integer overrideValue = null;
            if (hasSelection) {
                if (this.mode == Mode.BLOCKS) {
                    overrideValue = cfg.blockMultipliers.get(sel.toString());
                } else {
                    overrideValue = cfg.entityMultipliers.get(sel.toString());
                }
            }

            if (this.multiplierField != null) {
                if (overrideValue != null) {
                    this.multiplierField.setText(Integer.toString(overrideValue));
                } else if (hasSelection) {
                    this.multiplierField.setText(Integer.toString(cfg.defaultMultiplier));
                } else {
                    this.multiplierField.setText("");
                }
            }

            if (this.setButton != null) this.setButton.active = hasSelection;
            if (this.removeButton != null) this.removeButton.active = hasSelection && overrideValue != null;
        }

        void onSelectionChanged() {
            updateButtonsAndFields();
        }

        @Override
        public void render(DrawContext context, int mouseX, int mouseY, float delta) {

            int contentWidth = Math.min(520, this.width - 40);
            int x = (this.width - contentWidth) / 2;

            context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 8, 0xFFFFFF);

            Identifier sel = getSelectedId();
            DropMultiplierConfig cfg = DropMultiplierMod.CONFIG;

            String rightText = "Selected: (none)";
            if (sel != null) {
                Integer ov = (this.mode == Mode.BLOCKS) ? cfg.blockMultipliers.get(sel.toString()) : cfg.entityMultipliers.get(sel.toString());
                if (ov == null) {
                    rightText = "Selected: " + sel + "    Effective: " + cfg.defaultMultiplier + " (default)";
                } else {
                    rightText = "Selected: " + sel + "    Effective: " + ov + " (override)";
                }
            }
            context.drawTextWithShadow(this.textRenderer, Text.literal(rightText), x, this.height - 70, 0xA0A0A0);

            context.drawTextWithShadow(this.textRenderer, Text.literal("Multiplier"), x, this.height - 46, 0xFFFFFF);

            super.render(context, mouseX, mouseY, delta);
        }

        @Override
        public boolean mouseClicked(Click click, boolean doubled) {
            boolean handled = super.mouseClicked(click, doubled);
            updateButtonsAndFields();
            return handled;
        }

        @Override
        public boolean keyPressed(KeyInput input) {
            if (this.searchField != null && this.searchField.isFocused()) {
                return this.searchField.keyPressed(input) || super.keyPressed(input);
            }
            if (this.multiplierField != null && this.multiplierField.isFocused()) {
                return this.multiplierField.keyPressed(input) || super.keyPressed(input);
            }
            return super.keyPressed(input);
        }

        @Override
        public boolean charTyped(CharInput input) {
            if (this.searchField != null && this.searchField.isFocused()) {
                return this.searchField.charTyped(input);
            }
            if (this.multiplierField != null && this.multiplierField.isFocused()) {
                return this.multiplierField.charTyped(input);
            }
            return super.charTyped(input);
        }
    }

    private static final class RegistryListWidget extends AlwaysSelectedEntryListWidget<RegistryListWidget.Entry> {
        private final RegistryOverrideScreen owner;

        RegistryListWidget(MinecraftClient client, int width, int height, int y, int itemHeight, RegistryOverrideScreen owner) {
            super(client, width, height, y, itemHeight);
            this.owner = owner;
        }

        void setIds(List<Identifier> ids) {
            this.clearEntries();
            for (Identifier id : ids) {
                this.addEntry(new Entry(id, this, this.owner));
            }

            if (this.getEntryCount() > 0 && this.getSelectedOrNull() == null) {
                this.setSelected(this.children().get(0));
                this.owner.onSelectionChanged();
            }

            this.setScrollY(0);
        }

        @Override
        protected int getScrollbarX() {
            return this.getX() + this.getWidth() - 6;
        }

        @Override
        public int getRowWidth() {
            return this.getWidth() - 12;
        }

        static final class Entry extends AlwaysSelectedEntryListWidget.Entry<Entry> {
            final Identifier id;
            private final RegistryListWidget list;
            private final RegistryOverrideScreen owner;

            Entry(Identifier id, RegistryListWidget list, RegistryOverrideScreen owner) {
                this.id = id;
                this.list = list;
                this.owner = owner;
            }

            @Override
            public Text getNarration() {
                return Text.literal(this.id.toString());
            }

            @Override
            public void render(DrawContext context, int mouseX, int mouseY, boolean hovered, float tickDelta) {
                String s = this.id.toString();
                context.drawTextWithShadow(this.list.client.textRenderer, s, 2, 6, 0xFFFFFF);
            }

            @Override
            public boolean mouseClicked(Click click, boolean doubled) {
                this.list.setSelected(this);
                this.owner.onSelectionChanged();
                return true;
            }
        }
    }

    private static int parseIntSafe(String s, int fallback) {
        try {
            if (s == null) return fallback;
            String t = s.trim();
            if (t.isEmpty()) return fallback;
            return Integer.parseInt(t);
        } catch (Exception ignored) {
            return fallback;
        }
    }
}
