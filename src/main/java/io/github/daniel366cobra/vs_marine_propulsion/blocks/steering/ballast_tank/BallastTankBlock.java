package io.github.daniel366cobra.vs_marine_propulsion.blocks.steering.ballast_tank;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.fluids.tank.FluidTankBlock;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import io.github.daniel366cobra.vs_marine_propulsion.VSMarinePropulsionEntities;
import io.github.daniel366cobra.vs_marine_propulsion.VSMarinePropulsionWeights;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

//TODO: implement water-fillable multiblock with dynamically changing mass
public class BallastTankBlock extends FluidTankBlock {

    public BallastTankBlock(Properties properties) {
        super(properties, false);
    }

    @Override
    public BlockEntityType<? extends FluidTankBlockEntity> getBlockEntityType() {
        return VSMarinePropulsionEntities.BALLAST_TANK_BLOCK_ENTITY.get();
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos,
                        BlockState oldState, boolean isMoving) {
        if (oldState.getBlock() == state.getBlock()) return;
        if (isMoving) return;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos,
                         BlockState newState, boolean isMoving) {
        if (!level.isClientSide && state.getBlock() != newState.getBlock()) {
            double currentMass = VSMarinePropulsionWeights.getCurrentMass(level, pos, state);
            VSMarinePropulsionWeights.notifyMassChanged(level, pos, state, currentMass, 0.0);
        }

        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BallastTankBlockEntity(getBlockEntityType(), pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        return super.getTicker(level, state, type);
    }
}