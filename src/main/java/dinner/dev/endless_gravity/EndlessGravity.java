package dinner.dev.endless_gravity;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

@Mod(EndlessGravity.MODID)
public class EndlessGravity {
    public static final String MODID = "endless_gravity";
    private static final Logger LOGGER = LogUtils.getLogger();

    public EndlessGravity(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.COMMON_SPEC);

        modContainer.registerExtensionPoint(
                IConfigScreenFactory.class,
                (mc, parent) -> new EndlessGravityConfigScreen(parent)
        );

        LOGGER.info("Endless Gravity loaded");
    }
}
