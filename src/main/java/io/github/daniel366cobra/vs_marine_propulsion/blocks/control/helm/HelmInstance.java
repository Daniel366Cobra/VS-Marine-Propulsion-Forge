package io.github.daniel366cobra.vs_marine_propulsion.blocks.control.helm;

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

public class HelmInstance extends BlockEntityInstance<HelmBlockEntity> implements DynamicInstance {

    protected final ModelData wheel;

    public HelmInstance(MaterialManager materialManager, HelmBlockEntity blockEntity) {
        super(materialManager, blockEntity);

        Material<ModelData> mat = getTransformMaterial();
        wheel = mat.getModel(VSMarinePropulsionPartialModels.HELM_WHEEL, blockState).createInstance();

        animateWheel();
    }

    @Override
    public void beginFrame() {
        animateWheel();
    }

    protected void animateWheel() {
        // USE THE LERPEDFLOAT VALUE FOR SMOOTH ANIMATION

        float wheelAngle = blockEntity.getRenderWheelAngle(AnimationTickHolder.getPartialTicks());
        float angleRadians = (float) Math.toRadians(wheelAngle);

        float rotationY = AngleHelper.horizontalAngle(blockState.getValue(HelmBlock.FACING).getOpposite());

        wheel.loadIdentity()
                .translate(getInstancePosition())
                .translate(0.5f, 0.8125f, 0.5f)
                .rotate(Direction.UP, (float) Math.toRadians(rotationY))
                .rotate(Direction.SOUTH, angleRadians)
                .translate(-0.5f, -0.8125f, -0.5f);
    }

    @Override
    public void remove() {
        wheel.delete();
    }

    @Override
    public void updateLight() {
        relight(pos, wheel);
    }
}