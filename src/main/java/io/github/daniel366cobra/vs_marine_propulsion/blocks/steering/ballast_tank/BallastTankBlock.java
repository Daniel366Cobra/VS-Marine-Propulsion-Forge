package io.github.daniel366cobra.vs_marine_propulsion.blocks.steering.ballast_tank;

import com.simibubi.create.api.connectivity.ConnectivityHandler;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.advancement.AdvancementBehaviour;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.blockEntity.ComparatorUtil;
import io.github.daniel366cobra.vs_marine_propulsion.VSMarinePropulsionEntities;
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
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.util.ForgeSoundType;

import static io.github.daniel366cobra.vs_marine_propulsion.blocks.steering.ballast_tank.BallastTankBlockEntity.EMPTY_TANK_MASS;

//TODO: Check splitting multiblock tank mass!
/**
 *  Credit for the Fluid Tank block code goes to the Create team.
 */
public class BallastTankBlock extends Block implements IWrenchable, IBE<BallastTankBlockEntity> {

    public BallastTankBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(TOP, true)
                .setValue(BOTTOM, true));
    }

    public static final BooleanProperty TOP = BooleanProperty.create("top");
    public static final BooleanProperty BOTTOM = BooleanProperty.create("bottom");

    @Override
    public void setPlacedBy(Level pLevel, BlockPos pPos, BlockState pState, LivingEntity pPlacer, ItemStack pStack) {
        super.setPlacedBy(pLevel, pPos, pState, pPlacer, pStack);
        AdvancementBehaviour.setPlacedBy(pLevel, pPos, pPlacer);
    }

    public static boolean isTank(BlockState state) {
        return state.getBlock() instanceof BallastTankBlock;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TOP, BOTTOM);
    }

    @Override
    public int getLightEmission(BlockState state, BlockGetter world, BlockPos pos) {
        return 0;
    }

    //TODO add rotation
    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        return InteractionResult.SUCCESS;
    }

    @Override
    public VoxelShape getBlockSupportShape(BlockState pState, BlockGetter pReader, BlockPos pPos) {
        return Shapes.block();
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        if (oldState.getBlock() == state.getBlock())
            return;
        if (isMoving)
            return;
        withBlockEntityDo(level, pos, BallastTankBlockEntity::updateConnectivity);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.hasBlockEntity() && (state.getBlock() != newState.getBlock() || !newState.hasBlockEntity())) {

            if (!(level.getBlockEntity(pos) instanceof BallastTankBlockEntity tankBE))
                return;


            if (!level.isClientSide) {
                double oldMass = tankBE.lastCalculatedMass;
                ConnectivityHandler.splitMulti(tankBE);
                VSMarinePropulsionWeights.setMassChanged(level, pos, state, oldMass, EMPTY_TANK_MASS);
            }

            level.removeBlockEntity(pos);

        }
    }

    @Override
    public Class<BallastTankBlockEntity> getBlockEntityClass() {
        return BallastTankBlockEntity.class;
    }

    // Tanks are less noisy when placed in batch
    public static final SoundType SILENCED_METAL =
            new ForgeSoundType(0.1F, 1.5F, () -> SoundEvents.METAL_BREAK, () -> SoundEvents.METAL_STEP,
                    () -> SoundEvents.METAL_PLACE, () -> SoundEvents.METAL_HIT, () -> SoundEvents.METAL_FALL);

    @Override
    public SoundType getSoundType(BlockState state, LevelReader world, BlockPos pos, Entity entity) {
        SoundType soundType = super.getSoundType(state, world, pos, entity);
        if (entity != null && entity.getPersistentData()
                .contains("SilenceTankSound"))
            return SILENCED_METAL;
        return soundType;
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState blockState, Level worldIn, BlockPos pos) {
        return getBlockEntityOptional(worldIn, pos).map(BallastTankBlockEntity::getControllerBE)
                .map(be -> ComparatorUtil.fractionToRedstoneLevel(be.getFillPercentage()))
                .orElse(0);
    }

    @Override
    public BlockEntityType<? extends BallastTankBlockEntity> getBlockEntityType() {
        return VSMarinePropulsionEntities.BALLAST_TANK_BLOCK_ENTITY.get();
    }

}