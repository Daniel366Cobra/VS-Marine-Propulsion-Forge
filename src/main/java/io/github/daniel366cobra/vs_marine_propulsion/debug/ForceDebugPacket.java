package io.github.daniel366cobra.vs_marine_propulsion.debug;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.function.Supplier;

public class ForceDebugPacket {
    private final ForceVectorData data;

    public ForceDebugPacket(ForceVectorData data) {
        this.data = data;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVector3f(new Vector3f((float) data.worldPos().x, (float) data.worldPos().y, (float) data.worldPos().z));
        buf.writeVector3f(new Vector3f((float) data.force().x, (float) data.force().y, (float) data.force().z));
    }

    public static ForceDebugPacket decode(FriendlyByteBuf buf) {
        return new ForceDebugPacket(
                new ForceVectorData(
                        new Vector3d(buf.readVector3f()),
                        new Vector3d(buf.readVector3f()),
                        2 // Default duration
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
