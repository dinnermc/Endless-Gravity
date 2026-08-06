package dinner.dev.endless_gravity.network;

import dinner.dev.endless_gravity.EndlessGravity;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.function.Consumer;

/**
 * Streams the engine state of a flying starship to nearby clients so they can
 * start/update/stop the engine loop sound. The actual sound logic lives in the
 * client source set and is injected here at client startup, so a dedicated
 * server never loads (or resolves) any client sound classes.
 */
public record RocketSoundPayload(
        BlockPos padPos,
        ResourceLocation dimension,
        Vec3 soundPos,
        boolean engineRunning,
        float thrustPower
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<RocketSoundPayload> ID =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(EndlessGravity.MODID, "rocket_sound"));

    public static final StreamCodec<ByteBuf, RocketSoundPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public void encode(ByteBuf buf, RocketSoundPayload p) {
            BlockPos.STREAM_CODEC.encode(buf, p.padPos());
            ResourceLocation.STREAM_CODEC.encode(buf, p.dimension());
            buf.writeDouble(p.soundPos().x);
            buf.writeDouble(p.soundPos().y);
            buf.writeDouble(p.soundPos().z);
            buf.writeBoolean(p.engineRunning());
            buf.writeFloat(p.thrustPower());
        }

        @Override
        public RocketSoundPayload decode(ByteBuf buf) {
            BlockPos padPos = BlockPos.STREAM_CODEC.decode(buf);
            ResourceLocation dimension = ResourceLocation.STREAM_CODEC.decode(buf);
            double x = buf.readDouble();
            double y = buf.readDouble();
            double z = buf.readDouble();
            boolean engineRunning = buf.readBoolean();
            float thrustPower = buf.readFloat();
            return new RocketSoundPayload(padPos, dimension, new Vec3(x, y, z), engineRunning, thrustPower);
        }
    };

    private static volatile Consumer<RocketSoundPayload> clientHandler;

    /** Installed by the client at startup; never set on a dedicated server. */
    public static void setClientHandler(Consumer<RocketSoundPayload> handler) {
        clientHandler = handler;
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    public static void handle(RocketSoundPayload payload, IPayloadContext context) {
        // Runs on the main thread of the connected client only.
        context.enqueueWork(() -> {
            Consumer<RocketSoundPayload> handler = clientHandler;
            if (handler != null) handler.accept(payload);
        });
    }
}