package io.github.daniel366cobra.vs_marine_propulsion;

import io.github.daniel366cobra.vs_marine_propulsion.blocks.attitude.ballast_tank.BallastTankBlockEntity;
import kotlin.Pair;
import kotlin.Triple;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.valkyrienskies.core.apigame.world.chunks.BlockType;
import org.valkyrienskies.mod.common.BlockStateInfo;
import org.valkyrienskies.mod.common.BlockStateInfoProvider;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.physics_api.voxel.Lod1LiquidBlockState;
import org.valkyrienskies.physics_api.voxel.Lod1SolidBlockState;

import java.util.List;

import static io.github.daniel366cobra.vs_marine_propulsion.blocks.attitude.ballast_tank.BallastTankBlockEntity.EMPTY_TANK_MASS;

/** My immense gratitude goes to KindaVibey for allowing me to use their code.
 *  <a href="https://github.com/KindaVibey/Imitari/blob/master/src/main/java/com/vibey/imitari/vs2/VS2CopyBlockIntegrationImpl.java">...</a>
 */
public class VSMarinePropulsionWeights implements BlockStateInfoProvider {

    public static final VSMarinePropulsionWeights INSTANCE = new VSMarinePropulsionWeights();

    private static final ThreadLocal<TankContext> CURRENT_CONTEXT = new ThreadLocal<>();

    @Override
    public @NotNull List<Triple<Integer, Integer, Integer>> getBlockStateData() {
        return List.of();
    }

    @Override
    public @NotNull List<Lod1SolidBlockState> getSolidBlockStates() {
        return List.of();
    }

    @Override
    public @NotNull List<Lod1LiquidBlockState> getLiquidBlockStates() {
        return List.of();
    }

    private record TankContext(Level level, BlockPos pos) {}

    @Override
    public int getPriority() {
        return 200;
    }

    @Override
    public Double getBlockStateMass(BlockState blockState) {

        if (!blockState.is(VSMarinePropulsionBlocks.BALLAST_TANK_VERTICAL.get())
        && !blockState.is(VSMarinePropulsionBlocks.BALLAST_TANK_HORIZONTAL.get())) {
            return null;
        }

        TankContext ctx = CURRENT_CONTEXT.get();
        if (ctx == null) {
            return EMPTY_TANK_MASS;
        }

        BlockEntity be = ctx.level.getBlockEntity(ctx.pos);
        if (be instanceof BallastTankBlockEntity tankBE) {
            return EMPTY_TANK_MASS + tankBE.getDistributedWaterMass();
        }

        return EMPTY_TANK_MASS;
    }

    @Override
    public BlockType getBlockStateType(BlockState blockState) {
        return null;
    }

    private static double getCurrentMass(Level level, BlockPos pos, BlockState state) {
        CURRENT_CONTEXT.set(new TankContext(level, pos));
        try {
            Double mass = INSTANCE.getBlockStateMass(state);
            return mass != null ? mass : EMPTY_TANK_MASS;
        } finally {
            CURRENT_CONTEXT.remove();
        }
    }

    /**
     * Notify VS2 when BlockEntity loads NBT data.
     * At load time, VS2 queried mass before NBT was loaded (empty tank mass).
     * Load actual mass.
     */
    public static void onBallastTankBEDataLoaded(Level level, BlockPos pos, BlockState state) {
        double newMass = getCurrentMass(level, pos, state);
        updateBlockMass(level, pos, state, EMPTY_TANK_MASS, newMass);
    }

    /**
     * Set block mass changed
     * Called from Ballast Tank BE when fluid inside changes
     */
    public static void setMassChanged(Level level, BlockPos pos, BlockState state, double oldMass) {
        double newMass = getCurrentMass(level, pos, state);
        updateBlockMass(level, pos, state, oldMass, newMass);
    }

    /**
     *
     * Remove ballast tank
     * Called from Ballast Tank block on removal
     * Removes EXTRA (water) mass, VS handles removal of empty block mass
     */
    public static void removeBallastTank(Level level, BlockPos pos, BlockState state) {
        double oldMass = getCurrentMass(level, pos, state);
        updateBlockMass(level, pos, state, oldMass, EMPTY_TANK_MASS);
    }

    /**
     * Actually updates block mass, setting it from old to new.
     */
    private static void updateBlockMass(Level level, BlockPos pos, BlockState state, double oldMass, double newMass) {
        if (level.isClientSide || !(level.getBlockEntity(pos) instanceof BallastTankBlockEntity)) return;

        Pair<Double, BlockType> blockInfo = BlockStateInfo.INSTANCE.get(state);
        if (blockInfo == null) return;

        var shipWorld = VSGameUtilsKt.getShipObjectWorld(level);
        if (shipWorld == null) return;

        shipWorld.onSetBlock(
                pos.getX(), pos.getY(), pos.getZ(),
                VSGameUtilsKt.getDimensionId(level),
                blockInfo.getSecond(),
                blockInfo.getSecond(),
                oldMass,
                newMass
        );
    }

    public static void register() {
        Registry.register(
                BlockStateInfo.INSTANCE.getREGISTRY(),
                new ResourceLocation(VSMarinePropulsionMod.MOD_ID, "block_weights"),
                INSTANCE
        );
    }
}