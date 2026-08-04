package dinner.dev.endless_gravity.sound;

import dinner.dev.endless_gravity.EndlessGravity;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModSounds {

    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(Registries.SOUND_EVENT, EndlessGravity.MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> DINNER_PLUSH_TOY =
            SOUNDS.register("dinner_plush_toy", () ->
                    SoundEvent.createVariableRangeEvent(
                            ResourceLocation.fromNamespaceAndPath(EndlessGravity.MODID, "dinner_plush_toy")
                    )
            );

    private static final float STARSHIP_SOUND_RANGE = 128.0f;

    public static final DeferredHolder<SoundEvent, SoundEvent> STARSHIP_STARTUP =
            SOUNDS.register("starship_startup", () ->
                    SoundEvent.createFixedRangeEvent(
                            ResourceLocation.fromNamespaceAndPath(EndlessGravity.MODID, "starship_startup"),
                            STARSHIP_SOUND_RANGE
                    )
            );

    public static final DeferredHolder<SoundEvent, SoundEvent> STARSHIP_LOOP =
            SOUNDS.register("starship_loop", () ->
                    SoundEvent.createFixedRangeEvent(
                            ResourceLocation.fromNamespaceAndPath(EndlessGravity.MODID, "starship_loop"),
                            STARSHIP_SOUND_RANGE
                    )
            );

    public static final DeferredHolder<SoundEvent, SoundEvent> STARSHIP_SHUTDOWN =
            SOUNDS.register("starship_shutdown", () ->
                    SoundEvent.createFixedRangeEvent(
                            ResourceLocation.fromNamespaceAndPath(EndlessGravity.MODID, "starship_shutdown"),
                            STARSHIP_SOUND_RANGE
                    )
            );


    public static void init(IEventBus modEventBus) {
        SOUNDS.register(modEventBus);
    }
}
