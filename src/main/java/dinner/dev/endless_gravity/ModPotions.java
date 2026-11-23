package dinner.dev.endless_gravity;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.alchemy.Potion;

/**
 * The mod's potions, built from {@link ModEffects}. Brewing recipes are registered
 * in {@code EndlessGravity} via Fabric's {@code FabricBrewingRecipeRegistryBuilder}.
 */
public final class ModPotions {

    private ModPotions() {}

    private static final int SHORT_TICKS = 20 * 90;
    private static final int LONG_TICKS = 20 * 240;
    private static final int STRONG_TICKS = 20 * 45;

    public static final Potion WEIGHTLESSNESS = register("weightlessness", ModEffects.weightlessnessHolder(), SHORT_TICKS, 0);
    public static final Potion LONG_WEIGHTLESSNESS = register("long_weightlessness", ModEffects.weightlessnessHolder(), LONG_TICKS, 0);
    public static final Potion STRONG_WEIGHTLESSNESS = register("strong_weightlessness", ModEffects.weightlessnessHolder(), STRONG_TICKS, 1);
    public static final Potion HEAVINESS = register("heaviness", ModEffects.heavinessHolder(), SHORT_TICKS, 0);
    public static final Potion LONG_HEAVINESS = register("long_heaviness", ModEffects.heavinessHolder(), LONG_TICKS, 0);
    public static final Potion STRONG_HEAVINESS = register("strong_heaviness", ModEffects.heavinessHolder(), STRONG_TICKS, 1);

    private static Potion register(String name, net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect, int ticks, int amplifier) {
        return Registry.register(
                BuiltInRegistries.POTION,
                ResourceLocation.fromNamespaceAndPath(EndlessGravity.MODID, name),
                new Potion(new MobEffectInstance(effect, ticks, amplifier)));
    }

    /** No-op: potions are registered eagerly by the static initialisers. */
    public static void register() {}
}
