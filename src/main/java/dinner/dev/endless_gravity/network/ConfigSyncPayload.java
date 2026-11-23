package dinner.dev.endless_gravity.network;

import dinner.dev.endless_gravity.Config;
import dinner.dev.endless_gravity.GravityGameRules;
import dinner.dev.endless_gravity.api.BiomeGravityRegistry;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.List;

public record ConfigSyncPayload(
        boolean endEntityGravity,
        boolean enablePlayerGravity, double playerGravityOffset,
        boolean enableItemGravity, double itemGravityOffset,
        boolean enableArrowGravity, double arrowGravityOffset,
        boolean enableThrownGravity, double thrownGravityOffset,
        int fallDamageMode,
        int mobFallDamageMode,
        boolean enableBlockGravity, double blockGravityOffset,
        boolean enableAtmosphere, boolean enableAtmosphericDrag, boolean overworldEntityGravity, double atmosphereGravityMax,
        double noFallDamageAltitude,
        double atmosphereMuffleGain, double atmosphereMuffleGainHF,
        List<String> atmosphereLayers,
        boolean enableMobGravity, double mobGravityOffset,
        boolean enablePotionGravity, double weightlessnessGravityOffset, double heavinessGravityOffset,
        List<String> gravityImmuneEntities,
        List<String> dimensionProfiles,
        boolean gameRuleEnabled,
        List<String> biomeRules
) implements CustomPacketPayload {

    private static final int MAX_IMMUNE_ENTITIES = 4096;

    public static final CustomPacketPayload.Type<ConfigSyncPayload> ID =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("endless_gravity", "config_sync"));

    public static final StreamCodec<ByteBuf, ConfigSyncPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public void encode(ByteBuf buf, ConfigSyncPayload p) {
            buf.writeBoolean(p.endEntityGravity());
            buf.writeBoolean(p.enablePlayerGravity());
            buf.writeDouble(p.playerGravityOffset());
            buf.writeBoolean(p.enableItemGravity());
            buf.writeDouble(p.itemGravityOffset());
            buf.writeBoolean(p.enableArrowGravity());
            buf.writeDouble(p.arrowGravityOffset());
            buf.writeBoolean(p.enableThrownGravity());
            buf.writeDouble(p.thrownGravityOffset());
            buf.writeInt(p.fallDamageMode());
            buf.writeInt(p.mobFallDamageMode());
            buf.writeBoolean(p.enableBlockGravity());
            buf.writeDouble(p.blockGravityOffset());
            buf.writeBoolean(p.enableAtmosphere());
            buf.writeBoolean(p.enableAtmosphericDrag());
            buf.writeBoolean(p.overworldEntityGravity());
            buf.writeDouble(p.atmosphereGravityMax());
            buf.writeDouble(p.noFallDamageAltitude());
            buf.writeDouble(p.atmosphereMuffleGain());
            buf.writeDouble(p.atmosphereMuffleGainHF());
            buf.writeInt(p.atmosphereLayers().size());
            for (String layer : p.atmosphereLayers()) {
                ByteBufCodecs.STRING_UTF8.encode(buf, layer);
            }
            buf.writeBoolean(p.enableMobGravity());
            buf.writeDouble(p.mobGravityOffset());
            buf.writeBoolean(p.enablePotionGravity());
            buf.writeDouble(p.weightlessnessGravityOffset());
            buf.writeDouble(p.heavinessGravityOffset());
            buf.writeInt(p.gravityImmuneEntities().size());
            for (String id : p.gravityImmuneEntities()) {
                ByteBufCodecs.STRING_UTF8.encode(buf, id);
            }
            buf.writeInt(p.dimensionProfiles().size());
            for (String profile : p.dimensionProfiles()) {
                ByteBufCodecs.STRING_UTF8.encode(buf, profile);
            }
            buf.writeBoolean(p.gameRuleEnabled());
            buf.writeInt(p.biomeRules().size());
            for (String b : p.biomeRules()) {
                ByteBufCodecs.STRING_UTF8.encode(buf, b);
            }
        }

        @Override
        public ConfigSyncPayload decode(ByteBuf buf) {
            boolean endEntityGravity = buf.readBoolean();
            boolean enablePlayerGravity = buf.readBoolean();
            double playerGravityOffset = buf.readDouble();
            boolean enableItemGravity = buf.readBoolean();
            double itemGravityOffset = buf.readDouble();
            boolean enableArrowGravity = buf.readBoolean();
            double arrowGravityOffset = buf.readDouble();
            boolean enableThrownGravity = buf.readBoolean();
            double thrownGravityOffset = buf.readDouble();
            int fallDamageMode = buf.readInt();
            int mobFallDamageMode = buf.readInt();
            boolean enableBlockGravity = buf.readBoolean();
            double blockGravityOffset = buf.readDouble();
            boolean enableAtmosphere = buf.readBoolean();
            boolean enableAtmosphericDrag = buf.readBoolean();
            boolean overworldEntityGravity = buf.readBoolean();
            double atmosphereGravityMax = buf.readDouble();
            double noFallDamageAltitude = buf.readDouble();
            double atmosphereMuffleGain = buf.readDouble();
            double atmosphereMuffleGainHF = buf.readDouble();
            int layerCount = buf.readInt();
            if (layerCount < 0 || layerCount > Config.CustomDimConfig.MAX_ATMOSPHERE_LAYERS) {
                throw new IllegalArgumentException("Invalid synced atmosphere layer count: " + layerCount);
            }
            List<String> atmosphereLayers = new ArrayList<>(layerCount);
            for (int i = 0; i < layerCount; i++) {
                atmosphereLayers.add(ByteBufCodecs.STRING_UTF8.decode(buf));
            }
            boolean enableMobGravity = buf.readBoolean();
            double mobGravityOffset = buf.readDouble();
            boolean enablePotionGravity = buf.readBoolean();
            double weightlessnessGravityOffset = buf.readDouble();
            double heavinessGravityOffset = buf.readDouble();
            int immuneCount = buf.readInt();
            if (immuneCount < 0 || immuneCount > MAX_IMMUNE_ENTITIES) {
                throw new IllegalArgumentException("Invalid synced immune entity count: " + immuneCount);
            }
            List<String> gravityImmuneEntities = new ArrayList<>(immuneCount);
            for (int i = 0; i < immuneCount; i++) {
                gravityImmuneEntities.add(ByteBufCodecs.STRING_UTF8.decode(buf));
            }
            int profileCount = buf.readInt();
            if (profileCount < 0 || profileCount > Config.MAX_CUSTOM_DIMENSIONS) {
                throw new IllegalArgumentException("Invalid synced dimension profile count: " + profileCount);
            }
            List<String> dimensionProfiles = new ArrayList<>(profileCount);
            for (int i = 0; i < profileCount; i++) {
                dimensionProfiles.add(ByteBufCodecs.STRING_UTF8.decode(buf));
            }
            boolean gameRuleEnabled = buf.readBoolean();
            int biomeCount = buf.readInt();
            if (biomeCount < 0 || biomeCount > BiomeGravityRegistry.MAX_SYNCED_RULES) {
                throw new IllegalArgumentException("Invalid synced biome rule count: " + biomeCount);
            }
            List<String> biomeRules = new ArrayList<>(biomeCount);
            for (int i = 0; i < biomeCount; i++) {
                biomeRules.add(ByteBufCodecs.STRING_UTF8.decode(buf));
            }
            return new ConfigSyncPayload(
                    endEntityGravity,
                    enablePlayerGravity, playerGravityOffset,
                    enableItemGravity, itemGravityOffset,
                    enableArrowGravity, arrowGravityOffset,
                    enableThrownGravity, thrownGravityOffset,
                    fallDamageMode,
                    mobFallDamageMode,
                    enableBlockGravity, blockGravityOffset,
                    enableAtmosphere, enableAtmosphericDrag, overworldEntityGravity, atmosphereGravityMax,
                    noFallDamageAltitude,
                    atmosphereMuffleGain, atmosphereMuffleGainHF,
                    atmosphereLayers,
                    enableMobGravity, mobGravityOffset,
                    enablePotionGravity, weightlessnessGravityOffset, heavinessGravityOffset,
                    gravityImmuneEntities,
                    dimensionProfiles,
                    gameRuleEnabled,
                    biomeRules
            );
        }
    };

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    public static ConfigSyncPayload fromConfig() {
        return fromConfig(true);
    }

    public static ConfigSyncPayload fromServer(MinecraftServer server) {
        boolean ruleEnabled = server == null || server.getGameRules().getBoolean(GravityGameRules.ENABLE);
        return fromConfig(ruleEnabled);
    }

    private static ConfigSyncPayload fromConfig(boolean gameRuleEnabled) {
        return new ConfigSyncPayload(
                Config.COMMON.endEntityGravity.get(),
                Config.COMMON.enablePlayerGravity.get(), Config.COMMON.playerGravityOffset.get(),
                Config.COMMON.enableItemGravity.get(), Config.COMMON.itemGravityOffset.get(),
                Config.COMMON.enableArrowGravity.get(), Config.COMMON.arrowGravityOffset.get(),
                Config.COMMON.enableThrownGravity.get(), Config.COMMON.thrownGravityOffset.get(),
                Config.COMMON.fallDamageMode.get(),
                Config.COMMON.mobFallDamageMode.get(),
                Config.COMMON.enableBlockGravity.get(), Config.COMMON.blockGravityOffset.get(),
                Config.COMMON.enableAtmosphere.get(), Config.COMMON.enableAtmosphericDrag.get(), Config.COMMON.overworldEntityGravity.get(), Config.COMMON.atmosphereGravityMax.get(),
                Config.COMMON.noFallDamageAltitude.get(),
                Config.COMMON.atmosphereMuffleGain.get(), Config.COMMON.atmosphereMuffleGainHF.get(),
                List.copyOf(Config.COMMON.atmosphereLayers.get()),
                Config.COMMON.enableMobGravity.get(), Config.COMMON.mobGravityOffset.get(),
                Config.COMMON.enablePotionGravity.get(), Config.COMMON.weightlessnessGravityOffset.get(), Config.COMMON.heavinessGravityOffset.get(),
                List.copyOf(Config.COMMON.gravityImmuneEntities.get()),
                Config.getCustomDimensionsJson(),
                gameRuleEnabled,
                BiomeGravityRegistry.encodeServerRules()
        );
    }

    /**
     * Applies synced values in memory for this session only. Uses the non-dirty
     * setter so a server's config never gets written over the player's local file,
     * and it is fully restored on disconnect (see {@code client.EndlessGravityClient}).
     */
    public void applyToConfig() {
        Config.COMMON.endEntityGravity.setSynced(endEntityGravity);
        Config.COMMON.enablePlayerGravity.setSynced(enablePlayerGravity);
        Config.COMMON.playerGravityOffset.setSynced(playerGravityOffset);
        Config.COMMON.enableItemGravity.setSynced(enableItemGravity);
        Config.COMMON.itemGravityOffset.setSynced(itemGravityOffset);
        Config.COMMON.enableArrowGravity.setSynced(enableArrowGravity);
        Config.COMMON.arrowGravityOffset.setSynced(arrowGravityOffset);
        Config.COMMON.enableThrownGravity.setSynced(enableThrownGravity);
        Config.COMMON.thrownGravityOffset.setSynced(thrownGravityOffset);
        Config.COMMON.fallDamageMode.setSynced(fallDamageMode);
        Config.COMMON.mobFallDamageMode.setSynced(mobFallDamageMode);
        Config.COMMON.enableBlockGravity.setSynced(enableBlockGravity);
        Config.COMMON.blockGravityOffset.setSynced(blockGravityOffset);
        Config.COMMON.enableAtmosphere.setSynced(enableAtmosphere);
        Config.COMMON.enableAtmosphericDrag.setSynced(enableAtmosphericDrag);
        Config.COMMON.overworldEntityGravity.setSynced(overworldEntityGravity);
        Config.COMMON.atmosphereGravityMax.setSynced(atmosphereGravityMax);
        Config.COMMON.noFallDamageAltitude.setSynced(noFallDamageAltitude);
        Config.COMMON.atmosphereMuffleGain.setSynced(atmosphereMuffleGain);
        Config.COMMON.atmosphereMuffleGainHF.setSynced(atmosphereMuffleGainHF);
        Config.COMMON.atmosphereLayers.setSynced(atmosphereLayers);
        Config.COMMON.enableMobGravity.setSynced(enableMobGravity);
        Config.COMMON.mobGravityOffset.setSynced(mobGravityOffset);
        Config.COMMON.enablePotionGravity.setSynced(enablePotionGravity);
        Config.COMMON.weightlessnessGravityOffset.setSynced(weightlessnessGravityOffset);
        Config.COMMON.heavinessGravityOffset.setSynced(heavinessGravityOffset);
        Config.COMMON.gravityImmuneEntities.setSynced(gravityImmuneEntities);
        Config.applySyncedCustomDimensions(dimensionProfiles);
    }
}
