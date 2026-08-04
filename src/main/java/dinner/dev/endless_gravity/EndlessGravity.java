package dinner.dev.endless_gravity;

import dinner.dev.endless_gravity.block.ModBlocks;
import dinner.dev.endless_gravity.block.StarshipBlock;
import dinner.dev.endless_gravity.compat.sable.AtmosphereForceGroups;
import dinner.dev.endless_gravity.item.ModItems;
import dinner.dev.endless_gravity.network.ConfigSyncPayload;
import dinner.dev.endless_gravity.particle.ModParticles;
import dinner.dev.endless_gravity.sound.ModSounds;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

@Mod(EndlessGravity.MODID)
public class EndlessGravity {
    public static final String MODID = "endless_gravity";
    private static final Logger LOGGER = LogUtils.getLogger();

    public EndlessGravity(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.COMMON_SPEC);
        ModBlocks.init(modEventBus);
        AtmosphereForceGroups.init(modEventBus);
        ModItems.init(modEventBus);
        ModSounds.init(modEventBus);
        ModParticles.init(modEventBus);
        modEventBus.addListener(RegisterPayloadHandlersEvent.class, event -> {
            PayloadRegistrar registrar = event.registrar("1");
            registrar.playToClient(
                    ConfigSyncPayload.ID,
                    ConfigSyncPayload.STREAM_CODEC,
                    ConfigSyncPayload::handle
            );
        });

        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerLoggedInEvent.class, event -> {
            if (event.getEntity() instanceof ServerPlayer serverPlayer) {
                PacketDistributor.sendToPlayer(serverPlayer, ConfigSyncPayload.fromConfig());
            }
        });

        NeoForge.EVENT_BUS.addListener(ServerTickEvent.Pre.class,
                event -> StarshipBlock.onServerTick(event.getServer()));

        LOGGER.info("Endless Gravity loaded");
    }
}
