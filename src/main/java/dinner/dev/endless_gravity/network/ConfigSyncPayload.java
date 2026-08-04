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
        boolean enablePlayerGravity, double playerGravityOffset,
        boolean enableItemGravity, double itemGravityOffset,
        boolean enableArrowGravity, double arrowGravityOffset,
        boolean enableThrownGravity, double thrownGravityOffset,
        int fallDamageMode, double fallDamageVelocityScale, double fallDamageMinVelocity,
        boolean enableBlockGravity, double blockGravityOffset,
        boolean enableAtmosphere, double atmosphereGravityMax,
        double atmosphereMuffleGain, double atmosphereMuffleGainHF,
        List<String> atmosphereLayers,
        boolean enableTemperature, int temperatureFreezeInterval,
        boolean enableOxygen, int oxygenRate, int oxygenTankCapacity, int oxygenSuffocationFadeTicks
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ConfigSyncPayload> ID =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("endless_gravity", "config_sync"));

    public static final StreamCodec<ByteBuf, ConfigSyncPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public void encode(ByteBuf buf, ConfigSyncPayload p) {
            buf.writeBoolean(p.enablePlayerGravity());
            buf.writeDouble(p.playerGravityOffset());
            buf.writeBoolean(p.enableItemGravity());
            buf.writeDouble(p.itemGravityOffset());
            buf.writeBoolean(p.enableArrowGravity());
            buf.writeDouble(p.arrowGravityOffset());
            buf.writeBoolean(p.enableThrownGravity());
            buf.writeDouble(p.thrownGravityOffset());
            buf.writeInt(p.fallDamageMode());
            buf.writeDouble(p.fallDamageVelocityScale());
            buf.writeDouble(p.fallDamageMinVelocity());
            buf.writeBoolean(p.enableBlockGravity());
            buf.writeDouble(p.blockGravityOffset());
            buf.writeBoolean(p.enableAtmosphere());
            buf.writeDouble(p.atmosphereGravityMax());
            buf.writeDouble(p.atmosphereMuffleGain());
            buf.writeDouble(p.atmosphereMuffleGainHF());
            buf.writeInt(p.atmosphereLayers().size());
            for (String layer : p.atmosphereLayers()) {
                ByteBufCodecs.STRING_UTF8.encode(buf, layer);
            }
            buf.writeBoolean(p.enableTemperature());
            buf.writeInt(p.temperatureFreezeInterval());
            buf.writeBoolean(p.enableOxygen());
            buf.writeInt(p.oxygenRate());
            buf.writeInt(p.oxygenTankCapacity());
            buf.writeInt(p.oxygenSuffocationFadeTicks());
        }

        @Override
        public ConfigSyncPayload decode(ByteBuf buf) {
            boolean enablePlayerGravity = buf.readBoolean();
            double playerGravityOffset = buf.readDouble();
            boolean enableItemGravity = buf.readBoolean();
            double itemGravityOffset = buf.readDouble();
            boolean enableArrowGravity = buf.readBoolean();
            double arrowGravityOffset = buf.readDouble();
            boolean enableThrownGravity = buf.readBoolean();
            double thrownGravityOffset = buf.readDouble();
            int fallDamageMode = buf.readInt();
            double fallDamageVelocityScale = buf.readDouble();
            double fallDamageMinVelocity = buf.readDouble();
            boolean enableBlockGravity = buf.readBoolean();
            double blockGravityOffset = buf.readDouble();
            boolean enableAtmosphere = buf.readBoolean();
            double atmosphereGravityMax = buf.readDouble();
            double atmosphereMuffleGain = buf.readDouble();
            double atmosphereMuffleGainHF = buf.readDouble();
            int layerCount = buf.readInt();
            List<String> atmosphereLayers = new ArrayList<>(layerCount);
            for (int i = 0; i < layerCount; i++) {
                atmosphereLayers.add(ByteBufCodecs.STRING_UTF8.decode(buf));
            }
            boolean enableTemperature = buf.readBoolean();
            int temperatureFreezeInterval = buf.readInt();
            boolean enableOxygen = buf.readBoolean();
            int oxygenRate = buf.readInt();
            int oxygenTankCapacity = buf.readInt();
            int oxygenSuffocationFadeTicks = buf.readInt();
            return new ConfigSyncPayload(
                    enablePlayerGravity, playerGravityOffset,
                    enableItemGravity, itemGravityOffset,
                    enableArrowGravity, arrowGravityOffset,
                    enableThrownGravity, thrownGravityOffset,
                    fallDamageMode, fallDamageVelocityScale, fallDamageMinVelocity,
                    enableBlockGravity, blockGravityOffset,
                    enableAtmosphere, atmosphereGravityMax,
                    atmosphereMuffleGain, atmosphereMuffleGainHF,
                    atmosphereLayers,
                    enableTemperature, temperatureFreezeInterval,
                    enableOxygen, oxygenRate, oxygenTankCapacity, oxygenSuffocationFadeTicks
            );
        }
    };

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    public static ConfigSyncPayload fromConfig() {
        return new ConfigSyncPayload(
                Config.COMMON.enablePlayerGravity.get(), Config.COMMON.playerGravityOffset.get(),
                Config.COMMON.enableItemGravity.get(), Config.COMMON.itemGravityOffset.get(),
                Config.COMMON.enableArrowGravity.get(), Config.COMMON.arrowGravityOffset.get(),
                Config.COMMON.enableThrownGravity.get(), Config.COMMON.thrownGravityOffset.get(),
                Config.COMMON.fallDamageMode.get(), Config.COMMON.fallDamageVelocityScale.get(), Config.COMMON.fallDamageMinVelocity.get(),
                Config.COMMON.enableBlockGravity.get(), Config.COMMON.blockGravityOffset.get(),
                Config.COMMON.enableAtmosphere.get(), Config.COMMON.atmosphereGravityMax.get(),
                Config.COMMON.atmosphereMuffleGain.get(), Config.COMMON.atmosphereMuffleGainHF.get(),
                List.copyOf(Config.COMMON.atmosphereLayers.get()),
                Config.COMMON.enableTemperature.get(), Config.COMMON.temperatureFreezeInterval.get(),
                Config.COMMON.enableOxygen.get(), Config.COMMON.oxygenRate.get(),
                Config.COMMON.oxygenTankCapacity.get(), Config.COMMON.oxygenSuffocationFadeTicks.get()
        );
    }

    public void applyToConfig() {
        Config.COMMON.enablePlayerGravity.set(enablePlayerGravity);
        Config.COMMON.playerGravityOffset.set(playerGravityOffset);
        Config.COMMON.enableItemGravity.set(enableItemGravity);
        Config.COMMON.itemGravityOffset.set(itemGravityOffset);
        Config.COMMON.enableArrowGravity.set(enableArrowGravity);
        Config.COMMON.arrowGravityOffset.set(arrowGravityOffset);
        Config.COMMON.enableThrownGravity.set(enableThrownGravity);
        Config.COMMON.thrownGravityOffset.set(thrownGravityOffset);
        Config.COMMON.fallDamageMode.set(fallDamageMode);
        Config.COMMON.fallDamageVelocityScale.set(fallDamageVelocityScale);
        Config.COMMON.fallDamageMinVelocity.set(fallDamageMinVelocity);
        Config.COMMON.enableBlockGravity.set(enableBlockGravity);
        Config.COMMON.blockGravityOffset.set(blockGravityOffset);
        Config.COMMON.enableAtmosphere.set(enableAtmosphere);
        Config.COMMON.atmosphereGravityMax.set(atmosphereGravityMax);
        Config.COMMON.atmosphereMuffleGain.set(atmosphereMuffleGain);
        Config.COMMON.atmosphereMuffleGainHF.set(atmosphereMuffleGainHF);
        Config.COMMON.atmosphereLayers.set(atmosphereLayers);
        Config.COMMON.enableTemperature.set(enableTemperature);
        Config.COMMON.temperatureFreezeInterval.set(temperatureFreezeInterval);
        Config.COMMON.enableOxygen.set(enableOxygen);
        Config.COMMON.oxygenRate.set(oxygenRate);
        Config.COMMON.oxygenTankCapacity.set(oxygenTankCapacity);
        Config.COMMON.oxygenSuffocationFadeTicks.set(oxygenSuffocationFadeTicks);
    }

    public static void handle(ConfigSyncPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> payload.applyToConfig());
    }
}
