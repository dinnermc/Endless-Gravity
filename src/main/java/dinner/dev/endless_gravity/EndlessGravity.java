package dinner.dev.endless_gravity;

import dinner.dev.endless_gravity.client.EndlessGravityClient;
import dinner.dev.endless_gravity.command.EndlessGravityCommand;
import dinner.dev.endless_gravity.network.ConfigSyncPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

public class EndlessGravity implements ModInitializer {
    public static final String MODID = "endless_gravity";
    public static final Logger LOGGER = LogUtils.getLogger();


    @Override
    public void onInitialize() {
        Config.load();
        ModEffects.register();
        ModPotions.register();
        GravityGameRules.init();

        net.fabricmc.fabric.api.registry.FabricBrewingRecipeRegistryBuilder.BUILD.register(builder -> {
            var reg = net.minecraft.core.registries.BuiltInRegistries.POTION;
            builder.addMix(net.minecraft.world.item.alchemy.Potions.AWKWARD, net.minecraft.world.item.Items.PHANTOM_MEMBRANE,
                    reg.wrapAsHolder(ModPotions.WEIGHTLESSNESS));
            builder.addMix(net.minecraft.world.item.alchemy.Potions.AWKWARD, net.minecraft.world.item.Items.IRON_INGOT,
                    reg.wrapAsHolder(ModPotions.HEAVINESS));
            builder.addMix(reg.wrapAsHolder(ModPotions.WEIGHTLESSNESS), net.minecraft.world.item.Items.REDSTONE,
                    reg.wrapAsHolder(ModPotions.LONG_WEIGHTLESSNESS));
            builder.addMix(reg.wrapAsHolder(ModPotions.HEAVINESS), net.minecraft.world.item.Items.REDSTONE,
                    reg.wrapAsHolder(ModPotions.LONG_HEAVINESS));
        });

        GravityHandler.register();

        PayloadTypeRegistry.playS2C().register(ConfigSyncPayload.ID, ConfigSyncPayload.STREAM_CODEC);

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.player;
            if (player != null) {
                ServerPlayNetworking.send(player, ConfigSyncPayload.fromConfig());
            }
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> Config.saveIfDirty());

        CommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess, environment) -> EndlessGravityCommand.register(dispatcher));

        LOGGER.info("Endless Gravity loaded");
    }
}