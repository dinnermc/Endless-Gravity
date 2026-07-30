package dinner.dev.endless_gravity;

import dinner.dev.endless_gravity.network.ConfigSyncPayload;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

@Mod(EndlessGravity.MODID)
public class EndlessGravity {
    public static final String MODID = "endless_gravity";
    private static final Logger LOGGER = LogUtils.getLogger();

    public EndlessGravity(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.COMMON_SPEC);

        registerSableMixinConfig();

        modEventBus.addListener(RegisterPayloadHandlersEvent.class, event -> {
            PayloadRegistrar registrar = event.registrar("1");
            registrar.playToClient(
                    ConfigSyncPayload.ID,
                    ConfigSyncPayload.STREAM_CODEC,
                    ConfigSyncPayload::handle
            );
        });

        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerLoggedInEvent.class, event -> {
            if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                PacketDistributor.sendToPlayer(serverPlayer, ConfigSyncPayload.fromConfig());
            }
        });

        LOGGER.info("Endless Gravity loaded");
    }

    private static void registerSableMixinConfig() {
        try {
            org.spongepowered.asm.mixin.Mixins.addConfiguration("endless_gravity.sable.mixins.json");
        } catch (Exception e) {
            LOGGER.warn("Failed to register Sable mixin config (Sable may not be installed)", e);
        }
    }
}
