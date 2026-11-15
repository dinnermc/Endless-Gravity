package dinner.dev.endless_gravity;

import dinner.dev.endless_gravity.client.ConfigSyncClient;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
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
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.ModConfigSpec;

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
    private static final double CURVE_MIN_ALTITUDE = -2048.0;
    private static final double CURVE_MAX_ALTITUDE = 30000.0;
    private static final double CURVE_MIN_PRESSURE = 0.0;

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
        private CustomDimConfig.Mode mode;
        private double playerOffset;
        private double mobOffset;
        private double itemOffset;
        private double arrowOffset;
        private double thrownOffset;
        private double blockOffset;
        private double atmosphereGravityMax;
        private double atmosphereBaseAltitude;
        private double noFallDamageAltitude;
        private double atmosphereMuffleGain;
        private double atmosphereMuffleGainHF;
        private final List<AtmosphereLayers.Layer> layers = new ArrayList<>();
        private boolean curveExpanded;
        private boolean presetsExpanded;
        private String presetName = "";
        private DimensionDraft(CustomDimConfig config) {
            id = config.getDimensionId();
            mode = config.getMode();
            playerOffset = config.getPlayerGravityOffset();
            mobOffset = config.getMobGravityOffset();
            itemOffset = config.getItemGravityOffset();
            arrowOffset = config.getArrowGravityOffset();
            thrownOffset = config.getThrownGravityOffset();
            blockOffset = config.getBlockGravityOffset();
            atmosphereGravityMax = config.getAtmosphereGravityMax();
            atmosphereBaseAltitude = config.getAtmosphereBaseAltitude();
            noFallDamageAltitude = config.getNoFallDamageAltitude() == null
                    ? -1.0 : config.getNoFallDamageAltitude();
            atmosphereMuffleGain = config.getAtmosphereMuffleGain();
            atmosphereMuffleGainHF = config.getAtmosphereMuffleGainHF();
            List<AtmosphereLayers.Layer> configuredLayers =
                    AtmosphereLayers.parseUncached(config.getAtmosphereLayers());
            layers.addAll(configuredLayers);
        }

        private CustomDimConfig toConfig() {
            return new CustomDimConfig(id, mode,
                    playerOffset, mobOffset, itemOffset, arrowOffset, thrownOffset, blockOffset,
                    atmosphereGravityMax, atmosphereBaseAltitude,
                    noFallDamageAltitude < 0.0 ? null : noFallDamageAltitude,
                    atmosphereMuffleGain, atmosphereMuffleGainHF,
                    AtmosphereLayers.serialize(layers));
        }
    }

    private static final class EditorDraft {
        private final List<AtmosphereLayers.Layer> globalLayers = new ArrayList<>();
        private final List<DimensionDraft> dimensionDrafts = new ArrayList<>();
        private final List<DimensionDraft> removedProfiles = new ArrayList<>();
        private DimensionDraft newDimension = newDimensionDraft();
        private String dimensionStatus = "";
        private String expandedProfileId = "";
        private boolean newDimensionExpanded;
        private final List<AtmospherePreset> atmospherePresets = new ArrayList<>();
        private String importJson = "";
        private String presetStatus = "";

        private static DimensionDraft newDimensionDraft() {
            DimensionDraft newDraft = new DimensionDraft(
                    Config.createDimensionProfile("", CustomDimConfig.Mode.ATMOSPHERE));
            newDraft.mode = CustomDimConfig.Mode.FIXED_GRAVITY;
            return newDraft;
        }

        private EditorDraft() {
            List<AtmosphereLayers.Layer> parsed =
                    AtmosphereLayers.parse(Config.COMMON.atmosphereLayers.get());
            for (int i = 0; i < AtmosphereLayers.LAYER_COUNT; i++) {
                globalLayers.add(i < parsed.size() ? parsed.get(i) : AtmosphereLayers.defaults().get(i));
            }
            Config.getCustomDimensions().forEach(config -> dimensionDrafts.add(new DimensionDraft(config)));
            atmospherePresets.addAll(Config.getAtmospherePresets());
        }
    }

    private static final class ActionEntry extends TextListEntry {
        private final Button button;

        private ActionEntry(Component label, Runnable action) {
            this(label, action, null);
        }

        private ActionEntry(Component label, Runnable action, Component tooltip) {
            super(Component.empty(), Component.empty());
            button = Button.builder(label, ignored -> action.run()).bounds(0, 0, 220, 20).build();
            if (tooltip != null) button.setTooltip(Tooltip.create(tooltip));
        }

        @Override
        public int getItemHeight() {
            return 24;
        }

        @Override
        public void render(GuiGraphics graphics, int index, int y, int x, int entryWidth, int entryHeight,
                           int mouseX, int mouseY, boolean hovered, float delta) {
            int buttonX = x + Math.max(0, entryWidth - 220 - 8);
            button.setX(buttonX);
            button.setY(y + 2);
            button.render(graphics, mouseX, mouseY, delta);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            return this.button.mouseClicked(mouseX, mouseY, button);
        }
    }

    private static final class ModeChoiceEntry extends TextListEntry {
        private final Button atmosphereButton;
        private final Button fixedGravityButton;

        private ModeChoiceEntry(CustomDimConfig.Mode selected, Runnable selectAtmosphere,
                                Runnable selectFixedGravity) {
            super(Component.empty(), Component.empty());
            atmosphereButton = Button.builder(
                    Component.translatable("endless_gravity.config.dimensionMode.atmosphere"),
                    ignored -> selectAtmosphere.run()).bounds(0, 0, 150, 20).build();
            atmosphereButton.setTooltip(Tooltip.create(
                    Component.translatable("endless_gravity.config.dimensionMode.atmosphere.tooltip")));
            fixedGravityButton = Button.builder(
                    Component.translatable("endless_gravity.config.dimensionMode.fixed_gravity"),
                    ignored -> selectFixedGravity.run()).bounds(0, 0, 150, 20).build();
            fixedGravityButton.setTooltip(Tooltip.create(
                    Component.translatable("endless_gravity.config.dimensionMode.fixed_gravity.tooltip")));
            atmosphereButton.active = selected != CustomDimConfig.Mode.ATMOSPHERE;
            fixedGravityButton.active = selected != CustomDimConfig.Mode.FIXED_GRAVITY;
        }

        @Override
        public int getItemHeight() {
            return 24;
        }

        @Override
        public void render(GuiGraphics graphics, int index, int y, int x, int entryWidth, int entryHeight,
                           int mouseX, int mouseY, boolean hovered, float delta) {
            int buttonWidth = 150;
            int gap = 8;
            int startX = x + (entryWidth - buttonWidth * 2 - gap) / 2;
            atmosphereButton.setX(startX);
            atmosphereButton.setY(y + 2);
            fixedGravityButton.setX(startX + buttonWidth + gap);
            fixedGravityButton.setY(y + 2);
            atmosphereButton.render(graphics, mouseX, mouseY, delta);
            fixedGravityButton.render(graphics, mouseX, mouseY, delta);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            return atmosphereButton.mouseClicked(mouseX, mouseY, button)
                    || fixedGravityButton.mouseClicked(mouseX, mouseY, button);
        }
    }

    private static final class PresetEntry extends TextListEntry {
        private final List<Button> buttons = new ArrayList<>();

        private PresetEntry(List<String> presetKeys, java.util.function.Consumer<String> apply) {
            super(Component.empty(), Component.empty());
            for (String key : presetKeys) {
                Button button = Button.builder(Component.translatable("endless_gravity.config.preset." + key),
                        ignored -> apply.accept(key)).bounds(0, 0, 112, 20).build();
                button.setTooltip(Tooltip.create(
                        Component.translatable("endless_gravity.config.preset." + key + ".tooltip")));
                buttons.add(button);
            }
        }

        @Override
        public int getItemHeight() {
            return Math.max(1, (buttons.size() + 2) / 3) * 24;
        }

        @Override
        public void render(GuiGraphics graphics, int index, int y, int x, int entryWidth, int entryHeight,
                           int mouseX, int mouseY, boolean hovered, float delta) {
            int buttonWidth = 112;
            int gap = 5;
            int columns = Math.min(3, buttons.size());
            int totalWidth = columns * buttonWidth + Math.max(0, columns - 1) * gap;
            int startX = x + (entryWidth - totalWidth) / 2;
            for (int i = 0; i < buttons.size(); i++) {
                Button button = buttons.get(i);
                button.setX(startX + (i % 3) * (buttonWidth + gap));
                button.setY(y + 2 + (i / 3) * 24);
                button.render(graphics, mouseX, mouseY, delta);
            }
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            for (Button preset : buttons) {
                if (preset.mouseClicked(mouseX, mouseY, button)) return true;
            }
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
            int totalWidth = 210 + 6 + 82;
            int startX = x + (entryWidth - totalWidth) / 2;
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

    private static final class AtmosphereCurveEntry extends TextListEntry {
        private static final int CURVE_COLOR = 0xFF9AB8A5;
        private static final int POINT_COLOR = 0xFFE4C08A;
        private final List<AtmosphereLayers.Layer> layers;
        private int chartX;
        private int chartY;
        private int chartWidth;
        private int chartHeight;
        private int selectedPoint = -1;
        private double dragStartX;
        private double dragStartAltitude;
        private double dragAltitudeRange;

        private AtmosphereCurveEntry(List<AtmosphereLayers.Layer> layers) {
            super(Component.empty(), Component.empty());
            this.layers = layers;
        }

        @Override
        public int getItemHeight() {
            return 112;
        }

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
                int gridY = chartY + i * chartHeight / 4;
                graphics.fill(chartX + 1, gridY, chartX + chartWidth - 1, gridY + 1, 0x334F595E);
            }
            for (int i = 0; i <= 4; i++) {
                int gridX = chartX + i * chartWidth / 4;
                graphics.fill(gridX, chartY + 1, gridX + 1, chartY + chartHeight - 1, 0x334F595E);
            }

            for (int i = 0; i < layers.size() - 1; i++) {
                AtmosphereLayers.Layer first = layers.get(i);
                AtmosphereLayers.Layer second = layers.get(i + 1);
                int x1 = altitudeToX(first.altitude());
                int x2 = altitudeToX(second.altitude());
                int y1 = pressureToY(first.pressure());
                int y2 = pressureToY(second.pressure());
                int span = Math.max(1, x2 - x1);
                for (int px = x1; px <= x2; px++) {
                    double t = (double) (px - x1) / span;
                    int py = (int) Math.round(y1 + t * (y2 - y1));
                    graphics.fill(px, py, px + 2, py + 2, CURVE_COLOR);
                }
            }
            for (int i = 0; i < layers.size(); i++) {
                AtmosphereLayers.Layer layer = layers.get(i);
                int pointX = altitudeToX(layer.altitude());
                int pointY = pressureToY(layer.pressure());
                int radius = i == selectedPoint ? 4 : 3;
                graphics.fill(pointX - radius - 1, pointY - radius - 1, pointX + radius + 2,
                        pointY + radius + 2, 0xFF171A1D);
                graphics.fill(pointX - radius, pointY - radius, pointX + radius + 1,
                        pointY + radius + 1, POINT_COLOR);
            }
            boolean overChart = insideChart(mouseX, mouseY);
            if (overChart) {
                graphics.fill((int) mouseX, chartY, (int) mouseX + 1, chartY + chartHeight, 0x55FFFFFF);
                graphics.fill(chartX, (int) mouseY, chartX + chartWidth, (int) mouseY + 1, 0x55FFFFFF);
                double altitude = xToAltitude(mouseX);
                double curvePressure = AtmosphereLayers.getPressure(layers, altitude);
                double cursorPressure = yToPressure(mouseY);
                graphics.renderTooltip(Minecraft.getInstance().font,
                        Component.literal(String.format(Locale.ROOT,
                                "Y %.1f  |  P %.3f  |  Cursor %.3f", altitude, curvePressure, cursorPressure)),
                        (int) mouseX, (int) mouseY);
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
            if (button != 0 || !insideChart(mouseX, mouseY)) return false;
            double nearestDistance = Double.MAX_VALUE;
            int nearest = -1;
            for (int i = 0; i < layers.size(); i++) {
                double dx = altitudeToX(layers.get(i).altitude()) - mouseX;
                double dy = pressureToY(layers.get(i).pressure()) - mouseY;
                double distance = dx * dx + dy * dy;
                if (distance < nearestDistance) {
                    nearestDistance = distance;
                    nearest = i;
                }
            }
            if (nearestDistance > 20 * 20) return false;
            selectedPoint = nearest;
            dragStartX = mouseX;
            dragStartAltitude = layers.get(selectedPoint).altitude();
            dragAltitudeRange = altitudeMax() - altitudeMin();
            moveSelected(mouseX, mouseY);
            return true;
        }

        @Override
        public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
            if (button != 0 || selectedPoint < 0) return false;
            moveSelected(mouseX, mouseY);
            return true;
        }

        @Override
        public boolean mouseReleased(double mouseX, double mouseY, int button) {
            if (button != 0 || selectedPoint < 0) return false;
            selectedPoint = -1;
            return true;
        }

        private boolean insideChart(double mouseX, double mouseY) {
            return mouseX >= chartX && mouseX <= chartX + chartWidth
                    && mouseY >= chartY && mouseY <= chartY + chartHeight;
        }

        private int altitudeToX(double altitude) {
            double t = (altitude - altitudeMin()) / (altitudeMax() - altitudeMin());
            return chartX + (int) Math.round(AtmosphereLayers.clamp(t, 0.0, 1.0) * chartWidth);
        }

        private int pressureToY(double pressure) {
            double t = AtmosphereLayers.clamp(pressure / pressureMax(), 0.0, 1.0);
            return chartY + chartHeight - (int) Math.round(t * chartHeight);
        }

        private double pressureMax() {
            double max = layers.stream().mapToDouble(AtmosphereLayers.Layer::pressure).max().orElse(1.25);
            return Math.max(1.25, Math.ceil(max));
        }

        private double xToAltitude(double mouseX) {
            return AtmosphereLayers.clamp(
                    (mouseX - chartX) / chartWidth, 0.0, 1.0) * (altitudeMax() - altitudeMin())
                    + altitudeMin();
        }

        private double yToPressure(double mouseY) {
            return pressureMax() * (1.0 - AtmosphereLayers.clamp(
                    (mouseY - chartY) / chartHeight, 0.0, 1.0));
        }

        private double altitudeMin() {
            return layers.get(0).altitude();
        }

        private double altitudeMax() {
            return layers.get(layers.size() - 1).altitude();
        }

        private void moveSelected(double mouseX, double mouseY) {
            double altitude = selectedPoint == 0 || selectedPoint == layers.size() - 1
                    ? dragStartAltitude + (mouseX - dragStartX) * dragAltitudeRange / chartWidth
                    : xToAltitude(mouseX);
            double lowerBound = selectedPoint == 0 ? CURVE_MIN_ALTITUDE
                    : layers.get(selectedPoint - 1).altitude() + 1.0;
            double upperBound = selectedPoint == layers.size() - 1 ? CURVE_MAX_ALTITUDE
                    : layers.get(selectedPoint + 1).altitude() - 1.0;
            altitude = AtmosphereLayers.clamp(altitude, lowerBound, upperBound);
            double pressure = yToPressure(mouseY);
            layers.set(selectedPoint, new AtmosphereLayers.Layer(altitude, pressure));
        }
    }

    private static final class DividerEntry extends TextListEntry {
        private DividerEntry() {
            super(Component.empty(), Component.empty());
        }

        @Override
        public int getItemHeight() {
            return 8;
        }

        @Override
        public void render(GuiGraphics graphics, int index, int y, int x, int entryWidth, int entryHeight,
                           int mouseX, int mouseY, boolean hovered, float delta) {
            int dividerY = y + entryHeight / 2;
            graphics.fill(x + 6, dividerY, x + entryWidth - 6, dividerY + 1, DIVIDER_COLOR);
        }
    }

    private static final class Factory {
        private final Screen parent;
        private final boolean sableLoaded;
        private final EditorDraft draft;
        private final List<ConfigCategory> categories = new ArrayList<>();

        Factory(Screen parent) {
            this(parent, new EditorDraft());
        }

        private Factory(Screen parent, EditorDraft draft) {
            this.parent = parent;
            this.draft = draft;
            this.sableLoaded = ModList.get().isLoaded("sable");
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
            ConfigCategory cat = category(builder, "endless_gravity.config.section.dimensions");
            categories.add(cat);
            SubCategoryBuilder addProfile = entry.startSubCategory(
                    text("endless_gravity.config.dimensionCreate"))
                    .setExpanded(draft.newDimensionExpanded);
            addProfile.add(entry.startStrField(text("endless_gravity.config.dimensionAdd"), draft.newDimension.id)
                    .setTooltip(text("endless_gravity.config.dimensionAdd.tooltip"))
                    .setErrorSupplier(raw -> dimensionIdError(raw, true))
                    .setSaveConsumer(value -> draft.newDimension.id = value.trim())
                    .build());
            addProfile.add(new ModeChoiceEntry(draft.newDimension.mode,
                    () -> setMode(draft.newDimension, CustomDimConfig.Mode.ATMOSPHERE),
                    () -> setMode(draft.newDimension, CustomDimConfig.Mode.FIXED_GRAVITY)));
            if (!draft.dimensionStatus.isEmpty()) {
                boolean added = draft.dimensionStatus.equals("added");
                addProfile.add(entry.startTextDescription(
                        colored("endless_gravity.config.dimensionStatus." + draft.dimensionStatus,
                                added ? TEXT_GREEN : TEXT_RED)).build());
            }
            addProfileSettings(addProfile, entry, draft.newDimension);
            addProfile.add(new ActionEntry(text("endless_gravity.config.dimensionCreateButton"),
                    this::addPendingDimension, text("endless_gravity.config.dimensionCreateButton.tooltip")));
            cat.addEntry(addProfile.build());

            SubCategoryBuilder presetManager = entry.startSubCategory(
                    text("endless_gravity.config.customPresets"))
                    .setExpanded(false)
                    .setTooltip(text("endless_gravity.config.customPresetsHelp"));
            presetManager.add(entry.startStrField(text("endless_gravity.config.importPresetsJson"), draft.importJson)
                    .setTooltip(text("endless_gravity.config.importPresetsJson.tooltip"))
                    .setSaveConsumer(value -> draft.importJson = value)
                    .build());
            presetManager.add(new ActionEntry(text("endless_gravity.config.importPresets"),
                    this::importAtmospherePresets, text("endless_gravity.config.importPresets.tooltip")));
            presetManager.add(new ActionEntry(text("endless_gravity.config.exportPresets"),
                    this::exportAtmospherePresets, text("endless_gravity.config.exportPresets.tooltip")));
            if (!draft.presetStatus.isEmpty()) {
                presetManager.add(entry.startTextDescription(
                        text("endless_gravity.config.presetStatus." + draft.presetStatus)).build());
            }
            if (!draft.atmospherePresets.isEmpty()) {
                presetManager.add(entry.startTextDescription(text("endless_gravity.config.savedPresets",
                        draft.atmospherePresets.size())).build());
            }
            cat.addEntry(presetManager.build());

            cat.addEntry(entry.startTextDescription(text("endless_gravity.config.dimensionExistingProfiles",
                    draft.dimensionDrafts.size())).build());
            if (draft.dimensionDrafts.isEmpty()) {
                cat.addEntry(entry.startTextDescription(text("endless_gravity.config.dimensionNoProfiles")).build());
            }
            List<DimensionDraft> sortedDrafts = draft.dimensionDrafts.stream()
                    .sorted((a, b) -> a.id.compareToIgnoreCase(b.id)).toList();
            for (DimensionDraft profileDraft : sortedDrafts) {
                SubCategoryBuilder profile = entry.startSubCategory(
                        Component.literal(profileDraft.id)
                                .append(Component.literal("  |  ").withColor(TEXT_GRAY))
                                .append(modeName(profileDraft.mode).copy().withColor(modeColor(profileDraft.mode))))
                                .setExpanded(profileDraft.id.equals(draft.expandedProfileId));
                profile.add(new ModeChoiceEntry(profileDraft.mode,
                        () -> setMode(profileDraft, CustomDimConfig.Mode.ATMOSPHERE),
                        () -> setMode(profileDraft, CustomDimConfig.Mode.FIXED_GRAVITY)));
                addProfileSettings(profile, entry, profileDraft);
                profile.add(new ActionEntry(colored("endless_gravity.config.dimensionRemoveButton", TEXT_RED),
                        () -> removeProfile(profileDraft),
                        text("endless_gravity.config.dimensionRemoveButton.tooltip")));
                cat.addEntry(profile.build());
            }
            cat.addEntry(entry.startTextDescription(text("endless_gravity.config.dimensionProfilesSaveHint")).build());
            if (!draft.removedProfiles.isEmpty()) {
                cat.addEntry(entry.startTextDescription(text("endless_gravity.config.dimensionUndoHeader")).build());
                for (DimensionDraft removed : List.copyOf(draft.removedProfiles)) {
                    cat.addEntry(new ActionEntry(text("endless_gravity.config.dimensionUndo", removed.id),
                            () -> restoreProfile(removed),
                            text("endless_gravity.config.dimensionUndo.tooltip")));
                }
            }
        }

        private Optional<Component> dimensionIdError(String raw, boolean checkDuplicate) {
            String id = raw.trim();
            if (id.isEmpty()) return Optional.empty();
            if (!id.contains(":") || ResourceLocation.tryParse(id) == null) {
                return Optional.of(text("endless_gravity.config.dimensionId.invalid"));
            }
            if (checkDuplicate && draft.dimensionDrafts.stream().anyMatch(existing -> existing.id.equals(id))) {
                return Optional.of(text("endless_gravity.config.dimensionId.duplicate"));
            }
            return Optional.empty();
        }

        private void addProfileSettings(SubCategoryBuilder destination, ConfigEntryBuilder entry,
                                        DimensionDraft profile) {
            SubCategoryBuilder presets = entry.startSubCategory(
                    text("endless_gravity.config.dimensionPresets"))
                    .setExpanded(profile.presetsExpanded)
                    .setTooltip(text("endless_gravity.config.dimensionPresets.tooltip"));
            List<String> presetKeys = profile.mode == CustomDimConfig.Mode.FIXED_GRAVITY
                    ? List.of("vanilla", "light_gravity", "lunar")
                    : List.of("terrestrial", "gentle_atmosphere", "thin_atmosphere");
            presets.add(new PresetEntry(presetKeys, preset -> applyPreset(profile, preset)));

            if (profile.mode == CustomDimConfig.Mode.FIXED_GRAVITY) {
                destination.add(presets.build());
                SubCategoryBuilder gravity = entry.startSubCategory(
                        colored("endless_gravity.config.dimensionFixedGravity", TEXT_BLUE))
                        .setExpanded(true)
                        .setTooltip(text("endless_gravity.config.dimensionFixedGravity.tooltip"));
                addProfileDouble(gravity, entry, "endless_gravity.config.playerGravityOffset",
                        profile.playerOffset, -0.5, 0.5, 0.055, value -> profile.playerOffset = value);
                addProfileDouble(gravity, entry, "endless_gravity.config.mobGravityOffset",
                        profile.mobOffset, -0.5, 0.5, 0.025, value -> profile.mobOffset = value);
                addProfileDouble(gravity, entry, "endless_gravity.config.itemGravityOffset",
                        profile.itemOffset, -0.5, 0.5, 0.025, value -> profile.itemOffset = value);
                addProfileDouble(gravity, entry, "endless_gravity.config.arrowGravityOffset",
                        profile.arrowOffset, -0.5, 0.5, 0.03, value -> profile.arrowOffset = value);
                addProfileDouble(gravity, entry, "endless_gravity.config.thrownGravityOffset",
                        profile.thrownOffset, -0.5, 0.5, 0.018, value -> profile.thrownOffset = value);
                addProfileDouble(gravity, entry, "endless_gravity.config.blockGravityOffset",
                        profile.blockOffset, -0.5, 0.5, 0.035, value -> profile.blockOffset = value);
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
                    () -> saveAtmospherePreset(profile),
                    text("endless_gravity.config.saveCurrentAsPreset.tooltip")));
            destination.add(presets.build());

            SubCategoryBuilder atmosphere = entry.startSubCategory(
                    colored("endless_gravity.config.dimensionAtmosphere", TEXT_BLUE))
                    .setExpanded(true)
                    .setTooltip(text("endless_gravity.config.dimensionAtmosphere.tooltip"));
            addProfileDouble(atmosphere, entry, "endless_gravity.config.atmosphereGravityMax",
                    profile.atmosphereGravityMax, 0.0, 0.5, 0.075,
                    value -> profile.atmosphereGravityMax = value);
            addProfileDouble(atmosphere, entry, "endless_gravity.config.atmosphereBaseAltitude",
                    profile.atmosphereBaseAltitude, -2048.0, 30000.0, 319.0,
                    value -> profile.atmosphereBaseAltitude = value);
            addProfileDouble(atmosphere, entry, "endless_gravity.config.atmosphereMuffleGain",
                    profile.atmosphereMuffleGain, 0.0, 1.0, 0.01,
                    value -> profile.atmosphereMuffleGain = value);
            addProfileDouble(atmosphere, entry, "endless_gravity.config.atmosphereMuffleGainHF",
                    profile.atmosphereMuffleGainHF, 0.0, 1.0, 0.005,
                    value -> profile.atmosphereMuffleGainHF = value);
            addProfileDouble(atmosphere, entry, "endless_gravity.config.noFallDamageAltitude",
                    profile.noFallDamageAltitude, -1.0, 30000.0, 4500.0,
                    value -> profile.noFallDamageAltitude = value);
            SubCategoryBuilder curve = entry.startSubCategory(
                    colored("endless_gravity.config.atmosphereLayers", TEXT_BLUE))
                    .setExpanded(profile.curveExpanded)
                    .setTooltip(text("endless_gravity.config.curveGraphHelp"));
            curve.add(new AtmosphereCurveEntry(profile.layers));
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
                        () -> removeLayer(profile, index),
                        text("endless_gravity.config.curveRemoveLayer.tooltip")));
                }
                curve.add(row.build());
            }
            if (profile.layers.size() < CustomDimConfig.MAX_ATMOSPHERE_LAYERS) {
                curve.add(new ActionEntry(text("endless_gravity.config.curveAddLayer"),
                        () -> addLayer(profile),
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
            if (profile.layers.size() >= CustomDimConfig.MAX_ATMOSPHERE_LAYERS) return;
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

        private void applyPreset(DimensionDraft profile, String preset) {
            saveCurrentEntries();
            expandProfile(profile);
            profile.presetsExpanded = true;
            if (profile.mode == CustomDimConfig.Mode.FIXED_GRAVITY) {
                switch (preset) {
                    case "vanilla" -> setFixedGravity(profile, 0, 0, 0, 0, 0, 0);
                    case "light_gravity" -> setFixedGravity(profile, 0.025, 0.02, 0.01, 0.015, 0.01, 0.02);
                    case "lunar" -> setFixedGravity(profile, 0.055, 0.025, 0.025, 0.03, 0.018, 0.035);
                    default -> throw new IllegalArgumentException("Unknown fixed-gravity preset: " + preset);
                }
            } else {
                switch (preset) {
                    case "terrestrial" -> {
                        profile.atmosphereGravityMax = 0.075;
                        profile.atmosphereBaseAltitude = 319;
                        profile.noFallDamageAltitude = 4500;
                        profile.atmosphereMuffleGain = 0.01;
                        profile.atmosphereMuffleGainHF = 0.005;
                        profile.layers.clear();
                        profile.layers.addAll(AtmosphereLayers.defaults());
                    }
                    case "gentle_atmosphere" -> {
                        profile.atmosphereGravityMax = 0.04;
                        profile.atmosphereBaseAltitude = 319;
                        profile.noFallDamageAltitude = -1;
                        profile.atmosphereMuffleGain = 0.15;
                        profile.atmosphereMuffleGainHF = 0.1;
                        profile.layers.clear();
                        profile.layers.addAll(AtmosphereLayers.defaults());
                    }
                    case "thin_atmosphere" -> {
                        profile.atmosphereGravityMax = 0.05;
                        profile.atmosphereBaseAltitude = 64;
                        profile.noFallDamageAltitude = 1800;
                        profile.atmosphereMuffleGain = 0.02;
                        profile.atmosphereMuffleGainHF = 0.01;
                        profile.layers.clear();
                        profile.layers.addAll(List.of(
                                new AtmosphereLayers.Layer(-64, 1.0),
                                new AtmosphereLayers.Layer(64, 0.75),
                                new AtmosphereLayers.Layer(400, 0.25),
                                new AtmosphereLayers.Layer(1200, 0.08),
                                new AtmosphereLayers.Layer(2000, 0.0),
                                new AtmosphereLayers.Layer(3000, 0.0),
                                new AtmosphereLayers.Layer(4500, 0.0),
                                new AtmosphereLayers.Layer(6000, 0.0)));
                    }
                    default -> throw new IllegalArgumentException("Unknown atmosphere preset: " + preset);
                }
            }
            draft.dimensionStatus = "";
            refresh();
        }

        private void applyAtmospherePreset(DimensionDraft profile, AtmospherePreset preset) {
            saveCurrentEntries();
            expandProfile(profile);
            profile.presetsExpanded = true;
            profile.atmosphereGravityMax = preset.gravityMax();
            profile.atmosphereBaseAltitude = preset.startAltitude();
            profile.noFallDamageAltitude = preset.noFallDamageAbove() == null ? -1 : preset.noFallDamageAbove();
            profile.atmosphereMuffleGain = preset.muffleGain();
            profile.atmosphereMuffleGainHF = preset.muffleGainHF();
            profile.layers.clear();
            profile.layers.addAll(AtmosphereLayers.parseUncached(preset.layers()));
            draft.dimensionStatus = "";
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
                preset = new AtmospherePreset(name, profile.atmosphereGravityMax,
                        profile.atmosphereBaseAltitude,
                        profile.noFallDamageAltitude < 0 ? null : profile.noFallDamageAltitude,
                        profile.atmosphereMuffleGain, profile.atmosphereMuffleGainHF,
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

        private static void setFixedGravity(DimensionDraft profile, double player, double mob, double item,
                                            double arrow, double thrown, double block) {
            profile.playerOffset = player;
            profile.mobOffset = mob;
            profile.itemOffset = item;
            profile.arrowOffset = arrow;
            profile.thrownOffset = thrown;
            profile.blockOffset = block;
        }

        private void addPendingDimension() {
            saveCurrentEntries();

            String id = draft.newDimension.id.trim();
            Optional<Component> error = dimensionIdError(id, true);
            if (error.isPresent()) {
                draft.dimensionStatus = "invalid";
                refresh();
                return;
            }
            ResourceLocation parsedId = ResourceLocation.tryParse(id);
            if (parsedId == null || !id.contains(":")) {
                draft.dimensionStatus = "invalid";
                refresh();
                return;
            }
            draft.newDimension.id = parsedId.toString();
            draft.dimensionDrafts.add(draft.newDimension);
            draft.newDimension = EditorDraft.newDimensionDraft();
            draft.newDimensionExpanded = false;
            draft.dimensionStatus = "added";
            refresh();
        }

        private void setMode(DimensionDraft profile, CustomDimConfig.Mode mode) {
            if (profile.mode == mode) return;
            saveCurrentEntries();
            expandProfile(profile);
            profile.mode = mode;
            refresh();
        }

        private void expandProfile(DimensionDraft profile) {
            if (profile == draft.newDimension) {
                draft.newDimensionExpanded = true;
            } else {
                draft.expandedProfileId = profile.id;
            }
        }

        private void removeProfile(DimensionDraft profile) {
            saveCurrentEntries();
            draft.dimensionDrafts.remove(profile);
            draft.removedProfiles.add(profile);
            refresh();
        }

        private void restoreProfile(DimensionDraft profile) {
            saveCurrentEntries();
            if (draft.dimensionDrafts.stream().anyMatch(existing -> existing.id.equals(profile.id))) {
                draft.dimensionStatus = "duplicate";
                refresh();
                return;
            }
            draft.removedProfiles.remove(profile);
            draft.dimensionDrafts.add(profile);
            draft.dimensionStatus = "";
            refresh();
        }

        private void saveCurrentEntries() {
            categories.forEach(category -> category.getEntries().forEach(raw -> {
                if (raw instanceof AbstractConfigListEntry<?> entry) entry.save();
            }));
        }

        private void refresh() {
            Minecraft.getInstance().execute(() -> Minecraft.getInstance().setScreen(new Factory(parent, draft).build()));
        }

        private static int modeColor(CustomDimConfig.Mode mode) {
            return mode == CustomDimConfig.Mode.ATMOSPHERE ? TEXT_GREEN : TEXT_AMBER;
        }

        private void addProfileDouble(SubCategoryBuilder group, ConfigEntryBuilder entry, String key,
                                      double current, double min, double max, double defaultValue,
                                      java.util.function.DoubleConsumer save) {
            group.add(entry.startDoubleField(text(key), current)
                    .setMin(min).setMax(max).setDefaultValue(defaultValue)
                    .setTooltip(text(key + ".tooltip"))
                    .setSaveConsumer(value -> save.accept(value))
                    .build());
        }

        private static Component modeName(CustomDimConfig.Mode mode) {
            String key = "endless_gravity.config.dimensionMode."
                    + mode.name().toLowerCase(Locale.ROOT);
            return text(key);
        }

        private void buildGeneral(ConfigBuilder builder, ConfigEntryBuilder entry) {
            ConfigCategory cat = category(builder, "endless_gravity.config.section.general");
            categories.add(cat);
            cat.addEntry(entry.startTextDescription(text("endless_gravity.config.generalHelp")).build());

            SubCategoryBuilder fallDamage = group(entry, "endless_gravity.config.group.fall_damage")
                    .setExpanded(true);
            fallDamage.add(entry.startTextDescription(text("endless_gravity.config.group.fall_damage.help")).build());
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
            fallDamage.add(mode);

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
            fallDamage.add(mobMode);
            cat.addEntry(fallDamage.build());

            SubCategoryBuilder gameplay = group(entry, "endless_gravity.config.group.gameplay");
            gameplay.add(entry.startTextDescription(text("endless_gravity.config.group.gameplay.help")).build());
            var immune = entry.startStrList(text("endless_gravity.config.gravityImmuneEntities"),
                            new ArrayList<String>(Config.COMMON.gravityImmuneEntities.get()))
                    .setDefaultValue(List.of())
                    .setTooltip(text("endless_gravity.config.gravityImmuneEntities.tooltip"))
                    .setSaveConsumer(list -> Config.COMMON.gravityImmuneEntities.set(list))
                    .build();
            gameplay.add(immune);

            SubCategoryBuilder potions = group(entry, "endless_gravity.config.group.potions");
            addToggle(potions, entry, "endless_gravity.config.enablePotionGravity", Config.COMMON.enablePotionGravity, true);
            addDouble(potions, entry, "endless_gravity.config.weightlessnessGravityOffset", Config.COMMON.weightlessnessGravityOffset,
                    0.0, 0.07, 0.05, TEXT_WHITE);
            addDouble(potions, entry, "endless_gravity.config.heavinessGravityOffset", Config.COMMON.heavinessGravityOffset,
                    0.0, 0.07, 0.02, TEXT_GRAY);
            gameplay.add(potions.build());
            cat.addEntry(gameplay.build());

            SubCategoryBuilder client = group(entry, "endless_gravity.config.group.client");
            client.add(entry.startTextDescription(text("endless_gravity.config.group.client.help")).build());
            SubCategoryBuilder gravity = group(entry, "endless_gravity.config.group.gravity");
            addDouble(gravity, entry, "endless_gravity.config.particleGravityOffset", Config.COMMON.particleGravityOffset,
                    0.0, 0.5, 0.1, TEXT_WHITE);
            client.add(gravity.build());

            SubCategoryBuilder hud = group(entry, "endless_gravity.config.group.hud");
            addToggle(hud, entry, "endless_gravity.config.enableGravityHud", Config.COMMON.enableGravityHud, false);
            client.add(hud.build());

            SubCategoryBuilder audio = group(entry, "endless_gravity.config.group.audio");
            addToggle(audio, entry, "endless_gravity.config.enableLowPassFilter", Config.COMMON.enableLowPassFilter, true);
            addDouble(audio, entry, "endless_gravity.config.lowPassGain", Config.COMMON.lowPassGain,
                    0.0, 1.0, 0.35, TEXT_WHITE);
            addDouble(audio, entry, "endless_gravity.config.lowPassGainHF", Config.COMMON.lowPassGainHF,
                    0.0, 1.0, 0.25, TEXT_GRAY);
            client.add(audio.build());
            cat.addEntry(client.build());

            if (sableLoaded) {
                SubCategoryBuilder sable = group(entry, "endless_gravity.config.group.sable");
                sable.add(entry.startTextDescription(text("endless_gravity.config.group.sable.help")).build());
                SubCategoryBuilder sableEnd = group(entry, "endless_gravity.config.group.sable_end");
                sableEnd.add(entry.startTextDescription(colored("endless_gravity.config.restartWarning", TEXT_AMBER)).build());
                addToggle(sableEnd, entry, "endless_gravity.config.endSableGravity", Config.COMMON.endSableGravity, true);
                addDouble(sableEnd, entry, "endless_gravity.config.sableGravityY", Config.COMMON.sableGravityY,
                        -20.0, 0.0, -4.0, TEXT_WHITE);
                addDouble(sableEnd, entry, "endless_gravity.config.sablePressure", Config.COMMON.sablePressure,
                        0.0, 10.0, 0.0, TEXT_GRAY);
                addDouble(sableEnd, entry, "endless_gravity.config.sableDrag", Config.COMMON.sableDrag,
                        0.0, 10.0, 0.05, TEXT_GRAY);
                addIntSlider(sableEnd, entry, "endless_gravity.config.sableDatapackPriority", Config.COMMON.sableDatapackPriority,
                        1, 9999, 9999, TEXT_GRAY);
                sable.add(sableEnd.build());

                SubCategoryBuilder sableOverworld = group(entry, "endless_gravity.config.group.sable_overworld");
                sableOverworld.add(entry.startTextDescription(colored("endless_gravity.config.restartWarning", TEXT_AMBER)).build());
                addToggle(sableOverworld, entry, "endless_gravity.config.sableAtmosphereEnabled",
                        Config.COMMON.enableAtmosphere, true);
                addDouble(sableOverworld, entry, "endless_gravity.config.atmosphereGravityMax", Config.COMMON.atmosphereGravityMax,
                        0.0, 0.1, 0.075, TEXT_WHITE);
                addDouble(sableOverworld, entry, "endless_gravity.config.noFallDamageAltitude", Config.COMMON.noFallDamageAltitude,
                        64.0, 6000.0, 4500.0, TEXT_WHITE);
                addDouble(sableOverworld, entry, "endless_gravity.config.atmosphereMuffleGain", Config.COMMON.atmosphereMuffleGain,
                        0.0, 1.0, 0.01, TEXT_WHITE);
                addDouble(sableOverworld, entry, "endless_gravity.config.atmosphereMuffleGainHF", Config.COMMON.atmosphereMuffleGainHF,
                        0.0, 1.0, 0.005, TEXT_GRAY);
                addToggle(sableOverworld, entry, "endless_gravity.config.overworldSableGravity", Config.COMMON.overworldSableGravity, true);
                addIntSlider(sableOverworld, entry, "endless_gravity.config.overworldSableDatapackPriority", Config.COMMON.overworldSableDatapackPriority,
                        1, 9999, 9999, TEXT_GRAY);
                sable.add(sableOverworld.build());

                SubCategoryBuilder fallbackCurve = entry.startSubCategory(
                                text("endless_gravity.config.sableAtmosphereCurve"))
                        .setExpanded(false)
                        .setTooltip(text("endless_gravity.config.atmosphereLayers.tooltip"));
                addGlobalLayers(fallbackCurve, entry);
                sable.add(fallbackCurve.build());
                cat.addEntry(sable.build());
            }
        }

        private void addGlobalLayers(SubCategoryBuilder layersSub, ConfigEntryBuilder entry) {
            for (int i = 0; i < AtmosphereLayers.LAYER_COUNT; i++) {
                final int index = i;
                AtmosphereLayers.Layer layer = draft.globalLayers.get(index);
                SubCategoryBuilder layerSub = entry.startSubCategory(
                                colored("endless_gravity.config.layerShort", TEXT_GRAY, index + 1))
                        .setExpanded(false);
                layerSub.add(entry.startDoubleField(text("endless_gravity.config.columnAltitude"), layer.altitude())
                        .setMin(-64.0).setMax(6000.0)
                        .setDefaultValue(AtmosphereLayers.defaults().get(index).altitude())
                        .setSaveConsumer(value -> draft.globalLayers.set(index,
                                new AtmosphereLayers.Layer(value, draft.globalLayers.get(index).pressure())))
                        .build());
                layerSub.add(entry.startDoubleField(text("endless_gravity.config.columnPressure"), layer.pressure())
                        .setMin(0.0).setMax(2.0)
                        .setDefaultValue(AtmosphereLayers.defaults().get(index).pressure())
                        .setSaveConsumer(value -> draft.globalLayers.set(index,
                                new AtmosphereLayers.Layer(draft.globalLayers.get(index).altitude(), value)))
                        .build());
                layersSub.add(layerSub.build());
            }
        }

        private void addToggle(SubCategoryBuilder group, ConfigEntryBuilder entry, String key,
                               ModConfigSpec.BooleanValue value, boolean defaultValue) {
            group.add(entry.startBooleanToggle(text(key), value.get())
                    .setDefaultValue(defaultValue)
                    .setSaveConsumer(value::set)
                    .setYesNoTextSupplier(v -> colored("endless_gravity.config." + v, v ? TEXT_GREEN : TEXT_GRAY))
                    .build());
        }

        private void addDouble(SubCategoryBuilder group, ConfigEntryBuilder entry, String key,
                               ModConfigSpec.DoubleValue value, double min, double max, double defaultValue, int color) {
            group.add(entry.startDoubleField(colored(key, color), value.get())
                    .setMin(min).setMax(max).setDefaultValue(defaultValue)
                    .setSaveConsumer(value::set)
                    .build());
        }

        private void addDouble(SubCategoryBuilder group, ConfigEntryBuilder entry, String key,
                               ModConfigSpec.DoubleValue value, double min, double max, double defaultValue, int color,
                               String tooltipKey) {
            group.add(entry.startDoubleField(colored(key, color), value.get())
                    .setMin(min).setMax(max).setDefaultValue(defaultValue)
                    .setTooltip(text(tooltipKey))
                    .setSaveConsumer(value::set)
                    .build());
        }

        private void addIntSlider(SubCategoryBuilder group, ConfigEntryBuilder entry, String key,
                                  ModConfigSpec.IntValue value, int min, int max, int defaultValue, int color) {
            group.add(entry.startIntSlider(colored(key, color), value.get(), min, max)
                    .setDefaultValue(defaultValue)
                    .setSaveConsumer(value::set)
                    .build());
        }

        private void finishSave() {
            Config.COMMON.atmosphereLayers.set(AtmosphereLayers.serialize(draft.globalLayers));
            List<CustomDimConfig> savedProfiles = new ArrayList<>();
            for (DimensionDraft profile : draft.dimensionDrafts) {
                savedProfiles.add(profile.toConfig());
            }
            if (savedProfiles.size() > 512) {
                throw new IllegalArgumentException("At most 512 dimension profiles are supported");
            }
            Config.replaceCustomDimensions(savedProfiles, !ConfigSyncClient.isServerSyncActive());
            Config.saveAtmospherePresets(draft.atmospherePresets);
            if (sableLoaded && !ConfigSyncClient.isServerSyncActive()) {
                SableDatapackHandler.generateDatapack();
            }
            // NeoForge does not autosave; persist the player's edits explicitly. Skip while a remote
            // server's synced config is applied, so we never overwrite the player's own config file.
            if (!ConfigSyncClient.isServerSyncActive()) {
                Config.COMMON_SPEC.save();
            }
        }

        private static SubCategoryBuilder group(ConfigEntryBuilder entry, String key) {
            return entry.startSubCategory(colored(key, TEXT_BLUE)).setExpanded(false);
        }

        private static ConfigCategory category(ConfigBuilder builder, String key) {
            return builder.getOrCreateCategory(text(key));
        }

        private static Component text(String key, Object... args) {
            return Component.translatable(key, args);
        }

        private static Component colored(String key, int color, Object... args) {
            return Component.translatable(key, args).withColor(color);
        }
    }
}
