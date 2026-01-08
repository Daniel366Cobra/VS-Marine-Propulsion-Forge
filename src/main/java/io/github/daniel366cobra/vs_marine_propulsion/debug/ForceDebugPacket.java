package io.github.daniel366cobra.vs_marine_propulsion.debug;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.joml.Vector3d;
import org.joml.Vector3f;

import java.util.function.Supplier;

public class ForceDebugPacket {
    private final ForceVectorData data;

    public ForceDebugPacket(ForceVectorData data) {
        this.data = data;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeLong(data.shipID());
        buf.writeVector3f(new Vector3f((float) data.worldStart().x, (float) data.worldStart().y, (float) data.worldStart().z));
        buf.writeVector3f(new Vector3f((float) data.worldEnd().x, (float) data.worldEnd().y, (float) data.worldEnd().z));
        buf.writeInt(data.color());
        buf.writeUtf(data.label());
        buf.writeInt(data.tickDuration());
    }

    public static ForceDebugPacket decode(FriendlyByteBuf buf) {
        return new ForceDebugPacket(
                new ForceVectorData(
                        buf.readLong(),
                        new Vector3d(buf.readVector3f()),
                        new Vector3d(buf.readVector3f()),
                        buf.readInt(),
                        buf.readUtf(),
                        buf.readInt()
                )
        );
    }

    public static void handle(ForceDebugPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            LineRenderer.cacheForceData(msg.data);
        });
        ctx.get().setPacketHandled(true);
    }
}
