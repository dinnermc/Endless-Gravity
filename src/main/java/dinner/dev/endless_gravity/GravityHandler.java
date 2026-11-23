package dinner.dev.endless_gravity;

import dinner.dev.endless_gravity.api.GravityChannel;
import dinner.dev.endless_gravity.api.GravityContext;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.FlyingMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

@EventBusSubscriber(modid = EndlessGravity.MODID)
public class GravityHandler {

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Pre event) {
        Entity entity = event.getEntity();

        // Only apply to living entities (players, mobs, etc.)
        if (!(entity instanceof LivingEntity)) return;

        Level level = entity.level();

        // Server-authoritative: skip on the logical client. The local player's reduced gravity
        // is applied client-side by ClientGravityHandler, so singleplayer must not apply it twice.
        if (level.isClientSide()) return;

        // Skip gravity when paused (prevents velocity accumulation during pause)
        MinecraftServer server = level.getServer();
        if (server != null && server.isPaused()) return;

        GravityChannel channel = (entity instanceof Player) ? GravityChannel.PLAYER : GravityChannel.MOB;
        if (!GravityResolver.canApply(entity, channel)) return;

        GravityContext context = GravityResolver.compute(entity, channel);
        if (context.applicable()) {
            entity.setDeltaMovement(entity.getDeltaMovement().add(0, context.offset(), 0));
        }
        GravityResolver.applyAtmosphericDrag(entity);
    }

    /**
     * Flying or immobile living entities must not receive reduced-gravity impulses: they hover on
     * their own, or are anchored (riding, climbing, in lava, asleep). Mobs with bespoke move
     * controls (blaze, vex, wither) are not covered here, so the gravity_immune tag stays as the
     * safety net.
     */
    public static boolean isFlightOrImmobile(LivingEntity entity) {
        if (entity.isNoGravity()) return true;
        if (entity.isPassenger()) return true;
        if (entity.onClimbable()) return true;
        if (entity.isInLava()) return true;
        if (entity.isSleeping()) return true;
        if (entity instanceof FlyingAnimal) return true;
        if (entity instanceof FlyingMob) return true;
        if (entity instanceof Mob mob && mob.getMoveControl() instanceof FlyingMoveControl) return true;
        return false;
    }

    public static boolean isEndOrSable(Level level) {
        if (level.dimension() == Level.END) return true;
        return level.dimension().location().getNamespace().equals("sable");
    }

    public static boolean isEnd(Level level) {
        return level.dimension() == Level.END;
    }

    public static boolean isOverworld(Level level) {
        return level.dimension() == Level.OVERWORLD;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onFallDamage(LivingFallEvent event) {
        LivingEntity entity = event.getEntity();
        Level level = entity.level();
        if (!GravityGameRules.enabled(level)) return;

        boolean isPlayer = entity instanceof Player;
        double fallStartY = AtmosphereLayers.getFallStartAltitude(
                EndlessGravityAPI.getRealY(entity), event.getDistance());

        boolean cosmonauticsBackoff = EndlessGravityAPI.isOverworldOrSable(level)
                && EndlessGravityAPI.isCosmonauticsInstalled();

        CustomDimConfig profile = Config.getCustomDimConfig(level.dimension().location()).orElse(null);

        // Altitude-based immunity in atmosphere profiles (always checked first).
        if (!cosmonauticsBackoff
                && profile != null
                && profile.getMode() == CustomDimConfig.Mode.ATMOSPHERE
                && profile.getNoFallDamageAltitude() != null
                && fallStartY > profile.getNoFallDamageAltitude()) {
            event.setDamageMultiplier(0.0F);
            event.setDistance(0.0F);
            return;
        }

        // Legacy fallback for Sable sub-levels without an explicit dimension profile.
        if (!cosmonauticsBackoff
                && profile == null
                && Config.COMMON.enableAtmosphere.get()
                && EndlessGravityAPI.isSableManaged(level)
                && fallStartY > Config.COMMON.noFallDamageAltitude.get()) {
            event.setDamageMultiplier(0.0F);
            event.setDistance(0.0F);
            return;
        }

        // Per-dimension explicit fall-damage mode overrides global End/Sable behavior.
        if (profile != null && profile.getFallDamageMode() != CustomDimConfig.FallDamageMode.INHERIT) {
            switch (profile.getFallDamageMode()) {
                case NORMAL -> { return; }
                case DISABLED -> {
                    event.setDamageMultiplier(0.0F);
                    event.setDistance(0.0F);
                    return;
                }
                case PROPORTIONAL -> {
                    applyProportionalFallDamage(entity, isPlayer, event);
                    return;
                }
                default -> {}
            }
        }

        if (!isEndOrSable(level)) return;
        int mode = isPlayer ? Config.COMMON.fallDamageMode.get() : Config.COMMON.mobFallDamageMode.get();
        if (mode == 1) {
            event.setDamageMultiplier(0.0F);
            event.setDistance(0.0F);
        } else if (mode == 2) {
            applyProportionalFallDamage(entity, isPlayer, event);
        }
    }

    private static void applyProportionalFallDamage(LivingEntity entity, boolean isPlayer, LivingFallEvent event) {
        if (EndlessGravityAPI.isGravityImmune(entity)) return;
        GravityChannel channel = isPlayer ? GravityChannel.PLAYER : GravityChannel.MOB;
        GravityContext ctx = GravityResolver.compute(entity, channel);
        if (!ctx.applicable()) return;
        double base = ctx.baseGravity();
        if (base <= 0.0) return;
        double ratio = AtmosphereLayers.clamp(ctx.effectiveGravity() / base, 0.0, 4.0);
        if (ratio <= 0.01) {
            event.setDamageMultiplier(0.0F);
            event.setDistance(0.0F);
        } else {
            event.setDistance((float) (event.getDistance() * ratio));
        }
    }
}
