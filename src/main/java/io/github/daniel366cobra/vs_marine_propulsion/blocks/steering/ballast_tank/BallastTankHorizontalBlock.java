package io.github.daniel366cobra.vs_marine_propulsion.blocks.steering.ballast_tank;

import io.github.daniel366cobra.vs_marine_propulsion.VSMarinePropulsionEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;

public class BallastTankHorizontalBlock extends BallastTankBlockBase {

    public static final BooleanProperty POSITIVE = BooleanProperty.create("positive");
    public static final BooleanProperty NEGATIVE = BooleanProperty.create("negative");
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;

    public BallastTankHorizontalBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState()
                .setValue(POSITIVE, true)
                .setValue(NEGATIVE, true)
                .setValue(AXIS, Direction.Axis.X));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(POSITIVE, NEGATIVE, AXIS);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        if (state == null) state = defaultBlockState();

        // Axis alignment logic
        if (context.getPlayer() == null || !context.getPlayer().isShiftKeyDown()) {
            BlockPos placedOnPos = context.getClickedPos()
                    .relative(context.getClickedFace().getOpposite());
            BlockState placedOn = context.getLevel().getBlockState(placedOnPos);

            Direction.Axis preferredAxis = placedOn.getOptionalValue(AXIS).orElse(null);
            if (preferredAxis != null) {
                return state.setValue(AXIS, preferredAxis);
            }
        }

        return state.setValue(AXIS, context.getHorizontalDirection().getAxis());
    }

    @Override
    public boolean isHorizontal() {
        return true;
    }

    @Override
    public BlockEntityType<? extends BallastTankBlockEntity> getBlockEntityType() {
        return VSMarinePropulsionEntities.BALLAST_TANK_HORIZONTAL_BLOCK_ENTITY.get();
    }

    public static boolean isHorizontalTank(BlockState state) {
        return state.getBlock() instanceof BallastTankHorizontalBlock;
    }
}