package io.github.daniel366cobra.vs_marine_propulsion.blocks.steering.ballast_tank;

import com.simibubi.create.api.connectivity.ConnectivityHandler;
import com.simibubi.create.content.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.IMultiBlockEntityContainer;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.fluid.SmartFluidTank;
import com.simibubi.create.infrastructure.config.AllConfigs;
import io.github.daniel366cobra.vs_marine_propulsion.VSMarinePropulsionWeights;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidTank;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;

import static io.github.daniel366cobra.vs_marine_propulsion.blocks.steering.ballast_tank.BallastTankHorizontalBlock.*;
import static io.github.daniel366cobra.vs_marine_propulsion.blocks.steering.ballast_tank.BallastTankVerticalBlock.BOTTOM;
import static io.github.daniel366cobra.vs_marine_propulsion.blocks.steering.ballast_tank.BallastTankVerticalBlock.TOP;

public class BallastTankBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation, IMultiBlockEntityContainer.Fluid {

    private final boolean horizontal;

    private static final int MAX_SIZE = 3;

    protected LazyOptional<IFluidHandler> fluidCapability;

    protected FluidTank tankInventory;
    protected BlockPos controller;
    protected BlockPos lastKnownPos;
    protected boolean updateConnectivity;
    protected boolean updateCapability;
    protected int width;
    protected int height;

    private static final int SYNC_RATE = 8;
    protected int syncCooldown;
    protected boolean queuedSync;

    public double lastCalculatedMass = 0;

    public static final double WATER_DENSITY = 1000.0;
    public static final double EMPTY_TANK_MASS = 500.0;

    public static BallastTankBlockEntity horizontal(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        return new BallastTankBlockEntity(type, pos, state, true);
    }

    public static BallastTankBlockEntity vertical(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        return new BallastTankBlockEntity(type, pos, state, false);
    }

    public BallastTankBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, boolean horizontal) {
        super(type, pos, state);

        this.horizontal = horizontal;

        tankInventory = createInventory();
        fluidCapability = LazyOptional.of(() -> tankInventory);
        updateConnectivity = false;
        updateCapability = false;
        height = 1;
        width = 1;
        refreshCapability();
        lastCalculatedMass = EMPTY_TANK_MASS;
    }

    protected SmartFluidTank createInventory() {
        return new SmartFluidTank(getCapacityMultiplier(), this::onFluidStackChanged);
    }

    protected void updateConnectivity() {
        updateConnectivity = false;
        if (level.isClientSide)
            return;
        if (!isController())
            return;
        ConnectivityHandler.formMulti(this);
    }

    @Override
    public void tick() {

        super.tick();
        if (syncCooldown > 0) {
            syncCooldown--;
            if (syncCooldown == 0 && queuedSync)
                sendData();
        }

        if (lastKnownPos == null)
            lastKnownPos = getBlockPos();
        else if (!lastKnownPos.equals(worldPosition) && worldPosition != null) {
            onPositionChanged();
            return;
        }

        if (updateCapability) {
            updateCapability = false;
            refreshCapability();
        }
        if (updateConnectivity)
            updateConnectivity();

        //Non-controller blocks periodically update own mass
        if (!level.isClientSide && level.getGameTime() % 5 == 0) {
            updateMass();
            setChanged();
        }
    }

    @Override
    public BlockPos getLastKnownPos() {
        return lastKnownPos;
    }

    public Direction.Axis getAxis() {
        if (horizontal) return getBlockState().getValue(AXIS);
        else return Direction.Axis.Y;
    }

    @Override
    public boolean isController() {
        return controller == null || worldPosition.getX() == controller.getX()
                && worldPosition.getY() == controller.getY() && worldPosition.getZ() == controller.getZ();
    }

    @Override
    public BallastTankBlockEntity getControllerBE() {
        if (isController() || !hasLevel())
            return this;
        BlockEntity blockEntity = level.getBlockEntity(controller);
        if (blockEntity instanceof BallastTankBlockEntity)
            return (BallastTankBlockEntity) blockEntity;
        return null;
    }

    @Override
    public BlockPos getController() {
        return isController() ? worldPosition : controller;
    }

    @Override
    public void setController(BlockPos controller) {
        if (level.isClientSide && !isVirtual())
            return;
        if (controller.equals(this.controller))
            return;
        this.controller = controller;
        refreshCapability();
        setChanged();
        sendData();
    }

    public void removeController(boolean keepFluids) {
        if (level.isClientSide)
            return;
        updateConnectivity = true;
        if (!keepFluids)
            applyFluidTankSize(1);
        controller = null;
        width = 1;
        height = 1;

        onFluidStackChanged(tankInventory.getFluid());

        BlockState state = getBlockState();
        if (BallastTankBlockBase.isTank(state)) {
            if (horizontal) {
                state = state.setValue(POSITIVE, true);
                state = state.setValue(NEGATIVE, true);
                getLevel().setBlock(worldPosition, state, 22);
            } else {
                state = state.setValue(BOTTOM, true);
                state = state.setValue(TOP, true);
                getLevel().setBlock(worldPosition, state, 22);
            }
        }

        refreshCapability();
        setChanged();
        sendData();
    }

    @Override
    public void initialize() {
        super.initialize();
        sendData();
        if (level.isClientSide)
            invalidateRenderBoundingBox();
    }

    private void onPositionChanged() {
        removeController(true);
        lastKnownPos = worldPosition;
    }

    protected void onFluidStackChanged(FluidStack newFluidStack) {
        if (!hasLevel() || level.isClientSide)
            return;

        //Controller updates own mass instantly upon receiving fluid
        if (isController()) {
            updateMass();
        }

        setChanged();
        sendData();
    }

    public void applyFluidTankSize(int blocks) {
        tankInventory.setCapacity(blocks * getCapacityMultiplier());
        int overflow = tankInventory.getFluidAmount() - tankInventory.getCapacity();
        if (overflow > 0)
            tankInventory.drain(overflow, IFluidHandler.FluidAction.EXECUTE);
    }

    @Override
    public void sendData() {
        if (syncCooldown > 0) {
            queuedSync = true;
            return;
        }
        super.sendData();
        queuedSync = false;
        syncCooldown = SYNC_RATE;
    }

    private void refreshCapability() {
        LazyOptional<IFluidHandler> oldCap = fluidCapability;
        fluidCapability = LazyOptional.of(this::handlerForCapability);
        oldCap.invalidate();
    }

    private IFluidHandler handlerForCapability() {
        return isController()? tankInventory
                : getControllerBE() != null ? getControllerBE().handlerForCapability() : new FluidTank(0);
    }

    @Override
    protected AABB createRenderBoundingBox() {
        if (isController())
            if (horizontal) {
                Direction.Axis axis = getAxis();
                return super.createRenderBoundingBox().expandTowards(
                        axis == Direction.Axis.X ? (height - 1) : (width - 1),
                        width - 1,
                        axis == Direction.Axis.Z ? (height - 1) : (width - 1));
            } else {
                return super.createRenderBoundingBox().expandTowards(width - 1, height - 1, width - 1);
            }
        else
            return super.createRenderBoundingBox();
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        BallastTankBlockEntity controllerBE = getControllerBE();
        if (controllerBE == null)
            return false;

        return containedFluidTooltip(tooltip, isPlayerSneaking,
                controllerBE.getCapability(ForgeCapabilities.FLUID_HANDLER));
    }

    @Override
    protected void read(CompoundTag compound, boolean clientPacket) {
        super.read(compound, clientPacket);

        lastCalculatedMass = compound.getDouble("LastMass");

        if (this.level != null && !this.level.isClientSide)
            VSMarinePropulsionWeights.onBallastTankBEDataLoaded(this.level, this.getBlockPos(), this.getBlockState());

        BlockPos controllerBefore = controller;
        int prevSize = width;
        int prevHeight = height;

        updateConnectivity = compound.contains("Uninitialized");
        controller = null;
        lastKnownPos = null;

        if (compound.contains("LastKnownPos"))
            lastKnownPos = NbtUtils.readBlockPos(compound.getCompound("LastKnownPos"));
        if (compound.contains("Controller"))
            controller = NbtUtils.readBlockPos(compound.getCompound("Controller"));

        if (isController()) {
            width = compound.getInt("Size");
            height = compound.getInt("Height");
            tankInventory.setCapacity(getTotalTankSize() * getCapacityMultiplier());
            tankInventory.readFromNBT(compound.getCompound("TankContent"));
            if (tankInventory.getSpace() < 0)
                tankInventory.drain(-tankInventory.getSpace(), IFluidHandler.FluidAction.EXECUTE);
        }

        updateCapability = true;

        if (!clientPacket)
            return;

        boolean changeOfController = !Objects.equals(controllerBefore, controller);
        if (changeOfController || prevSize != width || prevHeight != height) {
            if (hasLevel())
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 16);
            if (isController())
                tankInventory.setCapacity(getCapacityMultiplier() * getTotalTankSize());
            invalidateRenderBoundingBox();
        }

    }

    @Override
    public void write(CompoundTag compound, boolean clientPacket) {
        if (updateConnectivity)
            compound.putBoolean("Uninitialized", true);

        if (lastKnownPos != null)
            compound.put("LastKnownPos", NbtUtils.writeBlockPos(lastKnownPos));
        if (!isController())
            compound.put("Controller", NbtUtils.writeBlockPos(controller));
        if (isController()) {
            compound.put("TankContent", tankInventory.writeToNBT(new CompoundTag()));
            compound.putInt("Size", width);
            compound.putInt("Height", height);
        }
        compound.putDouble("LastMass", lastCalculatedMass);
        super.write(compound, clientPacket);

    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (!fluidCapability.isPresent())
            refreshCapability();
        if (cap == ForgeCapabilities.FLUID_HANDLER)
            return fluidCapability.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidate() {
        super.invalidate();
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {

    }

    public int getTotalTankSize() {
        return width * width * height;
    }

    public static int getMaxSize() {
        return MAX_SIZE;
    }

    public static int getCapacityMultiplier() {
        return AllConfigs.server().fluids.fluidTankCapacity.get() * 1000;
    }

    public static int getMaxHeight() {
        return AllConfigs.server().fluids.fluidTankMaxHeight.get();
    }

    @Override
    public void preventConnectivityUpdate() {
        updateConnectivity = false;
    }

    @Override
    public void notifyMultiUpdated() {
        BlockState state = this.getBlockState();
        if (BallastTankBlockBase.isTank(state)) { // safety
            if (horizontal) {
                Direction.Axis axis = getAxis();
                state = state.setValue(NEGATIVE, axis == Direction.Axis.X
                        ? getController().getX() == getBlockPos().getX()
                        : getController().getZ() == getBlockPos().getZ());
                state = state.setValue(POSITIVE, axis == Direction.Axis.X
                        ? getController().getX() + height - 1 == getBlockPos().getX()
                        : getController().getZ() + height - 1 == getBlockPos().getZ());
                level.setBlock(getBlockPos(), state, 6);
            } else {
                state = state.setValue(BOTTOM, getController().getY() == getBlockPos().getY());
                state = state.setValue(TOP, getController().getY() + height - 1 == getBlockPos().getY());
                level.setBlock(getBlockPos(), state, 6);
            }
        }
        onFluidStackChanged(tankInventory.getFluid());
        setChanged();
    }

    @Override
    public Direction.Axis getMainConnectionAxis() {
        if (horizontal) return getAxis();
        else return Direction.Axis.Y;
    }

    @Override
    public int getMaxLength(Direction.Axis longAxis, int width) {
        if (longAxis == Direction.Axis.Y) {
            if (horizontal) {
                return getMaxHeight();
            } else return getMaxWidth();
        } else {
            if (horizontal) {
                return getMaxWidth();
            } else return getMaxHeight();
        }
    }

    @Override
    public int getMaxWidth() {
        return MAX_SIZE;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public void setHeight(int height) {
        this.height = height;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public void setWidth(int width) {
        this.width = width;
    }

    @Override
    public boolean hasTank() {
        return true;
    }

    @Override
    public int getTankSize(int tank) {
        return getCapacityMultiplier();
    }

    @Override
    public void setTankSize(int tank, int blocks) {
        applyFluidTankSize(blocks);
    }

    @Override
    public IFluidTank getTank(int tank) {
        return tankInventory;
    }

    @Override
    public FluidStack getFluid(int tank) {
        return tankInventory.getFluid().copy();
    }

    public double getFillPercentage() {
        if (tankInventory.getFluid().isEmpty()) return 0.0;
        long amount = tankInventory.getFluid().getAmount();
        long capacity = tankInventory.getCapacity();
        return capacity > 0 ? Math.min(1.0, (double) amount / capacity) : 0.0;
    }

    private void updateMass() {
        double newMass = EMPTY_TANK_MASS + getDistributedWaterMass();

        if (Math.abs(newMass - lastCalculatedMass) > 0.001) {
            VSMarinePropulsionWeights.setMassChanged(
                    level, worldPosition, getBlockState(),
                    lastCalculatedMass
            );
            lastCalculatedMass = newMass;
        }
    }

    public double getDistributedWaterMass() {
        double distributedMass;

        //Controller calculates for itself
        if (isController()) {
            distributedMass =  calculateCurrentWaterMass() / getTotalTankSize();
            return distributedMass;
        }

        //Non-controllers get from controller
        BallastTankBlockEntity controller = getControllerBE();
        if (controller != null) {
            distributedMass = controller.calculateCurrentWaterMass() / controller.getTotalTankSize();
            return distributedMass;
        }

        // Fallback for broken multi-blocks
        return calculateCurrentWaterMass();
    }

    public double calculateCurrentWaterMass() {
        double fluidAmount = tankInventory.getFluid().getAmount() / 1000.0; // mB -> buckets (= cu. m.)
        return fluidAmount * WATER_DENSITY;
    }
}
