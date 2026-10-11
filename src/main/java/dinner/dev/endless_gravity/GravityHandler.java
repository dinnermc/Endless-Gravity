package dinner.dev.endless_gravity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.server.MinecraftServer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

public class GravityHandler {

    private static volatile MinecraftServer server;

    public static void register() {
        ServerLifecycleEvents.SERVER_STARTING.register(server -> GravityHandler.server = server);
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> GravityHandler.server = null);
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server != null) {
                for (ServerLevel level : server.getAllLevels()) {
                    onLevelTick(level);
                }
            }
        });
        ServerLivingEntityEvents.ALLOW_DAMAGE.register(GravityHandler::onAllowDamage);
    }

    private static void onLevelTick(ServerLevel level) {
        for (ServerPlayer player : level.players()) {
            applyPlayerGravity(player, level);
        }
    }

    private static void applyPlayerGravity(Player player, Level level) {
        if (player.onGround() || player.isInWater() || player.isFallFlying()) return;
        if (player.getAbilities().flying) return;

        if (EndlessGravityAPI.isGravityImmune(player)) return;

        if (applyAtmosphereEffects(level, player)) return;

        if (!isEnd(level)) return;
        if (!Config.COMMON.endEntityGravity.get()) return;
        if (!Config.COMMON.enablePlayerGravity.get()) return;

        applyGravity(player, Config.COMMON.playerGravityOffset.get());
    }

    /**
     * Aplica efectos de atmósfera (gravedad reducida por altitud) a cualquier entidad.
     * Llamada desde mixins comunes (ItemEntity, AbstractArrow, FallingBlock, ThrowableProjectile)
     * y desde GravityHandler (jugadores servidor).
     *
     * @return true si se aplicó offset atmosférico y se debe saltar gravedad normal
     */
    public static boolean applyAtmosphereEffects(Level level, Entity entity) {
        if (!isOverworld(level)) {
            return false;
        }
        if (!Config.COMMON.overworldEntityGravity.get()) {
            return false;
        }
        if (!Config.COMMON.enableAtmosphere.get()) {
            return false;
        }

        double realY = EndlessGravityAPI.getRealY(entity);
        if (realY <= EndlessGravityAPI.BASE) {
            return false;
        }

        double offset = EndlessGravityAPI.getAtmosphereOffset(realY);
        if (offset <= 0) {
            return false;
        }

        // Cap the lift at the entity's own gravity or items would fly up.
        double entityGravity = entity.getGravity();
        if (offset > entityGravity) {
            offset = entityGravity;
        }
        if (offset <= 0) {
            return false;
        }

        applyGravity(entity, offset);
        return true;
    }

    /**
     * Fall-damage cancellation. Fabric vetoes the resulting damage before it is applied.
     */
    private static boolean onAllowDamage(LivingEntity entity, DamageSource source, float amount) {
        if (!(entity instanceof ServerPlayer player)) return true;
        if (!source.is(DamageTypeTags.IS_FALL)) return true;

        Level level = player.level();

        if (Config.COMMON.enableAtmosphere.get()
                && isOverworld(level)
                && EndlessGravityAPI.getRealY(player) > Config.COMMON.noFallDamageAltitude.get()) {
            return false;
        }

        if (isEnd(level) && Config.COMMON.fallDamageMode.get() == 1) {
            return false;
        }
        return true;
    }

    public static boolean isEnd(Level level) {
        return level.dimension() == Level.END;
    }

    public static boolean isOverworld(Level level) {
        return level.dimension() == Level.OVERWORLD;
    }

    static void applyGravity(Entity entity, double offset) {
        entity.setDeltaMovement(entity.getDeltaMovement().add(0, offset, 0));
    }
}