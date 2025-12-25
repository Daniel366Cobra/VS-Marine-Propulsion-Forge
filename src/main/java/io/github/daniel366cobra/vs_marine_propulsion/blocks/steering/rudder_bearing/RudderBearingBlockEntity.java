package io.github.daniel366cobra.vs_marine_propulsion.blocks.steering.rudder_bearing;

import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.AssemblyException;
import com.simibubi.create.content.contraptions.ControlledContraptionEntity;
import com.simibubi.create.content.contraptions.IDisplayAssemblyExceptions;
import com.simibubi.create.content.contraptions.bearing.BearingBlock;
import com.simibubi.create.content.contraptions.bearing.IBearingBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.utility.AngleHelper;
import io.github.daniel366cobra.vs_marine_propulsion.blocks.steering.RudderContraption;
import io.github.daniel366cobra.vs_marine_propulsion.ship.VSMarinePropulsionAttachment;
import io.github.daniel366cobra.vs_marine_propulsion.ship.data.ControlSurfaceData;
import io.github.daniel366cobra.vs_marine_propulsion.ship.data.HelmData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
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

public class RudderBearingBlockEntity extends KineticBlockEntity implements IBearingBlockEntity, IDisplayAssemblyExceptions {

    private ControlSurfaceData controlSurfaceData;
    private int fluidSamplingCooldown = 0;
    private int fluidSamplingPoints = 10;

    protected float rudderAngle;
    protected float clientRudderAngleDiff;
    private float targetAngle;
    private float prevRudderAngle;


    protected boolean running;
    protected boolean assembleNextTick;
    protected AssemblyException lastException;
    protected ControlledContraptionEntity rudderContraption;

    public RudderBearingBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        setLazyTickRate(3);
        this.targetAngle = 0.0f;
        this.controlSurfaceData = null; // Start with no data - we're just blocks
    }

    @Override
    public void initialize() {
        super.initialize();
        syncWithAttachment();
    }

    private void syncWithAttachment() {

        if (level == null || level.isClientSide || this.controlSurfaceData == null) return;

        VSMarinePropulsionAttachment shipControl = VSMarinePropulsionAttachment.get(level, worldPosition);
        if (shipControl != null) {
            ControlSurfaceData existingData = shipControl.getControlSurfaceAtPos(worldPosition);
            if (existingData != null) {
                // Pull from attachment
                this.controlSurfaceData = existingData;
            } else if (this.controlSurfaceData != null) {
                // First time - add to attachment
                shipControl.addControlSurface(worldPosition, this.controlSurfaceData);
            }
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (level.isClientSide) {
            prevRudderAngle = rudderAngle;
            clientRudderAngleDiff /= 2;
        }

        if (!level.isClientSide && assembleNextTick) {
            assembleNextTick = false;
            if (running) {
                if (speed == 0 && (rudderContraption == null || rudderContraption.getContraption()
                        .getBlocks()
                        .isEmpty())) {
                    if (rudderContraption != null)
                        rudderContraption.getContraption()
                                .stop(level);
                    disassemble();
                }
                return;
            } else
                assemble();
            return;
        }

        if (!running) return;

        if (!(rudderContraption != null && rudderContraption.isStalled())) {
            float newAngle = rudderAngle + getRudderSpeed();
            rudderAngle = rudderToBearing(newAngle);
        }

        applyRotations();

        applyRudderCalculations();
    }

    private void applyRotations() {
        BlockState blockState = getBlockState();
        Direction.Axis rotationAxis;

        if (blockState.hasProperty(BlockStateProperties.FACING)) {
            rotationAxis = blockState.getValue(BlockStateProperties.FACING).getAxis();

            if (rudderContraption != null) {
                rudderContraption.setAngle(rudderAngle);
                rudderContraption.setRotationAxis(rotationAxis);
            }
        }
    }

    //TODO unify speed calculations for attachment and angle; maybe set rudder angle by force without damping
    private void applyRudderCalculations() {
        // Only tick physics if we're properly assembled as a contraption
        if (controlSurfaceData == null) return;

        BlockPos blockPos = this.getBlockPos();

        if (this.rudderContraption == null) {
            resetDataAndAttachment();
            return;
        }

        Ship ship = VSGameUtilsKt.getShipManagingPos(level, blockPos);
        if (ship == null) return;

        if (!level.isClientSide && !isVirtual()) {
            VSMarinePropulsionAttachment shipControl = VSMarinePropulsionAttachment.get(level, blockPos);
            if (shipControl == null) return;

            // Get persistent data
            ControlSurfaceData persistentData = shipControl.getControlSurfaceAtPos(blockPos);
            if (persistentData == null) return;

            // Get target from captain helm
            //FIXME breaking the helm gets crash due to null pos
            HelmData captainHelmData = shipControl.getHelmAtPos(shipControl.getCaptainHelmPosition());

            if (captainHelmData != null) {
                // 1. Get helm data's rudder angle
                float helmAngle = captainHelmData.rudderAngle;

                // 2. Derive angle delta between control surface data and helm data
                float currentAngle = persistentData.angle; // -40..+40
                float angleDelta = helmAngle - currentAngle;

                // 3. Get actual angle step per tick based on bearing input speed
                float maxStep = getAngularSpeed(); // How fast we CAN move
                float actualStep;

                if (angleDelta < 0) {
                    // Need to move negative
                    actualStep = Math.max(-maxStep, angleDelta);
                } else {
                    // Need to move positive
                    actualStep = Math.min(maxStep, angleDelta);
                }

                // 4. Calculate new angle
                float newAngle = currentAngle + actualStep;

                // Clamp to rudder limits
                newAngle = Mth.clamp(newAngle, -40.0f, 40.0f);

                // 5. Update persistent and local control surface data with new angle
                persistentData.angle = newAngle;
                this.controlSurfaceData = persistentData;

                targetAngle = rudderToBearing(newAngle);

                /*
                Player nearbyPlayer = level.getNearestPlayer(blockPos.getX(), blockPos.getY(), blockPos.getZ(), 10, false);

                if (nearbyPlayer != null)
                nearbyPlayer.displayClientMessage(
                        Component.literal("helm angle: " + helmAngle
                                + "cur angle: " + currentAngle
                                + "new angle: " + newAngle
                                + "tgt angle: " + targetAngle
                                + "rud angle: " + rudderAngle),
                        true
                );

                 */

            }

            // Update submerged percentage periodically
            fluidSamplingCooldown++;
            if (fluidSamplingCooldown > 10) {
                fluidSamplingCooldown = 0;
                updateSubmergedPercentage(this.rudderContraption, ship);
                persistentData.submergedPercentage = this.controlSurfaceData.submergedPercentage;
            }
        }
    }

    @Override
    public void lazyTick() {
        super.lazyTick();
        if (rudderContraption != null && !level.isClientSide)
            sendData();
    }

    @Override
    public AssemblyException getLastAssemblyException() {
        return lastException;
    }

    @Override
    public void remove() {
        if (!this.getLevel().isClientSide()) {
            resetDataAndAttachment();
            disassemble();
            super.remove();
        }
    }


    private void resetDataAndAttachment() {
        this.controlSurfaceData = null;
        VSMarinePropulsionAttachment shipControl = VSMarinePropulsionAttachment.get(this.getLevel(), this.getBlockPos());
        if (shipControl != null)
            shipControl.removeControlSurface(this.getBlockPos());
    }

    @Override
    public void write(CompoundTag compound, boolean clientPacket) {
        compound.putBoolean("Running", running);
        compound.putFloat("RudderAngle", rudderAngle);
        compound.putFloat("TargetAngle", targetAngle);
        AssemblyException.write(compound, lastException);
        super.write(compound, clientPacket);
    }

    @Override
    protected void read(CompoundTag compound, boolean clientPacket) {
        float rudderAnglePrev = rudderAngle;

        running = compound.getBoolean("Running");
        rudderAngle = compound.getFloat("RudderAngle");
        targetAngle = compound.getFloat("TargetAngle");

        lastException = AssemblyException.read(compound);
        super.read(compound, clientPacket);

        if (!clientPacket)
            return;

        if (running) {
            clientRudderAngleDiff = rudderAngle - rudderAnglePrev;
            rudderAngle = rudderAnglePrev;
        } else {
            rudderContraption = null;
        }
    }

    public void assemble() {

        if (!(level.getBlockState(worldPosition).getBlock() instanceof RudderBearingBlock))
            return;

        Direction direction = getBlockState().getValue(RudderBearingBlock.FACING);

        // Scuttle any old data before creating new contraption
        resetDataAndAttachment();

        RudderContraption rudderContraption = new RudderContraption(direction);
        try {
            if (!rudderContraption.assemble(level, worldPosition))
                return;
            lastException = null;
        } catch (AssemblyException e) {
            lastException = e;
            sendData();
            return;
        }

        // Create FRESH control surface data for the new contraption
        this.controlSurfaceData = new ControlSurfaceData(
                worldPosition,
                rudderContraption.getNormalVector(),
                rudderContraption.getRotationAxisVector(),
                rudderContraption.getRudderBlocks()
        );

        rudderContraption.removeBlocksFromWorld(level, BlockPos.ZERO);
        this.rudderContraption = ControlledContraptionEntity.create(level, this, rudderContraption);
        BlockPos anchor = worldPosition.relative(direction);
        this.rudderContraption.setPos(anchor.getX(), anchor.getY(), anchor.getZ());
        this.rudderContraption.setRotationAxis(direction.getAxis());
        level.addFreshEntity(this.rudderContraption);

        AllSoundEvents.CONTRAPTION_ASSEMBLE.playOnServer(level, worldPosition);

        running = true;
        this.controlSurfaceData.angle = 0;
        rudderAngle = 0; // Reset to neutral
        // Now we're a proper contraption!

        syncWithAttachment();
        sendData();
    }

    public void disassemble() {

        if (!running && rudderContraption == null)
            return;

        // Scuttle control surface data - we're going back to blocks
        resetDataAndAttachment();

        rudderAngle = 0; // Reset angle

        applyRudderCalculations();

        if (rudderContraption != null) {
            rudderContraption.disassemble();
            AllSoundEvents.CONTRAPTION_DISASSEMBLE.playOnServer(level, worldPosition);
        }

        rudderContraption = null;
        running = false;
        assembleNextTick = false;
        sendData();
    }

    private float getRudderSpeed() {
        // For client interpolation only
        float speed = getAngularSpeed() / 2f;

        if (speed != 0 && rudderAngle != targetAngle) {

            float angleDiff = AngleHelper.getShortestAngleDiff(rudderAngle, targetAngle);

            speed = Mth.clamp(angleDiff, -speed, speed);
        } else {
            speed = 0;
        }

        return speed + clientRudderAngleDiff / 3f;
    }

    private float getAngularSpeed() {
        float speed = Math.abs(getSpeed() * 3 / 10f); // Scale factor
        if (level.isClientSide) {
            speed *= com.simibubi.create.foundation.utility.ServerSpeedProvider.get();
        }
        return speed;
    }

    private float bearingToRudder(float bearingAngle) {
        return bearingAngle > 180 ? bearingAngle - 360 : bearingAngle;
    }

    private float rudderToBearing(float rudderAngle) {
        rudderAngle %= 360;
        return (rudderAngle < 0) ? 360 + rudderAngle : rudderAngle;
    }

    private void updateSubmergedPercentage(ControlledContraptionEntity controlledContraption, Ship ship) {
        AABB shipyardRudderAabb = controlledContraption.getBoundingBox();
        Vector3d shipyardRudderAabbMin = new Vector3d(shipyardRudderAabb.minX, shipyardRudderAabb.minY, shipyardRudderAabb.minZ);
        Vector3d shipyardRudderAabbMax = new Vector3d(shipyardRudderAabb.maxX, shipyardRudderAabb.maxY, shipyardRudderAabb.maxZ);
        Vector3d worldRudderAabbMin = new Vector3d();
        Vector3d worldRudderAabbMax = new Vector3d();
        ship.getTransform().getShipToWorld().transformAab(shipyardRudderAabbMin, shipyardRudderAabbMax, worldRudderAabbMin, worldRudderAabbMax);
        AABBd worldRudderAabb = new AABBd(worldRudderAabbMin, worldRudderAabbMax);

        this.controlSurfaceData.submergedPercentage = getSubmergedRatio(worldRudderAabb, level, fluidSamplingPoints);
    }

    public static float getSubmergedRatio(AABBd worldAabb, Level world, int samplingPoints) {
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
            FluidState fluidState = world.getFluidState(pos);

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
        BlockState blockState = getBlockState();
        if (!(contraption.getContraption() instanceof RudderContraption rudderContraption))
            return;
        if (!blockState.hasProperty(BearingBlock.FACING))
            return;

        this.rudderContraption = contraption;
        //setChanged();
        BlockPos anchor = worldPosition.relative(blockState.getValue(BearingBlock.FACING));
        this.rudderContraption.setPos(anchor.getX(), anchor.getY(), anchor.getZ());

        if (!level.isClientSide) {
            this.running = true;

            // ALWAYS recreate control surface data on attach for consistency
            this.controlSurfaceData = new ControlSurfaceData(
                    worldPosition,
                    rudderContraption.getNormalVector(),
                    rudderContraption.getRotationAxisVector(),
                    rudderContraption.getRudderBlocks()
            );

            syncWithAttachment();
            sendData();
        }
    }

    @Override
    public void onSpeedChanged(float prevSpeed) {
        super.onSpeedChanged(prevSpeed);
        assembleNextTick = true;
    }

    @Override
    public void onStall() {
        if (!level.isClientSide)
            sendData();
    }

    @Override
    public boolean isValid() {
        return !isRemoved();
    }

    public boolean isRunning() {
        return running;
    }

    @Override
    public boolean isWoodenTop() {
        return false;
    }

    @Override
    public BlockPos getBlockPosition() {
        return worldPosition;
    }

    @Override
    public float getInterpolatedAngle(float partialTicks) {
        if (isVirtual())
            return Mth.lerp(partialTicks, prevRudderAngle, rudderAngle);
        if (rudderContraption == null || rudderContraption.isStalled())
            partialTicks = 0;
        return Mth.lerp(partialTicks, rudderAngle, rudderAngle + getRudderSpeed());
    }

    @Override
    public void setAngle(float forcedAngle) {
        rudderAngle = forcedAngle;
    }
}
