package io.github.daniel366cobra.vs_marine_propulsion.blocks.control.diving_plane_station;

import com.jozufozu.flywheel.backend.Backend;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import com.simibubi.create.foundation.render.CachedBufferer;
import com.simibubi.create.foundation.render.SuperByteBuffer;
import com.simibubi.create.foundation.utility.AngleHelper;
import io.github.daniel366cobra.vs_marine_propulsion.VSMarinePropulsionPartialModels;
import io.github.daniel366cobra.vs_marine_propulsion.blocks.control.helm.HelmBlock;
import io.github.daniel366cobra.vs_marine_propulsion.blocks.control.helm.HelmBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

public class DivingPlaneStationBlockEntityRenderer extends SafeBlockEntityRenderer<DivingPlaneStationBlockEntity> {

    public DivingPlaneStationBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    protected void renderSafe(DivingPlaneStationBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
                              int light, int overlay) {

        // Let Flywheel handle it if possible
        if (Backend.canUseInstancing(be.getLevel())) return;

        BlockState helmState = be.getBlockState();
        float wheelAngle = be.getRenderWheelAngle(partialTicks); // Use our angular lerped value

        VertexConsumer vb = buffer.getBuffer(RenderType.solid());

        // Render the wheel
        SuperByteBuffer wheel = CachedBufferer.partial(VSMarinePropulsionPartialModels.HELM_WHEEL, helmState);

        // Convert to radians and rotate
        float angleRadians = (float) Math.toRadians(wheelAngle);
        transform(wheel, helmState)
                .translate(0.5f, 0.5f, 0.5f)    // Rotate around center
                .rotate(Direction.UP, angleRadians)
                .translate(-0.5f, -0.5f, -0.5f);

        wheel.light(light)
                .renderInto(ms, vb);
    }

    private SuperByteBuffer transform(SuperByteBuffer buffer, BlockState helmState) {
        // Helm is always wall-mounted, so we only need horizontal rotation
        float rotationY = AngleHelper.horizontalAngle(helmState.getValue(HelmBlock.FACING));
        buffer.rotateCentered(Direction.UP, (float) Math.toRadians(rotationY));
        return buffer;
    }
}
