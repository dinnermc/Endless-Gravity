package dinner.dev.endless_gravity;

import net.fabricmc.fabric.api.gamerule.v1.GameRuleFactory;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleRegistry;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;

/**
 * A per-world vanilla game rule that turns this mod's gravity off for that world.
 *
 * <p>Registered eagerly ({@link #init()}) through the Fabric API so every {@link GameRules}
 * instance the game creates already contains it. NeoForge exposes the same rule through
 * vanilla's {@code GameRules.register} (which NeoForge widens to public), so the rule name,
 * category and behaviour are identical on both loaders.
 */
public final class GravityGameRules {

    private GravityGameRules() {}

    public static final GameRules.Key<GameRules.BooleanValue> ENABLE =
            GameRuleRegistry.register("endlessGravity",
                    GameRules.Category.MISC,
                    GameRuleFactory.createBooleanRule(true));

    /** Forces class initialisation so the rule is registered before any world loads. */
    public static void init() {}

    /** Whether this mod applies gravity in the given level. */
    public static boolean enabled(Level level) {
        return level.getGameRules().getBoolean(ENABLE);
    }
}
