package dinner.dev.endless_gravity;

import dinner.dev.endless_gravity.item.ModItems;
import dinner.dev.endless_gravity.item.StellarChestplateItem;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = EndlessGravity.MODID)
public class EnvironmentHandler {

    /**
     * Minimum atmosphere progress at which oxygen depletion/suffocation kicks in.
     * With default layers (progress 0.5 at Y 400, 1.0 at Y 3500) this keeps the
     * troposphere breathable so elevated spawns never loop-die.
     */
    public static final double SUFFOCATION_PROGRESS_THRESHOLD = 0.5;

    /**
     * Minimum atmosphere progress at which freezing kicks in. Mirrors the
     * suffocation threshold on the same curve: 0.0 below Y 400 (troposphere,
     * no frost), ramping from 0.5 at Y 400 to 1.0 at Y 3500. Keeps low-altitude
     * builds and valleys free of random cold spikes.
     */
    public static final double FREEZE_PROGRESS_THRESHOLD = 0.5;

    private static final Map<UUID, Integer> suffocationStartTicks = new HashMap<>();
    private static final Map<UUID, Integer> freezeStartTicks = new HashMap<>();

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        // A fresh respawn must never inherit a stale suffocation timer, otherwise the
        // player can be killed the same tick they reappear (infinite death loop).
        suffocationStartTicks.remove(event.getEntity().getUUID());
        freezeStartTicks.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof Player player) {
            suffocationStartTicks.remove(player.getUUID());
            freezeStartTicks.remove(player.getUUID());
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.isCreative() || player.isSpectator()) return;
        Level level = player.level();
        if (level.isClientSide) return;

        boolean isEnd = EndlessGravityAPI.isEnd(level);
        if (!EndlessGravityAPI.isOverworldOrSable(level) && !isEnd) return;

        double protection = getSpaceArmorProtection(player);

        if (isEnd) {
            // The End is a full vacuum at every altitude: freezing and suffocation
            // apply everywhere, and the tank can never recharge.
            if (Config.COMMON.enableTemperature.get()) {
                tickFreezing(player, 1.0, protection);
            } else {
                clearFreezing(player);
            }
            if (Config.COMMON.enableOxygen.get()) {
                tickOxygen(player, 1.0, 0.0);
            }
            return;
        }

        if (!Config.COMMON.enableAtmosphere.get()) return;

        double realY = EndlessGravityAPI.getRealY(player);

        if (realY > EndlessGravityAPI.BASE) {
            if (Config.COMMON.enableTemperature.get()) {
                double tempProgress = EndlessGravityAPI.getTemperatureProgress(realY);
                if (tempProgress >= FREEZE_PROGRESS_THRESHOLD) {
                    // Remap so freezing starts at 0 exactly above Y 400 (progress 0.5)
                    // and ramps to 1.0 at deep space (progress 1.0).
                    double freezeProgress = (tempProgress - FREEZE_PROGRESS_THRESHOLD) / (1.0 - FREEZE_PROGRESS_THRESHOLD);
                    tickFreezing(player, freezeProgress, protection);
                } else {
                    clearFreezing(player);
                }
            } else {
                clearFreezing(player);
            }
        } else {
            clearFreezing(player);
        }

        if (Config.COMMON.enableOxygen.get()) {
            tickOxygen(player, EndlessGravityAPI.getOxygenProgress(realY), EndlessGravityAPI.getPressure(realY));
        }
    }

    private static double getSpaceArmorProtection(Player player) {
        int pieces = 0;
        for (ItemStack stack : player.getArmorSlots()) {
            if (stack.getItem() == ModItems.STELLAR_HELMET.get()
                    || stack.getItem() == ModItems.STELLAR_CHESTPLATE.get()
                    || stack.getItem() == ModItems.STELLAR_LEGGINGS.get()
                    || stack.getItem() == ModItems.STELLAR_BOOTS.get()) {
                pieces++;
            }
        }
        return pieces / 4.0;
    }

    private static void tickFreezing(Player player, double progress, double protection) {
        double effectiveProgress = progress * (1.0 - protection);
        if (effectiveProgress <= 0) {
            clearFreezing(player);
            return;
        }

        int interval = (int) Math.round(Config.COMMON.temperatureFreezeInterval.get() / Math.max(0.01, effectiveProgress));
        if (interval < 1) interval = 1;

        if (player.tickCount % interval == 0) {
            int required = player.getTicksRequiredToFreeze();
            player.setTicksFrozen(required + 5);

            // Hypothermia scales with altitude and the time spent exposed: the longer you
            // stay up there, the harder every frost burst hits. Only the Stellar Suit stops it.
            int start = freezeStartTicks.computeIfAbsent(player.getUUID(), id -> player.tickCount);
            double exposureSeconds = (player.tickCount - start) / 20.0;
            float damage = (float) (1.0 + (6.0 + 0.4 * exposureSeconds) * effectiveProgress);
            player.hurt(player.damageSources().freeze(), damage);
        }
    }

    private static void clearFreezing(Player player) {
        freezeStartTicks.remove(player.getUUID());
    }

    private static void tickOxygen(Player player, double oxyProgress, double pressure) {
        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        ItemStack chestplate = player.getItemBySlot(EquipmentSlot.CHEST);
        if (helmet.getItem() == ModItems.STELLAR_HELMET.get()
                && chestplate.getItem() == ModItems.STELLAR_CHESTPLATE.get()) {
            // Helmet + chestplate: breathe from the tank in the chestplate
            tickSuitTank(player, chestplate, oxyProgress, pressure);
        } else {
            // Missing helmet or chestplate: no oxygen supply, suffocate in thin air
            if (oxyProgress >= SUFFOCATION_PROGRESS_THRESHOLD) {
                tickSuffocation(player);
            } else {
                clearSuffocation(player);
            }
        }
    }

    private static void tickSuitTank(Player player, ItemStack chestplate, double oxyProgress, double pressure) {
        int tank = StellarChestplateItem.getTank(chestplate);
        int capacity = Config.COMMON.oxygenTankCapacity.get();

        if (tank > 0) {
            clearSuffocation(player);
        }

        if (pressure < 1.0) {
            // Atmosphere thinner than full pressure: consume oxygen, faster with altitude
            int rate = (int) Math.round(Config.COMMON.oxygenRate.get() / Math.max(0.01, oxyProgress));
            if (rate < 1) rate = 1;

            if (player.tickCount % rate == 0) {
                tank = Math.max(0, tank - 1);
                if (tank <= 0) {
                    tickSuffocation(player);
                }
            }
        } else if (tank < capacity) {
            // Full atmosphere pressure: recharge the tank
            clearSuffocation(player);
            int rechargeRate = Config.COMMON.oxygenRechargeRate.get();
            if (player.tickCount % rechargeRate == 0) {
                tank = Math.min(capacity, tank + 1);
            }
        }

        if (StellarChestplateItem.getTank(chestplate) != tank) {
            StellarChestplateItem.setTank(chestplate, tank);
        }
    }

    private static void tickSuffocation(Player player) {
        if (!player.isAlive()) return;

        int fadeTicks = Config.COMMON.oxygenSuffocationFadeTicks.get();
        if (fadeTicks < 1) fadeTicks = 1;

        Integer start = suffocationStartTicks.get(player.getUUID());
        if (start == null) {
            suffocationStartTicks.put(player.getUUID(), player.tickCount);
            return;
        }

        if (player.tickCount - start >= fadeTicks) {
            suffocationStartTicks.remove(player.getUUID());
            player.hurt(player.damageSources().drown(), 1000.0F);
        }
    }

    private static void clearSuffocation(Player player) {
        suffocationStartTicks.remove(player.getUUID());
    }
}
