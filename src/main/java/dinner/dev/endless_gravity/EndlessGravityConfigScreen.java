package dinner.dev.endless_gravity;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.neoforged.fml.ModList;

import java.util.ArrayList;
import java.util.List;

public class EndlessGravityConfigScreen extends Screen {

    private static final int TOP_MARGIN = 28;
    private static final int BOTTOM_MARGIN = 36;
    private static final int SCROLL_SPEED = 18;

    private final Screen parent;
    private final boolean sableLoaded;

    // Scroll
    private int scrollOffset = 0;
    private int maxScroll = 0;
    private int contentBottom = 0;
    private final List<AbstractWidget> scrollWidgets = new ArrayList<>();
    private final List<Integer> scrollOriginalY = new ArrayList<>();
    private final List<HeaderEntry> headers = new ArrayList<>();

    // Gravity
    private double playerGravity;
    private double itemGravity;
    private double arrowGravity;
    private double thrownGravity;

    // Effects
    private double particleMultiplier;
    private boolean enableLowPassFilter;
    private double lowPassGain;
    private double lowPassGainHF;

    // Gameplay
    private int fallDamageMode;
    private double fallDamageVelocityScale;
    private double fallDamageMinVelocity;
    private boolean enableBlockGravity;
    private double blockGravityOffset;

    // Sable
    private double sableGravityY;
    private double sablePressure;
    private double sableDrag;
    private int sableDatapackPriority;
    private int restartWarningY = -1;

    // Widget references for reset
    private ConfigSlider playerSlider;
    private ConfigSlider itemSlider;
    private ConfigSlider arrowSlider;
    private ConfigSlider thrownSlider;
    private ConfigSlider particleSlider;
    private Button enableLowPassFilterToggle;
    private ConfigSlider filterGainSlider;
    private ConfigSlider filterHFSlider;
    private Button fallDamageModeToggle;
    private ConfigSlider velocityScaleSlider;
    private ConfigSlider minVelocitySlider;
    private Button enableBlockGravityToggle;
    private ConfigSlider blockGravitySlider;
    private ConfigSlider sablePrioritySlider;
    private ConfigSlider sableGravityYSlider;
    private ConfigSlider sablePressureSlider;
    private ConfigSlider sableDragSlider;

    public EndlessGravityConfigScreen(Screen parent) {
        super(Component.translatable("endless_gravity.config.title"));
        this.parent = parent;
        boolean loaded;
        try {
            loaded = ModList.get().isLoaded("sable");
        } catch (Exception e) {
            loaded = false;
        }
        this.sableLoaded = loaded;
    }

    @Override
    protected void init() {
        scrollOffset = 0;
        scrollWidgets.clear();
        scrollOriginalY.clear();
        headers.clear();
        contentBottom = 0;

        playerGravity = Config.COMMON.playerGravityOffset.get();
        itemGravity = Config.COMMON.itemGravityOffset.get();
        arrowGravity = Config.COMMON.arrowGravityOffset.get();
        thrownGravity = Config.COMMON.thrownGravityOffset.get();
        particleMultiplier = Config.COMMON.particleGravityMultiplier.get();
        enableLowPassFilter = Config.COMMON.enableLowPassFilter.get();
        lowPassGain = Config.COMMON.lowPassGain.get();
        lowPassGainHF = Config.COMMON.lowPassGainHF.get();
        fallDamageMode = Config.COMMON.fallDamageMode.get();
        fallDamageVelocityScale = Config.COMMON.fallDamageVelocityScale.get();
        fallDamageMinVelocity = Config.COMMON.fallDamageMinVelocity.get();
        enableBlockGravity = Config.COMMON.enableBlockGravity.get();
        blockGravityOffset = Config.COMMON.blockGravityOffset.get();

        if (sableLoaded) {
            sableGravityY = Config.COMMON.sableGravityY.get();
            sablePressure = Config.COMMON.sablePressure.get();
            sableDrag = Config.COMMON.sableDrag.get();
            sableDatapackPriority = Config.COMMON.sableDatapackPriority.get();
        }

        int x = this.width / 2 - 155;
        int w = 310;
        int gap = 22;
        int y = TOP_MARGIN;

        addHeader(y, "endless_gravity.config.section.gravity");
        y += 12;

        playerSlider = addScroll(new ConfigSlider(x, y, "endless_gravity.config.playerGravityOffset",
                playerGravity, 0.0, 0.08, v -> playerGravity = v, false));

        itemSlider = addScroll(new ConfigSlider(x, y += gap, "endless_gravity.config.itemGravityOffset",
                itemGravity, 0.0, 0.04, v -> itemGravity = v, false));

        arrowSlider = addScroll(new ConfigSlider(x, y += gap, "endless_gravity.config.arrowGravityOffset",
                arrowGravity, 0.0, 0.05, v -> arrowGravity = v, false));

        thrownSlider = addScroll(new ConfigSlider(x, y += gap, "endless_gravity.config.thrownGravityOffset",
                thrownGravity, 0.0, 0.03, v -> thrownGravity = v, false));

        y += gap + 10;
        addHeader(y, "endless_gravity.config.section.effects");
        y += 12;

        particleSlider = addScroll(new ConfigSlider(x, y, "endless_gravity.config.particleGravityMultiplier",
                particleMultiplier, 0.0, 1.0, v -> particleMultiplier = v, false));

        y += gap;
        enableLowPassFilterToggle = addScroll(Button.builder(
                toggleLabel("endless_gravity.config.enableLowPassFilter", enableLowPassFilter),
                b -> {
                    enableLowPassFilter = !enableLowPassFilter;
                    b.setMessage(toggleLabel("endless_gravity.config.enableLowPassFilter", enableLowPassFilter));
                    filterGainSlider.active = enableLowPassFilter;
                    filterHFSlider.active = enableLowPassFilter;
                }
        ).bounds(x, y, w, 20).build());

        filterGainSlider = addScroll(new ConfigSlider(x, y += gap, "endless_gravity.config.lowPassGain",
                lowPassGain, 0.0, 1.0, v -> lowPassGain = v, false));
        filterGainSlider.active = enableLowPassFilter;

        filterHFSlider = addScroll(new ConfigSlider(x, y += gap, "endless_gravity.config.lowPassGainHF",
                lowPassGainHF, 0.0, 1.0, v -> lowPassGainHF = v, false));
        filterHFSlider.active = enableLowPassFilter;

        y += gap + 10;
        addHeader(y, "endless_gravity.config.section.gameplay");
        y += 12;

        fallDamageModeToggle = addScroll(Button.builder(
                fallDamageModeLabel(),
                b -> {
                    fallDamageMode = (fallDamageMode + 1) % 3;
                    b.setMessage(fallDamageModeLabel());
                    velocityScaleSlider.active = (fallDamageMode == 2);
                }
        ).bounds(x, y, w, 20).build());

        velocityScaleSlider = addScroll(new ConfigSlider(x, y += gap, "endless_gravity.config.fallDamageVelocityScale",
                fallDamageVelocityScale, 0.1, 10.0, v -> fallDamageVelocityScale = v, false));
        velocityScaleSlider.active = (fallDamageMode == 2);

        minVelocitySlider = addScroll(new ConfigSlider(x, y += gap, "endless_gravity.config.fallDamageMinVelocity",
                fallDamageMinVelocity, 0.0, 5.0, v -> fallDamageMinVelocity = v, false));
        minVelocitySlider.active = (fallDamageMode == 2);

        y += gap;
        enableBlockGravityToggle = addScroll(Button.builder(
                toggleLabel("endless_gravity.config.enableBlockGravity", enableBlockGravity),
                b -> {
                    enableBlockGravity = !enableBlockGravity;
                    b.setMessage(toggleLabel("endless_gravity.config.enableBlockGravity", enableBlockGravity));
                    blockGravitySlider.active = enableBlockGravity;
                }
        ).bounds(x, y, w, 20).build());

        blockGravitySlider = addScroll(new ConfigSlider(x, y += gap, "endless_gravity.config.blockGravityOffset",
                blockGravityOffset, 0.0, 0.04, v -> blockGravityOffset = v, false));
        blockGravitySlider.active = enableBlockGravity;

        if (sableLoaded) {
            y += gap + 10;
            addHeader(y, "endless_gravity.config.section.sable");
            y += 12;

            sableGravityYSlider = addScroll(new ConfigSlider(x, y, "endless_gravity.config.sableGravityY",
                    sableGravityY, -20.0, 0.0, v -> sableGravityY = v, true));

            sablePressureSlider = addScroll(new ConfigSlider(x, y += gap, "endless_gravity.config.sablePressure",
                    sablePressure, 0.0, 10.0, v -> sablePressure = v, true));

            sableDragSlider = addScroll(new ConfigSlider(x, y += gap, "endless_gravity.config.sableDrag",
                    sableDrag, 0.0, 10.0, v -> sableDrag = v, true));

            sablePrioritySlider = addScroll(new ConfigSlider(x, y += gap, "endless_gravity.config.sableDatapackPriority",
                    sableDatapackPriority, 1, 9999, v -> sableDatapackPriority = (int) Math.round(v), true));

            restartWarningY = y + gap + 4;
        }

        contentBottom = y + gap + (sableLoaded ? 24 : 0);
        int contentClipBottom = this.height - BOTTOM_MARGIN + 8;
        maxScroll = Math.max(0, contentBottom - contentClipBottom);

        int btnY = this.height - BOTTOM_MARGIN + 8;
        int btnW = (w - 10) / 3;
        addRenderableWidget(Button.builder(
                Component.translatable("endless_gravity.config.resetDefaults"),
                b -> resetToDefaults()
        ).bounds(x, btnY, btnW, 20).build());

        addRenderableWidget(Button.builder(
                Component.translatable("gui.done"),
                b -> { save(); minecraft.setScreen(parent); }
        ).bounds(x + btnW + 5, btnY, btnW, 20).build());

        addRenderableWidget(Button.builder(
                Component.translatable("gui.cancel"),
                b -> minecraft.setScreen(parent)
        ).bounds(x + (btnW + 5) * 2, btnY, btnW, 20).build());
    }

    @SuppressWarnings("unchecked")
    private <T extends AbstractWidget> T addScroll(T widget) {
        addRenderableWidget(widget);
        scrollWidgets.add(widget);
        scrollOriginalY.add(widget.getY());
        return widget;
    }

    private void addHeader(int y, String key) {
        headers.add(new HeaderEntry(y, key));
    }

    private Component fallDamageModeLabel() {
        String key = switch (fallDamageMode) {
            case 0 -> "endless_gravity.config.fallDamageMode.normal";
            case 2 -> "endless_gravity.config.fallDamageMode.velocity";
            default -> "endless_gravity.config.fallDamageMode.disabled";
        };
        return Component.translatable("endless_gravity.config.fallDamageMode")
                .append(Component.literal(": "))
                .append(Component.translatable(key));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (maxScroll <= 0) return false;
        int old = scrollOffset;
        scrollOffset = Mth.clamp(scrollOffset - (int) (verticalAmount * SCROLL_SPEED), 0, maxScroll);
        if (scrollOffset != old) {
            for (int i = 0; i < scrollWidgets.size(); i++) {
                scrollWidgets.get(i).setY(scrollOriginalY.get(i) - scrollOffset);
            }
        }
        return true;
    }

    private void resetToDefaults() {
        playerGravity = 0.055;
        itemGravity = 0.025;
        arrowGravity = 0.03;
        thrownGravity = 0.018;
        particleMultiplier = 0.3;
        enableLowPassFilter = true;
        lowPassGain = 0.4;
        lowPassGainHF = 0.3;
        fallDamageMode = 1;
        fallDamageVelocityScale = 1.0;
        fallDamageMinVelocity = 0.6;
        enableBlockGravity = true;
        blockGravityOffset = 0.035;

        playerSlider.setActualValue(0.055);
        itemSlider.setActualValue(0.025);
        arrowSlider.setActualValue(0.03);
        thrownSlider.setActualValue(0.018);
        particleSlider.setActualValue(0.3);
        enableLowPassFilterToggle.setMessage(toggleLabel("endless_gravity.config.enableLowPassFilter", true));
        filterGainSlider.setActualValue(0.4);
        filterHFSlider.setActualValue(0.3);
        filterGainSlider.active = true;
        filterHFSlider.active = true;
        fallDamageModeToggle.setMessage(fallDamageModeLabel());
        velocityScaleSlider.setActualValue(1.0);
        velocityScaleSlider.active = false;
        minVelocitySlider.setActualValue(0.6);
        minVelocitySlider.active = false;
        enableBlockGravityToggle.setMessage(toggleLabel("endless_gravity.config.enableBlockGravity", true));
        blockGravitySlider.setActualValue(0.035);
        blockGravitySlider.active = true;

        if (sableLoaded) {
            sableGravityY = -4.0;
            sablePressure = 0.0;
            sableDrag = 0.05;
            sableDatapackPriority = 1001;
            sableGravityYSlider.setActualValue(-4.0);
            sablePressureSlider.setActualValue(0.0);
            sableDragSlider.setActualValue(0.05);
            sablePrioritySlider.setActualValue(1001.0);
        }
    }

    private void save() {
        Config.COMMON.playerGravityOffset.set(playerGravity);
        Config.COMMON.itemGravityOffset.set(itemGravity);
        Config.COMMON.arrowGravityOffset.set(arrowGravity);
        Config.COMMON.thrownGravityOffset.set(thrownGravity);
        Config.COMMON.particleGravityMultiplier.set(particleMultiplier);
        Config.COMMON.enableLowPassFilter.set(enableLowPassFilter);
        Config.COMMON.lowPassGain.set(lowPassGain);
        Config.COMMON.lowPassGainHF.set(lowPassGainHF);
        Config.COMMON.fallDamageMode.set(fallDamageMode);
        Config.COMMON.fallDamageVelocityScale.set(fallDamageVelocityScale);
        Config.COMMON.fallDamageMinVelocity.set(fallDamageMinVelocity);
        Config.COMMON.enableBlockGravity.set(enableBlockGravity);
        Config.COMMON.blockGravityOffset.set(blockGravityOffset);

        if (sableLoaded) {
            Config.COMMON.sableGravityY.set(sableGravityY);
            Config.COMMON.sablePressure.set(sablePressure);
            Config.COMMON.sableDrag.set(sableDrag);
            Config.COMMON.sableDatapackPriority.set(sableDatapackPriority);
            SableDatapackHandler.generateDatapack();
        }
    }

    private static Component toggleLabel(String key, boolean value) {
        return Component.translatable(key)
                .append(Component.literal(": "))
                .append(Component.translatable("endless_gravity.config." + value));
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(guiGraphics, mouseX, mouseY, partialTick);

        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 8, 0xFFFFFF);

        int contentTop = TOP_MARGIN;
        int contentClipBottom = this.height - BOTTOM_MARGIN + 8;
        guiGraphics.enableScissor(0, contentTop, this.width, contentClipBottom);

        for (HeaderEntry h : headers) {
            int drawY = h.y() - scrollOffset;
            if (drawY >= contentTop - 12) {
                guiGraphics.drawCenteredString(this.font,
                        Component.translatable(h.key()),
                        this.width / 2, drawY, 0x88AAFF);
            }
        }

        if (sableLoaded && restartWarningY > 0) {
            int drawY = restartWarningY - scrollOffset;
            if (drawY >= contentTop) {
                guiGraphics.drawCenteredString(this.font,
                        Component.translatable("endless_gravity.config.restartWarning"),
                        this.width / 2, drawY, 0xFFFF55);
            }
        }

        for (var w : this.renderables) {
            if (scrollWidgets.contains(w)) {
                w.render(guiGraphics, mouseX, mouseY, partialTick);
            }
        }
        guiGraphics.disableScissor();

        for (var w : this.renderables) {
            if (!scrollWidgets.contains(w)) {
                w.render(guiGraphics, mouseX, mouseY, partialTick);
            }
        }

        if (maxScroll > 0) {
            int sbX = this.width - 6;
            int sbW = 4;
            int sbY = contentTop;
            int sbH = contentClipBottom - contentTop;

            guiGraphics.fill(sbX, sbY, sbX + sbW, sbY + sbH, 0x40000000);

            int thumbH = Math.max(12, (int) ((float) sbH * (contentClipBottom - contentTop) / (contentBottom)));
            int thumbY = sbY + (int) ((float) scrollOffset / maxScroll * (sbH - thumbH));
            guiGraphics.fill(sbX, thumbY, sbX + sbW, thumbY + thumbH, 0xFFAAAAAA);
        }
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int contentClipBottom = this.height - BOTTOM_MARGIN + 8;
        if (mouseY >= contentClipBottom) {
            for (var child : this.children()) {
                if (child instanceof Button && child.isMouseOver(mouseX, mouseY)) {
                    return child.mouseClicked(mouseX, mouseY, button);
                }
            }
            return false;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private record HeaderEntry(int y, String key) {}

    private class ConfigSlider extends AbstractSliderButton {
        private final double min;
        private final double max;
        private final String key;
        private final java.util.function.DoubleConsumer setter;
        private final boolean isInteger;

        ConfigSlider(int x, int y, String key, double current, double min, double max,
                     java.util.function.DoubleConsumer setter, boolean isInteger) {
            super(x, y, 310, 20, Component.translatable(key), (current - min) / (max - min));
            this.min = min;
            this.max = max;
            this.key = key;
            this.setter = setter;
            this.isInteger = isInteger;
            this.updateMessage();
        }

        @Override
        protected void updateMessage() {
            if (key == null) return;
            double actual = min + this.value * (max - min);
            String valueStr = isInteger ? String.valueOf((int) Math.round(actual))
                    : String.format("%.3f", actual);
            setMessage(Component.translatable(key)
                    .append(Component.literal(": " + valueStr)));
        }

        @Override
        protected void applyValue() {
            setter.accept(min + this.value * (max - min));
        }

        public void setActualValue(double actual) {
            this.value = Mth.clamp((actual - min) / (max - min), 0.0, 1.0);
            updateMessage();
        }
    }
}
