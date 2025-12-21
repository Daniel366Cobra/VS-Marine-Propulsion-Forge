package io.github.daniel366cobra.vs_marine_propulsion.blocks.control.helm;

import com.simibubi.create.foundation.block.IBE;
import io.github.daniel366cobra.vs_marine_propulsion.VSMarinePropulsionEntities;
import io.github.daniel366cobra.vs_marine_propulsion.VSMarinePropulsionShapes;
import io.github.daniel366cobra.vs_marine_propulsion.ship.VSMarinePropulsionAttachment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
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
import org.valkyrienskies.core.api.ships.LoadedServerShip;
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

    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return this.defaultBlockState()
                .setValue(FACING, ctx.getHorizontalDirection());
    }

    @SuppressWarnings("deprecation")
    public void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean notify) {
        if (world.isClientSide) {
            return;
        }

        if (VSGameUtilsKt.isBlockInShipyard(world, pos)) {
            LoadedServerShip ship = VSGameUtilsKt.getShipObjectManagingPos((ServerLevel) world, pos);
            if (ship != null) {
                VSMarinePropulsionAttachment shipControl = VSMarinePropulsionAttachment.getOrCreate(ship);
                Direction helmFacing = state.getValue(HelmBlock.FACING);

                boolean success = shipControl.addHelm(helmFacing, pos);

                if (!success) {
                    // Invalid facing for additional helm
                    Player nearbyPlayer = world.getNearestPlayer(pos.getX(), pos.getY(), pos.getZ(), 10, false);
                    if (nearbyPlayer != null) {
                        nearbyPlayer.displayClientMessage(
                                Component.translatable("vs_marine_propulsion.helm.mismatched_facing")
                                        .append(" " + shipControl.getShipForwardDirection()),
                                true
                        );
                    }
                } else if (shipControl.getTotalHelms() == 1) {
                    // First helm placed - notify player
                    Player nearbyPlayer = world.getNearestPlayer(pos.getX(), pos.getY(), pos.getZ(), 10, false);
                    if (nearbyPlayer != null) {
                        nearbyPlayer.displayClientMessage(
                                Component.translatable("vs_marine_propulsion.helm.set_direction")
                                        .append(" " + helmFacing),
                                true
                        );
                    }
                }
            }
        }
    }


    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (world.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        // Check if this helm is valid before allowing use
        if (VSGameUtilsKt.isBlockInShipyard(world, pos)) {
            LoadedServerShip ship = VSGameUtilsKt.getShipObjectManagingPos((ServerLevel) world, pos);
            if (ship == null) {
                System.out.println("No ship found at helm position!");
                return InteractionResult.FAIL;
            }

            VSMarinePropulsionAttachment shipControl = ship.getAttachment(VSMarinePropulsionAttachment.class);
            if (shipControl != null && shipControl.hasValidOrientation()) {
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

        }

        // Helm is valid - proceed with normal use

        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof HelmBlockEntity blockEntity) {
            if (VSGameUtilsKt.isBlockInShipyard(world, pos)) {
                boolean result = blockEntity.sit(player);
                return result ? InteractionResult.CONSUME : InteractionResult.PASS;
            } else {
                // For testing outside of ship
                if (player.isShiftKeyDown()) {
                    blockEntity.rotateWheelLeft(state, (ServerLevel)world, pos);
                } else {
                    blockEntity.rotateWheelRight(state, (ServerLevel)world, pos);
                }
                player.displayClientMessage(Component.literal("Angle: "+ blockEntity.wheelAngle), true);
            }
            return InteractionResult.FAIL;
        }

        return InteractionResult.PASS;
    }

    @SuppressWarnings({"deprecation","UnstableApiUsage"})
    public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean moved) {
        if (!world.isClientSide) {
            if (VSGameUtilsKt.isBlockInShipyard(world, pos)) {
                LoadedServerShip ship = VSGameUtilsKt.getShipObjectManagingPos((ServerLevel) world, pos);
                if (ship != null) {
                    VSMarinePropulsionAttachment shipControl = ship.getAttachment(VSMarinePropulsionAttachment.class);
                    if (shipControl != null) {
                        shipControl.removeHelm(pos);
                    }
                }
            }
        }

        if (state.hasBlockEntity() && !state.is(newState.getBlock())) {
            world.removeBlockEntity(pos);
        }
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
