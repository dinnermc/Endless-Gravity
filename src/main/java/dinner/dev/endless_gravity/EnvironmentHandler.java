package dinner.dev.endless_gravity;

import dinner.dev.endless_gravity.item.ModItems;
import dinner.dev.endless_gravity.item.StellarChestplateItem;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = EndlessGravity.MODID)
public class EnvironmentHandler {

    private static final Map<UUID, Integer> suffocationStartTicks = new HashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.isCreative() || player.isSpectator()) return;
        Level level = player.level();
        if (level.isClientSide) return;
        if (!EndlessGravityAPI.isOverworldOrSable(level)) return;
        if (!Config.COMMON.enableAtmosphere.get()) return;

        double realY = EndlessGravityAPI.getRealY(player);

        double protection = getSpaceArmorProtection(player);

        if (realY > EndlessGravityAPI.BASE) {
            if (Config.COMMON.enableTemperature.get()) {
                double tempProgress = EndlessGravityAPI.getTemperatureProgress(realY);
                if (tempProgress > 0) {
                    tickFreezing(player, tempProgress, protection);
                }
            }
        }

        if (Config.COMMON.enableOxygen.get()) {
            tickOxygen(player, realY);
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
        if (effectiveProgress <= 0) return;

        int interval = (int) Math.round(Config.COMMON.temperatureFreezeInterval.get() / Math.max(0.01, effectiveProgress));
        if (interval < 1) interval = 1;

        if (player.tickCount % interval == 0) {
            int required = player.getTicksRequiredToFreeze();
            player.setTicksFrozen(required + 5);
        }

        int frozen = player.getTicksFrozen();
        int required = player.getTicksRequiredToFreeze();
        if (frozen > required) {
            player.hurt(player.damageSources().freeze(), 1.0F);
            player.setTicksFrozen(Math.round(required / 2.0F));
        }
    }

    private static void tickOxygen(Player player, double realY) {
        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        ItemStack chestplate = player.getItemBySlot(EquipmentSlot.CHEST);
        if (helmet.getItem() == ModItems.STELLAR_HELMET.get()
                && chestplate.getItem() == ModItems.STELLAR_CHESTPLATE.get()) {
            // Helmet + chestplate: breathe from the tank in the chestplate
            tickSuitTank(player, chestplate, realY);
        } else {
            // Missing helmet or chestplate: no oxygen supply, suffocate
            double oxyProgress = EndlessGravityAPI.getOxygenProgress(realY);
            if (oxyProgress > 0) {
                tickSuffocation(player);
            } else {
                clearSuffocation(player);
            }
        }
    }

    private static void tickSuitTank(Player player, ItemStack chestplate, double realY) {
        int tank = StellarChestplateItem.getTank(chestplate);
        int capacity = Config.COMMON.oxygenTankCapacity.get();

        if (tank > 0) {
            clearSuffocation(player);
        }

        if (AtmosphereLayers.getPressure(realY) < 1.0) {
            // Atmosphere thinner than full pressure: consume oxygen, faster with altitude
            double oxyProgress = EndlessGravityAPI.getOxygenProgress(realY);
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

        player.setAirSupply(Math.min(player.getMaxAirSupply(), tank));
    }

    private static void tickSuffocation(Player player) {
        player.setAirSupply(0);
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
