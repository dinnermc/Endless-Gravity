package dinner.dev.endless_gravity.network;

import dinner.dev.endless_gravity.Config;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

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
        boolean enableAtmosphere, boolean overworldEntityGravity, double atmosphereGravityMax,
        double noFallDamageAltitude,
        double atmosphereMuffleGain, double atmosphereMuffleGainHF,
        List<String> atmosphereLayers,
        boolean enableMobGravity, double mobGravityOffset,
        boolean enablePotionGravity, double weightlessnessGravityOffset, double heavinessGravityOffset,
        List<String> gravityImmuneEntities
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
            buf.writeInt(p.mobFallDamageMode());
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
            buf.writeBoolean(p.enablePotionGravity());
            buf.writeDouble(p.weightlessnessGravityOffset());
            buf.writeDouble(p.heavinessGravityOffset());
            buf.writeInt(p.gravityImmuneEntities().size());
            for (String id : p.gravityImmuneEntities()) {
                ByteBufCodecs.STRING_UTF8.encode(buf, id);
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
            boolean enablePotionGravity = buf.readBoolean();
            double weightlessnessGravityOffset = buf.readDouble();
            double heavinessGravityOffset = buf.readDouble();
            int immuneCount = buf.readInt();
            List<String> gravityImmuneEntities = new ArrayList<>(immuneCount);
            for (int i = 0; i < immuneCount; i++) {
                gravityImmuneEntities.add(ByteBufCodecs.STRING_UTF8.decode(buf));
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
                    enableAtmosphere, overworldEntityGravity, atmosphereGravityMax,
                    noFallDamageAltitude,
                    atmosphereMuffleGain, atmosphereMuffleGainHF,
                    atmosphereLayers,
                    enableMobGravity, mobGravityOffset,
                    enablePotionGravity, weightlessnessGravityOffset, heavinessGravityOffset,
                    gravityImmuneEntities
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
                Config.COMMON.mobFallDamageMode.get(),
                Config.COMMON.enableBlockGravity.get(), Config.COMMON.blockGravityOffset.get(),
                Config.COMMON.enableAtmosphere.get(), Config.COMMON.overworldEntityGravity.get(), Config.COMMON.atmosphereGravityMax.get(),
                Config.COMMON.noFallDamageAltitude.get(),
                Config.COMMON.atmosphereMuffleGain.get(), Config.COMMON.atmosphereMuffleGainHF.get(),
                List.copyOf(Config.COMMON.atmosphereLayers.get()),
                Config.COMMON.enableMobGravity.get(), Config.COMMON.mobGravityOffset.get(),
                Config.COMMON.enablePotionGravity.get(), Config.COMMON.weightlessnessGravityOffset.get(), Config.COMMON.heavinessGravityOffset.get(),
                new ArrayList<String>(Config.COMMON.gravityImmuneEntities.get())
        );
    }

    /**
     * Applies the synced gameplay values in memory for this session only, and never
     * marks the config dirty (NeoForge's {@code ConfigValue#set} does not write to disk).
     * The player's local values are restored on disconnect (see {@code client.ConfigSyncClient}).
     *
     * <p>Not synced on purpose, these always use the client's own config: particle
     * offset, the low-pass audio filter, Sable and custom dimensions.
     */
    public void applyToConfig() {
        Config.COMMON.endEntityGravity.set(endEntityGravity);
        Config.COMMON.enablePlayerGravity.set(enablePlayerGravity);
        Config.COMMON.playerGravityOffset.set(playerGravityOffset);
        Config.COMMON.enableItemGravity.set(enableItemGravity);
        Config.COMMON.itemGravityOffset.set(itemGravityOffset);
        Config.COMMON.enableArrowGravity.set(enableArrowGravity);
        Config.COMMON.arrowGravityOffset.set(arrowGravityOffset);
        Config.COMMON.enableThrownGravity.set(enableThrownGravity);
        Config.COMMON.thrownGravityOffset.set(thrownGravityOffset);
        Config.COMMON.fallDamageMode.set(fallDamageMode);
        Config.COMMON.mobFallDamageMode.set(mobFallDamageMode);
        Config.COMMON.enableBlockGravity.set(enableBlockGravity);
        Config.COMMON.blockGravityOffset.set(blockGravityOffset);
        Config.COMMON.enableAtmosphere.set(enableAtmosphere);
        Config.COMMON.overworldEntityGravity.set(overworldEntityGravity);
        Config.COMMON.atmosphereGravityMax.set(atmosphereGravityMax);
        Config.COMMON.noFallDamageAltitude.set(noFallDamageAltitude);
        Config.COMMON.atmosphereMuffleGain.set(atmosphereMuffleGain);
        Config.COMMON.atmosphereMuffleGainHF.set(atmosphereMuffleGainHF);
        Config.COMMON.atmosphereLayers.set(atmosphereLayers);
        Config.COMMON.enableMobGravity.set(enableMobGravity);
        Config.COMMON.mobGravityOffset.set(mobGravityOffset);
        Config.COMMON.enablePotionGravity.set(enablePotionGravity);
        Config.COMMON.weightlessnessGravityOffset.set(weightlessnessGravityOffset);
        Config.COMMON.heavinessGravityOffset.set(heavinessGravityOffset);
        Config.COMMON.gravityImmuneEntities.set(gravityImmuneEntities);
    }

    public static void handle(ConfigSyncPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> clientApplier.accept(payload));
    }

    /**
     * Client-only interceptor for applying a synced config. The client source set
     * replaces this during client setup (see {@code client.ConfigSyncClient}) so it
     * can snapshot/restore the player's local values and skip the integrated server.
     * On a dedicated server this stays as the plain applier and is never invoked.
     */
    public static java.util.function.Consumer<ConfigSyncPayload> clientApplier = ConfigSyncPayload::applyToConfig;
}
