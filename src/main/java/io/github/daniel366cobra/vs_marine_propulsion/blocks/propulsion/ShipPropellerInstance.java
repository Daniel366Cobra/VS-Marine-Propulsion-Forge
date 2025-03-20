package io.github.daniel366cobra.vs_marine_propulsion.blocks.propulsion;

import com.jozufozu.flywheel.api.MaterialManager;
import com.jozufozu.flywheel.api.instance.DynamicInstance;
import com.jozufozu.flywheel.core.PartialModel;
import com.jozufozu.flywheel.core.materials.oriented.OrientedData;
import com.mojang.math.Axis;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityInstance;
import com.simibubi.create.content.kinetics.base.flwdata.RotatingData;
import com.simibubi.create.foundation.utility.AngleHelper;
import com.simibubi.create.foundation.utility.AnimationTickHolder;
import net.minecraft.core.Direction;
import org.joml.Quaternionf;

import java.util.Map;

import static net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING;


public class ShipPropellerInstance extends KineticBlockEntityInstance<ShipPropellerBlockEntity> implements DynamicInstance {

    protected final RotatingData shaft;

    protected PartialModel propellerModel = null;
    protected OrientedData propellerInstance = null;

    private final Map<Integer, PartialModel> propellerModelsMap;

    private final Axis rotationAxis;

    final Quaternionf blockOrientation;

    final Direction facingDirection;

    public ShipPropellerInstance(MaterialManager materialManager, ShipPropellerBlockEntity blockEntity, Map<Integer, PartialModel> propellerModelsMap) {

        super(materialManager, blockEntity);

        this.propellerModelsMap = propellerModelsMap;

        facingDirection = blockState.getValue(FACING);
        rotationAxis = Axis.of(Direction.get(Direction.AxisDirection.POSITIVE, axis).step());
        blockOrientation = getBlockStateOrientation(facingDirection);

        shaft = setup(getRotatingMaterial().getModel(AllPartialModels.SHAFT_HALF, blockState, facingDirection.getOpposite()).createInstance());

        updatePropeller();
    }

    private void updatePropeller() {
        if (propellerModel == null || !propellerModel.equals(propellerModelsMap.get(blockEntity.propellerHandedness))) {

            if (propellerInstance!= null) propellerInstance.delete();

            propellerModel = propellerModelsMap.get(blockEntity.propellerHandedness);
            propellerInstance = getOrientedMaterial()
                    .getModel(propellerModel, blockState)
                    .createInstance();

            propellerInstance.setPosition(getInstancePosition()).setRotation(blockOrientation);
            updateLight();
        }
    }

    @Override
    public void update() {
        updateRotation(shaft);
        updatePropeller();
    }

    @Override
    public void beginFrame() {

        float partialTicks = AnimationTickHolder.getPartialTicks();
        float speed = blockEntity.actualSpeed.getValue(partialTicks) * 3 / 10f;
        float angle = blockEntity.angle + speed * partialTicks;

        Quaternionf rotation = rotationAxis.rotationDegrees(angle);
        rotation.mul(blockOrientation);

        propellerInstance.setRotation(rotation);

    }

    @Override
    public void updateLight() {
        super.updateLight();
        relight(pos, shaft, propellerInstance);
    }

    @Override
    public void remove() {
        shaft.delete();
        propellerInstance.delete();
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
