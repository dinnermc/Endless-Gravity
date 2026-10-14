package dinner.dev.endless_gravity;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.alchemy.Potion;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The mod's potions, built from {@link ModEffects}. Brewing recipes are registered
 * in {@code EndlessGravity} via NeoForge's {@code RegisterBrewingRecipesEvent}.
 */
public final class ModPotions {

    private ModPotions() {}

    public static final DeferredRegister<Potion> POTIONS =
            DeferredRegister.create(BuiltInRegistries.POTION, EndlessGravity.MODID);

    private static final int SHORT_TICKS = 20 * 90;
    private static final int LONG_TICKS = 20 * 240;

    public static final DeferredHolder<Potion, Potion> WEIGHTLESSNESS =
            POTIONS.register("weightlessness", () -> new Potion(new MobEffectInstance(ModEffects.weightlessnessHolder(), SHORT_TICKS)));

    public static final DeferredHolder<Potion, Potion> LONG_WEIGHTLESSNESS =
            POTIONS.register("long_weightlessness", () -> new Potion(new MobEffectInstance(ModEffects.weightlessnessHolder(), LONG_TICKS)));

    public static final DeferredHolder<Potion, Potion> HEAVINESS =
            POTIONS.register("heaviness", () -> new Potion(new MobEffectInstance(ModEffects.heavinessHolder(), SHORT_TICKS)));

    public static final DeferredHolder<Potion, Potion> LONG_HEAVINESS =
            POTIONS.register("long_heaviness", () -> new Potion(new MobEffectInstance(ModEffects.heavinessHolder(), LONG_TICKS)));

    public static void register(IEventBus modEventBus) {
        POTIONS.register(modEventBus);
    }
}
