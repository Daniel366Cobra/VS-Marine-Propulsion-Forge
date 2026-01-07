package io.github.daniel366cobra.vs_marine_propulsion.blocks.steering.ballast_tank;

import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

import static io.github.daniel366cobra.vs_marine_propulsion.VSMarinePropulsionEntities.BALLAST_TANK_VERTICAL_BLOCK_ENTITY;

public class BallastTankVerticalBlock extends BallastTankBlockBase {

    public static final BooleanProperty TOP = BooleanProperty.create("top");
    public static final BooleanProperty BOTTOM = BooleanProperty.create("bottom");

    public BallastTankVerticalBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState()
                .setValue(TOP, true)
                .setValue(BOTTOM, true));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TOP, BOTTOM);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // Vertical tanks don't need special placement logic
        return defaultBlockState();
    }

    @Override
    public boolean isHorizontal() {
        return false;
    }

    @Override
    public BlockEntityType<? extends BallastTankBlockEntity> getBlockEntityType() {
        return BALLAST_TANK_VERTICAL_BLOCK_ENTITY.get();
    }

    public static boolean isVerticalTank(BlockState state) {
        return state.getBlock() instanceof BallastTankVerticalBlock;
    }
}