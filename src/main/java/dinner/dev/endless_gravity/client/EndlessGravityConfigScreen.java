package dinner.dev.endless_gravity.client;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.gui.entries.TextListEntry;
import me.shedaniel.clothconfig2.impl.builders.SubCategoryBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import dinner.dev.endless_gravity.AtmosphereLayers;
import dinner.dev.endless_gravity.AtmospherePreset;
import dinner.dev.endless_gravity.Config;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class EndlessGravityConfigScreen {

    private static final int TEXT_WHITE = 0xFFFFFF;
    private static final int TEXT_GRAY = 0xAAAAAA;
    private static final int TEXT_BLUE = 0x66CCFF;
    private static final int TEXT_GREEN = 0x55FF55;
    private static final int TEXT_RED = 0xFF5555;
    private static final int TEXT_AMBER = 0xFFAA00;
    private static final int DIVIDER_COLOR = 0x8866CCFF;
    private static final double CURVE_MIN_ALTITUDE = -2048;
    private static final double CURVE_MAX_ALTITUDE = 30000;

    private EndlessGravityConfigScreen() {}

    public static Screen create(Screen parent) {
        return new Factory(parent).build();
    }

    private enum FallDamageMode {
        NORMAL, DISABLED;

        static FallDamageMode fromInt(int value) {
            return switch (value) {
                case 0 -> NORMAL;
                case 1 -> DISABLED;
                default -> DISABLED;
            };
        }
    }

    private static final class DimensionDraft {
        private String id;
        private Config.CustomDimConfig.Mode mode;
        private double player;
        private double mob;
        private double item;
        private double arrow;
        private double thrown;
        private double block;
        private double atmosphereMax;
        private double atmosphereBase;
        private double noFall;
        private double muffle;
        private double muffleHF;
        private final List<AtmosphereLayers.Layer> layers = new ArrayList<>();
        private boolean curveExpanded;
        private boolean presetsExpanded;
        private String presetName = "";

        private DimensionDraft(Config.CustomDimConfig config) {
            id = config.getDimensionId();
            mode = config.getMode();
            player = config.getPlayerGravityOffset();
            mob = config.getMobGravityOffset();
            item = config.getItemGravityOffset();
            arrow = config.getArrowGravityOffset();
            thrown = config.getThrownGravityOffset();
            block = config.getBlockGravityOffset();
            atmosphereMax = config.getAtmosphereGravityMax();
            atmosphereBase = config.getAtmosphereBaseAltitude();
            noFall = config.getNoFallDamageAltitude() == null ? -1.0 : config.getNoFallDamageAltitude();
            muffle = config.getAtmosphereMuffleGain();
            muffleHF = config.getAtmosphereMuffleGainHF();
            List<AtmosphereLayers.Layer> parsed = AtmosphereLayers.parse(config.getAtmosphereLayers());
            layers.addAll(parsed);
        }

        private Config.CustomDimConfig toConfig() {
            return new Config.CustomDimConfig(id, mode, player, mob, item, arrow, thrown, block,
                    atmosphereMax, atmosphereBase, noFall < 0 ? null : noFall,
                    muffle, muffleHF, AtmosphereLayers.serialize(layers));
        }
    }

    private static final class EditorDraft {
        private final List<DimensionDraft> profiles = new ArrayList<>();
        private final List<DimensionDraft> removed = new ArrayList<>();
        private DimensionDraft pending;
        private String status = "";
        private String expandedProfileId = "";
        private boolean pendingExpanded;
        private final List<AtmospherePreset> atmospherePresets = new ArrayList<>();
        private String importJson = "";
        private String presetStatus = "";

        private EditorDraft() {
            Config.getCustomDimensions().forEach(config -> profiles.add(new DimensionDraft(config)));
            atmospherePresets.addAll(Config.getAtmospherePresets());
            pending = new DimensionDraft(Config.CustomDimConfig.vanilla());
            pending.id = "";
            pending.mode = Config.CustomDimConfig.Mode.FIXED_GRAVITY;
        }
    }

    private static final class ActionEntry extends TextListEntry {
        private final Button button;
        private final int width;

        private ActionEntry(Component label, Runnable action, int color, int width) {
            this(label, action, color, width, null);
        }

        private ActionEntry(Component label, Runnable action, int color, int width, Component tooltip) {
            super(Component.empty(), Component.empty());
            this.width = width;
            button = Button.builder(label.copy().withStyle(style ->
                            style.withColor(net.minecraft.network.chat.TextColor.fromRgb(color))),
                    ignored -> action.run()).bounds(0, 0, width, 20).build();
            if (tooltip != null) button.setTooltip(Tooltip.create(tooltip));
        }

        @Override public int getItemHeight() { return 24; }

        @Override
        public void render(GuiGraphics graphics, int index, int y, int x, int entryWidth, int entryHeight,
                           int mouseX, int mouseY, boolean hovered, float delta) {
            button.setX(x + Math.max(0, entryWidth - width - 8));
            button.setY(y + 2);
            button.render(graphics, mouseX, mouseY, delta);
        }

        @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
            return this.button.mouseClicked(mouseX, mouseY, button);
        }
    }

    private static final class ModeEntry extends TextListEntry {
        private final Button atmosphere;
        private final Button fixed;

        private ModeEntry(Config.CustomDimConfig.Mode mode, Runnable chooseAtmosphere, Runnable chooseFixed) {
            super(Component.empty(), Component.empty());
            atmosphere = Button.builder(Component.translatable("endless_gravity.config.dimensionMode.atmosphere"),
                    ignored -> chooseAtmosphere.run()).bounds(0, 0, 150, 20).build();
            atmosphere.setTooltip(Tooltip.create(Component.translatable(
                    "endless_gravity.config.dimensionMode.atmosphere.tooltip")));
            fixed = Button.builder(Component.translatable("endless_gravity.config.dimensionMode.fixed_gravity"),
                    ignored -> chooseFixed.run()).bounds(0, 0, 150, 20).build();
            fixed.setTooltip(Tooltip.create(Component.translatable(
                    "endless_gravity.config.dimensionMode.fixed_gravity.tooltip")));
            atmosphere.active = mode != Config.CustomDimConfig.Mode.ATMOSPHERE;
            fixed.active = mode != Config.CustomDimConfig.Mode.FIXED_GRAVITY;
        }

        @Override public int getItemHeight() { return 24; }

        @Override
        public void render(GuiGraphics graphics, int index, int y, int x, int entryWidth, int entryHeight,
                           int mouseX, int mouseY, boolean hovered, float delta) {
            int startX = x + (entryWidth - 308) / 2;
            atmosphere.setX(startX);
            atmosphere.setY(y + 2);
            fixed.setX(startX + 158);
            fixed.setY(y + 2);
            atmosphere.render(graphics, mouseX, mouseY, delta);
            fixed.render(graphics, mouseX, mouseY, delta);
        }

        @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
            return atmosphere.mouseClicked(mouseX, mouseY, button)
                    || fixed.mouseClicked(mouseX, mouseY, button);
        }
    }

    private static final class PresetEntry extends TextListEntry {
        private final List<Button> buttons = new ArrayList<>();

        private PresetEntry(List<String> presetKeys, java.util.function.Consumer<String> apply) {
            super(Component.empty(), Component.empty());
            presetKeys.forEach(preset -> {
                Button button = Button.builder(Component.translatable("endless_gravity.config.preset." + preset),
                        ignored -> apply.accept(preset)).bounds(0, 0, 112, 20).build();
                button.setTooltip(Tooltip.create(Component.translatable(
                        "endless_gravity.config.preset." + preset + ".tooltip")));
                buttons.add(button);
            });
        }

        @Override public int getItemHeight() { return Math.max(1, (buttons.size() + 2) / 3) * 24; }

        @Override
        public void render(GuiGraphics graphics, int index, int y, int x, int entryWidth, int entryHeight,
                           int mouseX, int mouseY, boolean hovered, float delta) {
            int gap = 5;
            int columns = Math.min(3, buttons.size());
            int total = columns * 112 + Math.max(0, columns - 1) * gap;
            int start = x + (entryWidth - total) / 2;
            for (int i = 0; i < buttons.size(); i++) {
                Button button = buttons.get(i);
                button.setX(start + (i % 3) * (112 + gap));
                button.setY(y + 2 + (i / 3) * 24);
                button.render(graphics, mouseX, mouseY, delta);
            }
        }

        @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
            for (Button preset : buttons) if (preset.mouseClicked(mouseX, mouseY, button)) return true;
            return false;
        }
    }

    private static final class CustomPresetEntry extends TextListEntry {
        private record PresetButtons(Button apply, Button delete) {}

        private final List<PresetButtons> presets = new ArrayList<>();

        private CustomPresetEntry(List<AtmospherePreset> presets,
                                  java.util.function.Consumer<AtmospherePreset> apply,
                                  java.util.function.Consumer<AtmospherePreset> delete) {
            super(Component.empty(), Component.empty());
            for (AtmospherePreset preset : presets) {
                Button applyButton = Button.builder(Component.literal(preset.name()),
                        ignored -> apply.accept(preset)).bounds(0, 0, 210, 20).build();
                applyButton.setTooltip(Tooltip.create(Component.translatable(
                        "endless_gravity.config.myAtmospherePresets.tooltip")));
                Button deleteButton = Button.builder(
                        Component.translatable("endless_gravity.config.deletePresetButton"),
                        ignored -> delete.accept(preset)).bounds(0, 0, 82, 20).build();
                deleteButton.setTooltip(Tooltip.create(Component.translatable(
                        "endless_gravity.config.deletePresetButton.tooltip")));
                this.presets.add(new PresetButtons(applyButton, deleteButton));
            }
        }

        @Override public int getItemHeight() { return Math.max(1, presets.size()) * 24; }

        @Override
        public void render(GuiGraphics graphics, int index, int y, int x, int entryWidth, int entryHeight,
                           int mouseX, int mouseY, boolean hovered, float delta) {
            int startX = x + (entryWidth - 298) / 2;
            for (int i = 0; i < presets.size(); i++) {
                PresetButtons buttons = presets.get(i);
                buttons.apply().setX(startX);
                buttons.apply().setY(y + 2 + i * 24);
                buttons.delete().setX(startX + 216);
                buttons.delete().setY(y + 2 + i * 24);
                buttons.apply().render(graphics, mouseX, mouseY, delta);
                buttons.delete().render(graphics, mouseX, mouseY, delta);
            }
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            for (PresetButtons preset : presets) {
                if (preset.apply().mouseClicked(mouseX, mouseY, button)
                        || preset.delete().mouseClicked(mouseX, mouseY, button)) return true;
            }
            return false;
        }
    }

    private static final class DividerEntry extends TextListEntry {
        private DividerEntry() { super(Component.empty(), Component.empty()); }
        @Override public int getItemHeight() { return 8; }
        @Override
        public void render(GuiGraphics graphics, int index, int y, int x, int entryWidth, int entryHeight,
                           int mouseX, int mouseY, boolean hovered, float delta) {
            int lineY = y + entryHeight / 2;
            graphics.fill(x + 6, lineY, x + entryWidth - 6, lineY + 1, DIVIDER_COLOR);
        }
    }

    private static final class CurveEntry extends TextListEntry {
        private static final int CURVE_COLOR = 0xFF9AB8A5;
        private static final int POINT_COLOR = 0xFFE4C08A;
        private final List<AtmosphereLayers.Layer> layers;
        private int chartX, chartY, chartWidth, chartHeight, selected = -1;
        private double dragStartX, dragStartAltitude, dragAltitudeRange;

        private CurveEntry(List<AtmosphereLayers.Layer> layers) {
            super(Component.empty(), Component.empty());
            this.layers = layers;
        }

        @Override public int getItemHeight() { return 112; }

        @Override
        public void render(GuiGraphics graphics, int index, int y, int x, int entryWidth, int entryHeight,
                           int mouseX, int mouseY, boolean hovered, float delta) {
            chartX = x + 36;
            chartY = y + 9;
            chartWidth = Math.max(100, entryWidth - 48);
            chartHeight = 76;
            double pressureMax = pressureMax();
            graphics.fill(chartX, chartY, chartX + chartWidth, chartY + chartHeight, 0xAA171A1D);
            graphics.fill(chartX, chartY, chartX + chartWidth, chartY + 1, 0xFF394148);
            graphics.fill(chartX, chartY + chartHeight - 1, chartX + chartWidth,
                    chartY + chartHeight, 0xFF394148);
            graphics.fill(chartX, chartY, chartX + 1, chartY + chartHeight, 0xFF394148);
            graphics.fill(chartX + chartWidth - 1, chartY, chartX + chartWidth,
                    chartY + chartHeight, 0xFF394148);
            for (int i = 0; i <= 4; i++) {
                int gy = chartY + i * chartHeight / 4;
                int gx = chartX + i * chartWidth / 4;
                graphics.fill(chartX + 1, gy, chartX + chartWidth - 1, gy + 1, 0x334F595E);
                graphics.fill(gx, chartY + 1, gx + 1, chartY + chartHeight - 1, 0x334F595E);
            }
            for (int i = 0; i < layers.size() - 1; i++) {
                int x1 = toX(layers.get(i).altitude());
                int x2 = toX(layers.get(i + 1).altitude());
                int y1 = toY(layers.get(i).pressure());
                int y2 = toY(layers.get(i + 1).pressure());
                int span = Math.max(1, x2 - x1);
                for (int px = x1; px <= x2; px++) {
                    int py = (int) Math.round(y1 + (double) (px - x1) / span * (y2 - y1));
                    graphics.fill(px, py, px + 2, py + 2, CURVE_COLOR);
                }
            }
            for (int i = 0; i < layers.size(); i++) {
                int px = toX(layers.get(i).altitude()), py = toY(layers.get(i).pressure());
                int r = selected == i ? 4 : 3;
                graphics.fill(px - r - 1, py - r - 1, px + r + 2, py + r + 2, 0xFF171A1D);
                graphics.fill(px - r, py - r, px + r + 1, py + r + 1, POINT_COLOR);
            }
            if (insideChart(mouseX, mouseY)) {
                graphics.fill(mouseX, chartY, mouseX + 1, chartY + chartHeight, 0x55FFFFFF);
                graphics.fill(chartX, mouseY, chartX + chartWidth, mouseY + 1, 0x55FFFFFF);
                double altitude = xToAltitude(mouseX);
                double curvePressure = AtmosphereLayers.getPressure(layers, altitude);
                double cursorPressure = yToPressure(mouseY);
                graphics.renderTooltip(Minecraft.getInstance().font,
                        Component.literal(String.format(Locale.ROOT,
                                "Y %.1f  |  P %.3f  |  Cursor %.3f", altitude, curvePressure, cursorPressure)),
                        mouseX, mouseY);
            }
            graphics.drawString(Minecraft.getInstance().font, String.format(Locale.ROOT, "%.2f", pressureMax),
                    x + 3, chartY - 2, TEXT_GRAY);
            graphics.drawString(Minecraft.getInstance().font, "0", x + 18, chartY + chartHeight - 4, TEXT_GRAY);
            String minLabel = String.format(Locale.ROOT, "%.0f", altitudeMin());
            String maxLabel = String.format(Locale.ROOT, "%.0f", altitudeMax());
            graphics.drawString(Minecraft.getInstance().font, minLabel,
                    chartX, chartY + chartHeight + 4, TEXT_GRAY);
            graphics.drawString(Minecraft.getInstance().font, maxLabel,
                    chartX + chartWidth - Minecraft.getInstance().font.width(maxLabel),
                    chartY + chartHeight + 4, TEXT_GRAY);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button != 0 || mouseX < chartX || mouseX > chartX + chartWidth
                    || mouseY < chartY || mouseY > chartY + chartHeight) return false;
            double nearest = 400;
            for (int i = 0; i < layers.size(); i++) {
                double dx = toX(layers.get(i).altitude()) - mouseX;
                double dy = toY(layers.get(i).pressure()) - mouseY;
                double distance = dx * dx + dy * dy;
                if (distance < nearest) { nearest = distance; selected = i; }
            }
            if (selected < 0) return false;
            dragStartX = mouseX;
            dragStartAltitude = layers.get(selected).altitude();
            dragAltitudeRange = altitudeMax() - altitudeMin();
            move(mouseX, mouseY);
            return true;
        }

        @Override
        public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
            if (button != 0 || selected < 0) return false;
            move(mouseX, mouseY);
            return true;
        }

        @Override public boolean mouseReleased(double mouseX, double mouseY, int button) {
            if (button != 0 || selected < 0) return false;
            selected = -1;
            return true;
        }

        private int toX(double altitude) {
            double t = (altitude - altitudeMin()) / (altitudeMax() - altitudeMin());
            return chartX + (int) Math.round(AtmosphereLayers.clamp(t, 0, 1) * chartWidth);
        }

        private int toY(double pressure) {
            double t = AtmosphereLayers.clamp(pressure / pressureMax(), 0, 1);
            return chartY + chartHeight - (int) Math.round(t * chartHeight);
        }

        private double pressureMax() {
            double max = layers.stream().mapToDouble(AtmosphereLayers.Layer::pressure).max().orElse(1.25);
            return Math.max(1.25, Math.ceil(max));
        }

        private double altitudeMin() {
            return layers.get(0).altitude();
        }

        private double altitudeMax() {
            return layers.get(layers.size() - 1).altitude();
        }

        private double xToAltitude(double mouseX) {
            return altitudeMin() + AtmosphereLayers.clamp(
                    (mouseX - chartX) / chartWidth, 0, 1) * (altitudeMax() - altitudeMin());
        }

        private double yToPressure(double mouseY) {
            return pressureMax() * (1 - AtmosphereLayers.clamp(
                    (mouseY - chartY) / chartHeight, 0, 1));
        }

        private boolean insideChart(double mouseX, double mouseY) {
            return mouseX >= chartX && mouseX <= chartX + chartWidth
                    && mouseY >= chartY && mouseY <= chartY + chartHeight;
        }

        private void move(double mouseX, double mouseY) {
            double altitude = selected == 0 || selected == layers.size() - 1
                    ? dragStartAltitude + (mouseX - dragStartX) * dragAltitudeRange / chartWidth
                    : xToAltitude(mouseX);
            double min = selected == 0 ? CURVE_MIN_ALTITUDE : layers.get(selected - 1).altitude() + 1;
            double max = selected == layers.size() - 1 ? CURVE_MAX_ALTITUDE
                    : layers.get(selected + 1).altitude() - 1;
            altitude = AtmosphereLayers.clamp(altitude, min, max);
            double pressure = yToPressure(mouseY);
            layers.set(selected, new AtmosphereLayers.Layer(altitude, pressure));
        }
    }

    private static final class Factory {
        private final Screen parent;
        private final EditorDraft draft;
        private final List<ConfigCategory> categories = new ArrayList<>();

        Factory(Screen parent) {
            this(parent, new EditorDraft());
        }

        private Factory(Screen parent, EditorDraft draft) {
            this.parent = parent;
            this.draft = draft;
        }

        Screen build() {
            ConfigBuilder builder = ConfigBuilder.create()
                    .setParentScreen(parent)
                    .setTitle(Component.translatable("endless_gravity.config.title"))
                    .setDoesConfirmSave(false)
                    .setSavingRunnable(this::finishSave);

            ConfigEntryBuilder entry = builder.entryBuilder();

            buildDimensions(builder, entry);
            buildGeneral(builder, entry);
            return builder.build();
        }

        private void buildDimensions(ConfigBuilder builder, ConfigEntryBuilder entry) {
            ConfigCategory category = builder.getOrCreateCategory(text("endless_gravity.config.section.dimensions"));
            categories.add(category);
            SubCategoryBuilder add = entry.startSubCategory(text("endless_gravity.config.dimensionCreate"))
                    .setExpanded(draft.pendingExpanded);
            add.add(entry.startStrField(text("endless_gravity.config.dimensionAdd"), draft.pending.id)
                    .setTooltip(text("endless_gravity.config.dimensionAdd.tooltip"))
                    .setErrorSupplier(raw -> dimensionIdError(raw, true))
                    .setSaveConsumer(value -> draft.pending.id = value.trim())
                    .build());
            add.add(new ModeEntry(draft.pending.mode,
                    () -> setMode(draft.pending, Config.CustomDimConfig.Mode.ATMOSPHERE),
                    () -> setMode(draft.pending, Config.CustomDimConfig.Mode.FIXED_GRAVITY)));
            if (!draft.status.isEmpty()) {
                add.add(entry.startTextDescription(text("endless_gravity.config.dimensionStatus." + draft.status)).build());
            }
            addProfileSettings(add, entry, draft.pending);
            add.add(new ActionEntry(text("endless_gravity.config.dimensionCreateButton"),
                    this::addProfile, TEXT_GREEN, 220,
                    text("endless_gravity.config.dimensionCreateButton.tooltip")));
            category.addEntry(add.build());

            SubCategoryBuilder presetManager = entry.startSubCategory(text("endless_gravity.config.customPresets"))
                    .setExpanded(false)
                    .setTooltip(text("endless_gravity.config.customPresetsHelp"));
            presetManager.add(entry.startStrField(text("endless_gravity.config.importPresetsJson"), draft.importJson)
                    .setTooltip(text("endless_gravity.config.importPresetsJson.tooltip"))
                    .setSaveConsumer(value -> draft.importJson = value)
                    .build());
            presetManager.add(new ActionEntry(text("endless_gravity.config.importPresets"),
                    this::importAtmospherePresets, TEXT_BLUE, 220,
                    text("endless_gravity.config.importPresets.tooltip")));
            presetManager.add(new ActionEntry(text("endless_gravity.config.exportPresets"),
                    this::exportAtmospherePresets, TEXT_BLUE, 220,
                    text("endless_gravity.config.exportPresets.tooltip")));
            if (!draft.presetStatus.isEmpty()) {
                presetManager.add(entry.startTextDescription(
                        text("endless_gravity.config.presetStatus." + draft.presetStatus)).build());
            }
            if (!draft.atmospherePresets.isEmpty()) {
                presetManager.add(entry.startTextDescription(text("endless_gravity.config.savedPresets",
                        draft.atmospherePresets.size())).build());
            }
            category.addEntry(presetManager.build());

            category.addEntry(entry.startTextDescription(text("endless_gravity.config.dimensionExistingProfiles",
                    draft.profiles.size())).build());
            for (DimensionDraft profileDraft : draft.profiles.stream()
                    .sorted((a, b) -> a.id.compareToIgnoreCase(b.id)).toList()) {
                SubCategoryBuilder profile = entry.startSubCategory(Component.literal(profileDraft.id))
                        .setExpanded(profileDraft.id.equals(draft.expandedProfileId));
                profile.add(new ModeEntry(profileDraft.mode,
                        () -> setMode(profileDraft, Config.CustomDimConfig.Mode.ATMOSPHERE),
                        () -> setMode(profileDraft, Config.CustomDimConfig.Mode.FIXED_GRAVITY)));
                addProfileSettings(profile, entry, profileDraft);
                profile.add(new ActionEntry(text("endless_gravity.config.dimensionRemoveButton"),
                        () -> removeProfile(profileDraft), TEXT_RED, 220,
                        text("endless_gravity.config.dimensionRemoveButton.tooltip")));
                category.addEntry(profile.build());
            }
            if (!draft.removed.isEmpty()) {
                category.addEntry(entry.startTextDescription(text("endless_gravity.config.dimensionUndoHeader")).build());
                for (DimensionDraft removed : List.copyOf(draft.removed)) {
                    category.addEntry(new ActionEntry(text("endless_gravity.config.dimensionUndo", removed.id),
                            () -> restoreProfile(removed), TEXT_GREEN, 260,
                            text("endless_gravity.config.dimensionUndo.tooltip")));
                }
            }
            category.addEntry(entry.startTextDescription(text("endless_gravity.config.dimensionProfilesSaveHint")).build());
        }

        private void addProfileSettings(SubCategoryBuilder destination, ConfigEntryBuilder entry,
                                        DimensionDraft profile) {
            SubCategoryBuilder presets = entry.startSubCategory(
                    text("endless_gravity.config.dimensionPresets"))
                    .setExpanded(profile.presetsExpanded)
                    .setTooltip(text("endless_gravity.config.dimensionPresets.tooltip"));
            List<String> quickPresetKeys = profile.mode == Config.CustomDimConfig.Mode.FIXED_GRAVITY
                    ? List.of("vanilla", "light_gravity", "lunar")
                    : List.of("terrestrial", "gentle_atmosphere", "thin_atmosphere");
            presets.add(new PresetEntry(quickPresetKeys, preset -> applyPreset(profile, preset)));

            if (profile.mode == Config.CustomDimConfig.Mode.FIXED_GRAVITY) {
                destination.add(presets.build());
                SubCategoryBuilder gravity = entry.startSubCategory(text("endless_gravity.config.dimensionFixedGravity"))
                        .setExpanded(true)
                        .setTooltip(text("endless_gravity.config.dimensionFixedGravity.tooltip"));
                addProfileDouble(gravity, entry, "endless_gravity.config.playerGravityOffset",
                        profile.player, value -> profile.player = value);
                addProfileDouble(gravity, entry, "endless_gravity.config.mobGravityOffset",
                        profile.mob, value -> profile.mob = value);
                addProfileDouble(gravity, entry, "endless_gravity.config.itemGravityOffset",
                        profile.item, value -> profile.item = value);
                addProfileDouble(gravity, entry, "endless_gravity.config.arrowGravityOffset",
                        profile.arrow, value -> profile.arrow = value);
                addProfileDouble(gravity, entry, "endless_gravity.config.thrownGravityOffset",
                        profile.thrown, value -> profile.thrown = value);
                addProfileDouble(gravity, entry, "endless_gravity.config.blockGravityOffset",
                        profile.block, value -> profile.block = value);
                destination.add(gravity.build());
                return;
            }

            if (!draft.atmospherePresets.isEmpty()) {
                presets.add(entry.startTextDescription(text("endless_gravity.config.myAtmospherePresets")).build());
                presets.add(new CustomPresetEntry(draft.atmospherePresets,
                        preset -> applyAtmospherePreset(profile, preset),
                        preset -> deleteAtmospherePreset(profile, preset)));
            }
            presets.add(entry.startStrField(text("endless_gravity.config.savePresetName"), profile.presetName)
                    .setTooltip(text("endless_gravity.config.savePresetName.tooltip"))
                    .setSaveConsumer(value -> profile.presetName = value)
                    .build());
            presets.add(new ActionEntry(text("endless_gravity.config.saveCurrentAsPreset"),
                    () -> saveAtmospherePreset(profile), TEXT_GREEN, 260,
                    text("endless_gravity.config.saveCurrentAsPreset.tooltip")));
            destination.add(presets.build());

            SubCategoryBuilder atmosphere = entry.startSubCategory(text("endless_gravity.config.dimensionAtmosphere"))
                    .setExpanded(true)
                    .setTooltip(text("endless_gravity.config.dimensionAtmosphere.tooltip"));
            addProfileDouble(atmosphere, entry, "endless_gravity.config.atmosphereGravityMax",
                    profile.atmosphereMax, value -> profile.atmosphereMax = value, 0.0, 0.5);
            addProfileDouble(atmosphere, entry, "endless_gravity.config.atmosphereBaseAltitude",
                    profile.atmosphereBase, value -> profile.atmosphereBase = value, -2048, 30000);
            addProfileDouble(atmosphere, entry, "endless_gravity.config.atmosphereMuffleGain",
                    profile.muffle, value -> profile.muffle = value, 0, 1);
            addProfileDouble(atmosphere, entry, "endless_gravity.config.atmosphereMuffleGainHF",
                    profile.muffleHF, value -> profile.muffleHF = value, 0, 1);
            addProfileDouble(atmosphere, entry, "endless_gravity.config.noFallDamageAltitude",
                    profile.noFall, value -> profile.noFall = value, -1, 30000);
            SubCategoryBuilder curve = entry.startSubCategory(text("endless_gravity.config.atmosphereLayers"))
                    .setExpanded(profile.curveExpanded)
                    .setTooltip(text("endless_gravity.config.curveGraphHelp"));
            curve.add(new CurveEntry(profile.layers));
            for (int i = 0; i < profile.layers.size(); i++) {
                int index = i;
                AtmosphereLayers.Layer layer = profile.layers.get(i);
                SubCategoryBuilder row = entry.startSubCategory(
                        text("endless_gravity.config.curveLayer", i + 1))
                        .setExpanded(true)
                        .setTooltip(text("endless_gravity.config.curveLayer.tooltip"));
                row.add(entry.startDoubleField(text("endless_gravity.config.columnAltitude"), layer.altitude())
                        .setMin(-2048).setMax(30000)
                        .setTooltip(text("endless_gravity.config.columnAltitude.tooltip"))
                        .setSaveConsumer(value -> setLayerAltitude(profile, index, value))
                        .build());
                row.add(entry.startDoubleField(text("endless_gravity.config.columnPressure"), layer.pressure())
                        .setMin(0).setMax(10)
                        .setTooltip(text("endless_gravity.config.columnPressure.tooltip"))
                        .setSaveConsumer(value -> setLayerPressure(profile, index, value))
                        .build());
                if (profile.layers.size() > 2) {
                    row.add(new ActionEntry(text("endless_gravity.config.curveRemoveLayer"),
                            () -> removeLayer(profile, index), TEXT_RED, 220,
                            text("endless_gravity.config.curveRemoveLayer.tooltip")));
                }
                curve.add(row.build());
            }
            if (profile.layers.size() < Config.CustomDimConfig.MAX_ATMOSPHERE_LAYERS) {
                curve.add(new ActionEntry(text("endless_gravity.config.curveAddLayer"),
                        () -> addLayer(profile), TEXT_GREEN, 220,
                        text("endless_gravity.config.curveAddLayer.tooltip")));
            }
            atmosphere.add(curve.build());
            destination.add(atmosphere.build());
        }

        private static void setLayerAltitude(DimensionDraft profile, int index, double value) {
            double min = index == 0 ? CURVE_MIN_ALTITUDE
                    : Math.ceil(profile.layers.get(index - 1).altitude() + 1);
            double max = index == profile.layers.size() - 1 ? CURVE_MAX_ALTITUDE
                    : Math.floor(profile.layers.get(index + 1).altitude() - 1);
            AtmosphereLayers.Layer old = profile.layers.get(index);
            profile.layers.set(index, new AtmosphereLayers.Layer(
                    AtmosphereLayers.clamp(Math.rint(value), min, max), old.pressure()));
        }

        private static void setLayerPressure(DimensionDraft profile, int index, double value) {
            AtmosphereLayers.Layer old = profile.layers.get(index);
            profile.layers.set(index, new AtmosphereLayers.Layer(old.altitude(),
                    AtmosphereLayers.clamp(value, 0, 10)));
        }

        private void addLayer(DimensionDraft profile) {
            saveCurrentEntries();
            if (profile.layers.size() >= Config.CustomDimConfig.MAX_ATMOSPHERE_LAYERS) return;
            expandProfile(profile);
            profile.curveExpanded = true;
            double bestGap = 0;
            double altitude = 0;
            double previousAltitude = CURVE_MIN_ALTITUDE;
            for (int i = 0; i <= profile.layers.size(); i++) {
                double nextAltitude = i == profile.layers.size()
                        ? CURVE_MAX_ALTITUDE : profile.layers.get(i).altitude();
                double gap = nextAltitude - previousAltitude;
                if (gap > bestGap) {
                    bestGap = gap;
                    altitude = Math.floor((previousAltitude + nextAltitude) / 2);
                }
                previousAltitude = nextAltitude;
            }
            if (bestGap < 2) return;
            profile.layers.add(new AtmosphereLayers.Layer(altitude,
                    AtmosphereLayers.getPressure(profile.layers, altitude)));
            profile.layers.sort(java.util.Comparator.comparingDouble(AtmosphereLayers.Layer::altitude));
            refresh();
        }

        private void removeLayer(DimensionDraft profile, int index) {
            saveCurrentEntries();
            if (profile.layers.size() <= 2 || index < 0 || index >= profile.layers.size()) return;
            expandProfile(profile);
            profile.curveExpanded = true;
            profile.layers.remove(index);
            refresh();
        }

        private void addProfileDouble(SubCategoryBuilder group, ConfigEntryBuilder entry, String key,
                                      double current, java.util.function.DoubleConsumer save) {
            addProfileDouble(group, entry, key, current, save, -0.5, 0.5);
        }

        private void addProfileDouble(SubCategoryBuilder group, ConfigEntryBuilder entry, String key,
                                      double current, java.util.function.DoubleConsumer save,
                                      double min, double max) {
            group.add(entry.startDoubleField(text(key), current)
                    .setMin(min).setMax(max)
                    .setTooltip(text(key + ".tooltip"))
                    .setSaveConsumer(save::accept).build());
        }

        private void applyPreset(DimensionDraft profile, String preset) {
            saveCurrentEntries();
            expandProfile(profile);
            profile.presetsExpanded = true;
            if (profile.mode == Config.CustomDimConfig.Mode.FIXED_GRAVITY) {
                switch (preset) {
                    case "vanilla" -> setFixed(profile, 0, 0, 0, 0, 0, 0);
                    case "light_gravity" -> setFixed(profile, 0.025, 0.02, 0.01, 0.015, 0.01, 0.02);
                    case "lunar" -> setFixed(profile, 0.055, 0.025, 0.025, 0.03, 0.018, 0.035);
                    default -> throw new IllegalArgumentException("Unknown gravity preset: " + preset);
                }
            } else {
                switch (preset) {
                    case "terrestrial" -> {
                        profile.atmosphereMax = 0.075;
                        profile.atmosphereBase = 319;
                        profile.noFall = 4500;
                        profile.muffle = 0.01;
                        profile.muffleHF = 0.005;
                        profile.layers.clear();
                        profile.layers.addAll(AtmosphereLayers.defaults());
                    }
                    case "gentle_atmosphere" -> {
                        profile.atmosphereMax = 0.04;
                        profile.atmosphereBase = 319;
                        profile.noFall = -1;
                        profile.muffle = 0.15;
                        profile.muffleHF = 0.1;
                        profile.layers.clear();
                        profile.layers.addAll(AtmosphereLayers.defaults());
                    }
                    case "thin_atmosphere" -> {
                        profile.atmosphereMax = 0.05;
                        profile.atmosphereBase = 319;
                        profile.noFall = 1800;
                        profile.muffle = 0.02;
                        profile.muffleHF = 0.01;
                        profile.layers.clear();
                        profile.layers.addAll(List.of(
                                new AtmosphereLayers.Layer(-64, 1.0), new AtmosphereLayers.Layer(64, 0.75),
                                new AtmosphereLayers.Layer(400, 0.25), new AtmosphereLayers.Layer(1200, 0.08),
                                new AtmosphereLayers.Layer(2000, 0.0), new AtmosphereLayers.Layer(3000, 0.0),
                                new AtmosphereLayers.Layer(4500, 0.0), new AtmosphereLayers.Layer(6000, 0.0)));
                    }
                    default -> throw new IllegalArgumentException("Unknown atmosphere preset: " + preset);
                }
            }
            refresh();
        }

        private void applyAtmospherePreset(DimensionDraft profile, AtmospherePreset preset) {
            saveCurrentEntries();
            expandProfile(profile);
            profile.presetsExpanded = true;
            profile.atmosphereMax = preset.gravityMax();
            profile.atmosphereBase = preset.startAltitude();
            profile.noFall = preset.noFallDamageAbove() == null ? -1 : preset.noFallDamageAbove();
            profile.muffle = preset.muffleGain();
            profile.muffleHF = preset.muffleGainHF();
            profile.layers.clear();
            profile.layers.addAll(AtmosphereLayers.parse(preset.layers()));
            draft.status = "";
            refresh();
        }

        private void saveAtmospherePreset(DimensionDraft profile) {
            saveCurrentEntries();
            String name = profile.presetName.trim();
            if (name.isEmpty() || name.length() > 48) {
                draft.presetStatus = "invalid_name";
                refresh();
                return;
            }
            AtmospherePreset preset;
            try {
                preset = new AtmospherePreset(name, profile.atmosphereMax, profile.atmosphereBase,
                        profile.noFall < 0 ? null : profile.noFall, profile.muffle, profile.muffleHF,
                        AtmosphereLayers.serialize(profile.layers));
            } catch (IllegalArgumentException e) {
                draft.presetStatus = "invalid_preset";
                refresh();
                return;
            }
            int existing = -1;
            for (int i = 0; i < draft.atmospherePresets.size(); i++) {
                if (draft.atmospherePresets.get(i).name().equalsIgnoreCase(name)) {
                    existing = i;
                    break;
                }
            }
            if (existing >= 0) draft.atmospherePresets.set(existing, preset);
            else if (draft.atmospherePresets.size() >= AtmospherePreset.MAX_PRESETS) {
                draft.presetStatus = "limit";
                refresh();
                return;
            } else draft.atmospherePresets.add(preset);
            profile.presetName = "";
            draft.presetStatus = "saved";
            profile.presetsExpanded = true;
            expandProfile(profile);
            refresh();
        }

        private void deleteAtmospherePreset(DimensionDraft profile, AtmospherePreset preset) {
            saveCurrentEntries();
            draft.atmospherePresets.remove(preset);
            draft.presetStatus = "deleted";
            profile.presetsExpanded = true;
            expandProfile(profile);
            refresh();
        }

        private void importAtmospherePresets() {
            saveCurrentEntries();
            try {
                List<AtmospherePreset> imported = AtmospherePreset.parseImport(draft.importJson);
                List<AtmospherePreset> merged = new ArrayList<>(draft.atmospherePresets);
                for (AtmospherePreset preset : imported) {
                    int existing = -1;
                    for (int i = 0; i < merged.size(); i++) {
                        if (merged.get(i).name().equalsIgnoreCase(preset.name())) {
                            existing = i;
                            break;
                        }
                    }
                    if (existing >= 0) merged.set(existing, preset);
                    else merged.add(preset);
                }
                if (merged.size() > AtmospherePreset.MAX_PRESETS) {
                    throw new IllegalArgumentException("At most " + AtmospherePreset.MAX_PRESETS + " presets are supported");
                }
                draft.atmospherePresets.clear();
                draft.atmospherePresets.addAll(merged);
                draft.presetStatus = "imported";
                draft.importJson = "";
            } catch (RuntimeException e) {
                draft.presetStatus = "invalid_json";
            }
            refresh();
        }

        private void exportAtmospherePresets() {
            saveCurrentEntries();
            Minecraft.getInstance().keyboardHandler.setClipboard(
                    AtmospherePreset.exportJson(draft.atmospherePresets));
            draft.presetStatus = "exported";
            refresh();
        }

        private static void setFixed(DimensionDraft profile, double player, double mob, double item,
                                     double arrow, double thrown, double block) {
            profile.player = player; profile.mob = mob; profile.item = item;
            profile.arrow = arrow; profile.thrown = thrown; profile.block = block;
        }

        private Optional<Component> dimensionIdError(String raw, boolean checkDuplicate) {
            String id = raw.trim();
            if (id.isEmpty()) return Optional.empty();
            if (!id.contains(":") || ResourceLocation.tryParse(id) == null) {
                return Optional.of(text("endless_gravity.config.dimensionId.invalid"));
            }
            if (checkDuplicate && draft.profiles.stream().anyMatch(profile -> profile.id.equals(id))) {
                return Optional.of(text("endless_gravity.config.dimensionId.duplicate"));
            }
            return Optional.empty();
        }

        private void addProfile() {
            saveCurrentEntries();
            String id = draft.pending.id.trim();
            if (dimensionIdError(id, true).isPresent()) {
                draft.status = "invalid";
                refresh();
                return;
            }
            ResourceLocation parsed = ResourceLocation.tryParse(id);
            if (parsed == null || !id.contains(":")) {
                draft.status = "invalid";
                refresh();
                return;
            }
            draft.pending.id = parsed.toString();
            draft.profiles.add(draft.pending);
            draft.pending = new DimensionDraft(Config.CustomDimConfig.vanilla());
            draft.pending.id = "";
            draft.pending.mode = Config.CustomDimConfig.Mode.FIXED_GRAVITY;
            draft.pendingExpanded = false;
            draft.status = "added";
            refresh();
        }

        private void setMode(DimensionDraft profile, Config.CustomDimConfig.Mode mode) {
            if (profile.mode == mode) return;
            saveCurrentEntries();
            expandProfile(profile);
            profile.mode = mode;
            refresh();
        }

        private void expandProfile(DimensionDraft profile) {
            if (profile == draft.pending) {
                draft.pendingExpanded = true;
            } else {
                draft.expandedProfileId = profile.id;
            }
        }

        private void removeProfile(DimensionDraft profile) {
            saveCurrentEntries();
            draft.profiles.remove(profile);
            draft.removed.add(profile);
            refresh();
        }

        private void restoreProfile(DimensionDraft profile) {
            saveCurrentEntries();
            if (draft.profiles.stream().anyMatch(existing -> existing.id.equals(profile.id))) {
                draft.status = "duplicate";
                refresh();
                return;
            }
            draft.removed.remove(profile);
            draft.profiles.add(profile);
            draft.status = "";
            refresh();
        }

        private void saveCurrentEntries() {
            categories.forEach(category -> category.getEntries().forEach(raw -> {
                if (raw instanceof me.shedaniel.clothconfig2.api.AbstractConfigListEntry<?> entry) entry.save();
            }));
        }

        private void refresh() {
            Minecraft.getInstance().execute(() -> Minecraft.getInstance().setScreen(new Factory(parent, draft).build()));
        }

        private void buildGeneral(ConfigBuilder builder, ConfigEntryBuilder entry) {
            ConfigCategory cat = category(builder, "endless_gravity.config.section.general");
            categories.add(cat);
            SubCategoryBuilder dimensionToggles = group(entry, "endless_gravity.config.group.dimensionToggles");
            addToggle(dimensionToggles, entry, "endless_gravity.config.endEntityGravity",
                    Config.COMMON.endEntityGravity, true);
            addToggle(dimensionToggles, entry, "endless_gravity.config.enableAtmosphere",
                    Config.COMMON.enableAtmosphere, true);
            addToggle(dimensionToggles, entry, "endless_gravity.config.overworldEntityGravity",
                    Config.COMMON.overworldEntityGravity, true);
            cat.addEntry(dimensionToggles.build());

            SubCategoryBuilder audio = group(entry, "endless_gravity.config.group.audio");
            addToggle(audio, entry, "endless_gravity.config.enableLowPassFilter",
                    Config.COMMON.enableLowPassFilter, true);
            addDouble(audio, entry, "endless_gravity.config.lowPassGain", Config.COMMON.lowPassGain,
                    0.0, 1.0, 0.35, TEXT_WHITE);
            addDouble(audio, entry, "endless_gravity.config.lowPassGainHF", Config.COMMON.lowPassGainHF,
                    0.0, 1.0, 0.25, TEXT_GRAY);
            cat.addEntry(audio.build());

            var mode = entry.startEnumSelector(text("endless_gravity.config.fallDamageMode"), FallDamageMode.class,
                            FallDamageMode.fromInt(Config.COMMON.fallDamageMode.get()))
                    .setDefaultValue(FallDamageMode.DISABLED)
                    .setEnumNameProvider(e -> colored("endless_gravity.config.fallDamageMode."
                                    + ((FallDamageMode) e).name().toLowerCase(Locale.ROOT),
                            switch ((FallDamageMode) e) {
                                case DISABLED -> TEXT_RED;
                                default -> TEXT_WHITE;
                            }))
                    .setSaveConsumer(m -> Config.COMMON.fallDamageMode.set(m.ordinal()))
                    .build();
            cat.addEntry(mode);

            var mobMode = entry.startEnumSelector(text("endless_gravity.config.mobFallDamageMode"), FallDamageMode.class,
                            FallDamageMode.fromInt(Config.COMMON.mobFallDamageMode.get()))
                    .setDefaultValue(FallDamageMode.DISABLED)
                    .setEnumNameProvider(e -> colored("endless_gravity.config.fallDamageMode."
                                    + ((FallDamageMode) e).name().toLowerCase(Locale.ROOT),
                            switch ((FallDamageMode) e) {
                                case DISABLED -> TEXT_RED;
                                default -> TEXT_WHITE;
                            }))
                    .setSaveConsumer(m -> Config.COMMON.mobFallDamageMode.set(m.ordinal()))
                    .build();
            cat.addEntry(mobMode);

            var immune = entry.startStrList(text("endless_gravity.config.gravityImmuneEntities"),
                            new ArrayList<>(Config.COMMON.gravityImmuneEntities.get()))
                    .setDefaultValue(List.of())
                    .setTooltip(text("endless_gravity.config.gravityImmuneEntities.tooltip"))
                    .setSaveConsumer(Config.COMMON.gravityImmuneEntities::set)
                    .build();
            cat.addEntry(immune);

            SubCategoryBuilder potions = group(entry, "endless_gravity.config.group.potions");
            addToggle(potions, entry, "endless_gravity.config.enablePotionGravity", Config.COMMON.enablePotionGravity, true);
            addDouble(potions, entry, "endless_gravity.config.weightlessnessGravityOffset", Config.COMMON.weightlessnessGravityOffset,
                    0.0, 0.07, 0.05, TEXT_WHITE);
            addDouble(potions, entry, "endless_gravity.config.heavinessGravityOffset", Config.COMMON.heavinessGravityOffset,
                    0.0, 0.07, 0.02, TEXT_GRAY);
            cat.addEntry(potions.build());

            SubCategoryBuilder hud = group(entry, "endless_gravity.config.group.hud");
            addToggle(hud, entry, "endless_gravity.config.enableGravityHud", Config.COMMON.enableGravityHud, false);
            cat.addEntry(hud.build());
        }

        private void addToggle(SubCategoryBuilder group, ConfigEntryBuilder entry, String key,
                               Config.BoolValue value, boolean defaultValue) {
            group.add(entry.startBooleanToggle(text(key), value.get())
                    .setDefaultValue(defaultValue)
                    .setSaveConsumer(value::set)
                    .setYesNoTextSupplier(v -> colored("endless_gravity.config." + v, v ? TEXT_GREEN : TEXT_GRAY))
                    .build());
        }

        private void addDouble(SubCategoryBuilder group, ConfigEntryBuilder entry, String key,
                               Config.DoubleValue value, double min, double max, double defaultValue, int color) {
            group.add(entry.startDoubleField(colored(key, color), value.get())
                    .setMin(min).setMax(max).setDefaultValue(defaultValue)
                    .setSaveConsumer(value::set)
                    .build());
        }

        private void finishSave() {
            Config.saveCustomDimensions(draft.profiles.stream().map(DimensionDraft::toConfig).toList());
            Config.saveAtmospherePresets(draft.atmospherePresets);
            Config.save();
        }

        private static SubCategoryBuilder group(ConfigEntryBuilder entry, String key) {
            return entry.startSubCategory(colored(key, TEXT_BLUE)).setExpanded(true);
        }

        private static ConfigCategory category(ConfigBuilder builder, String key) {
            return builder.getOrCreateCategory(text(key));
        }

        private static Component text(String key, Object... args) {
            return Component.translatable(key, args);
        }

        private static Component colored(String key, int color, Object... args) {
            return Component.translatable(key, args)
                    .withStyle(style -> style.withColor(net.minecraft.network.chat.TextColor.fromRgb(color)));
        }
    }
}