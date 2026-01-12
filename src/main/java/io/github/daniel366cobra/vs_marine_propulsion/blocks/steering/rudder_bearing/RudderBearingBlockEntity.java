package io.github.daniel366cobra.vs_marine_propulsion.blocks.steering.rudder_bearing;

import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.AssemblyException;
import com.simibubi.create.content.contraptions.ControlledContraptionEntity;
import com.simibubi.create.content.contraptions.IDisplayAssemblyExceptions;
import com.simibubi.create.content.contraptions.bearing.BearingBlock;
import com.simibubi.create.content.contraptions.bearing.IBearingBlockEntity;
import com.simibubi.create.content.contraptions.bearing.WindmillBearingBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour;
import com.simibubi.create.foundation.utility.animation.LerpedFloat;
import io.github.daniel366cobra.vs_marine_propulsion.VSMarinePropulsionMod;
import io.github.daniel366cobra.vs_marine_propulsion.blocks.steering.RudderContraption;
import io.github.daniel366cobra.vs_marine_propulsion.ship.VSMarinePropulsionAttachment;
import io.github.daniel366cobra.vs_marine_propulsion.ship.data.ControlSurfaceData;
import io.github.daniel366cobra.vs_marine_propulsion.ship.data.HelmData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import org.joml.Vector3d;
import org.joml.primitives.AABBd;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import java.util.List;

public class RudderBearingBlockEntity extends KineticBlockEntity implements IBearingBlockEntity, IDisplayAssemblyExceptions {

    protected ScrollOptionBehaviour<WindmillBearingBlockEntity.RotationDirection> movementDirection;

    protected boolean running = false;
    protected boolean assembleNextTick = false;

    private ControlledContraptionEntity rudderContraption = null;
    private AssemblyException lastException = null;

    private final LerpedFloat rudderAngle = LerpedFloat.linear();

    private float targetAngle = 0.0f;
    private int directionModifier = 1;

    private ControlSurfaceData controlSurfaceData;

    private int fluidSamplingCooldown = 0;


    public RudderBearingBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.setLazyTickRate(3);
        this.controlSurfaceData = null; // Start with no data
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);

        movementDirection = new ScrollOptionBehaviour<>(WindmillBearingBlockEntity.RotationDirection.class,
                Component.translatable(VSMarinePropulsionMod.MOD_ID + ".rudder.rotation_direction"), this, getMovementModeSlot());
        movementDirection.withCallback(cb -> onRotationDirectionChanged());

        behaviours.add(movementDirection);
    }

    private void onRotationDirectionChanged() {
        this.directionModifier = movementDirection.get() == WindmillBearingBlockEntity.RotationDirection.CLOCKWISE ? 1 : -1;
        notifyUpdate();
    }

    @Override
    public void initialize() {
        super.initialize();
        syncWithAttachment();
    }

    private void syncWithAttachment() {

        if (this.level == null || this.level.isClientSide || this.controlSurfaceData == null) return;

        VSMarinePropulsionAttachment shipControl = VSMarinePropulsionAttachment.get(this.level, this.worldPosition);

        if (shipControl != null) {
            ControlSurfaceData existingData = shipControl.getControlSurfaceAtPos(this.worldPosition);
            if (existingData != null) {
                // Pull from attachment
                this.controlSurfaceData = existingData;
            } else if (this.controlSurfaceData != null) {
                // First time - add to attachment
                shipControl.addControlSurface(this.worldPosition, this.controlSurfaceData);
            }
        }
    }

    @Override
    public void tick() {
        super.tick();

        this.rudderAngle.tickChaser();
        if (this.running) {
            applyRotations();
        }

        if (this.level == null || this.level.isClientSide) {
            return;
        }

        if (this.rudderContraption != null) {
            this.rudderContraption.tick();
        }

        if (this.assembleNextTick) {
            this.assembleNextTick = false;
            assemble();
        }

        float lastAngle = this.targetAngle;

        applyRudderCalculations();

        this.rudderAngle.chase(this.targetAngle, getAngularSpeed(), LerpedFloat.Chaser.LINEAR);

        if (lastAngle != this.targetAngle) {
            sendData();
        }
    }

    private void applyRotations() {
        BlockState blockState = this.getBlockState();
        Direction.Axis rotationAxis;

        if (blockState.hasProperty(BlockStateProperties.FACING)) {
            rotationAxis = blockState.getValue(BlockStateProperties.FACING).getAxis();

            if (this.rudderContraption != null) {
                this.rudderContraption.setAngle(this.rudderAngle.getValue());
                this.rudderContraption.setRotationAxis(rotationAxis);
            }
        }
    }

    private void applyRudderCalculations() {

        // Only tick physics if assembled as a contraption and have attachment data
        if (this.controlSurfaceData == null) return;

        if (this.rudderContraption == null) {
            resetDataAndAttachment();
            return;
        }

        Ship ship = VSGameUtilsKt.getShipManagingPos(this.level, this.worldPosition);
        if (ship == null) return;

        if (!this.level.isClientSide && !isVirtual()) {

            VSMarinePropulsionAttachment shipControl = VSMarinePropulsionAttachment.get(this.level, this.worldPosition);
            if (shipControl == null) return;

            ControlSurfaceData persistentData = shipControl.getControlSurfaceAtPos(this.worldPosition);
            if (persistentData == null) return;

            BlockPos captainPos = shipControl.getCaptainHelmPosition();
            HelmData captainHelmData = null;

            if (captainPos != null) {
                captainHelmData = shipControl.getHelmAtPos(captainPos);
            }

            float currentAngle = persistentData.angle; // -40..+40
            float rudderSpeed = getAngularSpeed();
            float newAngle;

                if (captainHelmData != null) {

                    float helmAngle = captainHelmData.rudderAngle; //-40..+40
                    float correctedHelmAngle = helmAngle * directionModifier;
                    float angleDelta = correctedHelmAngle - currentAngle;
                    newAngle = currentAngle + Mth.clamp(angleDelta, -rudderSpeed, rudderSpeed);

                } else {
                    // No helm or helm broken: slowly return to center
                    float returnSpeed = rudderSpeed * 0.5f; // Slower return speed

                    if (Math.abs(currentAngle) < returnSpeed) {
                        newAngle = 0.0f; // Snap to center when close
                    } else {
                        // Move toward center
                        newAngle = currentAngle - Math.signum(currentAngle) * returnSpeed;
                    }
                }

            newAngle = Mth.clamp(newAngle, -40.0f, 40.0f);
            persistentData.angle = newAngle;
            this.targetAngle = newAngle;

            this.controlSurfaceData = persistentData;

            this.fluidSamplingCooldown++;

            if (this.fluidSamplingCooldown > 10) {
                this.fluidSamplingCooldown = 0;
                updateSubmergedPercentage(this.rudderContraption, ship);
                persistentData.submergedPercentage = this.controlSurfaceData.submergedPercentage;
            }
        }
    }

    @Override
    public void lazyTick() {
        super.lazyTick();
        if (this.level != null && this.rudderContraption != null && !this.level.isClientSide)
            sendData();
    }

    @Override
    public AssemblyException getLastAssemblyException() {
        return this.lastException;
    }

    @Override
    public void remove() {
        if (this.level != null && !this.level.isClientSide) {
            resetDataAndAttachment();
            disassemble();
            super.remove();
        }
    }

    private void resetDataAndAttachment() {
        this.controlSurfaceData = null;
        VSMarinePropulsionAttachment shipControl = VSMarinePropulsionAttachment.get(this.level, this.worldPosition);
        if (shipControl != null)
            shipControl.removeControlSurface(this.worldPosition);
    }

    @Override
    public void write(CompoundTag compound, boolean clientPacket) {
        super.write(compound, clientPacket);

        compound.putFloat("RudderAngle", this.rudderAngle.getValue());
        compound.putInt("DirectionModifier", this.directionModifier);
        compound.putFloat("TargetAngle", this.targetAngle);
        compound.putBoolean("IsRunning", this.running);
        compound.putFloat("AngularSpeed", getAngularSpeed());

        AssemblyException.write(compound, getLastAssemblyException());
    }

    @Override
    protected void read(CompoundTag compound, boolean clientPacket) {
        super.read(compound, clientPacket);

        this.rudderAngle.setValue(compound.getFloat("RudderAngle"));

        if (compound.contains("DirectionModifier"))
            this.directionModifier = compound.getInt("DirectionModifier");
        else
            this.directionModifier = 1;

        float targetAngle = compound.getFloat("TargetAngle");
        float angularSpeed = compound.getFloat("AngularSpeed");
        this.rudderAngle.chase(targetAngle, angularSpeed, LerpedFloat.Chaser.LINEAR);

        this.running = compound.getBoolean("IsRunning");

        this.lastException = AssemblyException.read(compound);
    }

    public void assemble() {

        if (this.level == null || !(this.level.getBlockState(worldPosition).getBlock() instanceof RudderBearingBlock))
            return;

        Direction direction = this.getBlockState().getValue(RudderBearingBlock.FACING);

        resetDataAndAttachment();

        RudderContraption rudder = new RudderContraption(direction);
        try {
            if (!rudder.assemble(this.level, this.worldPosition))
                return;
            this.lastException = null;
        } catch (AssemblyException e) {
            this.lastException = e;
            sendData();
            return;
        }

        this.controlSurfaceData = new ControlSurfaceData(
                this.worldPosition,
                rudder.getNormalVector(),
                rudder.getRotationAxisVector(),
                rudder.getRudderBlocks()
        );

        rudder.removeBlocksFromWorld(this.level, BlockPos.ZERO);
        this.rudderContraption = ControlledContraptionEntity.create(this.level, this, rudder);
        BlockPos anchor = this.worldPosition.relative(direction);
        this.rudderContraption.setPos(anchor.getX(), anchor.getY(), anchor.getZ());
        this.rudderContraption.setRotationAxis(direction.getAxis());
        this.level.addFreshEntity(this.rudderContraption);

        AllSoundEvents.CONTRAPTION_ASSEMBLE.playOnServer(this.level, this.worldPosition);

        this.running = true;
        this.rudderAngle.setValue(0.0f);

        syncWithAttachment();
        sendData();
    }

    public void disassemble() {

        if (!this.running && this.rudderContraption == null)
            return;

        resetDataAndAttachment();

        this.rudderAngle.setValue(0.0f);

        applyRudderCalculations();
        applyRotations();

        if (this.rudderContraption != null) {
            this.rudderContraption.disassemble();
            AllSoundEvents.CONTRAPTION_DISASSEMBLE.playOnServer(this.level, this.worldPosition);
        }

        this.rudderContraption = null;
        this.running = false;
        this.assembleNextTick = false;
        sendData();
    }

    private float getAngularSpeed() {
        float speed = Math.abs(getSpeed() * 0.3f);
        if (this.level != null && this.level.isClientSide) {
            speed *= com.simibubi.create.foundation.utility.ServerSpeedProvider.get();
        }
        return speed;
    }

    private void updateSubmergedPercentage(ControlledContraptionEntity controlledContraption, Ship ship) {
        AABB shipyardRudderAabb = controlledContraption.getBoundingBox();
        Vector3d shipyardRudderAabbMin = new Vector3d(shipyardRudderAabb.minX, shipyardRudderAabb.minY, shipyardRudderAabb.minZ);
        Vector3d shipyardRudderAabbMax = new Vector3d(shipyardRudderAabb.maxX, shipyardRudderAabb.maxY, shipyardRudderAabb.maxZ);
        Vector3d worldRudderAabbMin = new Vector3d();
        Vector3d worldRudderAabbMax = new Vector3d();
        ship.getTransform().getShipToWorld().transformAab(shipyardRudderAabbMin, shipyardRudderAabbMax, worldRudderAabbMin, worldRudderAabbMax);
        AABBd worldRudderAabb = new AABBd(worldRudderAabbMin, worldRudderAabbMax);

        int fluidSamplingPoints = 10;

        this.controlSurfaceData.submergedPercentage = getSubmergedRatio(worldRudderAabb, this.level, fluidSamplingPoints);
    }

    public static float getSubmergedRatio(AABBd worldAabb, Level level, int samplingPoints) {
        double minY = worldAabb.minY;
        double maxY = worldAabb.maxY;
        double height = maxY - minY;

        // Bottom center of the AABB (XZ midpoint)
        double centerX = (worldAabb.minX + worldAabb.maxX) / 2;
        double centerZ = (worldAabb.minZ + worldAabb.maxZ) / 2;

        float submergedPoints = 0;

        for (int i = 0; i < samplingPoints; i++) {
            double currentY = minY + (i * height / samplingPoints);
            BlockPos pos = BlockPos.containing(centerX, currentY, centerZ);
            FluidState fluidState = level.getFluidState(pos);

            if (fluidState.is(Fluids.WATER)) submergedPoints++;

        }
        return Math.min(submergedPoints / samplingPoints, 1f);
    }

    @Override
    public boolean isAttachedTo(AbstractContraptionEntity contraption) {
        if (!(contraption.getContraption() instanceof RudderContraption))
            return false;

        return this.rudderContraption == contraption;
    }

    @Override
    public void attach(ControlledContraptionEntity contraption) {

        BlockState blockState = this.getBlockState();

        if (!(contraption.getContraption() instanceof RudderContraption rudder))
            return;
        if (!blockState.hasProperty(BearingBlock.FACING))
            return;

        this.rudderContraption = contraption;
        setChanged();

        BlockPos anchor = this.worldPosition.relative(blockState.getValue(BearingBlock.FACING));
        this.rudderContraption.setPos(anchor.getX(), anchor.getY(), anchor.getZ());

        if (this.level != null && !this.level.isClientSide) {
            this.running = true;

            this.controlSurfaceData = new ControlSurfaceData(
                    this.worldPosition,
                    rudder.getNormalVector(),
                    rudder.getRotationAxisVector(),
                    rudder.getRudderBlocks()
            );

            syncWithAttachment();
            sendData();
        }
    }

    @Override
    public void onSpeedChanged(float prevSpeed) {
        super.onSpeedChanged(prevSpeed);
        if (!this.running)
            this.assembleNextTick = true;
    }

    @Override
    public void onStall() {
        if (this.level != null && !this.level.isClientSide)
            sendData();
    }

    @Override
    public boolean isValid() {
        return !this.isRemoved();
    }

    @Override
    public boolean isWoodenTop() {
        return false;
    }

    @Override
    public BlockPos getBlockPosition() {
        return this.worldPosition;
    }

    @Override
    public float getInterpolatedAngle(float partialTicks) {
        return this.rudderAngle.getValue(partialTicks);
    }

    @Override
    public void setAngle(float forcedAngle) {
        this.rudderAngle.setValue(forcedAngle);
        this.rudderAngle.updateChaseSpeed(forcedAngle);
    }
}