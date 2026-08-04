package dinner.dev.endless_gravity;

import dinner.dev.endless_gravity.item.ModItems;
import dinner.dev.endless_gravity.item.StellarChestplateItem;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import org.joml.Matrix4f;

@EventBusSubscriber(modid = EndlessGravity.MODID, value = Dist.CLIENT)
public class ClientGuiHandler {

    private static final int BAR_WIDTH = 120;
    private static final int BAR_HEIGHT = 10;

    private static float displayTank = -1.0F;
    private static float displayAlpha = 0.0F;
    private static float suffocationAlpha = 0.0F;
    private static int suffocationStartTick = -1;
    private static int lastPlayerId = -1;

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        if (player.getId() != lastPlayerId) {
            lastPlayerId = player.getId();
            displayTank = -1.0F;
            displayAlpha = 0.0F;
            suffocationAlpha = 0.0F;
            suffocationStartTick = -1;
        }

        if (!player.isCreative() && !player.isSpectator()) {
            tickSuffocationOverlay(event.getGuiGraphics(), player);
        } else {
            suffocationStartTick = -1;
            suffocationAlpha = Math.max(0.0F, suffocationAlpha - 0.06F);
            drawSuffocationOverlay(event.getGuiGraphics());
        }

        if (mc.options.hideGui) return;

        ItemStack chestplate = player.getItemBySlot(EquipmentSlot.CHEST);
        boolean wearing = chestplate.getItem() == ModItems.STELLAR_CHESTPLATE.get();

        if (wearing) {
            float target = StellarChestplateItem.getTank(chestplate);

            if (displayTank < 0.0F) displayTank = target;
            displayTank += (target - displayTank) * 0.1F;
            if (Math.abs(displayTank - target) < 0.5F) displayTank = target;
            displayAlpha = Math.min(1.0F, displayAlpha + 0.1F);

            drawBar(event.getGuiGraphics(), displayTank, displayAlpha);
        } else if (displayAlpha > 0.005F) {
            displayAlpha -= 0.05F;
            drawBar(event.getGuiGraphics(), Math.max(0.0F, displayTank), displayAlpha);
        } else {
            displayTank = -1.0F;
            displayAlpha = 0.0F;
        }
    }

    private static void tickSuffocationOverlay(GuiGraphics gui, Player player) {
        int fadeTicks = Config.COMMON.oxygenSuffocationFadeTicks.get();
        if (fadeTicks < 1) fadeTicks = 1;

        if (isSuffocating(player)) {
            if (suffocationStartTick < 0) suffocationStartTick = player.tickCount;
            suffocationAlpha = Mth.clamp((player.tickCount - suffocationStartTick) / (float) fadeTicks, 0.0F, 1.0F);
        } else {
            suffocationStartTick = -1;
            if (suffocationAlpha > 0.0F) {
                suffocationAlpha = Math.max(0.0F, suffocationAlpha - 0.06F);
            }
        }

        drawSuffocationOverlay(gui);
    }

    private static boolean isSuffocating(Player player) {
        boolean helmet = player.getItemBySlot(EquipmentSlot.HEAD).getItem() == ModItems.STELLAR_HELMET.get();
        boolean chestplate = player.getItemBySlot(EquipmentSlot.CHEST).getItem() == ModItems.STELLAR_CHESTPLATE.get();
        double oxyProgress = EndlessGravityAPI.getOxygenProgress(EndlessGravityAPI.getRealY(player));
        if (oxyProgress <= 0) return false;
        return !helmet || !chestplate || player.getAirSupply() <= 0;
    }

    private static void drawSuffocationOverlay(GuiGraphics gui) {
        if (suffocationAlpha <= 0.01F) return;
        int alpha = Math.round(suffocationAlpha * 255);
        gui.fill(0, 0, gui.guiWidth(), gui.guiHeight(), alpha << 24);
    }

    private static void drawBar(GuiGraphics gui, float tank, float alpha) {
        int capacity = Math.max(1, Config.COMMON.oxygenTankCapacity.get());
        float fill = Math.max(0.0F, Math.min(1.0F, tank / capacity));

        int x = 8;
        int y = gui.guiHeight() - 26;
        int a = Math.round(alpha * 255);
        boolean low = fill < 0.25F;

        int frame = (Math.round(alpha * 200) << 24) | 0xFFFFFF;
        gui.fill(x - 1, y - 1, x + BAR_WIDTH + 1, y, frame);
        gui.fill(x - 1, y + BAR_HEIGHT, x + BAR_WIDTH + 1, y + BAR_HEIGHT + 1, frame);
        gui.fill(x - 1, y, x, y + BAR_HEIGHT, frame);
        gui.fill(x + BAR_WIDTH, y, x + BAR_WIDTH + 1, y + BAR_HEIGHT, frame);

        gui.fillGradient(x, y, x + BAR_WIDTH, y + BAR_HEIGHT,
                (Math.round(alpha * 90) << 24), (Math.round(alpha * 40) << 24));

        float fillWidth = BAR_WIDTH * fill;
        if (fillWidth > 0.5F) {
            drawGradientRect(gui, x, y, x + fillWidth, y + BAR_HEIGHT, a, low);
        }

        gui.drawString(Minecraft.getInstance().font, "O\u2082", x + 2, y - 10, (a << 24) | 0xFFFFFF);
    }

    private static void drawGradientRect(GuiGraphics gui, float x1, float y1, float x2, float y2, int alpha, boolean low) {
        int rTop = low ? 0xFF : 0x9B;
        int gTop = low ? 0xA3 : 0xE8;
        int bTop = low ? 0xA3 : 0xFF;
        int rBot = low ? 0xC2 : 0x2F;
        int gBot = low ? 0x3A : 0x7F;
        int bBot = low ? 0x3A : 0xA3;

        Matrix4f pose = gui.pose().last().pose();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        builder.addVertex(pose, x1, y1, 0.0F).setColor(rTop, gTop, bTop, alpha);
        builder.addVertex(pose, x1, y2, 0.0F).setColor(rBot, gBot, bBot, alpha);
        builder.addVertex(pose, x2, y2, 0.0F).setColor(rBot, gBot, bBot, alpha);
        builder.addVertex(pose, x2, y1, 0.0F).setColor(rTop, gTop, bTop, alpha);
        BufferUploader.drawWithShader(builder.buildOrThrow());
    }
}
