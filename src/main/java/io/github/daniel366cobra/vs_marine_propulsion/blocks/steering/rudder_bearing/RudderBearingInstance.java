package io.github.daniel366cobra.vs_marine_propulsion.blocks.steering.rudder_bearing;

import com.jozufozu.flywheel.api.MaterialManager;
import com.jozufozu.flywheel.api.instance.DynamicInstance;
import com.jozufozu.flywheel.core.PartialModel;
import com.jozufozu.flywheel.core.materials.oriented.OrientedData;
import com.mojang.math.Axis;
import com.simibubi.create.content.kinetics.base.BackHalfShaftInstance;
import com.simibubi.create.foundation.utility.AngleHelper;
import com.simibubi.create.foundation.utility.AnimationTickHolder;
import io.github.daniel366cobra.vs_marine_propulsion.VSMarinePropulsionPartialModels;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.joml.Quaternionf;

public class RudderBearingInstance extends BackHalfShaftInstance<RudderBearingBlockEntity> implements DynamicInstance {
    final OrientedData topInstance;

    final Axis rotationAxis;
    final Quaternionf blockOrientation;

    public RudderBearingInstance(MaterialManager materialManager, RudderBearingBlockEntity blockEntity) {
        super(materialManager, blockEntity);

        Direction facing = blockState.getValue(BlockStateProperties.FACING);
        rotationAxis = Axis.of(Direction.get(Direction.AxisDirection.POSITIVE, axis).step());

        blockOrientation = getBlockStateOrientation(facing);

        PartialModel top = VSMarinePropulsionPartialModels.RUDDER_BEARING_ROTATOR;

        topInstance = getOrientedMaterial().getModel(top, blockState).createInstance();

        topInstance.setPosition(getInstancePosition()).setRotation(blockOrientation);
    }

    @Override
    public void beginFrame() {
        float interpolatedAngle = blockEntity.getInterpolatedAngle(AnimationTickHolder.getPartialTicks() - 1);
        Quaternionf rot = rotationAxis.rotationDegrees(interpolatedAngle);

        rot.mul(blockOrientation);

        topInstance.setRotation(rot);
    }

    @Override
    public void updateLight() {
        super.updateLight();
        relight(pos, topInstance);
    }

    @Override
    public void remove() {
        super.remove();
        topInstance.delete();
    }

    static Quaternionf getBlockStateOrientation(Direction facing) {
        Quaternionf orientation;

        if (facing.getAxis().isHorizontal()) {
            orientation = Axis.YP.rotationDegrees(AngleHelper.horizontalAngle(facing.getOpposite()));
        } else {
            orientation = new Quaternionf();
        }

        orientation.mul(Axis.XP.rotationDegrees(-90 - AngleHelper.verticalAngle(facing)));
        return orientation;
    }
}
