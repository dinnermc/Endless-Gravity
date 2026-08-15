package dinner.dev.endless_gravity;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@EventBusSubscriber(modid = EndlessGravity.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class EndlessGravityClient {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        if (!ModList.get().isLoaded("cloth_config")) return;
        net.neoforged.fml.ModLoadingContext.get().getActiveContainer()
                .registerExtensionPoint(
                        IConfigScreenFactory.class,
                        (mc, parent) -> EndlessGravityConfigScreen.create(parent)
                );
    }
}