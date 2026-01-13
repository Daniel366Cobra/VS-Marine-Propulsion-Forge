package io.github.daniel366cobra.vs_marine_propulsion.blocks.attitude.ballast_tank;

import com.simibubi.create.api.connectivity.ConnectivityHandler;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.advancement.AdvancementBehaviour;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.blockEntity.ComparatorUtil;
import io.github.daniel366cobra.vs_marine_propulsion.VSMarinePropulsionWeights;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.util.ForgeSoundType;

public abstract class BallastTankBlockBase extends Block implements IWrenchable, IBE<BallastTankBlockEntity> {

    public static final SoundType SILENCED_METAL = new ForgeSoundType(
            0.1F, 1.5F,
            () -> SoundEvents.METAL_BREAK,
            () -> SoundEvents.METAL_STEP,
            () -> SoundEvents.METAL_PLACE,
            () -> SoundEvents.METAL_HIT,
            () -> SoundEvents.METAL_FALL
    );

    protected BallastTankBlockBase(Properties properties) {
        super(properties);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        AdvancementBehaviour.setPlacedBy(level, pos, placer);
    }

    @Override
    public void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean moved) {
        if (oldState.getBlock() == state.getBlock() || moved) {
            return;
        }
        withBlockEntityDo(world, pos, BallastTankBlockEntity::updateConnectivity);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.hasBlockEntity() &&
                (state.getBlock() != newState.getBlock() || !newState.hasBlockEntity())) {

            if (!(level.getBlockEntity(pos) instanceof BallastTankBlockEntity tankBE)) {
                return;
            }

            VSMarinePropulsionWeights.removeBallastTank(level, pos, state);

            level.removeBlockEntity(pos);
            ConnectivityHandler.splitMulti(tankBE);
        }
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        return InteractionResult.SUCCESS;
    }

    @Override
    public VoxelShape getBlockSupportShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.block();
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return getBlockEntityOptional(level, pos)
                .map(BallastTankBlockEntity::getControllerBE)
                .map(be -> ComparatorUtil.fractionToRedstoneLevel(be.getFillPercentage()))
                .orElse(0);
    }

    @Override
    public SoundType getSoundType(BlockState state, LevelReader level, BlockPos pos, Entity entity) {
        SoundType soundType = super.getSoundType(state, level, pos, entity);
        if (entity != null && entity.getPersistentData().contains("SilenceTankSound")) {
            return SILENCED_METAL;
        }
        return soundType;
    }

    @Override
    public Class<BallastTankBlockEntity> getBlockEntityClass() {
        return BallastTankBlockEntity.class;
    }

    // Abstract methods
    public abstract boolean isHorizontal();
    public abstract BlockEntityType<? extends BallastTankBlockEntity> getBlockEntityType();

    public static boolean isTank(BlockState state) {
        return state.getBlock() instanceof BallastTankBlockBase;
    }

    public static boolean isHorizontalTank(BlockState state) {
        return state.getBlock() instanceof BallastTankHorizontalBlock;
    }

    public static boolean isVerticalTank(BlockState state) {
        return state.getBlock() instanceof BallastTankVerticalBlock;
    }
}
