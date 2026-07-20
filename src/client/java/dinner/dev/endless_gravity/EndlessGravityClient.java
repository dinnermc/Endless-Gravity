package dinner.dev.endless_gravity;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@EventBusSubscriber(modid = EndlessGravity.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public class EndlessGravityClient {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        net.neoforged.fml.ModLoadingContext.get().getActiveContainer()
                .registerExtensionPoint(
                        IConfigScreenFactory.class,
                        (mc, parent) -> new EndlessGravityConfigScreen(parent)
                );
    }
}
