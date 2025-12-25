package io.github.daniel366cobra.vs_marine_propulsion.blocks.auxiliary;

import com.simibubi.create.content.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.joml.Vector3d;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import java.util.List;

public class SeacockBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {

    SmartFluidTankBehaviour internalTank;

    int processingTicks;

    public SeacockBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        processingTicks = 0;
    }

    public void tick() {

        super.tick();
        Ship ship = VSGameUtilsKt.getShipManagingPos(level, getBlockPos());
        if (ship == null) return;

        if (!level.isClientSide && !isVirtual()) {
            if (internalTank.getPrimaryHandler().getFluidAmount() < 500 && isInWater(ship))
                internalTank.getPrimaryHandler().fill(new FluidStack(Fluids.WATER, 500), IFluidHandler.FluidAction.EXECUTE);

            if (processingTicks >= 5) {
                processingTicks = 0;
                notifyUpdate();
            }

        }
    }

    private boolean isInWater(Ship ship) {
        BlockPos blockPos = this.getBlockPos();
        Vector3d shipyardPos = new Vector3d(blockPos.getX(), blockPos.getY(), blockPos.getZ());
        Vector3d worldPos = new Vector3d(ship.getShipToWorld().transformPosition(shipyardPos));
        BlockPos worldBlockPos = BlockPos.containing(worldPos.x, worldPos.y, worldPos.z);
        FluidState fluidState = this.getLevel().getFluidState(worldBlockPos);

        return fluidState.is(Fluids.WATER);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        behaviours.add(internalTank = SmartFluidTankBehaviour.single(this, 1000)
                .allowExtraction()
                .forbidInsertion());
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {

        Direction facing = this.getBlockState().getValue(DirectionalBlock.FACING);

        //Without side == null the capability is not returned
        if ((side == null || side == facing.getOpposite()) && cap == ForgeCapabilities.FLUID_HANDLER)
            return internalTank.getCapability().cast();

        return super.getCapability(cap, side);
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        return containedFluidTooltip(tooltip, isPlayerSneaking, getCapability(ForgeCapabilities.FLUID_HANDLER));
    }
}
