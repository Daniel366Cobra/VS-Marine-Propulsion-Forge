package io.github.daniel366cobra.vs_marine_propulsion.blocks.control.diving_plane_station;

import com.simibubi.create.foundation.block.IBE;
import io.github.daniel366cobra.vs_marine_propulsion.VSMarinePropulsionEntities;
import io.github.daniel366cobra.vs_marine_propulsion.VSMarinePropulsionShapes;
import io.github.daniel366cobra.vs_marine_propulsion.blocks.control.helm.HelmBlockEntity;
import io.github.daniel366cobra.vs_marine_propulsion.ship.VSMarinePropulsionAttachment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

public class DivingPlaneStationBlock extends HorizontalDirectionalBlock implements IBE<DivingPlaneStationBlockEntity> {

    public DivingPlaneStationBlock(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        Direction direction = state.getValue(FACING);
        return VSMarinePropulsionShapes.DIVING_PLANE_STATION.get(direction);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder.add(FACING));
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        // Default to player's facing direction
        Direction defaultFacing = ctx.getHorizontalDirection();

        return this.defaultBlockState()
                .setValue(FACING, defaultFacing);
    }

    public Class<DivingPlaneStationBlockEntity> getBlockEntityClass() {
        return DivingPlaneStationBlockEntity.class;
    }

    public BlockEntityType<? extends DivingPlaneStationBlockEntity> getBlockEntityType() {
        return VSMarinePropulsionEntities.DIVING_PLANE_STATION_BLOCK_ENTITY.get();
    }

    @Override
    public @Nullable DivingPlaneStationBlockEntity getBlockEntity(BlockGetter worldIn, BlockPos pos) {
        return IBE.super.getBlockEntity(worldIn, pos);
    }
}
