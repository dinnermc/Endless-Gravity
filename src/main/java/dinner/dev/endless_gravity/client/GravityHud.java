package dinner.dev.endless_gravity.client;

import dinner.dev.endless_gravity.Config;
import dinner.dev.endless_gravity.EndlessGravityAPI;
import dinner.dev.endless_gravity.GravityResolver;
import dinner.dev.endless_gravity.api.GravityChannel;
import dinner.dev.endless_gravity.api.GravityContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.Locale;

/**
 * Optional on-screen readout of the local player's resolved gravity (and atmospheric
 * pressure when in an atmosphere dimension). Client-side only; toggled with
 * {@code enableGravityHud} or the toggle HUD keybind.
 */
public final class GravityHud {

    private GravityHud() {}

    private static int refreshTimer;
    private static double offset;
    private static double gravityRatio = 1.0;
    private static double pressure = 1.0;
    private static boolean showAtmosphere;
    private static boolean applicable;
    private static String source = "NONE";

    /** Called once per client tick (20Hz) so refresh rate is independent of FPS. */
    public static void tick(Minecraft client) {
        if (!Config.COMMON.enableGravityHud.get()) return;
        if (--refreshTimer > 0) return;
        refreshTimer = 4;

        Player player = client.player;
        if (player == null || client.level == null) {
            offset = 0.0;
            gravityRatio = 1.0;
            pressure = 1.0;
            showAtmosphere = false;
            applicable = false;
            source = "NONE";
            return;
        }
        GravityContext context = GravityResolver.compute(player, GravityChannel.PLAYER);
        offset = context.offset();
        applicable = context.applicable();
        double base = context.baseGravity();
        gravityRatio = (applicable && base > 0.0) ? Math.max(0.0, (base - offset) / base) : 1.0;
        source = context.source().name();

        Config.CustomDimConfig profile = Config.getCustomDimConfig(client.level.dimension().location()).orElse(null);
        showAtmosphere = profile != null && profile.getMode() == Config.CustomDimConfig.Mode.ATMOSPHERE;
        if (showAtmosphere) {
            pressure = profile.getAtmospherePressure(EndlessGravityAPI.getRealY(player));
        } else {
            pressure = 1.0;
        }
    }

    public static void render(GuiGraphics graphics) {
        Minecraft client = Minecraft.getInstance();
        if (!Config.COMMON.enableGravityHud.get()) return;
        if (client.options.hideGui || client.getDebugOverlay().showDebugScreen()
                || client.player == null || client.level == null) return;

        String pressurePart = "";
        if (showAtmosphere && Config.COMMON.hudShowPressure.get()) {
            pressurePart = pressure <= 0.005
                    ? " | Vacuum"
                    : String.format(Locale.ROOT, " | %.2f atm", pressure);
        }

        Component line;
        if (Config.COMMON.hudCompact.get()) {
            String gText = applicable
                    ? String.format(Locale.ROOT, "%.2fg", gravityRatio)
                    : "1.00g";
            line = Component.literal(gText + pressurePart);
        } else {
            String value = applicable
                    ? String.format(Locale.ROOT, "%.2fg (%+.3f)", gravityRatio, offset)
                    : "1.00g (0.000)";
            String src = source.isEmpty() ? "-" : source.charAt(0) + source.substring(1).toLowerCase(Locale.ROOT);
            line = Component.translatable("endless_gravity.hud.gravity")
                    .append(Component.literal(": " + value + pressurePart + " [" + src + "]"));
        }

        int color = !applicable || Math.abs(offset) < 1e-6
                ? 0xFFA0A0A0
                : (offset > 0 ? 0xFFE6E6E6 : 0xFFC9A66B);
        int textWidth = client.font.width(line);
        int screenWidth = graphics.guiWidth();
        int screenHeight = graphics.guiHeight();
        int pos = Config.COMMON.hudPosition.get();
        int x = (pos == 1 || pos == 3) ? Math.max(6, screenWidth - textWidth - 6) : 6;
        int y = (pos == 2 || pos == 3) ? Math.max(6, screenHeight - client.font.lineHeight - 6) : 6;

        graphics.drawString(client.font, line, x, y, color, true);
    }
}
