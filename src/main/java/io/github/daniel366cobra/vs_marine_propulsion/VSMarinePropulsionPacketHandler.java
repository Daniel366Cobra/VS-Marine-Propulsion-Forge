package io.github.daniel366cobra.vs_marine_propulsion;

import io.github.daniel366cobra.vs_marine_propulsion.debug.DebugVectorPacket;
import io.github.daniel366cobra.vs_marine_propulsion.network.WheelAnglePacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import static io.github.daniel366cobra.vs_marine_propulsion.VSMarinePropulsionMod.MOD_ID;
import static org.antlr.runtime.debug.DebugEventListener.PROTOCOL_VERSION;

public class VSMarinePropulsionPacketHandler {

    public static final ResourceLocation WHEEL_ANGLE_PACKET = new ResourceLocation(MOD_ID, "wheel_angle_packet");

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(MOD_ID, "channel"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public static void register() {
        CHANNEL.registerMessage(0,
                DebugVectorPacket.class,
                DebugVectorPacket::encode,
                DebugVectorPacket::decode,
                DebugVectorPacket::handle
        );
        CHANNEL.registerMessage(1,
                WheelAnglePacket.class,
                WheelAnglePacket::encode,
                WheelAnglePacket::decode,
                WheelAnglePacket::handle
        );
    }
}
