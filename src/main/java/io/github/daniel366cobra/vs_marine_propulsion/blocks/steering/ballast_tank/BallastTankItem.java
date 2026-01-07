package io.github.daniel366cobra.vs_marine_propulsion.blocks.steering.ballast_tank;

import com.simibubi.create.api.connectivity.ConnectivityHandler;
import com.simibubi.create.foundation.utility.VecHelper;
import io.github.daniel366cobra.vs_marine_propulsion.VSMarinePropulsionEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fluids.FluidStack;

import static io.github.daniel366cobra.vs_marine_propulsion.blocks.steering.ballast_tank.BallastTankHorizontalBlock.AXIS;

public class BallastTankItem extends BlockItem {

    boolean horizontal;

    public static BallastTankItem horizontal(Block block, Properties properties) {
        return new BallastTankItem(block, properties, true);
    }

    public static BallastTankItem vertical(Block block, Properties properties) {
        return new BallastTankItem(block, properties, false);
    }

    private BallastTankItem(Block block, Properties properties, boolean horizontal) {
        super(block, properties);
        this.horizontal = horizontal;
    }

    @Override
    public InteractionResult place(BlockPlaceContext ctx) {
        InteractionResult initialResult = super.place(ctx);
        if (!initialResult.consumesAction())
            return initialResult;
        tryMultiPlace(ctx);
        return initialResult;
    }

    @Override
    protected boolean updateCustomBlockEntityTag(BlockPos blockPos, Level level, Player player,
                                                 ItemStack stack, BlockState blockState) {
        MinecraftServer minecraftserver = level.getServer();
        if (minecraftserver == null)
            return false;
        CompoundTag nbt = stack.getTagElement("BlockEntityTag");
        if (nbt != null) {
            nbt.remove("Size");
            nbt.remove("Height");
            nbt.remove("Controller");
            nbt.remove("LastKnownPos");
            if (nbt.contains("TankContent")) {
                FluidStack fluid = FluidStack.loadFluidStackFromNBT(nbt.getCompound("TankContent"));
                if (!fluid.isEmpty()) {
                    fluid.setAmount(Math.min(BallastTankBlockEntity.getCapacityMultiplier(), fluid.getAmount()));
                    nbt.put("TankContent", fluid.writeToNBT(new CompoundTag()));
                }
            }
        }
        return super.updateCustomBlockEntityTag(blockPos, level, player, stack, blockState);
    }

    private void tryMultiPlace(BlockPlaceContext ctx) {
        Player player = ctx.getPlayer();
        if (player == null || player.isShiftKeyDown())
            return;

        Direction face = ctx.getClickedFace();

        // Validate face direction based on orientation
        if (horizontal) {
            if (!face.getAxis().isHorizontal()) return;
        } else {
            if (!face.getAxis().isVertical()) return;
        }

        ItemStack stack = ctx.getItemInHand();
        Level world = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        BlockPos placedOnPos = pos.relative(face.getOpposite());
        BlockState placedOnState = world.getBlockState(placedOnPos);

        // Check if placed on a ballast tank
        if (!BallastTankBlockBase.isTank(placedOnState))
            return;

        BlockEntityType<?> beType = horizontal
                ? VSMarinePropulsionEntities.BALLAST_TANK_HORIZONTAL_BLOCK_ENTITY.get()
                : VSMarinePropulsionEntities.BALLAST_TANK_VERTICAL_BLOCK_ENTITY.get();

        BallastTankBlockEntity tankAt = ConnectivityHandler.partAt(beType, world, placedOnPos);

        if (tankAt == null)
            return;

        BallastTankBlockEntity controllerBE = tankAt.getControllerBE();
        if (controllerBE == null)
            return;

        int width = controllerBE.getWidth();
        if (width == 1)
            return;

        // Calculate start position for multi-placement
        BlockPos startPos = calculateStartPos(controllerBE, face, placedOnState);
        if (startPos == null)
            return;

        // Validate start position matches clicked position
        if (!isPositionValid(startPos, pos, face, placedOnState))
            return;

        // Count how many tanks need to be placed
        int tanksToPlace = countTanksToPlace(world, startPos, width, placedOnState, face);
        if (tanksToPlace == 0)
            return;

        if (!player.isCreative() && stack.getCount() < tanksToPlace)
            return;

        // Place all tanks
        placeTanks(ctx, player, world, startPos, width, face, placedOnState);
    }

    private BlockPos calculateStartPos(BallastTankBlockEntity controllerBE, Direction face, BlockState placedOnState) {
        if (horizontal) {
            Direction.Axis axis = placedOnState.getOptionalValue(AXIS).orElse(null);
            if (axis == null || face.getAxis() != axis)
                return null;

            Direction positiveFacing = Direction.fromAxisAndDirection(axis, Direction.AxisDirection.POSITIVE);
            return face == positiveFacing.getOpposite()
                    ? controllerBE.getBlockPos().relative(positiveFacing.getOpposite())
                    : controllerBE.getBlockPos().relative(positiveFacing, controllerBE.getHeight());
        } else {
            return face == Direction.DOWN
                    ? controllerBE.getBlockPos().below()
                    : controllerBE.getBlockPos().above(controllerBE.getHeight());
        }
    }

    private boolean isPositionValid(BlockPos startPos, BlockPos clickedPos, Direction face, BlockState placedOnState) {
        if (horizontal) {
            Direction.Axis axis = placedOnState.getOptionalValue(AXIS).orElse(null);
            if (axis == null) return false;
            return VecHelper.getCoordinate(startPos, axis) == VecHelper.getCoordinate(clickedPos, axis);
        } else {
            // For vertical, Y coordinate must match
            return startPos.getY() == clickedPos.getY();
        }
    }

    private int countTanksToPlace(Level world, BlockPos startPos, int width, BlockState placedOnState, Direction face) {
        int tanksToPlace = 0;

        for (int i = 0; i < width; i++) {
            for (int j = 0; j < width; j++) {
                BlockPos offsetPos = calculateOffsetPos(startPos, i, j, placedOnState, face);
                if (offsetPos == null) continue;

                BlockState blockState = world.getBlockState(offsetPos);

                if (BallastTankBlockBase.isTank(blockState))
                    continue;
                if (!blockState.canBeReplaced())
                    return 0; // Can't place here at all

                tanksToPlace++;
            }
        }

        return tanksToPlace;
    }

    private BlockPos calculateOffsetPos(BlockPos startPos, int i, int j, BlockState placedOnState, Direction face) {
        if (horizontal) {
            Direction.Axis axis = placedOnState.getOptionalValue(AXIS).orElse(Direction.Axis.X);
            if (axis == Direction.Axis.X) {
                // X axis: expand in Y and Z directions
                return startPos.offset(0, i, j);
            } else {
                // Z axis: expand in X and Y directions
                return startPos.offset(i, j, 0);
            }
        } else {
            // Vertical: expand in X and Z directions
            return startPos.offset(i, 0, j);
        }
    }

    private void placeTanks(BlockPlaceContext ctx, Player player, Level world,
                            BlockPos startPos, int width, Direction face, BlockState placedOnState) {

        for (int i = 0; i < width; i++) {
            for (int j = 0; j < width; j++) {
                BlockPos offsetPos = calculateOffsetPos(startPos, i, j, placedOnState, face);
                if (offsetPos == null) continue;

                BlockState blockState = world.getBlockState(offsetPos);

                if (BallastTankBlockBase.isTank(blockState))
                    continue;

                BlockPlaceContext context = BlockPlaceContext.at(ctx, offsetPos, face);
                player.getPersistentData().putBoolean("SilenceTankSound", true);
                super.place(context);
                player.getPersistentData().remove("SilenceTankSound");
            }
        }
    }

}