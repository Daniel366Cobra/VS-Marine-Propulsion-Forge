package io.github.daniel366cobra.vs_marine_propulsion.blocks.control.engine_order_telegraph;

import com.simibubi.create.foundation.block.IBE;
import io.github.daniel366cobra.vs_marine_propulsion.VSMarinePropulsionBlockEntities;
import io.github.daniel366cobra.vs_marine_propulsion.VSMarinePropulsionShapes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class EngineOrderTelegraphBlock extends FaceAttachedHorizontalDirectionalBlock implements IBE<EngineOrderTelegraphBlockEntity> {

    public EngineOrderTelegraphBlock(Properties p_i48402_1_) {
        super(p_i48402_1_);
    }

    @Override
    public InteractionResult use(BlockState state, Level worldIn, BlockPos pos, Player player, InteractionHand handIn, BlockHitResult hit) {
        if (worldIn.isClientSide) return InteractionResult.SUCCESS;

        return onBlockEntityUse(worldIn, pos, be -> {
            boolean sneak = player.isShiftKeyDown();
            be.changeState(sneak);
            return InteractionResult.SUCCESS;
        });
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context) {
        AttachFace face = state.getValue(FACE);
        Direction direction = state.getValue(FACING);
        return face == AttachFace.CEILING ? VSMarinePropulsionShapes.ENGINE_ORDER_TELEGRAPH_CEILING.get(direction.getAxis())
                : face == AttachFace.FLOOR ? VSMarinePropulsionShapes.ENGINE_ORDER_TELEGRAPH_FLOOR.get(direction.getAxis())
                : VSMarinePropulsionShapes.ENGINE_ORDER_TELEGRAPH_WALL.get(direction);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder.add(FACING, FACE));
    }

    @Override
    public Class<EngineOrderTelegraphBlockEntity> getBlockEntityClass() {
        return EngineOrderTelegraphBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends EngineOrderTelegraphBlockEntity> getBlockEntityType() {
        return VSMarinePropulsionBlockEntities.ENGINE_ORDER_TELEGRAPH_BLOCK_ENTITY.get();
    }

    @Override
    public boolean isPathfindable(BlockState state, BlockGetter reader, BlockPos pos, PathComputationType type) {
        return false;
    }
}
