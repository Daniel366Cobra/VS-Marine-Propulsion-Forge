package io.github.daniel366cobra.vs_marine_propulsion.blocks.control.helm;

import com.simibubi.create.foundation.block.IBE;
import io.github.daniel366cobra.vs_marine_propulsion.VSMarinePropulsionEntities;
import io.github.daniel366cobra.vs_marine_propulsion.VSMarinePropulsionShapes;
import io.github.daniel366cobra.vs_marine_propulsion.ship.VSMarinePropulsionAttachment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

/**
 * My gratitude to Quinton Center (Verquinox), author of Valkyrien Sails, for help and fragments of code
 */
public class HelmBlock extends HorizontalDirectionalBlock implements IBE<HelmBlockEntity> {

    public HelmBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        Direction direction = state.getValue(FACING);
        return VSMarinePropulsionShapes.HELM.get(direction);
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder.add(FACING));
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Level world = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();

        // Default to player's facing direction
        Direction defaultFacing = ctx.getHorizontalDirection();

        // On server side in shipyard, try to auto-rotate to match ship direction
        if (!world.isClientSide && VSGameUtilsKt.isBlockInShipyard(world, pos)) {
            VSMarinePropulsionAttachment shipControl = VSMarinePropulsionAttachment.get(world, pos);
            if (shipControl != null && shipControl.hasValidOrientation()) {
                // Use ship's established forward direction
                return this.defaultBlockState()
                        .setValue(FACING, shipControl.getShipForwardDirection());
            }
        }

        return this.defaultBlockState()
                .setValue(FACING, defaultFacing);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean notify) {
        super.onPlace(state, world, pos, oldState, notify);

        if (world.isClientSide) return;

        if (!VSGameUtilsKt.isBlockInShipyard(world, pos)) return;

        VSMarinePropulsionAttachment shipControl = VSMarinePropulsionAttachment.get(world, pos);
        if (shipControl == null) return;

        Direction helmFacing = state.getValue(HelmBlock.FACING);

        boolean firstHelm = (!shipControl.hasValidOrientation());

        if (firstHelm) {
            // First helm placed - notify player
            Player nearbyPlayer = world.getNearestPlayer(pos.getX(), pos.getY(), pos.getZ(), 10, false);
            if (nearbyPlayer != null) {
                nearbyPlayer.displayClientMessage(
                        Component.translatable("vs_marine_propulsion.helm.set_direction")
                                .append(" " + helmFacing),
                        true
                );
            }
        } else {
            if (helmFacing != shipControl.getShipForwardDirection()) {
                // Invalid facing for additional helm
                Player nearbyPlayer = world.getNearestPlayer(pos.getX(), pos.getY(), pos.getZ(), 10, false);
                if (nearbyPlayer != null) {
                    nearbyPlayer.displayClientMessage(
                            Component.translatable("vs_marine_propulsion.helm.mismatched_facing")
                                    .append(" " + shipControl.getShipForwardDirection()),
                            true
                    );
                }
            }
        }
    }

    @Override
    @SuppressWarnings({"deprecation","UnstableApiUsage"})
    public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean moved) {
        if (state.hasBlockEntity() && !state.is(newState.getBlock())) {
            world.removeBlockEntity(pos);
        }
        super.onRemove(state, world, pos, newState, moved);
    }

    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (world.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        VSMarinePropulsionAttachment shipControl = VSMarinePropulsionAttachment.get(world, pos);
        // Check if this helm is valid before allowing use
        if (shipControl != null) {

            if (shipControl.hasValidOrientation()) {
                Direction requiredFacing = shipControl.getShipForwardDirection();
                Direction actualFacing = state.getValue(HelmBlock.FACING);

                if (actualFacing != requiredFacing) {
                    player.displayClientMessage(
                            Component.translatable("vs_marine_propulsion.helm.mismatched_facing")
                                    .append(" " + requiredFacing),
                            true
                    );
                    return InteractionResult.FAIL;
                }
            }

        } else if (VSGameUtilsKt.isBlockInShipyard(world, pos)) {
            System.out.println("No ship found at helm position!");
            return InteractionResult.FAIL;
        }

        // Helm is valid - proceed with normal use

        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof HelmBlockEntity blockEntity) {
            if (VSGameUtilsKt.isBlockInShipyard(world, pos)) {
                boolean result = blockEntity.sit(player);
                return result ? InteractionResult.CONSUME : InteractionResult.PASS;
            }
        }

        return InteractionResult.PASS;
    }

    public Class<HelmBlockEntity> getBlockEntityClass() {
        return HelmBlockEntity.class;
    }

    public BlockEntityType<? extends HelmBlockEntity> getBlockEntityType() {
        return VSMarinePropulsionEntities.HELM_BLOCK_ENTITY.get();
    }

    @Override
    public @Nullable HelmBlockEntity getBlockEntity(BlockGetter worldIn, BlockPos pos) {
        return IBE.super.getBlockEntity(worldIn, pos);
    }
}