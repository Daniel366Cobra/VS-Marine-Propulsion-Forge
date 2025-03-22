package io.github.daniel366cobra.vs_marine_propulsion.blocks.propulsion.large_ship_propeller;

import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import io.github.daniel366cobra.vs_marine_propulsion.VSMarinePropulsionEntities;
import io.github.daniel366cobra.vs_marine_propulsion.blocks.propulsion.ShipPropellerBlockEntity;
import io.github.daniel366cobra.vs_marine_propulsion.ship_control.ShipForcesApplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

public class LargeShipPropellerBlock extends DirectionalKineticBlock implements IBE<ShipPropellerBlockEntity> {

    public LargeShipPropellerBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        super.onRemove(state, level, pos, newState, isMoving);
        if (level.isClientSide()) return;
        ShipForcesApplier shipControl = ShipForcesApplier.get(level, pos);
        if (shipControl != null)
            shipControl.removePropulsor(pos);
    }

    @Override
    public SpeedLevel getMinimumRequiredSpeedLevel() {
        return super.getMinimumRequiredSpeedLevel();
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(FACING).getAxis();
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return face == state.getValue(FACING).getOpposite();
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {

        Direction preferredFacing = getPreferredFacing(context);
        if (preferredFacing == null)
            preferredFacing = context.getNearestLookingDirection();

        return defaultBlockState().setValue(FACING, context.getPlayer() != null && context.getPlayer()
                .isShiftKeyDown() ? preferredFacing : preferredFacing.getOpposite());
    }

    @Override
    public Class<ShipPropellerBlockEntity> getBlockEntityClass() {
        return ShipPropellerBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends ShipPropellerBlockEntity> getBlockEntityType() {
        return VSMarinePropulsionEntities.LARGE_SHIP_PROPELLER_BLOCK_ENTITY.get();
    }
}
