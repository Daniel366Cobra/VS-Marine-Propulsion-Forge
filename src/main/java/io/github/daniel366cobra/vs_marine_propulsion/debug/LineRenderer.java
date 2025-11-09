package io.github.daniel366cobra.vs_marine_propulsion.debug;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import org.joml.Vector3d;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LineRenderer {

    private static final Map<UUID, ForceVectorData> lineCache = new HashMap<>();

    public static void cacheForceData(ForceVectorData data) {
        lineCache.put(
                UUID.randomUUID(), // Or use entity ID if available
                data
        );
    }

    private static void renderLine(Vector3d origin, Vector3d vector, Color color, PoseStack poseStack) {

        //Convert world positions to camera-relative coordinates
        Vec3 cameraPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        Vector3d fromRelative = new Vector3d(origin.sub(cameraPos.x, cameraPos.y, cameraPos.z));
        Vector3d toRelative = new Vector3d(fromRelative).add(vector.sub(cameraPos.x, cameraPos.y, cameraPos.z)).mul(10);

        //Set up rendering
        MultiBufferSource.BufferSource buffer = Minecraft.getInstance().renderBuffers().bufferSource();
        VertexConsumer lineConsumer = buffer.getBuffer(RenderType.lines());

        //Render line with color
        RenderSystem.setShader(GameRenderer::getRendertypeLinesShader);
        RenderSystem.lineWidth(2.0f);
        LevelRenderer.renderLineBox(
                poseStack, lineConsumer,
                fromRelative.x, fromRelative.y, fromRelative.z,
                toRelative.x, toRelative.y, toRelative.z,
                color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()
        );
        //Draw
        buffer.endBatch(RenderType.lines());
    }

    public static void renderLines(RenderLevelStageEvent event) {
        // Temporary map for updated entries
        Map<UUID, ForceVectorData> updatedCache = new HashMap<>();

        lineCache.forEach((id, data) -> {

            renderLine(data.worldPos(), data.force(), Color.GREEN, event.getPoseStack());

            // Only keep if duration remains
            if (data.tickDuration() > 1) {
                updatedCache.put(id, data.withDecrementedDuration());
            }
        });

        lineCache.clear();
        lineCache.putAll(updatedCache);
    }
}
