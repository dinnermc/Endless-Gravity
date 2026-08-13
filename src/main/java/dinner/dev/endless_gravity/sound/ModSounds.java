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

    public static void init(IEventBus modEventBus) {
        SOUNDS.register(modEventBus);
    }
}