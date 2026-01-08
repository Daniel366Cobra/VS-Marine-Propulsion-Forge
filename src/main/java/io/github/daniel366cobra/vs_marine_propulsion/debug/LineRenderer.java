package io.github.daniel366cobra.vs_marine_propulsion.debug;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.joml.Matrix4f;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LineRenderer {
    // Map from shipId (long) to force data
    private static final Map<Long, ArrayList<CachedForceData>> forceCache = new ConcurrentHashMap<>();

    private static class CachedForceData {
        final Vector3d worldStart;
        final Vector3d worldEnd;
        final int color;
        final String label;
        int remainingTicks;

        CachedForceData(ForceVectorData data) {
            this.worldStart = data.worldStart();
            this.worldEnd = data.worldEnd();
            this.color = data.color();
            this.label = data.label();
            this.remainingTicks = data.tickDuration();
        }
    }

    public static void cacheForceData(ForceVectorData data) {
        forceCache.computeIfAbsent(data.shipID(), k -> new ArrayList<>())
                .add(new CachedForceData(data));
    }

    public static void render(PoseStack poseStack, MultiBufferSource bufferSource,
                              double camX, double camY, double camZ) {
        // Only render if F3+B is enabled
        if (!Minecraft.getInstance().getEntityRenderDispatcher().shouldRenderHitBoxes()) {
            // Don't clear cache immediately, just don't render
            // This allows vectors to persist if F3+B is toggled quickly
            return;
        }

        poseStack.pushPose();
        poseStack.translate(-camX, -camY, -camZ);

        Matrix4f matrix = poseStack.last().pose();

        // Update and render all forces

        if (bufferSource instanceof MultiBufferSource.BufferSource bufferSourceInstance) {

            Iterator<Map.Entry<Long, ArrayList<CachedForceData>>> shipIterator = forceCache.entrySet().iterator();

            while (shipIterator.hasNext()) {

                Map.Entry<Long, ArrayList<CachedForceData>> entry = shipIterator.next();
                ArrayList<CachedForceData> forces = entry.getValue();

                Iterator<CachedForceData> forceIterator = forces.iterator();

                while (forceIterator.hasNext()) {
                    CachedForceData force = forceIterator.next();
                    force.remainingTicks--;

                    if (force.remainingTicks <= 0) {
                        forceIterator.remove();
                        continue;
                    }

                    VertexConsumer vertexConsumer = bufferSourceInstance.getBuffer(RenderType.debugQuads());
                    // Render this force
                    renderForce(matrix, vertexConsumer, force);

                    bufferSourceInstance.endBatch();
                }

                if (forces.isEmpty()) {
                    shipIterator.remove();
                }
            }
        }

        poseStack.popPose();
    }

    private static void renderForce(Matrix4f matrix, VertexConsumer consumer,
                                    CachedForceData force) {
        float alpha = Math.max(0.7f, 1.0f - (force.remainingTicks / (float) 10));
        float r = ((force.color >> 16) & 0xFF) / 255.0f;
        float g = ((force.color >> 8) & 0xFF) / 255.0f;
        float b = (force.color & 0xFF) / 255.0f;

        Vector3d start = force.worldStart;
        Vector3d end = force.worldEnd;
        Vector3d direction = new Vector3d(end).sub(start);

        if (direction.lengthSquared() < 0.001) return;

        direction.normalize();

        // Find a perpendicular vector for triangle base

        Vector3d perp = new Vector3d().orthogonalize(direction);

        float baseWidth = 0.3f; // Width of triangle base

        Vector3d tip = end;

        Vector3d base1 = new Vector3d(start).add(perp.mul(baseWidth));
        Vector3d base2 = new Vector3d(start).sub(perp.mul(baseWidth));

        vertex(consumer, matrix, tip, r, g, b, alpha);
        vertex(consumer, matrix, base1, r, g, b, alpha);
        vertex(consumer, matrix, base2, r, g, b, alpha);

        vertex(consumer, matrix, tip, r, g, b, alpha);
        vertex(consumer, matrix, base2, r, g, b, alpha);
        vertex(consumer, matrix, base1, r, g, b, alpha);
    }

    private static Vector3d findPerpendicular(Vector3d v) {
        if (Math.abs(v.x) > Math.abs(v.y)) {
            return new Vector3d(-v.z, 0, v.x).normalize();
        } else {
            return new Vector3d(0, v.z, -v.y).normalize();
        }
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix,
                               Vector3d pos, float r, float g, float b, float a) {
        consumer.vertex(matrix, (float)pos.x, (float)pos.y, (float)pos.z)
                .color(r, g, b, a)
                .endVertex();
    }

    public static void clearCache() {
        forceCache.clear();
    }

    // Clear cache for a specific ship
    public static void clearForShip(long shipId) {
        forceCache.remove(shipId);
    }

    // Optional: Get count of active forces (for debugging)
    public static int getActiveForceCount() {
        return forceCache.values().stream()
                .mapToInt(ArrayList::size)
                .sum();
    }

}