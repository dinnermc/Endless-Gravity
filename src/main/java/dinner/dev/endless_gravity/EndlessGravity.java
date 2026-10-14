package dinner.dev.endless_gravity;

import dinner.dev.endless_gravity.compat.sable.AtmosphereForceGroups;
import dinner.dev.endless_gravity.command.EndlessGravityCommand;
import dinner.dev.endless_gravity.network.ConfigSyncPayload;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

@Mod(EndlessGravity.MODID)
public class EndlessGravity {
    public static final String MODID = "endless_gravity";
    private static final Logger LOGGER = LogUtils.getLogger();

    public EndlessGravity(net.neoforged.bus.api.IEventBus modEventBus, net.neoforged.fml.ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.COMMON_SPEC);
        ModEffects.register(modEventBus);
        ModPotions.register(modEventBus);
        GravityGameRules.init();
        if (ModList.get().isLoaded("sable")) {
            AtmosphereForceGroups.init(modEventBus);
        }
        NeoForge.EVENT_BUS.addListener(RegisterCommandsEvent.class, event ->
                EndlessGravityCommand.register(event.getDispatcher()));
        NeoForge.EVENT_BUS.addListener(net.neoforged.neoforge.event.brewing.RegisterBrewingRecipesEvent.class, event -> {
            var builder = event.getBuilder();
            builder.addMix(net.minecraft.world.item.alchemy.Potions.AWKWARD, net.minecraft.world.item.Items.PHANTOM_MEMBRANE, ModPotions.WEIGHTLESSNESS);
            builder.addMix(net.minecraft.world.item.alchemy.Potions.AWKWARD, net.minecraft.world.item.Items.IRON_INGOT, ModPotions.HEAVINESS);
            builder.addMix(ModPotions.WEIGHTLESSNESS, net.minecraft.world.item.Items.REDSTONE, ModPotions.LONG_WEIGHTLESSNESS);
            builder.addMix(ModPotions.HEAVINESS, net.minecraft.world.item.Items.REDSTONE, ModPotions.LONG_HEAVINESS);
        });
        modEventBus.addListener(RegisterPayloadHandlersEvent.class, event -> {
            PayloadRegistrar registrar = event.registrar("1");
            registrar.playToClient(
                    ConfigSyncPayload.ID,
                    ConfigSyncPayload.STREAM_CODEC,
                    ConfigSyncPayload::handle
            );
        });

        NeoForge.EVENT_BUS.addListener(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent.class, event -> {
            if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(serverPlayer, ConfigSyncPayload.fromConfig());
            }
        });

        LOGGER.info("Endless Gravity loaded");
    }
}