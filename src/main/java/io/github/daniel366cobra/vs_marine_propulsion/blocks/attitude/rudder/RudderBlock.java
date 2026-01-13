package io.github.daniel366cobra.vs_marine_propulsion.blocks.attitude.rudder;

import com.simibubi.create.foundation.placement.IPlacementHelper;
import com.simibubi.create.foundation.placement.PlacementHelpers;
import com.simibubi.create.foundation.placement.PlacementOffset;
import com.simibubi.create.foundation.utility.Iterate;
import io.github.daniel366cobra.vs_marine_propulsion.VSMarinePropulsionBlocks;
import io.github.daniel366cobra.vs_marine_propulsion.VSMarinePropulsionShapes;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;
import java.util.function.Predicate;

public class RudderBlock extends RotatedPillarBlock {

    private static final int placementHelperId = PlacementHelpers.register(new PlacementHelper());

    public RudderBlock(Properties pProperties) {
        super(pProperties);
    }

    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        Axis axis = state.getValue(RudderBlock.AXIS);
        return VSMarinePropulsionShapes.RUDDER.get(axis);
    }

    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Axis preferredAxis = getPreferredAxis(context);
        if (preferredAxis != null && (context.getPlayer() == null || !context.getPlayer().isShiftKeyDown())) {
            return this.defaultBlockState()
                    .setValue(AXIS, preferredAxis);
        } else {
            return this.defaultBlockState()
                    .setValue(AXIS, preferredAxis != null && context.getPlayer()
                            .isShiftKeyDown() ? context.getClickedFace()
                            .getAxis()
                            : context.getNearestLookingDirection()
                            .getAxis());
        }
    }

    public static Axis getPreferredAxis(BlockPlaceContext context) {
        Axis preferredAxis = null;

        for (Direction side : Iterate.directions) {
            BlockState blockState = context.getLevel()
                    .getBlockState(context.getClickedPos()
                            .relative(side));
            if (blockState.getBlock() instanceof RudderBlock) {
                if (preferredAxis != null && preferredAxis != side.getAxis()) {
                    preferredAxis = null;
                    break; //found conflicting axes so we drop the preferred axis to null and break
                } else {
                    preferredAxis = side.getAxis(); //set the preferred axis to be the same as surrounding blocks
                }
            }
        }
        return preferredAxis;
    }

    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult ray) {

        ItemStack heldItem = player.getItemInHand(hand);

        IPlacementHelper placementHelper = PlacementHelpers.get(placementHelperId);
        if (!player.isShiftKeyDown() && player.mayBuild()) {
            if (placementHelper.matchesItem(heldItem)) {
                placementHelper.getOffset(player, world, state, pos, ray)
                        .placeInWorld(world, (BlockItem) heldItem.getItem(), player, hand, ray);
                return InteractionResult.SUCCESS;
            }
        }


        return InteractionResult.PASS;
    }

    @MethodsReturnNonnullByDefault
    private static class PlacementHelper implements IPlacementHelper {
        @Override
        public Predicate<ItemStack> getItemPredicate() {
            return VSMarinePropulsionBlocks.RUDDER::isIn;
        }

        @Override
        public Predicate<BlockState> getStatePredicate() {
            return s -> s.getBlock() instanceof RudderBlock;
        }

        @Override
        public PlacementOffset getOffset(Player player, Level world, BlockState state, BlockPos pos, BlockHitResult ray) {

            Direction.Axis currentAxis = state.getValue(AXIS);
            Direction clickedFace = ray.getDirection();

            //clicked the 16x16 face
            if (currentAxis == clickedFace.getAxis()) {
                List<Direction> directions = IPlacementHelper.orderedByDistanceExceptAxis(
                        pos, ray.getLocation(), currentAxis,
                        dir -> world.getBlockState(pos.relative(dir)).canBeReplaced());

                if (directions.isEmpty())
                    return PlacementOffset.fail();
                else
                    return PlacementOffset.success(pos.relative(directions.get(0)),
                            s -> s.setValue(AXIS, currentAxis));
            } else { //clicked the 16x4 face
                return PlacementOffset.success(pos.relative(clickedFace),
                        s -> s.setValue(AXIS, currentAxis));
            }


        }
    }
}
