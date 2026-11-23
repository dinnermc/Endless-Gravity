package dinner.dev.endless_gravity.client;

import dinner.dev.endless_gravity.AtmosphereLayers;
import dinner.dev.endless_gravity.AtmospherePreset;
import dinner.dev.endless_gravity.Config;
import dinner.dev.endless_gravity.network.ConfigSync;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;

/**
 * Native, zero-flicker two-pane configuration screen for Endless Gravity (Fabric).
 *
 * <p>All edits are buffered inside {@link EditorDraft} until Save is pressed, so Cancel
 * discards cleanly. Switching modes, dragging atmosphere points, adding/removing layers,
 * or applying presets updates widgets in place without resetting scroll or closing panels.
 */
public final class EndlessGravityConfigScreen extends Screen {

    // Palette (minimalist black/gray)
    private static final int BG_HEADER = 0xE60B0B0B;
    private static final int BG_SIDEBAR = 0xCC121212;
    private static final int BG_PANEL = 0xC4141414;
    private static final int BG_CARD = 0xB81C1C1C;
    private static final int BG_ROW_SELECTED = 0xFF2B2B2B;
    private static final int BG_ROW_HOVER = 0x772B2B2B;
    private static final int BORDER_SUBTLE = 0xFF323232;
    private static final int ACCENT_LIGHT = 0xFFE6E6E6;
    private static final int ACCENT_GREEN = 0xFF8AB896;
    private static final int ACCENT_AMBER = 0xFFC9A66B;
    private static final int ACCENT_RED = 0xFFC97373;
    private static final int TEXT_WHITE = 0xFFF2F2F2;
    private static final int TEXT_MUTED = 0xFFA0A0A0;
    private static final int TEXT_DIM = 0xFF6E6E6E;

    private static final int HEADER_H = 34;
    private static final int FOOTER_H = 36;
    private static final double CURVE_MIN_ALT = -2048.0;
    private static final double CURVE_MAX_ALT = 30000.0;
    private static final double VANILLA_GRAVITY = 0.08;

    private enum Tab { DIMENSIONS, GENERAL }
    private enum DimSubView { EDITOR, ADD_DIMENSION, PRESETS }

    private enum FallDamageMode {
        NORMAL, DISABLED, PROPORTIONAL;

        static FallDamageMode fromInt(int value) {
            return switch (value) {
                case 0 -> NORMAL;
                case 2 -> PROPORTIONAL;
                default -> DISABLED;
            };
        }
    }

    private enum HudPosition {
        TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT;

        static HudPosition fromInt(int value) {
            return switch (value) {
                case 1 -> TOP_RIGHT;
                case 2 -> BOTTOM_LEFT;
                case 3 -> BOTTOM_RIGHT;
                default -> TOP_LEFT;
            };
        }
    }

    public static Screen create(Screen parent) {
        return new EndlessGravityConfigScreen(parent);
    }

    private final Screen parent;
    private final EditorDraft draft;

    private Tab activeTab = Tab.DIMENSIONS;
    private DimSubView dimSubView = DimSubView.EDITOR;
    private String selectedDimId = "";

    private int sidebarX, sidebarY, sidebarW, sidebarH;
    private int contentX, contentY, contentW, contentH;

    private double leftScroll = 0.0;
    private double rightScroll = 0.0;
    private int rightContentHeight = 0;

    private Button tabDimsBtn;
    private Button tabGeneralBtn;
    private Button addDimBtn;
    private Button undoRemoveBtn;
    private EditBox sidebarSearchBox;
    private String sidebarFilter = "";

    private final List<PanelItem> panelItems = new ArrayList<>();
    private final List<LayerRowBoxes> activeLayerBoxes = new ArrayList<>();
    private AbstractWidget focusedPanelWidget = null;
    private CurveCanvas activeCanvas = null;
    private boolean suppressFieldCallbacks = false;

    private Component deferredTooltip = null;
    private int deferredTooltipX, deferredTooltipY;

    private Component statusText = Component.empty();
    private int statusColor = TEXT_MUTED;

    private String pendingDimId = "";
    private Config.CustomDimConfig.Mode pendingDimMode = Config.CustomDimConfig.Mode.FIXED_GRAVITY;
    private String pendingPresetName = "";
    private String pendingImportJson = "";
    private String pendingImmuneId = "";

    private EndlessGravityConfigScreen(Screen parent) {
        super(Component.translatable("endless_gravity.config.title"));
        this.parent = parent;
        this.draft = new EditorDraft();
        if (!draft.profiles.isEmpty()) {
            this.selectedDimId = draft.profiles.get(0).id;
        }
        if (Config.isServerSyncActive()) {
            setStatus(Component.translatable("endless_gravity.config.serverSyncNotice"), ACCENT_AMBER);
        }
    }

    private DimensionDraft selectedProfile() {
        for (DimensionDraft p : draft.profiles) {
            if (p.id.equals(selectedDimId)) return p;
        }
        if (!draft.profiles.isEmpty()) {
            selectedDimId = draft.profiles.get(0).id;
            return draft.profiles.get(0);
        }
        return null;
    }

    private void setStatus(Component msg, int color) {
        this.statusText = msg;
        this.statusColor = color;
    }

    @Override
    protected void init() {
        clearWidgets();
        computeLayout();

        int tabY = 7;
        int tabW = 96;
        int tabGap = 6;
        int totalTabsW = tabW * 2 + tabGap;
        int tabStartX = Math.max(150, this.width - totalTabsW - 12);

        tabDimsBtn = addRenderableWidget(Button.builder(
                Component.translatable("endless_gravity.config.section.dimensions"),
                b -> switchTab(Tab.DIMENSIONS)).bounds(tabStartX, tabY, tabW, 20).build());
        tabGeneralBtn = addRenderableWidget(Button.builder(
                Component.translatable("endless_gravity.config.section.general"),
                b -> switchTab(Tab.GENERAL)).bounds(tabStartX + tabW + tabGap, tabY, tabW, 20).build());

        sidebarSearchBox = addRenderableWidget(new EditBox(this.font,
                sidebarX + 6, sidebarY + 20, Math.max(40, sidebarW - 12), 16,
                Component.translatable("endless_gravity.config.searchDimensions")));
        sidebarSearchBox.setMaxLength(64);
        sidebarSearchBox.setHint(Component.translatable("endless_gravity.config.searchDimensions"));
        sidebarSearchBox.setValue(sidebarFilter);
        sidebarSearchBox.setResponder(v -> {
            sidebarFilter = v == null ? "" : v.trim().toLowerCase(Locale.ROOT);
            leftScroll = 0.0;
        });

        int sbBtnY = sidebarY + sidebarH - 24;
        addDimBtn = addRenderableWidget(Button.builder(
                Component.translatable("endless_gravity.config.addDimensionShort"),
                b -> {
                    dimSubView = DimSubView.ADD_DIMENSION;
                    pendingDimId = "";
                    rebuildRightPanel(false);
                }).bounds(sidebarX + 6, sbBtnY, sidebarW - 12, 20).build());
        undoRemoveBtn = addRenderableWidget(Button.builder(
                Component.translatable("endless_gravity.config.undoShort"),
                b -> restoreLastRemoved()).bounds(sidebarX + 6, sbBtnY - 24, sidebarW - 12, 20).build());

        int footerY = this.height - FOOTER_H + 8;
        int btnW = 96;
        addRenderableWidget(Button.builder(
                Component.translatable("gui.cancel"),
                b -> onClose()).bounds(this.width - btnW * 2 - 18, footerY, btnW, 20).build());
        addRenderableWidget(Button.builder(
                Component.translatable("endless_gravity.config.saveAndClose"),
                b -> {
                    saveAll();
                    onClose();
                }).bounds(this.width - btnW - 10, footerY, btnW, 20).build());

        updateTabChrome();
        rebuildRightPanel(true);
    }

    private void computeLayout() {
        int bodyTop = HEADER_H + 4;
        int bodyH = Math.max(80, this.height - HEADER_H - FOOTER_H - 8);
        if (activeTab == Tab.DIMENSIONS) {
            sidebarX = 8;
            sidebarY = bodyTop;
            sidebarW = Math.max(145, Math.min(195, this.width / 4));
            sidebarH = bodyH;
            contentX = sidebarX + sidebarW + 8;
            contentY = bodyTop;
            contentW = Math.max(200, this.width - contentX - 8);
            contentH = bodyH;
        } else {
            sidebarX = 0;
            sidebarY = bodyTop;
            sidebarW = 0;
            sidebarH = bodyH;
            int maxW = Math.min(640, this.width - 24);
            contentX = (this.width - maxW) / 2;
            contentY = bodyTop;
            contentW = maxW;
            contentH = bodyH;
        }
    }

    private void switchTab(Tab next) {
        if (activeTab == next) return;
        activeTab = next;
        dimSubView = DimSubView.EDITOR;
        rightScroll = 0.0;
        clearPanelFocus();
        computeLayout();
        updateTabChrome();
        rebuildRightPanel(false);
    }

    private void updateTabChrome() {
        tabDimsBtn.active = activeTab != Tab.DIMENSIONS;
        tabGeneralBtn.active = activeTab != Tab.GENERAL;

        boolean showSidebar = activeTab == Tab.DIMENSIONS;
        sidebarSearchBox.visible = showSidebar;
        sidebarSearchBox.active = showSidebar;
        addDimBtn.visible = showSidebar;
        addDimBtn.active = showSidebar;
        if (showSidebar) {
            sidebarSearchBox.setX(sidebarX + 6);
            sidebarSearchBox.setY(sidebarY + 20);
            sidebarSearchBox.setWidth(Math.max(40, sidebarW - 12));
            int sbBtnY = sidebarY + sidebarH - 24;
            addDimBtn.setX(sidebarX + 6);
            addDimBtn.setY(sbBtnY);
            addDimBtn.setWidth(sidebarW - 12);
            boolean hasUndo = !draft.removedProfiles.isEmpty();
            undoRemoveBtn.visible = hasUndo;
            undoRemoveBtn.active = hasUndo;
            undoRemoveBtn.setX(sidebarX + 6);
            undoRemoveBtn.setY(sbBtnY - 24);
            undoRemoveBtn.setWidth(sidebarW - 12);
            if (hasUndo) {
                DimensionDraft last = draft.removedProfiles.get(draft.removedProfiles.size() - 1);
                undoRemoveBtn.setTooltip(Tooltip.create(
                        Component.translatable("endless_gravity.config.dimensionUndo", last.id)));
            }
        } else {
            undoRemoveBtn.visible = false;
            undoRemoveBtn.active = false;
        }
    }

    private void clearPanelFocus() {
        if (focusedPanelWidget != null) {
            focusedPanelWidget.setFocused(false);
            focusedPanelWidget = null;
        }
    }

    private void rebuildRightPanel(boolean keepScroll) {
        clearPanelFocus();
        panelItems.clear();
        activeLayerBoxes.clear();
        activeCanvas = null;

        int pad = 10;
        int innerW = contentW - pad * 2 - 8;
        int y = pad;

        if (activeTab == Tab.DIMENSIONS) {
            y = switch (dimSubView) {
                case ADD_DIMENSION -> buildAddDimensionPanel(pad, y, innerW);
                case PRESETS -> buildPresetsPanel(pad, y, innerW);
                case EDITOR -> buildDimensionEditorPanel(pad, y, innerW);
            };
        } else {
            y = buildGeneralPanel(pad, y, innerW);
        }

        rightContentHeight = y + 10;
        double maxScroll = Math.max(0.0, rightContentHeight - contentH);
        rightScroll = keepScroll ? AtmosphereLayers.clamp(rightScroll, 0.0, maxScroll) : 0.0;
        updateTabChrome();
        syncPanelWidgetPositions();
    }

    // ==================== DIMENSIONS TAB PANELS ====================

    private int buildDimensionEditorPanel(int x, int y, int w) {
        DimensionDraft profile = selectedProfile();
        if (profile == null) {
            panelItems.add(new TextItem(x, y + 8, w,
                    Component.translatable("endless_gravity.config.dimensionNoProfiles"), TEXT_MUTED));
            return y + 32;
        }

        int removeW = 62;
        int resetW = 54;
        int copyW = 68;
        int rightBtnsW = removeW + resetW + copyW + 8;
        panelItems.add(new SectionTitleItem(x, y + 4, Math.max(60, w - rightBtnsW - 8),
                Component.literal(profile.id), ACCENT_LIGHT));

        int bx = x + w - rightBtnsW;
        Button copyBtn = Button.builder(
                Component.translatable("endless_gravity.config.copyDatapackJson"),
                b -> {
                    Minecraft.getInstance().keyboardHandler.setClipboard(
                            Config.toPrettyDatapackJson(profile.toConfig()));
                    setStatus(Component.translatable("endless_gravity.config.copiedDatapackJson", profile.id), ACCENT_GREEN);
                }).bounds(bx, y, copyW, 20).build();
        copyBtn.setTooltip(Tooltip.create(Component.translatable("endless_gravity.config.copyDatapackJson.tooltip")));
        panelItems.add(new WidgetItem(copyBtn, bx, y));
        bx += copyW + 4;

        Button resetBtn = Button.builder(
                Component.translatable("endless_gravity.config.resetDefaults"),
                b -> resetDimensionProfile(profile)).bounds(bx, y, resetW, 20).build();
        resetBtn.setTooltip(Tooltip.create(Component.translatable("endless_gravity.config.resetDimension.tooltip")));
        panelItems.add(new WidgetItem(resetBtn, bx, y));
        bx += resetW + 4;

        Button removeBtn = Button.builder(
                Component.translatable("endless_gravity.config.dimensionRemoveButton").copy().withColor(ACCENT_RED),
                b -> removeProfile(profile)).bounds(bx, y, removeW, 20).build();
        removeBtn.setTooltip(Tooltip.create(Component.translatable("endless_gravity.config.dimensionRemoveButton.tooltip")));
        panelItems.add(new WidgetItem(removeBtn, bx, y));
        y += 28;

        int modeW = Math.min(110, (w - 140) / 2);
        Button atmBtn = Button.builder(
                Component.translatable("endless_gravity.config.dimensionMode.atmosphere"),
                b -> {
                    profile.mode = Config.CustomDimConfig.Mode.ATMOSPHERE;
                    rebuildRightPanel(false);
                }).bounds(x, y, modeW, 20).build();
        atmBtn.active = profile.mode != Config.CustomDimConfig.Mode.ATMOSPHERE;
        atmBtn.setTooltip(Tooltip.create(Component.translatable("endless_gravity.config.dimensionMode.atmosphere.tooltip")));
        panelItems.add(new WidgetItem(atmBtn, x, y));

        Button fixBtn = Button.builder(
                Component.translatable("endless_gravity.config.dimensionMode.fixed_gravity"),
                b -> {
                    profile.mode = Config.CustomDimConfig.Mode.FIXED_GRAVITY;
                    rebuildRightPanel(false);
                }).bounds(x + modeW + 4, y, modeW, 20).build();
        fixBtn.active = profile.mode != Config.CustomDimConfig.Mode.FIXED_GRAVITY;
        fixBtn.setTooltip(Tooltip.create(Component.translatable("endless_gravity.config.dimensionMode.fixed_gravity.tooltip")));
        panelItems.add(new WidgetItem(fixBtn, x + modeW + 4, y));

        int fallX = x + modeW * 2 + 12;
        int fallW = Math.max(100, w - (modeW * 2 + 12));
        CycleButton<Config.CustomDimConfig.FallDamageMode> fallBtn = CycleButton.<Config.CustomDimConfig.FallDamageMode>builder(
                        m -> Component.translatable("endless_gravity.config.dimensionFallDamageMode."
                                + m.name().toLowerCase(Locale.ROOT)))
                .withValues(Config.CustomDimConfig.FallDamageMode.values())
                .withInitialValue(profile.fallDamageMode)
                .create(fallX, y, fallW, 20,
                        Component.translatable("endless_gravity.config.dimensionFallDamageMode"),
                        (btn, val) -> profile.fallDamageMode = val);
        fallBtn.setTooltip(Tooltip.create(Component.translatable("endless_gravity.config.dimensionFallDamageMode.tooltip")));
        panelItems.add(new WidgetItem(fallBtn, fallX, y));
        y += 28;

        y = addQuickPresetsRow(x, y, w, profile);
        y += 6;
        panelItems.add(new DividerItem(x, y, w));
        y += 10;

        if (profile.mode == Config.CustomDimConfig.Mode.FIXED_GRAVITY) {
            panelItems.add(new SectionTitleItem(x, y, w,
                    Component.translatable("endless_gravity.config.dimensionFixedGravity"), TEXT_WHITE));
            y += 14;
            panelItems.add(new TextItem(x, y, w,
                    Component.translatable("endless_gravity.config.dimensionFixedGravity.tooltip"), TEXT_MUTED));
            y += 16;

            int colGap = 12;
            int colW = (w - colGap) / 2;
            y = addDoublePair(x, y, colW, colGap,
                    "endless_gravity.config.playerGravityOffset", profile.playerOffset, -0.5, 0.5, 0.08, v -> profile.playerOffset = v,
                    "endless_gravity.config.mobGravityOffset", profile.mobOffset, -0.5, 0.5, 0.08, v -> profile.mobOffset = v);
            y = addDoublePair(x, y, colW, colGap,
                    "endless_gravity.config.itemGravityOffset", profile.itemOffset, -0.5, 0.5, 0.04, v -> profile.itemOffset = v,
                    "endless_gravity.config.arrowGravityOffset", profile.arrowOffset, -0.5, 0.5, 0.05, v -> profile.arrowOffset = v);
            y = addDoublePair(x, y, colW, colGap,
                    "endless_gravity.config.thrownGravityOffset", profile.thrownOffset, -0.5, 0.5, 0.03, v -> profile.thrownOffset = v,
                    "endless_gravity.config.blockGravityOffset", profile.blockOffset, -0.5, 0.5, 0.04, v -> profile.blockOffset = v);

            y += 6;
            panelItems.add(new DividerItem(x, y, w));
            y += 10;
            panelItems.add(new SectionTitleItem(x, y, w,
                    Component.translatable("endless_gravity.config.dimensionFixedAudio"), TEXT_WHITE));
            y += 14;
            panelItems.add(new TextItem(x, y, w,
                    Component.translatable("endless_gravity.config.dimensionFixedAudioHelp"), TEXT_MUTED));
            y += 16;
            y = addDoublePair(x, y, colW, colGap,
                    "endless_gravity.config.fixedMuffleGain", profile.fixedMuffleGain, 0.0, 1.0, 0.0, v -> profile.fixedMuffleGain = v,
                    "endless_gravity.config.fixedMuffleGainHF", profile.fixedMuffleGainHF, 0.0, 1.0, 0.0, v -> profile.fixedMuffleGainHF = v);
            return y + 4;
        }

        // ATMOSPHERE MODE
        panelItems.add(new SectionTitleItem(x, y, w,
                Component.translatable("endless_gravity.config.dimensionAtmosphere"), TEXT_WHITE));
        y += 16;

        int colGap = 12;
        int colW = (w - colGap) / 2;
        y = addDoublePair(x, y, colW, colGap,
                "endless_gravity.config.surfaceGravityOffset", profile.surfaceGravityOffset, -0.5, 0.5, VANILLA_GRAVITY,
                v -> profile.surfaceGravityOffset = v,
                "endless_gravity.config.atmosphereGravityMax", profile.atmosphereGravityMax, 0.0, 0.5, VANILLA_GRAVITY,
                v -> profile.atmosphereGravityMax = v);
        y = addDoublePair(x, y, colW, colGap,
                "endless_gravity.config.atmosphereBaseAltitude", profile.atmosphereBaseAltitude, CURVE_MIN_ALT, CURVE_MAX_ALT, 0.0,
                v -> profile.atmosphereBaseAltitude = v,
                "endless_gravity.config.noFallDamageAltitude", profile.noFallDamageAltitude, -1.0, CURVE_MAX_ALT, 0.0,
                v -> profile.noFallDamageAltitude = v);
        y = addDoublePair(x, y, colW, colGap,
                "endless_gravity.config.atmosphereMuffleGain", profile.atmosphereMuffleGain, 0.0, 1.0, 0.0,
                v -> profile.atmosphereMuffleGain = v,
                "endless_gravity.config.atmosphereMuffleGainHF", profile.atmosphereMuffleGainHF, 0.0, 1.0, 0.0,
                v -> profile.atmosphereMuffleGainHF = v);

        CycleButton<Boolean> nonLivingBtn = CycleButton.onOffBuilder(profile.atmosphereAffectNonLiving)
                .create(x, y, w, 20,
                        Component.translatable("endless_gravity.config.atmosphereAffectNonLiving"),
                        (btn, val) -> profile.atmosphereAffectNonLiving = val);
        nonLivingBtn.setTooltip(Tooltip.create(Component.translatable("endless_gravity.config.atmosphereAffectNonLiving.tooltip")));
        panelItems.add(new WidgetItem(nonLivingBtn, x, y));
        y += 28;

        int addLayerW = 112;
        panelItems.add(new SectionTitleItem(x, y + 4, w - addLayerW - 8,
                Component.translatable("endless_gravity.config.atmosphereLayers"), ACCENT_LIGHT));
        if (profile.layers.size() < Config.CustomDimConfig.MAX_ATMOSPHERE_LAYERS) {
            Button addLayerBtn = Button.builder(
                    Component.translatable("endless_gravity.config.addLayerShort"),
                    b -> addLayer(profile)).bounds(x + w - addLayerW, y, addLayerW, 20).build();
            addLayerBtn.setTooltip(Tooltip.create(Component.translatable("endless_gravity.config.curveAddLayer.tooltip")));
            panelItems.add(new WidgetItem(addLayerBtn, x + w - addLayerW, y));
        }
        y += 24;

        CurveCanvas canvas = new CurveCanvas(profile, x, y, w, 128, this::onCanvasLayerDragged);
        activeCanvas = canvas;
        panelItems.add(canvas);
        y += 134;

        panelItems.add(new TextItem(x, y, w,
                Component.translatable("endless_gravity.config.curveManualHelp",
                        profile.layers.size(), Config.CustomDimConfig.MAX_ATMOSPHERE_LAYERS), TEXT_MUTED));
        y += 14;

        boolean canRemove = profile.layers.size() > 2;
        int delW = canRemove ? 22 : 0;
        int indexW = 28;
        int labelAltW = 26;
        int labelPresW = 26;
        int boxW = Math.max(54, (w - indexW - labelAltW - labelPresW - delW - 22) / 2);

        for (int i = 0; i < profile.layers.size(); i++) {
            final int idx = i;
            AtmosphereLayers.Layer layer = profile.layers.get(i);
            int cx = x;
            panelItems.add(new TextItem(cx, y + 5, indexW,
                    Component.literal("#" + (i + 1)).withColor(TEXT_MUTED), TEXT_MUTED));
            cx += indexW;

            panelItems.add(new TextItem(cx, y + 5, labelAltW, Component.literal("Y:"), TEXT_DIM));
            cx += labelAltW;

            EditBox altBox = createNumericBox(cx, y, boxW, formatClean(layer.altitude()),
                    raw -> onLayerAltitudeTyped(profile, idx, raw));
            panelItems.add(new WidgetItem(altBox, cx, y));
            cx += boxW + 8;

            panelItems.add(new TextItem(cx, y + 5, labelPresW, Component.literal("P:"), TEXT_DIM));
            cx += labelPresW;

            EditBox presBox = createNumericBox(cx, y, boxW, formatClean(layer.pressure()),
                    raw -> onLayerPressureTyped(profile, idx, raw));
            panelItems.add(new WidgetItem(presBox, cx, y));
            cx += boxW + 6;

            activeLayerBoxes.add(new LayerRowBoxes(altBox, presBox));

            if (canRemove) {
                Button delBtn = Button.builder(Component.literal("×").withColor(ACCENT_RED),
                        b -> removeLayer(profile, idx)).bounds(cx, y, delW, 18).build();
                delBtn.setTooltip(Tooltip.create(Component.translatable("endless_gravity.config.curveRemoveLayer.tooltip")));
                panelItems.add(new WidgetItem(delBtn, cx, y));
            }
            y += 22;
        }
        return y;
    }

    private int addQuickPresetsRow(int x, int y, int w, DimensionDraft profile) {
        List<String> keys = profile.mode == Config.CustomDimConfig.Mode.FIXED_GRAVITY
                ? List.of("vanilla", "light_gravity", "lunar")
                : List.of("terrestrial", "gentle_atmosphere", "thin_atmosphere");
        boolean showPresetMgr = profile.mode == Config.CustomDimConfig.Mode.ATMOSPHERE;
        int slots = keys.size() + (showPresetMgr ? 1 : 0);
        int gap = 4;
        int btnW = Math.max(60, (w - gap * (slots - 1)) / slots);
        int cx = x;
        for (String key : keys) {
            Button btn = Button.builder(Component.translatable("endless_gravity.config.preset." + key),
                    b -> applyBuiltInPreset(profile, key)).bounds(cx, y, btnW, 20).build();
            btn.setTooltip(Tooltip.create(Component.translatable("endless_gravity.config.preset." + key + ".tooltip")));
            panelItems.add(new WidgetItem(btn, cx, y));
            cx += btnW + gap;
        }
        if (showPresetMgr) {
            Button mgrBtn = Button.builder(
                    Component.translatable("endless_gravity.config.presetsButton", draft.atmospherePresets.size()),
                    b -> {
                        dimSubView = DimSubView.PRESETS;
                        rebuildRightPanel(false);
                    }).bounds(cx, y, w - (cx - x), 20).build();
            mgrBtn.setTooltip(Tooltip.create(Component.translatable("endless_gravity.config.customPresetsHelp")));
            panelItems.add(new WidgetItem(mgrBtn, cx, y));
        }
        return y + 22;
    }

    private int buildAddDimensionPanel(int x, int y, int w) {
        int backW = 72;
        panelItems.add(new SectionTitleItem(x, y + 4, w - backW - 8,
                Component.translatable("endless_gravity.config.dimensionCreate"), ACCENT_GREEN));
        Button backBtn = Button.builder(Component.translatable("gui.back"),
                b -> {
                    dimSubView = DimSubView.EDITOR;
                    rebuildRightPanel(false);
                }).bounds(x + w - backW, y, backW, 20).build();
        panelItems.add(new WidgetItem(backBtn, x + w - backW, y));
        y += 28;

        panelItems.add(new TextItem(x, y, w,
                Component.translatable("endless_gravity.config.dimensionAdd"), TEXT_MUTED));
        y += 12;

        EditBox idBox = new EditBox(this.font, x, y, w, 20,
                Component.translatable("endless_gravity.config.dimensionAdd"));
        idBox.setMaxLength(128);
        idBox.setValue(pendingDimId);
        idBox.setResponder(v -> pendingDimId = v.trim());
        panelItems.add(new WidgetItem(idBox, x, y));
        y += 26;

        int halfW = (w - 6) / 2;
        Button atmBtn = Button.builder(
                Component.translatable("endless_gravity.config.dimensionMode.atmosphere"),
                b -> {
                    pendingDimMode = Config.CustomDimConfig.Mode.ATMOSPHERE;
                    rebuildRightPanel(true);
                }).bounds(x, y, halfW, 20).build();
        atmBtn.active = pendingDimMode != Config.CustomDimConfig.Mode.ATMOSPHERE;
        panelItems.add(new WidgetItem(atmBtn, x, y));

        Button fixBtn = Button.builder(
                Component.translatable("endless_gravity.config.dimensionMode.fixed_gravity"),
                b -> {
                    pendingDimMode = Config.CustomDimConfig.Mode.FIXED_GRAVITY;
                    rebuildRightPanel(true);
                }).bounds(x + halfW + 6, y, halfW, 20).build();
        fixBtn.active = pendingDimMode != Config.CustomDimConfig.Mode.FIXED_GRAVITY;
        panelItems.add(new WidgetItem(fixBtn, x + halfW + 6, y));
        y += 28;

        Button createBtn = Button.builder(
                Component.translatable("endless_gravity.config.dimensionCreateButton").copy().withColor(ACCENT_GREEN),
                b -> commitAddDimension()).bounds(x, y, w, 20).build();
        panelItems.add(new WidgetItem(createBtn, x, y));
        y += 30;

        List<String> available = discoverUnconfiguredDimensions();
        if (!available.isEmpty()) {
            panelItems.add(new DividerItem(x, y, w));
            y += 10;
            panelItems.add(new TextItem(x, y, w,
                    Component.translatable("endless_gravity.config.quickPickDimension"), TEXT_MUTED));
            y += 14;

            int cols = w >= 320 ? 2 : 1;
            int gap = 6;
            int colW = (w - gap * (cols - 1)) / cols;
            for (int i = 0; i < available.size(); i++) {
                String dimId = available.get(i);
                int col = i % cols;
                int row = i / cols;
                int bx = x + col * (colW + gap);
                int by = y + row * 24;
                Button pickBtn = Button.builder(Component.literal(dimId), b -> {
                    pendingDimId = dimId;
                    idBox.setValue(dimId);
                }).bounds(bx, by, colW, 20).build();
                pickBtn.setTooltip(Tooltip.create(Component.translatable("endless_gravity.config.quickPickDimension.tooltip")));
                panelItems.add(new WidgetItem(pickBtn, bx, by));
            }
            y += ((available.size() + cols - 1) / cols) * 24;
        }
        return y;
    }

    private int buildPresetsPanel(int x, int y, int w) {
        DimensionDraft profile = selectedProfile();
        int backW = 72;
        panelItems.add(new SectionTitleItem(x, y + 4, w - backW - 8,
                Component.translatable("endless_gravity.config.customPresets"), ACCENT_LIGHT));
        Button backBtn = Button.builder(Component.translatable("gui.back"),
                b -> {
                    dimSubView = DimSubView.EDITOR;
                    rebuildRightPanel(false);
                }).bounds(x + w - backW, y, backW, 20).build();
        panelItems.add(new WidgetItem(backBtn, x + w - backW, y));
        y += 28;

        if (profile != null) {
            panelItems.add(new TextItem(x, y, w,
                    Component.translatable("endless_gravity.config.saveCurrentAsPreset"), TEXT_MUTED));
            y += 12;
            int saveW = 86;
            EditBox nameBox = new EditBox(this.font, x, y, w - saveW - 6, 20,
                    Component.translatable("endless_gravity.config.savePresetName"));
            nameBox.setMaxLength(48);
            nameBox.setValue(pendingPresetName);
            nameBox.setResponder(v -> pendingPresetName = v);
            panelItems.add(new WidgetItem(nameBox, x, y));

            Button savePresetBtn = Button.builder(
                    Component.translatable("endless_gravity.config.saveShort").copy().withColor(ACCENT_GREEN),
                    b -> saveAtmospherePreset(profile)).bounds(x + w - saveW, y, saveW, 20).build();
            panelItems.add(new WidgetItem(savePresetBtn, x + w - saveW, y));
            y += 28;
        }

        panelItems.add(new DividerItem(x, y, w));
        y += 10;
        panelItems.add(new TextItem(x, y, w,
                Component.translatable("endless_gravity.config.savedPresets", draft.atmospherePresets.size()), TEXT_WHITE));
        y += 14;

        for (AtmospherePreset preset : List.copyOf(draft.atmospherePresets)) {
            int delW = 64;
            int applyW = w - delW - 6;
            Button applyBtn = Button.builder(Component.literal(preset.name()), b -> {
                if (profile != null) {
                    applyCustomPreset(profile, preset);
                    dimSubView = DimSubView.EDITOR;
                    rebuildRightPanel(false);
                }
            }).bounds(x, y, applyW, 20).build();
            applyBtn.setTooltip(Tooltip.create(Component.translatable("endless_gravity.config.myAtmospherePresets.tooltip")));
            panelItems.add(new WidgetItem(applyBtn, x, y));

            Button delBtn = Button.builder(
                    Component.translatable("endless_gravity.config.deletePresetButton").copy().withColor(ACCENT_RED),
                    b -> {
                        draft.atmospherePresets.remove(preset);
                        setStatus(Component.translatable("endless_gravity.config.presetStatus.deleted"), ACCENT_AMBER);
                        rebuildRightPanel(true);
                    }).bounds(x + applyW + 6, y, delW, 20).build();
            panelItems.add(new WidgetItem(delBtn, x + applyW + 6, y));
            y += 24;
        }

        y += 6;
        panelItems.add(new DividerItem(x, y, w));
        y += 10;
        panelItems.add(new TextItem(x, y, w,
                Component.translatable("endless_gravity.config.importPresetsJson"), TEXT_MUTED));
        y += 12;

        EditBox jsonBox = new EditBox(this.font, x, y, w, 20,
                Component.translatable("endless_gravity.config.importPresetsJson"));
        jsonBox.setMaxLength(16384);
        jsonBox.setValue(pendingImportJson);
        jsonBox.setResponder(v -> pendingImportJson = v);
        panelItems.add(new WidgetItem(jsonBox, x, y));
        y += 24;

        int halfW = (w - 6) / 2;
        Button impBtn = Button.builder(Component.translatable("endless_gravity.config.importPresets"),
                b -> importPresets()).bounds(x, y, halfW, 20).build();
        panelItems.add(new WidgetItem(impBtn, x, y));

        Button expBtn = Button.builder(Component.translatable("endless_gravity.config.exportPresets"),
                b -> {
                    Minecraft.getInstance().keyboardHandler.setClipboard(
                            AtmospherePreset.exportJson(draft.atmospherePresets));
                    setStatus(Component.translatable("endless_gravity.config.presetStatus.exported"), ACCENT_GREEN);
                }).bounds(x + halfW + 6, y, halfW, 20).build();
        panelItems.add(new WidgetItem(expBtn, x + halfW + 6, y));
        return y + 26;
    }

    // ==================== GENERAL TAB ====================

    private int buildGeneralPanel(int x, int y, int w) {
        int colGap = 12;
        int colW = (w - colGap) / 2;

        // 1. Dimension feature toggles
        int resetGenW = 96;
        panelItems.add(new SectionTitleItem(x, y + 4, w - resetGenW - 8,
                Component.translatable("endless_gravity.config.group.dimensionToggles"), ACCENT_LIGHT));
        Button resetGenBtn = Button.builder(
                Component.translatable("endless_gravity.config.resetDefaults"),
                b -> resetGeneralDefaults()).bounds(x + w - resetGenW, y, resetGenW, 18).build();
        resetGenBtn.setTooltip(Tooltip.create(Component.translatable("endless_gravity.config.resetGeneral.tooltip")));
        panelItems.add(new WidgetItem(resetGenBtn, x + w - resetGenW, y));
        y += 22;

        CycleButton<Boolean> endToggle = CycleButton.onOffBuilder(draft.endEntityGravity)
                .create(x, y, colW, 20,
                        Component.translatable("endless_gravity.config.endEntityGravity"),
                        (btn, val) -> draft.endEntityGravity = val);
        panelItems.add(new WidgetItem(endToggle, x, y));

        CycleButton<Boolean> atmToggle = CycleButton.onOffBuilder(draft.enableAtmosphere)
                .create(x + colW + colGap, y, colW, 20,
                        Component.translatable("endless_gravity.config.enableAtmosphere"),
                        (btn, val) -> draft.enableAtmosphere = val);
        panelItems.add(new WidgetItem(atmToggle, x + colW + colGap, y));
        y += 24;

        CycleButton<Boolean> owToggle = CycleButton.onOffBuilder(draft.overworldEntityGravity)
                .create(x, y, colW, 20,
                        Component.translatable("endless_gravity.config.overworldEntityGravity"),
                        (btn, val) -> draft.overworldEntityGravity = val);
        panelItems.add(new WidgetItem(owToggle, x, y));

        CycleButton<Boolean> dragToggle = CycleButton.onOffBuilder(draft.enableAtmosphericDrag)
                .create(x + colW + colGap, y, colW, 20,
                        Component.translatable("endless_gravity.config.enableAtmosphericDrag"),
                        (btn, val) -> draft.enableAtmosphericDrag = val);
        dragToggle.setTooltip(Tooltip.create(Component.translatable("endless_gravity.config.enableAtmosphericDrag.tooltip")));
        panelItems.add(new WidgetItem(dragToggle, x + colW + colGap, y));
        y += 28;

        // 2. Fall Damage
        panelItems.add(new DividerItem(x, y, w));
        y += 10;
        panelItems.add(new SectionTitleItem(x, y, w,
                Component.translatable("endless_gravity.config.fallDamageMode"), ACCENT_LIGHT));
        y += 16;

        CycleButton<FallDamageMode> playerFall = CycleButton.<FallDamageMode>builder(
                        m -> Component.translatable("endless_gravity.config.fallDamageMode." + m.name().toLowerCase(Locale.ROOT)))
                .withValues(FallDamageMode.values())
                .withInitialValue(draft.fallDamageMode)
                .create(x, y, colW, 20,
                        Component.translatable("endless_gravity.config.fallDamageMode"),
                        (btn, val) -> draft.fallDamageMode = val);
        panelItems.add(new WidgetItem(playerFall, x, y));

        CycleButton<FallDamageMode> mobFall = CycleButton.<FallDamageMode>builder(
                        m -> Component.translatable("endless_gravity.config.fallDamageMode." + m.name().toLowerCase(Locale.ROOT)))
                .withValues(FallDamageMode.values())
                .withInitialValue(draft.mobFallDamageMode)
                .create(x + colW + colGap, y, colW, 20,
                        Component.translatable("endless_gravity.config.mobFallDamageMode"),
                        (btn, val) -> draft.mobFallDamageMode = val);
        panelItems.add(new WidgetItem(mobFall, x + colW + colGap, y));
        y += 28;

        // 3. Potions
        panelItems.add(new DividerItem(x, y, w));
        y += 10;
        panelItems.add(new SectionTitleItem(x, y, w,
                Component.translatable("endless_gravity.config.group.potions"), ACCENT_LIGHT));
        y += 16;

        CycleButton<Boolean> potToggle = CycleButton.onOffBuilder(draft.enablePotionGravity)
                .create(x, y, w, 20,
                        Component.translatable("endless_gravity.config.enablePotionGravity"),
                        (btn, val) -> draft.enablePotionGravity = val);
        panelItems.add(new WidgetItem(potToggle, x, y));
        y += 26;

        y = addDoublePair(x, y, colW, colGap,
                "endless_gravity.config.weightlessnessGravityOffset", draft.weightlessnessOffset, 0.0, 0.07, 0.0,
                v -> draft.weightlessnessOffset = v,
                "endless_gravity.config.heavinessGravityOffset", draft.heavinessOffset, 0.0, 0.07, 0.0,
                v -> draft.heavinessOffset = v);

        // 4. HUD, Audio & Particles
        panelItems.add(new DividerItem(x, y, w));
        y += 10;
        panelItems.add(new SectionTitleItem(x, y, w,
                Component.translatable("endless_gravity.config.group.audio"), ACCENT_LIGHT));
        y += 16;

        CycleButton<Boolean> hudToggle = CycleButton.onOffBuilder(draft.enableGravityHud)
                .create(x, y, colW, 20,
                        Component.translatable("endless_gravity.config.enableGravityHud"),
                        (btn, val) -> draft.enableGravityHud = val);
        panelItems.add(new WidgetItem(hudToggle, x, y));

        CycleButton<Boolean> hudPresToggle = CycleButton.onOffBuilder(draft.hudShowPressure)
                .create(x + colW + colGap, y, colW, 20,
                        Component.translatable("endless_gravity.config.hudShowPressure"),
                        (btn, val) -> draft.hudShowPressure = val);
        panelItems.add(new WidgetItem(hudPresToggle, x + colW + colGap, y));
        y += 24;

        CycleButton<HudPosition> hudPosBtn = CycleButton.<HudPosition>builder(
                        p -> Component.translatable("endless_gravity.config.hudPosition." + p.name().toLowerCase(Locale.ROOT)))
                .withValues(HudPosition.values())
                .withInitialValue(draft.hudPosition)
                .create(x, y, colW, 20,
                        Component.translatable("endless_gravity.config.hudPosition"),
                        (btn, val) -> draft.hudPosition = val);
        panelItems.add(new WidgetItem(hudPosBtn, x, y));

        CycleButton<Boolean> hudCompactToggle = CycleButton.onOffBuilder(draft.hudCompact)
                .create(x + colW + colGap, y, colW, 20,
                        Component.translatable("endless_gravity.config.hudCompact"),
                        (btn, val) -> draft.hudCompact = val);
        hudCompactToggle.setTooltip(Tooltip.create(Component.translatable("endless_gravity.config.hudCompact.tooltip")));
        panelItems.add(new WidgetItem(hudCompactToggle, x + colW + colGap, y));
        y += 24;

        CycleButton<Boolean> lpToggle = CycleButton.onOffBuilder(draft.enableLowPassFilter)
                .create(x, y, w, 20,
                        Component.translatable("endless_gravity.config.enableLowPassFilter"),
                        (btn, val) -> draft.enableLowPassFilter = val);
        panelItems.add(new WidgetItem(lpToggle, x, y));
        y += 26;

        y = addDoublePair(x, y, colW, colGap,
                "endless_gravity.config.lowPassGain", draft.lowPassGain, 0.0, 1.0, 0.0,
                v -> draft.lowPassGain = v,
                "endless_gravity.config.lowPassGainHF", draft.lowPassGainHF, 0.0, 1.0, 0.0,
                v -> draft.lowPassGainHF = v);

        y = addSingleDouble(x, y, colW,
                "endless_gravity.config.particleGravityOffset", draft.particleGravityOffset, 0.0, 0.5, 0.0,
                v -> draft.particleGravityOffset = v);

        // 5. Immune Entities
        panelItems.add(new DividerItem(x, y, w));
        y += 10;
        panelItems.add(new SectionTitleItem(x, y, w,
                Component.translatable("endless_gravity.config.gravityImmuneEntities"), ACCENT_LIGHT));
        y += 14;
        panelItems.add(new TextItem(x, y, w,
                Component.translatable("endless_gravity.config.gravityImmuneEntities.tooltip"), TEXT_MUTED));
        y += 16;

        int addW = 70;
        EditBox immuneBox = new EditBox(this.font, x, y, w - addW - 6, 20,
                Component.translatable("endless_gravity.config.gravityImmuneEntities"));
        immuneBox.setMaxLength(128);
        immuneBox.setValue(pendingImmuneId);
        immuneBox.setResponder(v -> pendingImmuneId = v.trim());
        panelItems.add(new WidgetItem(immuneBox, x, y));

        Button addImmBtn = Button.builder(Component.literal("+ Add").withColor(ACCENT_GREEN), b -> {
            String trimmed = pendingImmuneId.trim();
            if (!trimmed.isEmpty() && ResourceLocation.tryParse(trimmed) != null
                    && !draft.gravityImmuneEntities.contains(trimmed)) {
                draft.gravityImmuneEntities.add(trimmed);
                pendingImmuneId = "";
                rebuildRightPanel(true);
            }
        }).bounds(x + w - addW, y, addW, 20).build();
        panelItems.add(new WidgetItem(addImmBtn, x + w - addW, y));
        y += 26;

        for (int i = 0; i < draft.gravityImmuneEntities.size(); i++) {
            final int idx = i;
            String entityId = draft.gravityImmuneEntities.get(i);
            panelItems.add(new TextItem(x + 4, y + 5, w - 34, Component.literal(entityId), TEXT_WHITE));
            Button delBtn = Button.builder(Component.literal("×").withColor(ACCENT_RED), b -> {
                draft.gravityImmuneEntities.remove(idx);
                rebuildRightPanel(true);
            }).bounds(x + w - 24, y, 24, 18).build();
            panelItems.add(new WidgetItem(delBtn, x + w - 24, y));
            y += 22;
        }
        return y;
    }

    // ==================== FIELD BUILDERS ====================

    private int addDoublePair(int x, int y, int colW, int gap,
                              String keyA, double valA, double minA, double maxA, double baseG_A, DoubleConsumer setA,
                              String keyB, double valB, double minB, double maxB, double baseG_B, DoubleConsumer setB) {
        addFieldCell(x, y, colW, keyA, valA, minA, maxA, baseG_A, setA);
        addFieldCell(x + colW + gap, y, colW, keyB, valB, minB, maxB, baseG_B, setB);
        return y + 38;
    }

    private int addSingleDouble(int x, int y, int colW,
                                String key, double val, double min, double max, double baseG, DoubleConsumer setter) {
        addFieldCell(x, y, colW, key, val, min, max, baseG, setter);
        return y + 38;
    }

    private void addFieldCell(int x, int y, int w, String key, double initial,
                              double min, double max, double baseG, DoubleConsumer setter) {
        int badgeW = baseG > 0.0 ? 44 : 0;
        panelItems.add(new TextItem(x, y, w - badgeW, Component.translatable(key), TEXT_MUTED));
        GravityBadgeItem badge = null;
        if (baseG > 0.0) {
            badge = new GravityBadgeItem(x + w - badgeW, y, badgeW, baseG, initial);
            panelItems.add(badge);
        }
        final GravityBadgeItem finalBadge = badge;
        EditBox box = new EditBox(this.font, x, y + 12, w, 18, Component.translatable(key));
        box.setMaxLength(20);
        box.setValue(formatClean(initial));
        String tooltipKey = key + ".tooltip";
        if (Language.getInstance().has(tooltipKey)) {
            box.setTooltip(Tooltip.create(Component.translatable(tooltipKey)));
        }
        box.setResponder(raw -> {
            if (suppressFieldCallbacks) return;
            try {
                double parsed = Double.parseDouble(raw.trim());
                if (Double.isFinite(parsed) && parsed >= min && parsed <= max) {
                    box.setTextColor(0xE0E0E0);
                    setter.accept(parsed);
                    if (finalBadge != null) finalBadge.offset = parsed;
                } else {
                    box.setTextColor(ACCENT_RED);
                }
            } catch (NumberFormatException e) {
                box.setTextColor(ACCENT_RED);
            }
        });
        panelItems.add(new WidgetItem(box, x, y + 12));
    }

    private EditBox createNumericBox(int x, int y, int w, String initial, Consumer<String> responder) {
        EditBox box = new EditBox(this.font, x, y, w, 18, Component.empty());
        box.setMaxLength(16);
        box.setValue(initial);
        box.setResponder(raw -> {
            if (!suppressFieldCallbacks) responder.accept(raw);
        });
        return box;
    }

    // ==================== ACTIONS ====================

    private void onCanvasLayerDragged(int index, double altitude, double pressure) {
        if (index < 0 || index >= activeLayerBoxes.size()) return;
        LayerRowBoxes boxes = activeLayerBoxes.get(index);
        suppressFieldCallbacks = true;
        try {
            boxes.altBox.setValue(formatClean(Math.rint(altitude)));
            boxes.altBox.setTextColor(0xE0E0E0);
            boxes.presBox.setValue(formatClean(Math.round(pressure * 1000.0) / 1000.0));
            boxes.presBox.setTextColor(0xE0E0E0);
        } finally {
            suppressFieldCallbacks = false;
        }
    }

    private void onLayerAltitudeTyped(DimensionDraft profile, int index, String raw) {
        if (index < 0 || index >= profile.layers.size() || index >= activeLayerBoxes.size()) return;
        EditBox box = activeLayerBoxes.get(index).altBox;
        try {
            double val = Double.parseDouble(raw.trim());
            double min = index == 0 ? CURVE_MIN_ALT : profile.layers.get(index - 1).altitude() + 1.0;
            double max = index == profile.layers.size() - 1 ? CURVE_MAX_ALT : profile.layers.get(index + 1).altitude() - 1.0;
            if (Double.isFinite(val) && val >= min && val <= max) {
                box.setTextColor(0xE0E0E0);
                AtmosphereLayers.Layer old = profile.layers.get(index);
                profile.layers.set(index, new AtmosphereLayers.Layer(Math.rint(val), old.pressure()));
            } else {
                box.setTextColor(ACCENT_RED);
            }
        } catch (NumberFormatException e) {
            box.setTextColor(ACCENT_RED);
        }
    }

    private void onLayerPressureTyped(DimensionDraft profile, int index, String raw) {
        if (index < 0 || index >= profile.layers.size() || index >= activeLayerBoxes.size()) return;
        EditBox box = activeLayerBoxes.get(index).presBox;
        try {
            double val = Double.parseDouble(raw.trim());
            if (Double.isFinite(val) && val >= 0.0 && val <= 10.0) {
                box.setTextColor(0xE0E0E0);
                AtmosphereLayers.Layer old = profile.layers.get(index);
                profile.layers.set(index, new AtmosphereLayers.Layer(old.altitude(), val));
            } else {
                box.setTextColor(ACCENT_RED);
            }
        } catch (NumberFormatException e) {
            box.setTextColor(ACCENT_RED);
        }
    }

    private void addLayer(DimensionDraft profile) {
        if (profile.layers.size() >= Config.CustomDimConfig.MAX_ATMOSPHERE_LAYERS) return;
        double bestGap = 0.0;
        double altitude = 0.0;
        double prev = CURVE_MIN_ALT;
        for (int i = 0; i <= profile.layers.size(); i++) {
            double next = i == profile.layers.size() ? CURVE_MAX_ALT : profile.layers.get(i).altitude();
            double gap = next - prev;
            if (gap > bestGap) {
                bestGap = gap;
                altitude = Math.floor((prev + next) / 2.0);
            }
            prev = next;
        }
        if (bestGap < 2.0) return;
        profile.layers.add(new AtmosphereLayers.Layer(altitude, AtmosphereLayers.getPressure(profile.layers, altitude)));
        profile.layers.sort(Comparator.comparingDouble(AtmosphereLayers.Layer::altitude));
        rebuildRightPanel(true);
    }

    private void removeLayer(DimensionDraft profile, int index) {
        if (profile.layers.size() <= 2 || index < 0 || index >= profile.layers.size()) return;
        profile.layers.remove(index);
        rebuildRightPanel(true);
    }

    private void applyBuiltInPreset(DimensionDraft profile, String preset) {
        if (profile.mode == Config.CustomDimConfig.Mode.FIXED_GRAVITY) {
            switch (preset) {
                case "vanilla" -> setFixed(profile, 0, 0, 0, 0, 0, 0);
                case "light_gravity" -> setFixed(profile, 0.025, 0.02, 0.01, 0.015, 0.01, 0.02);
                case "lunar" -> setFixed(profile, 0.055, 0.025, 0.025, 0.03, 0.018, 0.035);
                default -> {}
            }
        } else {
            switch (preset) {
                case "terrestrial" -> {
                    profile.surfaceGravityOffset = 0.0;
                    profile.atmosphereGravityMax = 0.075;
                    profile.atmosphereBaseAltitude = 319;
                    profile.noFallDamageAltitude = 4500;
                    profile.atmosphereMuffleGain = 0.01;
                    profile.atmosphereMuffleGainHF = 0.005;
                    profile.layers.clear();
                    profile.layers.addAll(AtmosphereLayers.defaults());
                }
                case "gentle_atmosphere" -> {
                    profile.surfaceGravityOffset = 0.0;
                    profile.atmosphereGravityMax = 0.04;
                    profile.atmosphereBaseAltitude = 319;
                    profile.noFallDamageAltitude = -1;
                    profile.atmosphereMuffleGain = 0.15;
                    profile.atmosphereMuffleGainHF = 0.1;
                    profile.layers.clear();
                    profile.layers.addAll(AtmosphereLayers.defaults());
                }
                case "thin_atmosphere" -> {
                    profile.surfaceGravityOffset = 0.0;
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
                default -> {}
            }
        }
        rebuildRightPanel(true);
    }

    private void applyCustomPreset(DimensionDraft profile, AtmospherePreset preset) {
        profile.atmosphereGravityMax = preset.gravityMax();
        profile.atmosphereBaseAltitude = preset.startAltitude();
        profile.noFallDamageAltitude = preset.noFallDamageAbove() == null ? -1 : preset.noFallDamageAbove();
        profile.atmosphereMuffleGain = preset.muffleGain();
        profile.atmosphereMuffleGainHF = preset.muffleGainHF();
        profile.layers.clear();
        profile.layers.addAll(AtmosphereLayers.parseUncached(preset.layers()));
        setStatus(Component.translatable("endless_gravity.config.presetStatus.applied", preset.name()), ACCENT_GREEN);
    }

    private void saveAtmospherePreset(DimensionDraft profile) {
        String name = pendingPresetName.trim();
        if (name.isEmpty() || name.length() > 48) {
            setStatus(Component.translatable("endless_gravity.config.presetStatus.invalid_name"), ACCENT_RED);
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
            setStatus(Component.translatable("endless_gravity.config.presetStatus.invalid_preset"), ACCENT_RED);
            return;
        }
        int existing = -1;
        for (int i = 0; i < draft.atmospherePresets.size(); i++) {
            if (draft.atmospherePresets.get(i).name().equalsIgnoreCase(name)) {
                existing = i;
                break;
            }
        }
        if (existing >= 0) {
            draft.atmospherePresets.set(existing, preset);
        } else if (draft.atmospherePresets.size() >= AtmospherePreset.MAX_PRESETS) {
            setStatus(Component.translatable("endless_gravity.config.presetStatus.limit"), ACCENT_RED);
            return;
        } else {
            draft.atmospherePresets.add(preset);
        }
        pendingPresetName = "";
        setStatus(Component.translatable("endless_gravity.config.presetStatus.saved"), ACCENT_GREEN);
        rebuildRightPanel(true);
    }

    private void importPresets() {
        try {
            List<AtmospherePreset> imported = AtmospherePreset.parseImport(pendingImportJson);
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
                setStatus(Component.translatable("endless_gravity.config.presetStatus.limit"), ACCENT_RED);
                return;
            }
            draft.atmospherePresets.clear();
            draft.atmospherePresets.addAll(merged);
            pendingImportJson = "";
            setStatus(Component.translatable("endless_gravity.config.presetStatus.imported"), ACCENT_GREEN);
            rebuildRightPanel(true);
        } catch (RuntimeException e) {
            setStatus(Component.translatable("endless_gravity.config.presetStatus.invalid_json"), ACCENT_RED);
        }
    }

    private void commitAddDimension() {
        if (draft.profiles.size() >= Config.MAX_CUSTOM_DIMENSIONS) {
            setStatus(Component.translatable("endless_gravity.config.dimensionStatus.limit"), ACCENT_RED);
            return;
        }
        String raw = pendingDimId.trim();
        ResourceLocation parsed = raw.contains(":") ? ResourceLocation.tryParse(raw) : null;
        if (parsed == null) {
            setStatus(Component.translatable("endless_gravity.config.dimensionStatus.invalid"), ACCENT_RED);
            return;
        }
        String normalized = parsed.toString();
        if (draft.profiles.stream().anyMatch(p -> p.id.equals(normalized))) {
            setStatus(Component.translatable("endless_gravity.config.dimensionId.duplicate"), ACCENT_RED);
            return;
        }
        Config.CustomDimConfig base = new Config.CustomDimConfig(normalized, pendingDimMode,
                0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                Config.COMMON.atmosphereGravityMax.get(), AtmosphereLayers.GRAVITY_START_ALTITUDE,
                pendingDimMode == Config.CustomDimConfig.Mode.ATMOSPHERE ? Config.COMMON.noFallDamageAltitude.get() : null,
                Config.COMMON.atmosphereMuffleGain.get(), Config.COMMON.atmosphereMuffleGainHF.get(),
                List.copyOf(Config.COMMON.atmosphereLayers.get()));
        draft.profiles.add(new DimensionDraft(base));
        draft.profiles.sort((a, b) -> a.id.compareToIgnoreCase(b.id));
        selectedDimId = normalized;
        dimSubView = DimSubView.EDITOR;
        setStatus(Component.translatable("endless_gravity.config.dimensionStatus.added"), ACCENT_GREEN);
        rebuildRightPanel(false);
    }

    private void removeProfile(DimensionDraft profile) {
        draft.profiles.remove(profile);
        draft.removedProfiles.add(profile);
        if (selectedDimId.equals(profile.id)) {
            selectedDimId = draft.profiles.isEmpty() ? "" : draft.profiles.get(0).id;
        }
        rebuildRightPanel(false);
    }

    private void restoreLastRemoved() {
        if (draft.removedProfiles.isEmpty()) return;
        DimensionDraft last = draft.removedProfiles.remove(draft.removedProfiles.size() - 1);
        if (draft.profiles.stream().anyMatch(p -> p.id.equals(last.id))) {
            setStatus(Component.translatable("endless_gravity.config.dimensionStatus.duplicate"), ACCENT_RED);
            updateTabChrome();
            return;
        }
        draft.profiles.add(last);
        draft.profiles.sort((a, b) -> a.id.compareToIgnoreCase(b.id));
        selectedDimId = last.id;
        dimSubView = DimSubView.EDITOR;
        rebuildRightPanel(false);
    }

    private List<String> discoverUnconfiguredDimensions() {
        Set<String> ids = new LinkedHashSet<>();
        var mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.connection != null) {
            mc.player.connection.levels().forEach(key -> ids.add(key.location().toString()));
        }
        ids.add("minecraft:overworld");
        ids.add("minecraft:the_nether");
        ids.add("minecraft:the_end");
        Set<String> configured = new HashSet<>();
        draft.profiles.forEach(p -> configured.add(p.id));
        return ids.stream().filter(id -> !configured.contains(id)).sorted().limit(12).toList();
    }

    private static void setFixed(DimensionDraft p, double player, double mob, double item,
                                 double arrow, double thrown, double block) {
        p.playerOffset = player;
        p.mobOffset = mob;
        p.itemOffset = item;
        p.arrowOffset = arrow;
        p.thrownOffset = thrown;
        p.blockOffset = block;
    }

    private List<DimensionDraft> filteredProfiles() {
        if (sidebarFilter.isEmpty()) return draft.profiles;
        List<DimensionDraft> out = new ArrayList<>();
        for (DimensionDraft p : draft.profiles) {
            if (p.id.toLowerCase(Locale.ROOT).contains(sidebarFilter)) {
                out.add(p);
            }
        }
        return out;
    }

    private void resetDimensionProfile(DimensionDraft profile) {
        Config.CustomDimConfig def;
        if ("minecraft:overworld".equals(profile.id)) {
            def = Config.defaultOverworldProfile();
        } else if ("minecraft:the_end".equals(profile.id)) {
            def = Config.defaultEndProfile();
        } else {
            def = Config.defaultGenericProfile(profile.id, profile.mode);
        }
        DimensionDraft fresh = new DimensionDraft(def);
        profile.mode = fresh.mode;
        profile.playerOffset = fresh.playerOffset;
        profile.mobOffset = fresh.mobOffset;
        profile.itemOffset = fresh.itemOffset;
        profile.arrowOffset = fresh.arrowOffset;
        profile.thrownOffset = fresh.thrownOffset;
        profile.blockOffset = fresh.blockOffset;
        profile.atmosphereGravityMax = fresh.atmosphereGravityMax;
        profile.atmosphereBaseAltitude = fresh.atmosphereBaseAltitude;
        profile.noFallDamageAltitude = fresh.noFallDamageAltitude;
        profile.atmosphereMuffleGain = fresh.atmosphereMuffleGain;
        profile.atmosphereMuffleGainHF = fresh.atmosphereMuffleGainHF;
        profile.surfaceGravityOffset = fresh.surfaceGravityOffset;
        profile.atmosphereAffectNonLiving = fresh.atmosphereAffectNonLiving;
        profile.fallDamageMode = fresh.fallDamageMode;
        profile.fixedMuffleGain = fresh.fixedMuffleGain;
        profile.fixedMuffleGainHF = fresh.fixedMuffleGainHF;
        profile.layers.clear();
        profile.layers.addAll(fresh.layers);
        setStatus(Component.translatable("endless_gravity.config.resetDone"), ACCENT_GREEN);
        rebuildRightPanel(true);
    }

    private void resetGeneralDefaults() {
        draft.endEntityGravity = true;
        draft.enableAtmosphere = true;
        draft.overworldEntityGravity = true;
        draft.enableAtmosphericDrag = false;
        draft.fallDamageMode = FallDamageMode.fromInt(1);
        draft.mobFallDamageMode = FallDamageMode.fromInt(1);
        draft.enablePotionGravity = true;
        draft.weightlessnessOffset = 0.05;
        draft.heavinessOffset = 0.02;
        draft.enableGravityHud = false;
        draft.hudPosition = HudPosition.TOP_LEFT;
        draft.hudShowPressure = true;
        draft.hudCompact = false;
        draft.enableLowPassFilter = true;
        draft.lowPassGain = 0.35;
        draft.lowPassGainHF = 0.25;
        draft.particleGravityOffset = 0.1;
        setStatus(Component.translatable("endless_gravity.config.resetDone"), ACCENT_GREEN);
        rebuildRightPanel(true);
    }

    // ==================== SAVE ====================

    private void saveAll() {
        Config.saveAtmospherePresets(draft.atmospherePresets);

        Config.COMMON.enableGravityHud.set(draft.enableGravityHud);
        Config.COMMON.hudPosition.set(draft.hudPosition.ordinal());
        Config.COMMON.hudShowPressure.set(draft.hudShowPressure);
        Config.COMMON.hudCompact.set(draft.hudCompact);
        Config.COMMON.enableLowPassFilter.set(draft.enableLowPassFilter);
        Config.COMMON.lowPassGain.set(draft.lowPassGain);
        Config.COMMON.lowPassGainHF.set(draft.lowPassGainHF);
        Config.COMMON.particleGravityOffset.set(draft.particleGravityOffset);

        if (Config.isServerSyncActive()) {
            EndlessGravityClient.persistClientOnlyChanges();
            return;
        }

        Config.COMMON.endEntityGravity.set(draft.endEntityGravity);
        Config.COMMON.enableAtmosphere.set(draft.enableAtmosphere);
        Config.COMMON.enableAtmosphericDrag.set(draft.enableAtmosphericDrag);
        Config.COMMON.overworldEntityGravity.set(draft.overworldEntityGravity);
        Config.COMMON.fallDamageMode.set(draft.fallDamageMode.ordinal());
        Config.COMMON.mobFallDamageMode.set(draft.mobFallDamageMode.ordinal());
        Config.COMMON.enablePotionGravity.set(draft.enablePotionGravity);
        Config.COMMON.weightlessnessGravityOffset.set(draft.weightlessnessOffset);
        Config.COMMON.heavinessGravityOffset.set(draft.heavinessOffset);
        Config.COMMON.gravityImmuneEntities.set(new ArrayList<>(draft.gravityImmuneEntities));

        List<Config.CustomDimConfig> savedProfiles = new ArrayList<>();
        for (DimensionDraft p : draft.profiles) {
            savedProfiles.add(p.toConfig());
        }
        Config.saveCustomDimensions(savedProfiles);
        Config.save();

        var server = Minecraft.getInstance().getSingleplayerServer();
        if (server != null) {
            ConfigSync.broadcast(server);
        }
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    // ==================== RENDERING ====================

    private void syncPanelWidgetPositions() {
        int scrollInt = (int) Math.round(rightScroll);
        for (PanelItem item : panelItems) {
            item.syncPosition(contentX, contentY - scrollInt);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        deferredTooltip = null;
        this.renderBackground(g, mouseX, mouseY, delta);

        g.fill(0, 0, this.width, HEADER_H, BG_HEADER);
        g.fill(0, HEADER_H - 1, this.width, HEADER_H, BORDER_SUBTLE);
        g.drawString(this.font, this.title, 12, 13, TEXT_WHITE, true);

        if (activeTab == Tab.DIMENSIONS) {
            renderSidebar(g, mouseX, mouseY);
        }

        renderRightPanel(g, mouseX, mouseY, delta);

        int footerTop = this.height - FOOTER_H;
        g.fill(0, footerTop, this.width, this.height, BG_HEADER);
        g.fill(0, footerTop, this.width, footerTop + 1, BORDER_SUBTLE);
        if (!statusText.getString().isEmpty()) {
            int maxStatusW = Math.max(80, this.width - 230);
            String clipped = this.font.plainSubstrByWidth(statusText.getString(), maxStatusW);
            g.drawString(this.font, clipped, 12, footerTop + 14, statusColor, false);
        }

        for (var child : this.children()) {
            if (child instanceof Renderable renderable) {
                renderable.render(g, mouseX, mouseY, delta);
            }
        }

        if (deferredTooltip != null) {
            g.renderTooltip(this.font, deferredTooltip, deferredTooltipX, deferredTooltipY);
        }
    }

    private void renderSidebar(GuiGraphics g, int mouseX, int mouseY) {
        g.fill(sidebarX, sidebarY, sidebarX + sidebarW, sidebarY + sidebarH, BG_SIDEBAR);
        drawBorder(g, sidebarX, sidebarY, sidebarW, sidebarH, BORDER_SUBTLE);

        List<DimensionDraft> visible = filteredProfiles();
        int listTop = sidebarY + 42;
        int bottomReserve = draft.removedProfiles.isEmpty() ? 30 : 54;
        int listBottom = sidebarY + sidebarH - bottomReserve;
        int listH = Math.max(20, listBottom - listTop);

        g.drawString(this.font,
                Component.translatable("endless_gravity.config.dimensionExistingProfiles", visible.size()),
                sidebarX + 8, sidebarY + 6, TEXT_MUTED, false);

        int rowH = 24;
        int totalH = visible.size() * rowH;
        leftScroll = AtmosphereLayers.clamp(leftScroll, 0.0, Math.max(0.0, totalH - listH));

        g.enableScissor(sidebarX + 1, listTop, sidebarX + sidebarW - 1, listBottom);
        int scrollInt = (int) Math.round(leftScroll);
        for (int i = 0; i < visible.size(); i++) {
            DimensionDraft p = visible.get(i);
            int ry = listTop + i * rowH - scrollInt;
            if (ry + rowH < listTop || ry > listBottom) continue;

            boolean selected = dimSubView == DimSubView.EDITOR && p.id.equals(selectedDimId);
            boolean hovered = mouseX >= sidebarX + 2 && mouseX < sidebarX + sidebarW - 2
                    && mouseY >= Math.max(listTop, ry) && mouseY < Math.min(listBottom, ry + rowH);
            if (selected) {
                g.fill(sidebarX + 2, ry, sidebarX + sidebarW - 2, ry + rowH - 1, BG_ROW_SELECTED);
                g.fill(sidebarX + 2, ry, sidebarX + 4, ry + rowH - 1, ACCENT_LIGHT);
            } else if (hovered) {
                g.fill(sidebarX + 2, ry, sidebarX + sidebarW - 2, ry + rowH - 1, BG_ROW_HOVER);
            }

            boolean isAtm = p.mode == Config.CustomDimConfig.Mode.ATMOSPHERE;
            int badgeColor = isAtm ? ACCENT_GREEN : ACCENT_AMBER;
            String badgeText = isAtm ? "ATM" : "FIX";
            int badgeW = this.font.width(badgeText) + 4;
            int maxNameW = sidebarW - badgeW - 18;
            String shortId = p.id.startsWith("minecraft:") ? p.id.substring("minecraft:".length()) : p.id;
            String display = this.font.plainSubstrByWidth(shortId, maxNameW);
            g.drawString(this.font, display, sidebarX + 8, ry + 8, selected ? TEXT_WHITE : TEXT_MUTED, false);
            g.drawString(this.font, badgeText, sidebarX + sidebarW - badgeW - 4, ry + 8, badgeColor, false);

            if (hovered && !display.equals(p.id)) {
                deferredTooltip = Component.literal(p.id);
                deferredTooltipX = mouseX;
                deferredTooltipY = mouseY;
            }
        }
        g.disableScissor();

        if (totalH > listH) {
            drawScrollbar(g, sidebarX + sidebarW - 4, listTop, listH, totalH, leftScroll);
        }
    }

    private void renderRightPanel(GuiGraphics g, int mouseX, int mouseY, float delta) {
        g.fill(contentX, contentY, contentX + contentW, contentY + contentH, BG_PANEL);
        drawBorder(g, contentX, contentY, contentW, contentH, BORDER_SUBTLE);

        syncPanelWidgetPositions();

        boolean inPanel = mouseX >= contentX && mouseX < contentX + contentW
                && mouseY >= contentY && mouseY < contentY + contentH;
        int effMouseX = inPanel ? mouseX : -10000;
        int effMouseY = inPanel ? mouseY : -10000;

        g.enableScissor(contentX + 1, contentY + 1, contentX + contentW - 1, contentY + contentH - 1);
        int scrollInt = (int) Math.round(rightScroll);
        for (PanelItem item : panelItems) {
            item.render(g, contentX, contentY - scrollInt, effMouseX, effMouseY, delta);
        }
        g.disableScissor();

        if (rightContentHeight > contentH) {
            drawScrollbar(g, contentX + contentW - 5, contentY + 2, contentH - 4, rightContentHeight, rightScroll);
        }
    }

    private static void drawBorder(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + 1, color);
        g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y, x + 1, y + h, color);
        g.fill(x + w - 1, y, x + w, y + h, color);
    }

    private static void drawScrollbar(GuiGraphics g, int x, int y, int viewH, int totalH, double scroll) {
        g.fill(x, y, x + 3, y + viewH, 0x26FFFFFF);
        int thumbH = Math.max(18, (int) ((double) viewH * viewH / totalH));
        double frac = totalH > viewH ? scroll / (totalH - viewH) : 0.0;
        int thumbY = y + (int) Math.round(frac * (viewH - thumbH));
        g.fill(x, thumbY, x + 3, thumbY + thumbH, 0x88999999);
    }

    // ==================== INPUT HANDLING ====================

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            clearPanelFocus();
            return true;
        }

        if (activeTab == Tab.DIMENSIONS && button == 0) {
            int listTop = sidebarY + 42;
            int bottomReserve = draft.removedProfiles.isEmpty() ? 30 : 54;
            int listBottom = sidebarY + sidebarH - bottomReserve;
            if (mouseX >= sidebarX + 2 && mouseX < sidebarX + sidebarW - 2
                    && mouseY >= listTop && mouseY < listBottom) {
                List<DimensionDraft> visible = filteredProfiles();
                int idx = (int) ((mouseY - listTop + leftScroll) / 24);
                if (idx >= 0 && idx < visible.size()) {
                    String clickedId = visible.get(idx).id;
                    if (!clickedId.equals(selectedDimId) || dimSubView != DimSubView.EDITOR) {
                        selectedDimId = clickedId;
                        dimSubView = DimSubView.EDITOR;
                        rebuildRightPanel(false);
                    }
                    return true;
                }
            }
        }

        if (mouseX >= contentX && mouseX < contentX + contentW
                && mouseY >= contentY && mouseY < contentY + contentH) {
            syncPanelWidgetPositions();
            for (PanelItem item : panelItems) {
                if (item.mouseClicked(mouseX, mouseY, button)) {
                    if (item instanceof WidgetItem wi) {
                        if (focusedPanelWidget != wi.widget) {
                            clearPanelFocus();
                            focusedPanelWidget = wi.widget;
                            focusedPanelWidget.setFocused(true);
                        }
                    } else {
                        clearPanelFocus();
                    }
                    return true;
                }
            }
        }
        clearPanelFocus();
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (activeCanvas != null && activeCanvas.mouseDragged(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (activeCanvas != null && activeCanvas.mouseReleased(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (activeTab == Tab.DIMENSIONS
                && mouseX >= sidebarX && mouseX < sidebarX + sidebarW
                && mouseY >= sidebarY && mouseY < sidebarY + sidebarH) {
            int bottomReserve = draft.removedProfiles.isEmpty() ? 30 : 54;
            int listH = Math.max(20, sidebarH - 20 - bottomReserve);
            int totalH = draft.profiles.size() * 24;
            leftScroll = AtmosphereLayers.clamp(leftScroll - scrollY * 18.0, 0.0, Math.max(0.0, totalH - listH));
            return true;
        }
        if (mouseX >= contentX && mouseX < contentX + contentW
                && mouseY >= contentY && mouseY < contentY + contentH) {
            double maxScroll = Math.max(0.0, rightContentHeight - contentH);
            rightScroll = AtmosphereLayers.clamp(rightScroll - scrollY * 22.0, 0.0, maxScroll);
            syncPanelWidgetPositions();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (focusedPanelWidget != null && focusedPanelWidget.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (focusedPanelWidget != null && focusedPanelWidget.charTyped(codePoint, modifiers)) {
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    private static String formatClean(double v) {
        if (Math.abs(v - Math.rint(v)) < 1e-6) {
            return String.format(Locale.ROOT, "%.0f", v);
        }
        String s = String.format(Locale.ROOT, "%.4f", v);
        int end = s.length();
        while (end > 1 && s.charAt(end - 1) == '0') end--;
        if (s.charAt(end - 1) == '.') end--;
        return s.substring(0, end);
    }

    // ==================== PANEL ITEMS ====================

    private interface PanelItem {
        default void syncPosition(int originX, int originY) {}
        void render(GuiGraphics g, int originX, int originY, int mouseX, int mouseY, float delta);
        default boolean mouseClicked(double mouseX, double mouseY, int button) { return false; }
    }

    private record LayerRowBoxes(EditBox altBox, EditBox presBox) {}

    private static final class WidgetItem implements PanelItem {
        final AbstractWidget widget;
        final int relX, relY;

        WidgetItem(AbstractWidget widget, int relX, int relY) {
            this.widget = widget;
            this.relX = relX;
            this.relY = relY;
        }

        @Override
        public void syncPosition(int originX, int originY) {
            widget.setX(originX + relX);
            widget.setY(originY + relY);
        }

        @Override
        public void render(GuiGraphics g, int originX, int originY, int mouseX, int mouseY, float delta) {
            widget.render(g, mouseX, mouseY, delta);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            return widget.mouseClicked(mouseX, mouseY, button);
        }
    }

    private final class TextItem implements PanelItem {
        final int relX, relY, maxW, color;
        final Component text;

        TextItem(int relX, int relY, int maxW, Component text, int color) {
            this.relX = relX;
            this.relY = relY;
            this.maxW = maxW;
            this.text = text;
            this.color = color;
        }

        @Override
        public void render(GuiGraphics g, int originX, int originY, int mouseX, int mouseY, float delta) {
            String clipped = font.plainSubstrByWidth(text.getString(), maxW);
            g.drawString(font, clipped, originX + relX, originY + relY, color, false);
        }
    }

    private final class SectionTitleItem implements PanelItem {
        final int relX, relY, maxW, color;
        final Component text;

        SectionTitleItem(int relX, int relY, int maxW, Component text, int color) {
            this.relX = relX;
            this.relY = relY;
            this.maxW = maxW;
            this.text = text;
            this.color = color;
        }

        @Override
        public void render(GuiGraphics g, int originX, int originY, int mouseX, int mouseY, float delta) {
            String clipped = font.plainSubstrByWidth(text.getString(), maxW);
            g.drawString(font, clipped, originX + relX, originY + relY, color, true);
        }
    }

    private static final class DividerItem implements PanelItem {
        final int relX, relY, w;

        DividerItem(int relX, int relY, int w) {
            this.relX = relX;
            this.relY = relY;
            this.w = w;
        }

        @Override
        public void render(GuiGraphics g, int originX, int originY, int mouseX, int mouseY, float delta) {
            g.fill(originX + relX, originY + relY, originX + relX + w, originY + relY + 1, BORDER_SUBTLE);
        }
    }

    private final class GravityBadgeItem implements PanelItem {
        final int relX, relY, w;
        final double baseGravity;
        double offset;

        GravityBadgeItem(int relX, int relY, int w, double baseGravity, double offset) {
            this.relX = relX;
            this.relY = relY;
            this.w = w;
            this.baseGravity = baseGravity;
            this.offset = offset;
        }

        @Override
        public void render(GuiGraphics g, int originX, int originY, int mouseX, int mouseY, float delta) {
            double ratio = Math.max(0.0, (baseGravity - offset) / baseGravity);
            String text = String.format(Locale.ROOT, "%.2fg", ratio);
            int color = Math.abs(offset) < 1e-6 ? TEXT_DIM : (offset > 0 ? ACCENT_LIGHT : ACCENT_AMBER);
            int tw = font.width(text);
            g.drawString(font, text, originX + relX + w - tw, originY + relY, color, false);
        }
    }

    @FunctionalInterface
    private interface LayerDragListener {
        void onDragged(int index, double altitude, double pressure);
    }

    private final class CurveCanvas implements PanelItem {
        private static final int CURVE_COLOR = 0xFF8AB896;
        private static final int GRAVITY_COLOR = 0xFFD8D8D8;
        private static final int START_MARKER = 0x88A0A0A0;
        private static final int NO_FALL_MARKER = 0x88C9A66B;
        private static final int POINT_COLOR = 0xFFE6E6E6;

        private final DimensionDraft profile;
        private final List<AtmosphereLayers.Layer> layers;
        private final int relX, relY, w, h;
        private final LayerDragListener onDragged;

        private int chartX, chartY, chartW, chartH;
        private int selectedPoint = -1;
        private double dragStartX, dragStartAlt, dragAltRange;

        CurveCanvas(DimensionDraft profile, int relX, int relY, int w, int h, LayerDragListener onDragged) {
            this.profile = profile;
            this.layers = profile.layers;
            this.relX = relX;
            this.relY = relY;
            this.w = w;
            this.h = h;
            this.onDragged = onDragged;
        }

        @Override
        public void render(GuiGraphics g, int originX, int originY, int mouseX, int mouseY, float delta) {
            int x = originX + relX;
            int y = originY + relY;
            g.fill(x, y, x + w, y + h, BG_CARD);
            drawBorder(g, x, y, w, h, BORDER_SUBTLE);

            int lx = x + 6;
            int ly = y + 4;
            g.fill(lx, ly + 2, lx + 6, ly + 6, CURVE_COLOR);
            g.drawString(font, "P", lx + 9, ly, TEXT_MUTED, false);
            lx += 22;
            g.fill(lx, ly + 2, lx + 6, ly + 6, GRAVITY_COLOR);
            g.drawString(font, "G", lx + 9, ly, TEXT_MUTED, false);
            lx += 22;
            g.fill(lx, ly + 2, lx + 6, ly + 6, START_MARKER);
            g.drawString(font, "Start", lx + 9, ly, TEXT_DIM, false);
            lx += 38;
            g.fill(lx, ly + 2, lx + 6, ly + 6, NO_FALL_MARKER);
            g.drawString(font, "NoFall", lx + 9, ly, TEXT_DIM, false);

            chartX = x + 34;
            chartY = y + 18;
            chartW = Math.max(80, w - 44);
            chartH = h - 34;

            g.fill(chartX, chartY, chartX + chartW, chartY + chartH, 0xD40C0C0C);
            drawBorder(g, chartX, chartY, chartW, chartH, BORDER_SUBTLE);

            for (int i = 1; i < 4; i++) {
                int gy = chartY + i * chartH / 4;
                int gx = chartX + i * chartW / 4;
                g.fill(chartX + 1, gy, chartX + chartW - 1, gy + 1, 0x1EFFFFFF);
                g.fill(gx, chartY + 1, gx + 1, chartY + chartH - 1, 0x1EFFFFFF);
            }

            drawMarker(g, profile.atmosphereBaseAltitude, START_MARKER);
            if (profile.noFallDamageAltitude >= 0.0) {
                drawMarker(g, profile.noFallDamageAltitude, NO_FALL_MARKER);
            }

            for (int px = chartX; px < chartX + chartW; px++) {
                double alt = xToAltitude(px);
                int py = valueToY(gravityRatioAt(alt));
                g.fill(px, py, px + 1, py + 2, GRAVITY_COLOR);
            }

            for (int i = 0; i < layers.size() - 1; i++) {
                int x1 = altitudeToX(layers.get(i).altitude());
                int x2 = altitudeToX(layers.get(i + 1).altitude());
                int y1 = valueToY(layers.get(i).pressure());
                int y2 = valueToY(layers.get(i + 1).pressure());
                int span = Math.max(1, x2 - x1);
                for (int px = x1; px <= x2; px++) {
                    int py = (int) Math.round(y1 + (double) (px - x1) / span * (y2 - y1));
                    g.fill(px, py, px + 2, py + 2, CURVE_COLOR);
                }
            }

            for (int i = 0; i < layers.size(); i++) {
                int px = altitudeToX(layers.get(i).altitude());
                int py = valueToY(layers.get(i).pressure());
                int r = i == selectedPoint ? 4 : 3;
                g.fill(px - r - 1, py - r - 1, px + r + 2, py + r + 2,
                        i == selectedPoint ? TEXT_WHITE : 0xFF0C0C0C);
                g.fill(px - r, py - r, px + r + 1, py + r + 1, POINT_COLOR);
            }

            double pMax = pressureMax();
            g.drawString(font, String.format(Locale.ROOT, "%.1f", pMax), x + 5, chartY, TEXT_DIM, false);
            g.drawString(font, "0", x + 20, chartY + chartH - 7, TEXT_DIM, false);
            String minLbl = formatClean(altitudeMin());
            String maxLbl = formatClean(altitudeMax());
            g.drawString(font, minLbl, chartX, chartY + chartH + 4, TEXT_DIM, false);
            g.drawString(font, maxLbl, chartX + chartW - font.width(maxLbl), chartY + chartH + 4, TEXT_DIM, false);

            if (insideChart(mouseX, mouseY)) {
                g.fill(mouseX, chartY, mouseX + 1, chartY + chartH, 0x44FFFFFF);
                g.fill(chartX, mouseY, chartX + chartW, mouseY + 1, 0x44FFFFFF);
                double alt = xToAltitude(mouseX);
                double p = AtmosphereLayers.getPressure(layers, alt);
                double gRatio = gravityRatioAt(alt);
                deferredTooltip = Component.literal(String.format(Locale.ROOT,
                        "Y %.0f  |  P %.2f atm  |  G %.2fg", alt, p, gRatio));
                deferredTooltipX = mouseX;
                deferredTooltipY = mouseY;
            }
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button != 0 || !insideChart(mouseX, mouseY)) return false;
            double bestDist = Double.MAX_VALUE;
            int bestIdx = -1;
            for (int i = 0; i < layers.size(); i++) {
                double dx = altitudeToX(layers.get(i).altitude()) - mouseX;
                double dy = valueToY(layers.get(i).pressure()) - mouseY;
                double d = dx * dx + dy * dy;
                if (d < bestDist) {
                    bestDist = d;
                    bestIdx = i;
                }
            }
            if (bestDist > 18 * 18 || bestIdx < 0) return false;
            selectedPoint = bestIdx;
            dragStartX = mouseX;
            dragStartAlt = layers.get(selectedPoint).altitude();
            dragAltRange = Math.max(1.0, altitudeMax() - altitudeMin());
            moveSelected(mouseX, mouseY);
            return true;
        }

        boolean mouseDragged(double mouseX, double mouseY, int button) {
            if (button != 0 || selectedPoint < 0) return false;
            moveSelected(mouseX, mouseY);
            return true;
        }

        boolean mouseReleased(double mouseX, double mouseY, int button) {
            if (button != 0 || selectedPoint < 0) return false;
            selectedPoint = -1;
            return true;
        }

        private void moveSelected(double mouseX, double mouseY) {
            double alt = (selectedPoint == 0 || selectedPoint == layers.size() - 1)
                    ? dragStartAlt + (mouseX - dragStartX) * dragAltRange / chartW
                    : xToAltitude(mouseX);
            double lo = selectedPoint == 0 ? CURVE_MIN_ALT : layers.get(selectedPoint - 1).altitude() + 1.0;
            double hi = selectedPoint == layers.size() - 1 ? CURVE_MAX_ALT : layers.get(selectedPoint + 1).altitude() - 1.0;
            alt = Math.rint(AtmosphereLayers.clamp(alt, lo, hi));
            double p = Math.round(yToValue(mouseY) * 1000.0) / 1000.0;
            layers.set(selectedPoint, new AtmosphereLayers.Layer(alt, p));
            onDragged.onDragged(selectedPoint, alt, p);
        }

        private void drawMarker(GuiGraphics g, double alt, int color) {
            if (alt < altitudeMin() || alt > altitudeMax()) return;
            int mx = altitudeToX(alt);
            for (int my = chartY + 2; my < chartY + chartH - 2; my += 4) {
                g.fill(mx, my, mx + 1, Math.min(my + 2, chartY + chartH - 1), color);
            }
        }

        private double gravityRatioAt(double alt) {
            double progress = AtmosphereLayers.getGravityProgress(layers, profile.atmosphereBaseAltitude, alt);
            double offset;
            if (profile.surfaceGravityOffset == 0.0) {
                offset = progress * profile.atmosphereGravityMax;
            } else {
                double target = Math.max(profile.surfaceGravityOffset, profile.atmosphereGravityMax);
                offset = profile.surfaceGravityOffset + progress * (target - profile.surfaceGravityOffset);
            }
            return Math.max(0.0, (VANILLA_GRAVITY - offset) / VANILLA_GRAVITY);
        }

        private boolean insideChart(double mx, double my) {
            return mx >= chartX && mx <= chartX + chartW && my >= chartY && my <= chartY + chartH;
        }

        private double pressureMax() {
            double max = 1.25;
            for (AtmosphereLayers.Layer l : layers) max = Math.max(max, l.pressure());
            return Math.max(1.25, Math.ceil(max));
        }

        private double altitudeMin() { return layers.get(0).altitude(); }
        private double altitudeMax() { return layers.get(layers.size() - 1).altitude(); }

        private int altitudeToX(double alt) {
            double span = altitudeMax() - altitudeMin();
            if (span <= 0.0) return chartX;
            double t = AtmosphereLayers.clamp((alt - altitudeMin()) / span, 0.0, 1.0);
            return chartX + (int) Math.round(t * chartW);
        }

        private int valueToY(double val) {
            double t = AtmosphereLayers.clamp(val / pressureMax(), 0.0, 1.0);
            return chartY + chartH - (int) Math.round(t * chartH);
        }

        private double xToAltitude(double mx) {
            double span = altitudeMax() - altitudeMin();
            if (span <= 0.0) return altitudeMin();
            return altitudeMin() + AtmosphereLayers.clamp((mx - chartX) / chartW, 0.0, 1.0) * span;
        }

        private double yToValue(double my) {
            return pressureMax() * (1.0 - AtmosphereLayers.clamp((my - chartY) / chartH, 0.0, 1.0));
        }
    }

    // ==================== DRAFT STATE ====================

    private static final class DimensionDraft {
        private String id;
        private Config.CustomDimConfig.Mode mode;
        private double playerOffset, mobOffset, itemOffset, arrowOffset, thrownOffset, blockOffset;
        private double atmosphereGravityMax, atmosphereBaseAltitude, noFallDamageAltitude;
        private double atmosphereMuffleGain, atmosphereMuffleGainHF, surfaceGravityOffset;
        private double fixedMuffleGain, fixedMuffleGainHF;
        private boolean atmosphereAffectNonLiving;
        private Config.CustomDimConfig.FallDamageMode fallDamageMode;
        private final List<AtmosphereLayers.Layer> layers = new ArrayList<>();

        private DimensionDraft(Config.CustomDimConfig config) {
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
            noFallDamageAltitude = config.getNoFallDamageAltitude() == null ? -1.0 : config.getNoFallDamageAltitude();
            atmosphereMuffleGain = config.getAtmosphereMuffleGain();
            atmosphereMuffleGainHF = config.getAtmosphereMuffleGainHF();
            surfaceGravityOffset = config.getSurfaceGravityOffset();
            atmosphereAffectNonLiving = config.isAtmosphereAffectNonLiving();
            fallDamageMode = config.getFallDamageMode();
            fixedMuffleGain = config.getFixedMuffleGain();
            fixedMuffleGainHF = config.getFixedMuffleGainHF();
            layers.addAll(config.getParsedAtmosphereLayers());
        }

        private Config.CustomDimConfig toConfig() {
            return new Config.CustomDimConfig(id, mode,
                    playerOffset, mobOffset, itemOffset, arrowOffset, thrownOffset, blockOffset,
                    atmosphereGravityMax, atmosphereBaseAltitude,
                    noFallDamageAltitude < 0.0 ? null : noFallDamageAltitude,
                    atmosphereMuffleGain, atmosphereMuffleGainHF,
                    AtmosphereLayers.serialize(layers),
                    surfaceGravityOffset, atmosphereAffectNonLiving, fallDamageMode,
                    fixedMuffleGain, fixedMuffleGainHF);
        }
    }

    private static final class EditorDraft {
        private final List<DimensionDraft> profiles = new ArrayList<>();
        private final List<DimensionDraft> removedProfiles = new ArrayList<>();
        private final List<AtmospherePreset> atmospherePresets = new ArrayList<>();

        private boolean endEntityGravity = Config.COMMON.endEntityGravity.get();
        private boolean enableAtmosphere = Config.COMMON.enableAtmosphere.get();
        private boolean enableAtmosphericDrag = Config.COMMON.enableAtmosphericDrag.get();
        private boolean overworldEntityGravity = Config.COMMON.overworldEntityGravity.get();
        private FallDamageMode fallDamageMode = FallDamageMode.fromInt(Config.COMMON.fallDamageMode.get());
        private FallDamageMode mobFallDamageMode = FallDamageMode.fromInt(Config.COMMON.mobFallDamageMode.get());
        private boolean enablePotionGravity = Config.COMMON.enablePotionGravity.get();
        private double weightlessnessOffset = Config.COMMON.weightlessnessGravityOffset.get();
        private double heavinessOffset = Config.COMMON.heavinessGravityOffset.get();
        private boolean enableGravityHud = Config.COMMON.enableGravityHud.get();
        private HudPosition hudPosition = HudPosition.fromInt(Config.COMMON.hudPosition.get());
        private boolean hudShowPressure = Config.COMMON.hudShowPressure.get();
        private boolean hudCompact = Config.COMMON.hudCompact.get();
        private boolean enableLowPassFilter = Config.COMMON.enableLowPassFilter.get();
        private double lowPassGain = Config.COMMON.lowPassGain.get();
        private double lowPassGainHF = Config.COMMON.lowPassGainHF.get();
        private double particleGravityOffset = Config.COMMON.particleGravityOffset.get();
        private final List<String> gravityImmuneEntities = new ArrayList<>(Config.COMMON.gravityImmuneEntities.get());

        private EditorDraft() {
            Config.getCustomDimensions().forEach(c -> profiles.add(new DimensionDraft(c)));
            profiles.sort((a, b) -> a.id.compareToIgnoreCase(b.id));
            atmospherePresets.addAll(Config.getAtmospherePresetsSafe());
        }
    }
}
