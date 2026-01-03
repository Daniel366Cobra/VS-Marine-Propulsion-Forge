package io.github.daniel366cobra.vs_marine_propulsion.blocks.steering.ballast_tank;

import com.simibubi.create.api.connectivity.ConnectivityHandler;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import io.github.daniel366cobra.vs_marine_propulsion.VSMarinePropulsionWeights;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class BallastTankBlockEntity extends FluidTankBlockEntity {

    private double lastCalculatedMass = 0;
    private double lastFillPercentage = 0;

    public BallastTankBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        lastCalculatedMass = VSMarinePropulsionWeights.EMPTY_TANK_MASS;
    }

    @Override
    public void updateBoilerState() {
        // No boilers for ballast tanks
    }

    @Override
    public BallastTankBlockEntity getControllerBE() {
        if (isController() || !hasLevel()) return this;

        BlockEntity be = level.getBlockEntity(controller);
        return be instanceof BallastTankBlockEntity ? (BallastTankBlockEntity) be : null;
    }

    @Override
    public void tick() {
        super.tick();

        if (level == null || level.isClientSide || isRemoved()) return;

        double currentFill = getFillPercentage();

        if (Math.abs(currentFill - lastFillPercentage) > 0.005) {
            lastFillPercentage = currentFill;

            double newMass = VSMarinePropulsionWeights.getCurrentMass(level, worldPosition, getBlockState());

            if (Math.abs(newMass - lastCalculatedMass) > 0.1) {
                VSMarinePropulsionWeights.notifyMassChanged(
                        level, worldPosition, getBlockState(),
                        lastCalculatedMass,
                        newMass
                );
                lastCalculatedMass = newMass;
            }
        }
    }

    private double getFillPercentage() {
        if (tankInventory.getFluid().isEmpty()) return 0.0;
        long amount = tankInventory.getFluid().getAmount();
        long capacity = tankInventory.getCapacity();
        return capacity > 0 ? Math.min(1.0, (double) amount / capacity) : 0.0;
    }

    @Override
    public void write(CompoundTag tag, boolean clientPacket) {
        super.write(tag, clientPacket);
        if (!clientPacket) {
            tag.putDouble("LastFill", lastFillPercentage);
            tag.putDouble("LastMass", lastCalculatedMass);
        }
    }

    @Override
    protected void read(CompoundTag tag, boolean clientPacket) {
        super.read(tag, clientPacket);
        if (!clientPacket) {
            lastFillPercentage = tag.getDouble("LastFill");
            lastCalculatedMass = tag.getDouble("LastMass");
        }
    }
}