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
import io.github.daniel366cobra.vs_marine_propulsion.ship_control.PropellerThrustCalculator;
import io.github.daniel366cobra.vs_marine_propulsion.ship_control.PropulsorData;
import io.github.daniel366cobra.vs_marine_propulsion.ship_control.ShipForcesApplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

import java.util.List;

import static com.simibubi.create.content.kinetics.base.DirectionalKineticBlock.FACING;


public class ShipPropellerBlockEntity extends KineticBlockEntity {

    private final PropellerThrustCalculator thrustCalculator;

    //Propulsor data for the propeller block entity.
    private final PropulsorData propulsorData;

    private boolean updateThrust = true;
    private int thrustUpdateCooldown = 0;

    protected ScrollOptionBehaviour<WindmillBearingBlockEntity.RotationDirection> rotationDirectionBehavior;

    public int propellerHandedness = 1;
    public LerpedFloat actualSpeed;
    public float angle;

    //FACING is where the propeller's hub is looking, the thrust is in the opposite direction
    public ShipPropellerBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state, PropellerThrustCalculator thrustCalculator) {
        super(typeIn, pos, state);
        this.thrustCalculator = thrustCalculator;
        this.propulsorData = new PropulsorData(VectorConversionsMCKt.toJOMLD(getBlockState().getValue(FACING).getOpposite().getNormal()), 0.0f);
        this.actualSpeed = LerpedFloat.linear()
                .startWithValue(0)
                .chase(0, 1 / 64f, LerpedFloat.Chaser.EXP);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        rotationDirectionBehavior = new ScrollOptionBehaviour<>(WindmillBearingBlockEntity.RotationDirection.class,
                Component.translatable(VSMarinePropulsionMod.MOD_ID + ".propeller.rotation_direction"), this, new RotationDirectionValueBox());
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

        if (compound.contains("PropellerSpeed"))
            this.actualSpeed.readNBT(compound.getCompound("PropellerSpeed"), clientPacket);
    }

    @Override
    public void write(CompoundTag compound, boolean clientPacket) {
        super.write(compound, clientPacket);
        compound.putInt("PropellerHandedness", this.propellerHandedness);
        compound.put("PropellerSpeed", this.actualSpeed.writeNBT());
    }

    public void onRotationDirectionChanged() {
        //1 for CW, -1 for CCW
        this.propellerHandedness = rotationDirectionBehavior.get() == WindmillBearingBlockEntity.RotationDirection.CLOCKWISE ? 1 : -1;
        notifyUpdate();
    }

    @Override
    public void remove() {
        if (!this.getLevel().isClientSide()) {
            ShipForcesApplier shipControl = ShipForcesApplier.get(this.getLevel(), this.getBlockPos());
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

        Level level = this.getLevel();
        if (level == null) return;

        BlockPos blockPos = this.getBlockPos();

        float chaseSpeed = this.getSpeed();

        actualSpeed.updateChaseTarget(chaseSpeed);
        actualSpeed.tickChaser();

        angle += actualSpeed.getValue() * 3 / 10f;
        angle %= 360;

        Ship ship = VSGameUtilsKt.getShipManagingPos(level, blockPos);

        if (ship == null) return;

        if (level.isClientSide || isVirtual()) {
            if (Math.abs(this.actualSpeed.getValue()) >= 0.1f) {
                updateParticles(ship);
            }

        } else {
            ShipForcesApplier shipControl = ShipForcesApplier.get(level, blockPos);

            if (shipControl != null) {
                if (shipControl.getPropulsorAtPos(blockPos) == null)
                    shipControl.addPropulsor(blockPos, this.propulsorData);
            }
            if (thrustUpdateCooldown-- <= 0) {
                thrustUpdateCooldown = 5;
                updateThrust = true;
            }

            if (updateThrust) {
                updateThrust = false;
                updateThrust(ship);
            }

            sendData();
        }
    }

    public void updateThrust(Ship ship) {

        float RPM = this.actualSpeed.getValue();

        //CRINGE x2
        int dirMultiplier = this.getBlockState().getValue(FACING).getOpposite().getAxisDirection().getStep();

        float estThrust = this.thrustCalculator.thrust(Math.abs(RPM));

        float shipVelocity = (float)ship.getVelocity().length();

        VSMarinePropulsionMod.LOGGER.info("SHIP SPEED: " + shipVelocity
                + ", Ca/CaCrit: " + this.thrustCalculator.caNumber(Math.abs(RPM), shipVelocity, 1.0f) / this.thrustCalculator.caNumberCritKeller()
                + ", EAR/EARmin: " + this.thrustCalculator.getPropEffectiveArea() / this.thrustCalculator.earKeller(estThrust, 1.0f)
        );

        Vector3d transformedPosVector = new Vector3d(ship.getTransform().getShipToWorld()
                .transformPosition(VectorConversionsMCKt.toJOMLD(this.getBlockPos())
                        .add(0.5, 0.5, 0.5)));

        this.propulsorData.submerged = this.getLevel().isWaterAt(new BlockPos((int) transformedPosVector.x, (int) transformedPosVector.y, (int) transformedPosVector.z));

        this.propulsorData.thrust = this.thrustCalculator.thrust(RPM * dirMultiplier) * this.propellerHandedness;

    }

    public void updateParticles(Ship ship) {

        //VSMarinePropulsionMod.LOGGER.info("ANGLE: " + angle + ", SPEED: " + this.getSpeed() + ", HANDEDNESS: " + propellerHandedness);

        Vec3 shipyardBlockCenter = this.getBlockPos().getCenter();
        Vector3d worldBlockCenter = ship.getTransform().getShipToWorld().transformPosition(new Vector3d(shipyardBlockCenter.x, shipyardBlockCenter.y, shipyardBlockCenter.z));

        Vector3d shipyardFacingVector = new Vector3d(propulsorData.dir).negate();

        int dirMultiplier = this.getBlockState().getValue(FACING).getAxisDirection().getStep();

        Vector3d worldFacingVector = ship.getTransform().getShipToWorldRotation().transform(new Vector3d(shipyardFacingVector));
        Vector3d worldAxisVector = new Vector3d(worldFacingVector).mul(dirMultiplier);

        int maxParticleCount = 10;
        float propellerRadius = 1.5f;
        float propellerPitchAngle = 22.5f;

        float shipSpeed = (float) ship.getVelocity().length();
        float propellerSpeed = this.actualSpeed.getValue();
        int absSpeed = (int) Math.abs(propellerSpeed);


        //TODO: get rid of (-1) in particles speed direction?
        float particleSpeedScalar = (float) (Math.PI * propellerRadius * Math.tan(Math.toRadians(propellerPitchAngle))
                * propellerSpeed * this.propellerHandedness * (-dirMultiplier) / 30.0f);
        Vector3d particleSpeed = new Vector3d().set(worldFacingVector).mul(particleSpeedScalar);

        Vector3d orthogonal = new Vector3d().orthogonalize(worldFacingVector);
        Vector3d forceVector = new Vector3d(worldFacingVector).negate();
        orthogonal.rotateAxis(Math.toRadians(angle), worldAxisVector.x, worldAxisVector.y, worldAxisVector.z);
        Vector3d bladeCircleOffset = new Vector3d().set(worldFacingVector).mul(0.3);
        Vector3d bladeCircleCenter = new Vector3d().set(worldBlockCenter).add(bladeCircleOffset);

        int particleCount = maxParticleCount * absSpeed / 256;
        float bubbleCount = Math.max(0, particleCount
                * (this.thrustCalculator.caNumber(absSpeed, shipSpeed, 1.0f) / this.thrustCalculator.caNumberCritKeller()));

        for (int j = 0; j < 4; j++) {
            orthogonal.rotateAxis(Math.PI / 2, forceVector.x, forceVector.y, forceVector.z);

            for (int i = 0; i < particleCount; i++) {
                SimpleParticleType particleType = (i < bubbleCount)? ParticleTypes.BUBBLE : ParticleTypes.UNDERWATER;
                float bladeParticleOffset = 1.0f * (maxParticleCount - i) / maxParticleCount * propellerRadius;
                Vector3d particleOriginVector = new Vector3d().set(bladeCircleCenter).add(orthogonal.mul(bladeParticleOffset, new Vector3d()));

                level.addParticle(
                        particleType,
                        particleOriginVector.x, particleOriginVector.y, particleOriginVector.z,
                        particleSpeed.x, particleSpeed.y, particleSpeed.z);
            }
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
