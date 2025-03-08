package io.github.daniel366cobra.vs_marine_propulsion.blocks.propulsion;

import com.simibubi.create.content.contraptions.bearing.WindmillBearingBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.CenteredSideValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour;
import com.simibubi.create.foundation.utility.AngleHelper;
import com.simibubi.create.foundation.utility.VecHelper;
import com.simibubi.create.foundation.utility.animation.LerpedFloat;
import io.github.daniel366cobra.vs_marine_propulsion.VSMarinePropulsionMod;
import io.github.daniel366cobra.vs_marine_propulsion.ship_control.PropellerThrustCurve;
import io.github.daniel366cobra.vs_marine_propulsion.ship_control.PropulsorData;
import io.github.daniel366cobra.vs_marine_propulsion.ship_control.ShipControl;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

import java.util.List;

import static com.simibubi.create.content.kinetics.base.DirectionalKineticBlock.FACING;


public class ShipPropellerBlockEntity extends KineticBlockEntity {

    //Thrust curve for the propeller block entity. Assigned at registration.
    private PropellerThrustCurve thrustCurve;

    //Propulsor data for the propeller block entity.
    private PropulsorData propulsorData = new PropulsorData(VectorConversionsMCKt.toJOMLD(getBlockState().getValue(FACING).getNormal()), 0);

    private boolean updateThrust = true;
    private int thrustUpdateCooldown = 0;

    protected ScrollOptionBehaviour<WindmillBearingBlockEntity.RotationDirection> rotationDirectionBehavior;

    public int propellerHandedness = 1;
    public LerpedFloat visualSpeed = LerpedFloat.linear();
    public float angle;

    public ShipPropellerBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        rotationDirectionBehavior = new ScrollOptionBehaviour<>(WindmillBearingBlockEntity.RotationDirection.class,
                Component.translatable("propeller.rotation_direction"), this, new RotationDirectionValueBox());
        rotationDirectionBehavior.withCallback(cb -> onRotationDirectionChanged());
        behaviours.add(rotationDirectionBehavior);
    }

    @Override
    protected void read(CompoundTag compound, boolean clientPacket) {
        super.read(compound, clientPacket);
        if (compound.contains("PropellerHandedness"))
            this.propellerHandedness = compound.getInt("PropellerHandedness");
        else
            this.propellerHandedness = 1;

        if (clientPacket) {
            visualSpeed.chase(getGeneratedSpeed(), 1 / 64f, LerpedFloat.Chaser.EXP);
        }
    }

    @Override
    public void write(CompoundTag compound, boolean clientPacket) {
        super.write(compound, clientPacket);
        compound.putInt("PropellerHandedness", this.propellerHandedness);
    }

    public Direction getThrustOriginSide() {
        return this.getBlockState()
                .getValue(BlockStateProperties.FACING);
    }

    public void setThrustCurve(PropellerThrustCurve thrustCurve) {
        this.thrustCurve = thrustCurve;
    }

    public void onRotationDirectionChanged() {
        //1 for CW, -1 for CCW
        this.propellerHandedness = rotationDirectionBehavior.get() == WindmillBearingBlockEntity.RotationDirection.CLOCKWISE ? 1 : -1;
        notifyUpdate();
    }

    public Direction getThrustDirection() {
        float speed = getSpeed();
        if (speed == 0)
            return null;
        Direction facing = getBlockState().getValue(BlockStateProperties.FACING);
        speed = convertToDirection(speed, facing);
        return speed > 0 ? facing : facing.getOpposite();
    }

    @Override
    public void remove() {
        if (!this.getLevel().isClientSide()) {
            ShipControl shipControl = ShipControl.get(this.getLevel(), this.getBlockPos());
            if (shipControl != null)
                shipControl.removePropulsor(this.getBlockPos());
        }
        super.remove();
    }

    @Override
    public void onSpeedChanged(float prevSpeed) {
        super.onSpeedChanged(prevSpeed);
        updateThrust = true;
    }

    @Override
    public void tick() {
        super.tick();

        if (level.isClientSide || isVirtual()) {
            float targetSpeed = this.getSpeed();
            visualSpeed.updateChaseTarget(targetSpeed);
            visualSpeed.tickChaser();
            angle += visualSpeed.getValue() * 3 / 10f;
            angle %= 360;
        } else {

            if (thrustUpdateCooldown-- <= 0) {
                thrustUpdateCooldown = 5;
                updateThrust = true;
            }

            if (updateThrust) {
                updateThrust = false;
                updatePropellerThrust();
                sendData();
            }
        }

    }

    public void updatePropellerThrust() {
        this.propulsorData.thrust = this.thrustCurve.calculateThrust(this.getSpeed());

        VSMarinePropulsionMod.LOGGER.info("SPEED: " + this.getSpeed() + ", THRUST: " + this.propulsorData.thrust);
        ShipControl shipControl = ShipControl.get(this.getLevel(), this.getBlockPos());

        if (shipControl != null) {
            if (shipControl.getPropulsorAtPos(this.getBlockPos()) == null)
                shipControl.addPropulsor(this.getBlockPos(), this.propulsorData);
        }
    }

    private static class RotationDirectionValueBox extends CenteredSideValueBoxTransform {

        public RotationDirectionValueBox() {
            super((state, direction) -> {
                Direction.Axis axis = direction.getAxis();
                Direction.Axis facingAxis = state.getValue(FACING).getAxis();
                return facingAxis != axis;
            });
        }

        @Override
        public Vec3 getLocalOffset(BlockState state) {
            Direction facing = state.getValue(FACING);
            Vec3 vec = VecHelper.voxelSpace(8f, 8f, 15.5f);
            vec = VecHelper.rotateCentered(vec, AngleHelper.horizontalAngle(getSide()), Direction.Axis.Y);
            vec = VecHelper.rotateCentered(vec, AngleHelper.verticalAngle(getSide()), Direction.Axis.X);
            Vec3 facingNormal = Vec3.atLowerCornerOf(facing.getNormal());
            vec = vec.subtract(facingNormal.scale(5 / 16f));
            return vec;
        }

        @Override
        public float getScale() {
            return super.getScale();
        }

    }


}
