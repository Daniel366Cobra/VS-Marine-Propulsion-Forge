package io.github.daniel366cobra.vs_marine_propulsion.blocks.control.diving_plane_station;

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
import io.github.daniel366cobra.vs_marine_propulsion.blocks.control.helm.HelmBlock;
import io.github.daniel366cobra.vs_marine_propulsion.blocks.control.helm.HelmBlockEntity;
import net.minecraft.core.Direction;

public class DivingPlaneStationInstance extends BlockEntityInstance<DivingPlaneStationBlockEntity> implements DynamicInstance {

        protected final ModelData wheel;
        final Direction facing;
        final float baseRotationY;

    public DivingPlaneStationInstance(MaterialManager materialManager, DivingPlaneStationBlockEntity blockEntity) {
            super(materialManager, blockEntity);

            Material<ModelData> mat = getTransformMaterial();
            wheel = mat.getModel(VSMarinePropulsionPartialModels.DIVING_PLANE_STATION_WHEEL, blockState).createInstance();

            facing = blockState.getValue(HelmBlock.FACING).getOpposite();
            baseRotationY = AngleHelper.horizontalAngle(facing);

            animateWheel();
        }

        @Override
        public void beginFrame() {
            animateWheel();
        }

        protected void animateWheel() {
            // USE THE LERPEDFLOAT VALUE FOR SMOOTH ANIMATION
            float renderAngle = blockEntity.getRenderWheelAngle(AnimationTickHolder.getPartialTicks());
            float angleRadians = (float) Math.toRadians(renderAngle);

            transform(wheel.loadIdentity())
                    .rotateCentered(Direction.NORTH, angleRadians);
        }

        @Override
        public void remove() {
            wheel.delete();
        }

        @Override
        public void updateLight() {
            relight(pos, wheel);
        }

        private <T extends Translate<T> &Rotate<T>> T transform(T msr) {
            return msr.translate(getInstancePosition())
                    .centre()
                    .translate(0.0f, 0.0f, 0.0f)
                    .rotate(Direction.UP, (float) Math.toRadians(baseRotationY))
                    .unCentre();
        }
}
