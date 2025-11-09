package io.github.daniel366cobra.vs_marine_propulsion.blocks.control.engine_order_telegraph;

import com.jozufozu.flywheel.api.Material;
import com.jozufozu.flywheel.api.MaterialManager;
import com.jozufozu.flywheel.api.instance.DynamicInstance;
import com.jozufozu.flywheel.backend.instancing.blockentity.BlockEntityInstance;
import com.jozufozu.flywheel.core.materials.model.ModelData;
import com.jozufozu.flywheel.util.AnimationTickHolder;
import com.jozufozu.flywheel.util.transform.Rotate;
import com.jozufozu.flywheel.util.transform.Translate;
import com.simibubi.create.foundation.utility.AngleHelper;
import io.github.daniel366cobra.vs_marine_propulsion.VSMarinePropulsionPartialModels;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.AttachFace;

public class EngineOrderTelegraphInstance extends BlockEntityInstance<EngineOrderTelegraphBlockEntity> implements DynamicInstance {

    protected final ModelData handle;

    final float rX;
    final float rY;

    public EngineOrderTelegraphInstance(MaterialManager materialManager, EngineOrderTelegraphBlockEntity blockEntity) {
        super(materialManager, blockEntity);

        Material<ModelData> mat = getTransformMaterial();

        handle = mat.getModel(VSMarinePropulsionPartialModels.ENGINE_ORDER_TELEGRAPH_LEVER, blockState)
                .createInstance();

        AttachFace face = blockState.getValue(EngineOrderTelegraphBlock.FACE);
        rX = face == AttachFace.FLOOR ? 0 : face == AttachFace.WALL ? 90 : 180;
        rY = AngleHelper.horizontalAngle(blockState.getValue(EngineOrderTelegraphBlock.FACING));

        animateLever();
    }

    @Override
    public void beginFrame() {
        if (!blockEntity.clientLeverState.settled())
            animateLever();
    }

    protected void animateLever() {
        float state = blockEntity.clientLeverState.getValue(AnimationTickHolder.getPartialTicks());

        float angle = (float) ((state / 7) * 120 / 180 * Math.PI);

        if (blockState.getValue(EngineOrderTelegraphBlock.FACE) == AttachFace.WALL
                || blockState.getValue(EngineOrderTelegraphBlock.FACE) == AttachFace.CEILING) angle = -angle;

        transform(handle.loadIdentity()).translate(1 / 2f, 1 / 4f, 1 / 2f)
                .rotate(Direction.EAST, angle)
                .translate(-1 / 2f, -1 / 4f, -1 / 2f);
    }

    @Override
    public void remove() {
        handle.delete();
    }

    @Override
    public void updateLight() {
        relight(pos, handle);
    }

    private <T extends Translate<T> & Rotate<T>> T transform(T msr) {
        return msr.translate(getInstancePosition())
                .centre()
                .rotate(Direction.UP, (float) (rY / 180 * Math.PI))
                .rotate(Direction.EAST, (float) (rX / 180 * Math.PI))
                .unCentre();
    }
}
