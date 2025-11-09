package io.github.daniel366cobra.vs_marine_propulsion.network;

import com.simibubi.create.foundation.utility.animation.LerpedFloat;
import io.github.daniel366cobra.vs_marine_propulsion.blocks.control.helm.HelmBlockEntity;
import io.github.daniel366cobra.vs_marine_propulsion.debug.ForceDebugPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class WheelAnglePacket {
    public final int wheelAngle;
    public final BlockPos pos;

    public WheelAnglePacket(int wheelAngle, BlockPos pos) {
        // Message creation
        this.wheelAngle = wheelAngle;
        this.pos = pos;
    }

    public static WheelAnglePacket decode (FriendlyByteBuf buf) {
        // Decode data into a message
        return new WheelAnglePacket(buf.readInt(), buf.readBlockPos());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(wheelAngle);
        buf.writeBlockPos(pos);
    }

    public static void handle(WheelAnglePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ClientLevel level = Minecraft.getInstance().level;
            if (level != null) {
                BlockEntity be = level.getBlockEntity(msg.pos);
                if (be instanceof HelmBlockEntity helm) {
                    helm.wheelAngle = msg.wheelAngle;
                    helm.clientWheelAngle.chase(msg.wheelAngle, 0.2f, LerpedFloat.Chaser.EXP);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
