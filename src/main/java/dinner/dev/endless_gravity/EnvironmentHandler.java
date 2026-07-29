package dinner.dev.endless_gravity;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = EndlessGravity.MODID)
public class EnvironmentHandler {

    private static final String TAG_OXYGEN = "endlessgravity_oxygen";

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.isCreative() || player.isSpectator()) return;
        Level level = player.level();
        if (!EndlessGravityAPI.isOverworldOrSable(level)) return;
        if (!Config.COMMON.enableAtmosphere.get()) return;

        double realY = EndlessGravityAPI.getRealY(player);
        if (realY <= EndlessGravityAPI.BASE) return;

        if (Config.COMMON.enableTemperature.get()) {
            double tempProgress = EndlessGravityAPI.getTemperatureProgress(realY);
            if (tempProgress > 0) {
                tickFreezing(player, tempProgress, !level.isClientSide);
            }
        }

        if (Config.COMMON.enableOxygen.get()) {
            double oxyProgress = EndlessGravityAPI.getOxygenProgress(realY);
            if (oxyProgress > 0) {
                tickOxygen(player, oxyProgress, !level.isClientSide);
            }
        }
    }

    private static void tickFreezing(Player player, double progress, boolean server) {
        int interval = (int) Math.round(Config.COMMON.temperatureFreezeInterval.get() / Math.max(0.01, progress));
        if (interval < 1) interval = 1;

        if (player.tickCount % interval == 0) {
            int required = player.getTicksRequiredToFreeze();
            player.setTicksFrozen(required + 5);
        }

        if (server) {
            int frozen = player.getTicksFrozen();
            int required = player.getTicksRequiredToFreeze();
            if (frozen > required) {
                player.hurt(player.damageSources().freeze(), 1.0F);
                player.setTicksFrozen(Math.round(required / 2.0F));
            }
        }
    }

    private static void tickOxygen(Player player, double progress, boolean server) {
        int rate = (int) Math.round(Config.COMMON.oxygenRate.get() / Math.max(0.01, progress));
        if (rate < 1) rate = 1;

        var data = player.getPersistentData();
        int oxygen = data.getInt(TAG_OXYGEN);
        if (oxygen == 0) oxygen = 300;

        if (player.tickCount % rate == 0) {
            oxygen--;
            if (oxygen <= 0) {
                if (server) {
                    player.hurt(player.damageSources().drown(), 1.0F);
                }
                oxygen = 300;
            }
            data.putInt(TAG_OXYGEN, oxygen);
        }

        player.setAirSupply(Math.min(player.getMaxAirSupply(), oxygen));
    }
}
