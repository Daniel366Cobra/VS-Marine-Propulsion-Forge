package io.github.daniel366cobra.vs_marine_propulsion.blocks.propulsion;

import com.jozufozu.flywheel.backend.Backend;
import com.jozufozu.flywheel.core.PartialModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.foundation.render.CachedBufferer;
import com.simibubi.create.foundation.render.SuperByteBuffer;
import com.simibubi.create.foundation.utility.AngleHelper;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;

import static com.simibubi.create.content.kinetics.base.DirectionalKineticBlock.FACING;


public class ShipPropellerBlockEntityRenderer extends KineticBlockEntityRenderer<ShipPropellerBlockEntity> {

    private final Map<Integer, PartialModel> propellerModelsMap;

    public ShipPropellerBlockEntityRenderer(BlockEntityRendererProvider.Context context, Map<Integer, PartialModel> propellerModelsMap) {
        super(context);
        this.propellerModelsMap = propellerModelsMap;
    }

    @Override
    protected void renderSafe(ShipPropellerBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
                              int light, int overlay) {
        super.renderSafe(be, partialTicks, ms, buffer, light, overlay);

        if (Backend.canUseInstancing(be.getLevel()))
            return;

        BlockState blockState = be.getBlockState();

        float speed = be.visualSpeed.getValue(partialTicks) * 3 / 10f;
        float angle = be.angle + speed * partialTicks;

        VertexConsumer vb = buffer.getBuffer(RenderType.solid());

        SuperByteBuffer propeller = getPropellerModel(be, blockState);
        kineticRotationTransform(propeller, be, getRotationAxisOf(be), AngleHelper.rad(angle), light);
        propeller.renderInto(ms, vb);
    }

    @Override
    protected SuperByteBuffer getRotatedModel(ShipPropellerBlockEntity blockEntity, BlockState state) {
        return CachedBufferer.partialFacing(AllPartialModels.SHAFT_HALF, state, state
                .getValue(FACING));
    }

    private SuperByteBuffer getPropellerModel(ShipPropellerBlockEntity blockEntity, BlockState state) {
        return CachedBufferer.partialFacing(propellerModelsMap.get(blockEntity.propellerHandedness), state);
    }

    @Override
    protected BlockState getRenderedBlockState(ShipPropellerBlockEntity be) {
        return shaft(getRotationAxisOf(be));
    }

}
