package io.github.daniel366cobra.vs_marine_propulsion.blocks.steering.rudder_bearing;

import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.contraptions.AssemblyException;
import com.simibubi.create.content.contraptions.ControlledContraptionEntity;
import com.simibubi.create.content.contraptions.bearing.BearingBlock;
import com.simibubi.create.content.contraptions.bearing.MechanicalBearingBlockEntity;
import io.github.daniel366cobra.vs_marine_propulsion.blocks.steering.RudderContraption;
import io.github.daniel366cobra.vs_marine_propulsion.ship.VSMarinePropulsionAttachment;
import io.github.daniel366cobra.vs_marine_propulsion.ship.data.ControlSurfaceData;
import io.github.daniel366cobra.vs_marine_propulsion.ship.data.HelmData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import org.joml.Vector3d;
import org.joml.primitives.AABBd;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

//TODO change logic to act like clockwork bearing
public class RudderBearingBlockEntityOld extends MechanicalBearingBlockEntity {

    private ControlSurfaceData controlSurfaceData;
    private int fluidSamplingCooldown = 0;
    private int fluidSamplingPoints = 10;
    private boolean isAssembled = false;

    public RudderBearingBlockEntityOld(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.controlSurfaceData = null; // Start with no data - we're just blocks
    }

    @Override
    public void initialize() {
        super.initialize();
        syncWithAttachment();
    }

    private void syncWithAttachment() {

        if (level == null || level.isClientSide || !isAssembled || this.controlSurfaceData == null) return;

        VSMarinePropulsionAttachment shipControl = VSMarinePropulsionAttachment.get(level, worldPosition);
        if (shipControl != null) {
            ControlSurfaceData existingData = shipControl.getControlSurfaceAtPos(worldPosition);
            if (existingData != null) {
                // Pull from attachment
                this.controlSurfaceData = existingData;
                this.angle = this.controlSurfaceData.angle;
            } else if (this.controlSurfaceData != null) {
                // First time - add to attachment
                shipControl.addControlSurface(worldPosition, this.controlSurfaceData);
            }
        }
    }

    @Override
    public void remove() {
        if (!this.getLevel().isClientSide()) {
            resetAttachment();
            super.remove();
        }
    }

    private void resetAttachment() {
        VSMarinePropulsionAttachment shipControl = VSMarinePropulsionAttachment.get(this.getLevel(), this.getBlockPos());
        if (shipControl != null)
            shipControl.removeControlSurface(this.getBlockPos());
    }

    private void resetDataAndAttachment() {
        this.controlSurfaceData = null;
        this.isAssembled = false;
        resetAttachment();
    }

    @Override
    public void write(CompoundTag compound, boolean clientPacket) {
        super.write(compound, clientPacket);
    }

    @Override
    protected void read(CompoundTag compound, boolean clientPacket) {
        super.read(compound, clientPacket);
    }

    @Override
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
        movedContraption = ControlledContraptionEntity.create(level, this, rudderContraption);
        BlockPos anchor = worldPosition.relative(direction);
        movedContraption.setPos(anchor.getX(), anchor.getY(), anchor.getZ());
        movedContraption.setRotationAxis(direction.getAxis());
        level.addFreshEntity(movedContraption);

        AllSoundEvents.CONTRAPTION_ASSEMBLE.playOnServer(level, worldPosition);

        running = true;
        angle = 0; // Reset to neutral
        isAssembled = true; // Now we're a proper contraption!

        syncWithAttachment();
        sendData();
    }

    @Override
    public void disassemble() {
        if (!running && movedContraption == null)
            return;

        // Scuttle control surface data - we're going back to blocks
        resetDataAndAttachment();

        angle = 0; // Reset angle
        sequencedAngleLimit = -1;

        if (movedContraption != null) {
            movedContraption.disassemble();
            AllSoundEvents.CONTRAPTION_DISASSEMBLE.playOnServer(level, worldPosition);
        }

        movedContraption = null;
        running = false;
        assembleNextTick = false;
        sendData();
    }

    @Override
    public void tick() {
        super.tick();

        // Only tick physics if we're properly assembled as a contraption
        if (!isAssembled || controlSurfaceData == null) return;

        BlockPos blockPos = this.getBlockPos();
        ControlledContraptionEntity controlledContraption = this.getMovedContraption();
        if (controlledContraption == null) {
            resetDataAndAttachment();
            return;
        }

        Ship ship = VSGameUtilsKt.getShipManagingPos(level, blockPos);
        if (ship == null) return;

        if (!level.isClientSide && !isVirtual()) {
            // Get the attachment
            VSMarinePropulsionAttachment shipControl = VSMarinePropulsionAttachment.get(level, blockPos);
            if (shipControl == null) return;

            //syncWithAttachment();

            // Get captain helm data and control surface data
            HelmData captainHelmData = shipControl.getHelmAtPos(shipControl.getCaptainHelmPosition());

            ControlSurfaceData persistentData = shipControl.getControlSurfaceAtPos(blockPos);
            if (persistentData == null) return;

            // PULL angle from captain helm IF we have input speed
            if (this.speed > 0 && captainHelmData != null) {
                float targetAngle = captainHelmData.rudderAngle;

                // Smooth movement in PERSISTENT data
                float angleDiff = targetAngle - persistentData.angle;
                float maxChange = Math.abs(this.speed * 0.5f);

                if (Math.abs(angleDiff) > maxChange) {
                    persistentData.angle += Math.copySign(maxChange, angleDiff);
                } else {
                    persistentData.angle = targetAngle;
                }

                // Clamp in persistent
                if (persistentData.angle > 40) persistentData.angle = 40;
                else if (persistentData.angle < -40) persistentData.angle = -40;
            }

            // Update local data and angle
            this.controlSurfaceData = persistentData; // Update reference
            this.angle = persistentData.angle; // Bearing follows persistent

            // Update the visual angle of the contraption
            if (controlledContraption != null) {
                controlledContraption.setAngle(this.angle);
            }

            // Update submerged percentage periodically
            fluidSamplingCooldown++;
            if (fluidSamplingCooldown > 10) {
                fluidSamplingCooldown = 0;
                updateSubmergedPercentage(controlledContraption, ship);
                persistentData.submergedPercentage = this.controlSurfaceData.submergedPercentage;
            }
        }
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
    public void attach(ControlledContraptionEntity contraption) {
        BlockState blockState = getBlockState();
        if (!(contraption.getContraption() instanceof RudderContraption rudderContraption))
            return;
        if (!blockState.hasProperty(BearingBlock.FACING))
            return;

        this.movedContraption = contraption;
        setChanged();
        BlockPos anchor = worldPosition.relative(blockState.getValue(BearingBlock.FACING));
        movedContraption.setPos(anchor.getX(), anchor.getY(), anchor.getZ());

        if (!level.isClientSide) {
            this.running = true;
            this.isAssembled = true;

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


}
