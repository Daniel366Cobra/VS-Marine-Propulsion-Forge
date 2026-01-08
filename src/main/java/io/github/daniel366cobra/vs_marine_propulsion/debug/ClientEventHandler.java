package io.github.daniel366cobra.vs_marine_propulsion.debug;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(Dist.CLIENT)
public class ClientEventHandler {

    @SubscribeEvent
    public static void onRenderWorld(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_TRIPWIRE_BLOCKS) {
            Minecraft mc = Minecraft.getInstance();

            // Only render if we're in a game world
            if (mc.level == null || mc.player == null) return;

            PoseStack poseStack = event.getPoseStack();
            MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();

            Vec3 cameraPos = mc.gameRenderer.getMainCamera().getPosition();
            LineRenderer.render(poseStack, bufferSource,
                    cameraPos.x, cameraPos.y, cameraPos.z);
        }
    }

    // Clear cache when leaving world
    @SubscribeEvent
    public static void onClientDisconnect(ClientPlayerNetworkEvent.LoggingOut event) {
        LineRenderer.clearCache();
    }

}
