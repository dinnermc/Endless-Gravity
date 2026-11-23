package dinner.dev.endless_gravity;

import com.mojang.blaze3d.platform.InputConstants;
import dinner.dev.endless_gravity.client.GravityHud;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@EventBusSubscriber(modid = EndlessGravity.MODID, value = Dist.CLIENT)
public class EndlessGravityClient {

    public static final KeyMapping TOGGLE_HUD_KEY = new KeyMapping(
            "key.endless_gravity.toggle_hud",
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            "key.categories.endless_gravity"
    );

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        ModLoadingContext.get().getActiveContainer()
                .registerExtensionPoint(
                        IConfigScreenFactory.class,
                        (mc, parent) -> EndlessGravityConfigScreen.create(parent)
                );
    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(TOGGLE_HUD_KEY);
    }

    @SubscribeEvent
    public static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(
                ResourceLocation.fromNamespaceAndPath(EndlessGravity.MODID, "gravity_hud"),
                (guiGraphics, deltaTracker) -> GravityHud.render(guiGraphics));
    }
}
