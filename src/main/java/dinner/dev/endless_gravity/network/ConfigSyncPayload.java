package dinner.dev.endless_gravity.network;

import dinner.dev.endless_gravity.Config;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public record ConfigSyncPayload(
        boolean endEntityGravity,
        boolean enablePlayerGravity, double playerGravityOffset,
        boolean enableItemGravity, double itemGravityOffset,
        boolean enableArrowGravity, double arrowGravityOffset,
        boolean enableThrownGravity, double thrownGravityOffset,
        int fallDamageMode,
        boolean enableBlockGravity, double blockGravityOffset,
        boolean enableAtmosphere, boolean overworldEntityGravity, double atmosphereGravityMax,
        double noFallDamageAltitude,
        double atmosphereMuffleGain, double atmosphereMuffleGainHF,
        List<String> atmosphereLayers,
        boolean enableMobGravity, double mobGravityOffset
) implements CustomPacketPayload {

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
            buf.writeBoolean(p.enableBlockGravity());
            buf.writeDouble(p.blockGravityOffset());
            buf.writeBoolean(p.enableAtmosphere());
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
            boolean enableBlockGravity = buf.readBoolean();
            double blockGravityOffset = buf.readDouble();
            boolean enableAtmosphere = buf.readBoolean();
            boolean overworldEntityGravity = buf.readBoolean();
            double atmosphereGravityMax = buf.readDouble();
            double noFallDamageAltitude = buf.readDouble();
            double atmosphereMuffleGain = buf.readDouble();
            double atmosphereMuffleGainHF = buf.readDouble();
            int layerCount = buf.readInt();
            List<String> atmosphereLayers = new ArrayList<>(layerCount);
            for (int i = 0; i < layerCount; i++) {
                atmosphereLayers.add(ByteBufCodecs.STRING_UTF8.decode(buf));
            }
            boolean enableMobGravity = buf.readBoolean();
            double mobGravityOffset = buf.readDouble();
            return new ConfigSyncPayload(
                    endEntityGravity,
                    enablePlayerGravity, playerGravityOffset,
                    enableItemGravity, itemGravityOffset,
                    enableArrowGravity, arrowGravityOffset,
                    enableThrownGravity, thrownGravityOffset,
                    fallDamageMode,
                    enableBlockGravity, blockGravityOffset,
                    enableAtmosphere, overworldEntityGravity, atmosphereGravityMax,
                    noFallDamageAltitude,
                    atmosphereMuffleGain, atmosphereMuffleGainHF,
                    atmosphereLayers,
                    enableMobGravity, mobGravityOffset
            );
        }
    };

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    public static ConfigSyncPayload fromConfig() {
        return new ConfigSyncPayload(
                Config.COMMON.endEntityGravity.get(),
                Config.COMMON.enablePlayerGravity.get(), Config.COMMON.playerGravityOffset.get(),
                Config.COMMON.enableItemGravity.get(), Config.COMMON.itemGravityOffset.get(),
                Config.COMMON.enableArrowGravity.get(), Config.COMMON.arrowGravityOffset.get(),
                Config.COMMON.enableThrownGravity.get(), Config.COMMON.thrownGravityOffset.get(),
                Config.COMMON.fallDamageMode.get(),
                Config.COMMON.enableBlockGravity.get(), Config.COMMON.blockGravityOffset.get(),
                Config.COMMON.enableAtmosphere.get(), Config.COMMON.overworldEntityGravity.get(), Config.COMMON.atmosphereGravityMax.get(),
                Config.COMMON.noFallDamageAltitude.get(),
                Config.COMMON.atmosphereMuffleGain.get(), Config.COMMON.atmosphereMuffleGainHF.get(),
                List.copyOf(Config.COMMON.atmosphereLayers.get()),
                Config.COMMON.enableMobGravity.get(), Config.COMMON.mobGravityOffset.get()
        );
    }

    /**
     * Applies synced values in memory for this session only. Uses the non-dirty
     * setter so a server's config never gets written over the player's local file,
     * and it is fully restored on disconnect (see {@code client.EndlessGravityClient}).
     *
     * <p>Not synced on purpose, these always use the client's own config: particle
     * offset, the low-pass audio filter, Sable and custom dimensions.
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
        Config.COMMON.enableBlockGravity.setSynced(enableBlockGravity);
        Config.COMMON.blockGravityOffset.setSynced(blockGravityOffset);
        Config.COMMON.enableAtmosphere.setSynced(enableAtmosphere);
        Config.COMMON.overworldEntityGravity.setSynced(overworldEntityGravity);
        Config.COMMON.atmosphereGravityMax.setSynced(atmosphereGravityMax);
        Config.COMMON.noFallDamageAltitude.setSynced(noFallDamageAltitude);
        Config.COMMON.atmosphereMuffleGain.setSynced(atmosphereMuffleGain);
        Config.COMMON.atmosphereMuffleGainHF.setSynced(atmosphereMuffleGainHF);
        Config.COMMON.atmosphereLayers.setSynced(atmosphereLayers);
        Config.COMMON.enableMobGravity.setSynced(enableMobGravity);
        Config.COMMON.mobGravityOffset.setSynced(mobGravityOffset);
    }
}