package dinner.dev.endless_gravity;

import dinner.dev.endless_gravity.network.ConfigSync;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleFactory;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleRegistry;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;

/**
 * A per-world vanilla game rule that turns this mod's gravity off for that world.
 *
 * <p>Because vanilla does not sync custom boolean game rules to clients, the server's
 * value is carried in {@link dinner.dev.endless_gravity.network.ConfigSyncPayload} and
 * applied via {@link #setClientSyncedEnabled(Boolean)}.
 */
public final class GravityGameRules {

    private GravityGameRules() {}

    private static volatile Boolean clientSyncedEnabled = null;

    public static final GameRules.Key<GameRules.BooleanValue> ENABLE =
            GameRuleRegistry.register("endlessGravity",
                    GameRules.Category.MISC,
                    GameRuleFactory.createBooleanRule(true, (server, rule) -> {
                        if (server != null) {
                            ConfigSync.broadcast(server);
                        }
                    }));

    /** Forces class initialisation so the rule is registered before any world loads. */
    public static void init() {}

    /** Sets (or clears with {@code null}) the server-synced value used on remote clients. */
    public static void setClientSyncedEnabled(Boolean enabled) {
        clientSyncedEnabled = enabled;
    }

    /** Whether this mod applies gravity in the given level. */
    public static boolean enabled(Level level) {
        if (level.isClientSide()) {
            Boolean synced = clientSyncedEnabled;
            if (synced != null) return synced;
        }
        return level.getGameRules().getBoolean(ENABLE);
    }
}
