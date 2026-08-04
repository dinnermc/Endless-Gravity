package dinner.dev.endless_gravity.compat.sable;

import dev.ryanhcode.sable.api.physics.force.ForceGroup;
import dev.ryanhcode.sable.api.physics.force.ForceGroups;
import dinner.dev.endless_gravity.EndlessGravity;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class AtmosphereForceGroups {

    public static final DeferredRegister<ForceGroup> FORCE_GROUPS =
            DeferredRegister.create(ForceGroups.REGISTRY_KEY, EndlessGravity.MODID);

    public static final DeferredHolder<ForceGroup, ForceGroup> GRAVITY_REDUCTION = FORCE_GROUPS.register("gravity_reduction",
            () -> new ForceGroup(
                    Component.literal("Gravity Reduction"),
                    Component.literal("Counteracts gravity with altitude"),
                    0x00FF00,
                    true
            )
    );

    public static void init(IEventBus modEventBus) {
        FORCE_GROUPS.register(modEventBus);
    }
}