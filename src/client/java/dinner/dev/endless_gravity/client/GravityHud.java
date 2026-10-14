package dinner.dev.endless_gravity.client;

import dinner.dev.endless_gravity.Config;
import dinner.dev.endless_gravity.GravityResolver;
import dinner.dev.endless_gravity.api.GravityChannel;
import dinner.dev.endless_gravity.api.GravityContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

/**
 * Optional on-screen readout of the local player's resolved gravity offset.
 * Client-side only; toggled with {@code enableGravityHud}.
 */
public final class GravityHud {

    private GravityHud() {}

    private static int refreshTimer;
    private static double offset;
    private static boolean applicable;
    private static String source = "NONE";

    /** Recomputed a few times per second from the render pass to keep it cheap. */
    private static void refresh(Minecraft client) {
        Player player = client.player;
        if (player == null || client.level == null) {
            offset = 0.0;
            applicable = false;
            source = "NONE";
            return;
        }
        GravityContext context = GravityResolver.compute(player, GravityChannel.PLAYER);
        offset = context.offset();
        applicable = context.applicable();
        source = context.source().name();
    }

    public static void render(GuiGraphics graphics) {
        Minecraft client = Minecraft.getInstance();
        if (!Config.COMMON.enableGravityHud.get()) return;
        if (client.options.hideGui || client.player == null || client.level == null) return;

        if (--refreshTimer <= 0) {
            refreshTimer = 5;
            refresh(client);
        }

        String value = applicable ? String.format("%+.3f", offset) : "0.000";
        String src = source.isEmpty() ? "-" : source.charAt(0) + source.substring(1).toLowerCase(java.util.Locale.ROOT);        int color = !applicable ? 0xFFAAAAAA : (offset >= 0 ? 0xFF7FCFFF : 0xFFCC8844);

        graphics.drawString(client.font,
                Component.translatable("endless_gravity.hud.gravity").append(Component.literal(": " + value + "  [" + src + "]")),
                6, 6, color, true);
    }
}
