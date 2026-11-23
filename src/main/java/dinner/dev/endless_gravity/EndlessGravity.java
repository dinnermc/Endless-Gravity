package dinner.dev.endless_gravity;

import com.mojang.logging.LogUtils;
import dinner.dev.endless_gravity.command.EndlessGravityCommand;
import dinner.dev.endless_gravity.network.ConfigSync;
import dinner.dev.endless_gravity.network.ConfigSyncPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.registry.FabricBrewingRecipeRegistryBuilder;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potions;
import org.slf4j.Logger;

public class EndlessGravity implements ModInitializer {
    public static final String MODID = "endless_gravity";
    public static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public void onInitialize() {
        Config.load();
        ModEffects.register();
        ModPotions.register();
        GravityGameRules.init();

        ResourceManagerHelper.get(PackType.SERVER_DATA)
                .registerReloadListener(new DimensionProfileReloadListener());
        ResourceManagerHelper.get(PackType.SERVER_DATA)
                .registerReloadListener(new BiomeGravityReloadListener());

        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resourceManager, success) -> {
            if (success) {
                ConfigSync.broadcast(server);
            }
        });

        FabricBrewingRecipeRegistryBuilder.BUILD.register(builder -> {
            var reg = BuiltInRegistries.POTION;
            builder.addMix(Potions.AWKWARD, Items.SHULKER_SHELL, reg.wrapAsHolder(ModPotions.WEIGHTLESSNESS));
            builder.addMix(Potions.AWKWARD, Items.IRON_INGOT, reg.wrapAsHolder(ModPotions.HEAVINESS));
            builder.addMix(reg.wrapAsHolder(ModPotions.WEIGHTLESSNESS), Items.REDSTONE, reg.wrapAsHolder(ModPotions.LONG_WEIGHTLESSNESS));
            builder.addMix(reg.wrapAsHolder(ModPotions.HEAVINESS), Items.REDSTONE, reg.wrapAsHolder(ModPotions.LONG_HEAVINESS));
            builder.addMix(reg.wrapAsHolder(ModPotions.WEIGHTLESSNESS), Items.GLOWSTONE_DUST, reg.wrapAsHolder(ModPotions.STRONG_WEIGHTLESSNESS));
            builder.addMix(reg.wrapAsHolder(ModPotions.HEAVINESS), Items.GLOWSTONE_DUST, reg.wrapAsHolder(ModPotions.STRONG_HEAVINESS));
        });

        GravityHandler.register();

        PayloadTypeRegistry.playS2C().register(ConfigSyncPayload.ID, ConfigSyncPayload.STREAM_CODEC);

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.player;
            if (player != null) {
                ServerPlayNetworking.send(player, ConfigSyncPayload.fromServer(server));
            }
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> Config.saveIfDirty());

        CommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess, environment) -> EndlessGravityCommand.register(dispatcher));

        LOGGER.info("Endless Gravity loaded");
    }
}
