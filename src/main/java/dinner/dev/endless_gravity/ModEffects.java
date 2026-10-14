package dinner.dev.endless_gravity;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

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

    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(BuiltInRegistries.MOB_EFFECT, EndlessGravity.MODID);

    public static final DeferredHolder<MobEffect, MobEffect> WEIGHTLESSNESS =
            MOB_EFFECTS.register("weightlessness", () -> new SimpleEffect(MobEffectCategory.BENEFICIAL, 0x7FCFFF));

    public static final DeferredHolder<MobEffect, MobEffect> HEAVINESS =
            MOB_EFFECTS.register("heaviness", () -> new SimpleEffect(MobEffectCategory.HARMFUL, 0x8B5A2B));

    public static void register(IEventBus modEventBus) {
        MOB_EFFECTS.register(modEventBus);
    }

    /** The Weightlessness effect instance, for comparing against an entity's active effects. */
    public static MobEffect weightlessness() {
        return WEIGHTLESSNESS.get();
    }

    /** The Heaviness effect instance. */
    public static MobEffect heaviness() {
        return HEAVINESS.get();
    }

    /** The Weightlessness effect holder, for building potions. */
    public static net.minecraft.core.Holder<MobEffect> weightlessnessHolder() {
        return WEIGHTLESSNESS;
    }

    /** The Heaviness effect holder, for building potions. */
    public static net.minecraft.core.Holder<MobEffect> heavinessHolder() {
        return HEAVINESS;
    }

    /** A plain effect with no per-tick behaviour; gravity reads it via {@link GravityResolver}. */
    private static final class SimpleEffect extends MobEffect {
        SimpleEffect(MobEffectCategory category, int color) {
            super(category, color);
        }
    }
}
