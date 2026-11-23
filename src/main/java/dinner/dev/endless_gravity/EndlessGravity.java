package dinner.dev.endless_gravity;

import com.mojang.logging.LogUtils;
import dinner.dev.endless_gravity.command.EndlessGravityCommand;
import dinner.dev.endless_gravity.compat.sable.AtmosphereForceGroups;
import dinner.dev.endless_gravity.network.ConfigSyncPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potions;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.brewing.RegisterBrewingRecipesEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.slf4j.Logger;

@Mod(EndlessGravity.MODID)
public class EndlessGravity {
    public static final String MODID = "endless_gravity";
    private static final Logger LOGGER = LogUtils.getLogger();

    public EndlessGravity(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.COMMON_SPEC);
        ModEffects.register(modEventBus);
        ModPotions.register(modEventBus);
        GravityGameRules.init();
        if (ModList.get().isLoaded("sable")) {
            AtmosphereForceGroups.init(modEventBus);
        }
        NeoForge.EVENT_BUS.addListener(RegisterCommandsEvent.class, event ->
                EndlessGravityCommand.register(event.getDispatcher()));
        NeoForge.EVENT_BUS.addListener(net.neoforged.neoforge.event.AddReloadListenerEvent.class, event -> {
            event.addListener(new DimensionProfileReloadListener());
            event.addListener(new BiomeGravityReloadListener());
        });
        NeoForge.EVENT_BUS.addListener(net.neoforged.neoforge.event.OnDatapackSyncEvent.class, event -> {
            ServerPlayer player = event.getPlayer();
            if (player != null) {
                PacketDistributor.sendToPlayer(player, ConfigSyncPayload.fromServer(player.getServer()));
            } else {
                event.getRelevantPlayers().forEach(p ->
                        PacketDistributor.sendToPlayer(p, ConfigSyncPayload.fromServer(p.getServer())));
            }
        });
        NeoForge.EVENT_BUS.addListener(RegisterBrewingRecipesEvent.class, event -> {
            var builder = event.getBuilder();
            builder.addMix(Potions.AWKWARD, Items.SHULKER_SHELL, ModPotions.WEIGHTLESSNESS);
            builder.addMix(Potions.AWKWARD, Items.IRON_INGOT, ModPotions.HEAVINESS);
            builder.addMix(ModPotions.WEIGHTLESSNESS, Items.REDSTONE, ModPotions.LONG_WEIGHTLESSNESS);
            builder.addMix(ModPotions.HEAVINESS, Items.REDSTONE, ModPotions.LONG_HEAVINESS);
            builder.addMix(ModPotions.WEIGHTLESSNESS, Items.GLOWSTONE_DUST, ModPotions.STRONG_WEIGHTLESSNESS);
            builder.addMix(ModPotions.HEAVINESS, Items.GLOWSTONE_DUST, ModPotions.STRONG_HEAVINESS);
        });
        modEventBus.addListener(RegisterPayloadHandlersEvent.class, event -> {
            PayloadRegistrar registrar = event.registrar("4");
            registrar.playToClient(
                    ConfigSyncPayload.ID,
                    ConfigSyncPayload.STREAM_CODEC,
                    ConfigSyncPayload::handle
            );
        });

        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerLoggedInEvent.class, event -> {
            if (event.getEntity() instanceof ServerPlayer serverPlayer) {
                PacketDistributor.sendToPlayer(serverPlayer, ConfigSyncPayload.fromServer(serverPlayer.getServer()));
            }
        });

        LOGGER.info("Endless Gravity loaded");
    }
}
