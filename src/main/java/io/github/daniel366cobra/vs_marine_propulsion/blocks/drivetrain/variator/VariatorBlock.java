package io.github.daniel366cobra.vs_marine_propulsion.blocks.drivetrain.variator;

import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import io.github.daniel366cobra.vs_marine_propulsion.VSMarinePropulsionBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

public class VariatorBlock extends DirectionalKineticBlock implements IBE<VariatorBlockEntity> {

    public static final BooleanProperty LINKED = BooleanProperty.create("linked");
    public static final IntegerProperty ORDER = IntegerProperty.create("order", 0, 6);

    public VariatorBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState()
                .setValue(LINKED, false)
                .setValue(ORDER, 3));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder
                .add(LINKED)
                .add(ORDER);
        super.createBlockStateDefinition(builder);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return super.getStateForPlacement(context)
                .setValue(LINKED,false)
                .setValue(ORDER, 3);
    }

    @Override
    public Class<VariatorBlockEntity> getBlockEntityClass() {
        return VariatorBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends VariatorBlockEntity> getBlockEntityType() {
        return VSMarinePropulsionBlockEntities.VARIATOR_BLOCK_ENTITY.get();
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(FACING).getAxis();
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return face.getAxis() == state.getValue(FACING).getAxis();
    }

}
