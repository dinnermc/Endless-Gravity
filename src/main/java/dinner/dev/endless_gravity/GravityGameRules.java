package dinner.dev.endless_gravity;

import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;

/**
 * A per-world vanilla game rule that turns this mod's gravity off for that world.
 *
 * <p>Registered eagerly ({@link #init()}) so every {@link GameRules} instance the game
 * creates already contains it; a rule added after a world exists would be missing from
 * that world's rule map and blow up lookups.
 */
public final class GravityGameRules {

    private GravityGameRules() {}

    public static final GameRules.Key<GameRules.BooleanValue> ENABLE =
            GameRules.register("endlessGravity",
                    GameRules.Category.MISC,
                    GameRules.BooleanValue.create(true));

    /** Forces class initialisation so the rule is registered before any world loads. */
    public static void init() {}

    /** Whether this mod applies gravity in the given level. */
    public static boolean enabled(Level level) {
        return level.getGameRules().getBoolean(ENABLE);
    }
}
