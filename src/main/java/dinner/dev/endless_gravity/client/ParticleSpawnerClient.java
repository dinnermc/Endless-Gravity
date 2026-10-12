package dinner.dev.endless_gravity.client;

import dinner.dev.endless_gravity.Config;
import dinner.dev.endless_gravity.EndlessGravity;
import dinner.dev.endless_gravity.EndlessGravityAPI;
import dinner.dev.endless_gravity.GravityHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

@EventBusSubscriber(modid = EndlessGravity.MODID, value = Dist.CLIENT)
public class ParticleSpawnerClient {

    private static final RandomSource RANDOM = RandomSource.create();

    public static void register() {
        NeoForge.EVENT_BUS.addListener(ParticleSpawnerClient::onClientTick);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent event) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return;

        Player player = mc.player;
        if (player == null) return;
        if (level.dimension() != Level.END) return;
        if (!GravityHandler.isEndEntityGravity()) return;
        if (!Config.COMMON.enablePlayerGravity.get()) return;

        // Spawn low gravity particles around the player in the End
        if (net.minecraft.util.RandomSource.create().nextFloat() < 0.05f) { // ~5% chance per tick
            double x = player.getX() + (RANDOM.nextDouble() - 0.5) * 8.0;
            double y = player.getY() + RANDOM.nextDouble() * 4.0;
            double z = player.getZ() + (RANDOM.nextDouble() - 0.5) * 8.0;

            // Small upward velocity to simulate floating
            double xSpeed = (RANDOM.nextDouble() - 0.5) * 0.02;
            double ySpeed = 0.01 + RANDOM.nextDouble() * 0.02;
            double zSpeed = (RANDOM.nextDouble() - 0.5) * 0.02;

            level.addParticle(
                EndlessGravity.LOW_GRAVITY_PARTICLE,
                true, // force particle
                x, y, z,
                xSpeed, ySpeed, zSpeed
            );
        }
    }
}