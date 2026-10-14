package dinner.dev.endless_gravity;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * The mod's status effects:
 * <ul>
 *     <li>{@code endless_gravity:weightlessness} - "Weightlessness": lighter gravity (upward offset).</li>
 *     <li>{@code endless_gravity:heaviness} - "Heaviness": heavier gravity (downward offset).</li>
 * </ul>
 * The effects themselves do nothing on tick; their gravity contribution is read by
 * {@link GravityResolver} from the entity's active effects.
 */
public final class ModEffects {

    private ModEffects() {}

    public static final MobEffect WEIGHTLESSNESS = register("weightlessness", MobEffectCategory.BENEFICIAL, 0x7FCFFF);
    public static final MobEffect HEAVINESS = register("heaviness", MobEffectCategory.HARMFUL, 0x8B5A2B);

    private static MobEffect register(String name, MobEffectCategory category, int color) {
        return Registry.register(
                BuiltInRegistries.MOB_EFFECT,
                ResourceLocation.fromNamespaceAndPath(EndlessGravity.MODID, name),
                new SimpleEffect(category, color));
    }

    /** No-op: effects are registered eagerly by the static initialisers. */
    public static void register() {}

    /** The Weightlessness effect instance, for comparing against an entity's active effects. */
    public static MobEffect weightlessness() {
        return WEIGHTLESSNESS;
    }

    /** The Heaviness effect instance. */
    public static MobEffect heaviness() {
        return HEAVINESS;
    }

    /** The Weightlessness effect holder, for building potions. */
    public static net.minecraft.core.Holder<MobEffect> weightlessnessHolder() {
        return BuiltInRegistries.MOB_EFFECT.wrapAsHolder(WEIGHTLESSNESS);
    }

    /** The Heaviness effect holder, for building potions. */
    public static net.minecraft.core.Holder<MobEffect> heavinessHolder() {
        return BuiltInRegistries.MOB_EFFECT.wrapAsHolder(HEAVINESS);
    }

    /** A plain effect with no per-tick behaviour; gravity reads it via {@link GravityResolver}. */
    private static final class SimpleEffect extends MobEffect {
        SimpleEffect(MobEffectCategory category, int color) {
            super(category, color);
        }
    }
}
