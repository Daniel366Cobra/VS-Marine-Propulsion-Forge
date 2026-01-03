package io.github.daniel366cobra.vs_marine_propulsion;

import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
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

/** My immense gratitude goes to KindaVibey for allowing me to use their code.
 *  <a href="https://github.com/KindaVibey/Imitari/blob/master/src/main/java/com/vibey/imitari/vs2/VS2CopyBlockIntegrationImpl.java">...</a>
 */
public class VSMarinePropulsionWeights implements BlockStateInfoProvider {

    public static final VSMarinePropulsionWeights INSTANCE = new VSMarinePropulsionWeights();

    // Physics constants
    public static final double EMPTY_TANK_MASS = 500.0; // kg - empty tank structure
    public static final double WATER_DENSITY = 1000.0;  // kg/m³ - real water density
    public static final double MAX_TANK_VOLUME = 27.0;  // m³ - for 3x3x3 Create tank

    // ThreadLocal context for accessing Level and BlockPos
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
        return 200; // High priority to override defaults
    }

    @Override
    public Double getBlockStateMass(BlockState blockState) {
        // Only handle our ballast tank blocks
        if (blockState.getBlock() != VSMarinePropulsionBlocks.BALLAST_TANK.get()) {
            return null;
        }

        // Get context if available (set when we call this ourselves)
        TankContext ctx = CURRENT_CONTEXT.get();
        if (ctx == null) {
            // VS2 calling without context - return safe empty mass
            return EMPTY_TANK_MASS;
        }

        // Access the actual tank BlockEntity
        BlockEntity be = ctx.level.getBlockEntity(ctx.pos);
        if (be instanceof FluidTankBlockEntity tank) {
            double fillPercentage = calculateFillPercentage(tank);
            double waterMass = calculateWaterMass(fillPercentage);
            return EMPTY_TANK_MASS + waterMass;
        }

        // Block exists but no BlockEntity (shouldn't happen) - return empty
        return EMPTY_TANK_MASS;
    }

    @Override
    public BlockType getBlockStateType(BlockState blockState) {
        return null; // Use default block type
    }

    // ==================== CORE TANK LOGIC ====================

    /**
     * Calculate fill percentage from Create's FluidTankBlockEntity
     */
    private double calculateFillPercentage(FluidTankBlockEntity tank) {
        // Check if tank has fluid
        if (tank.getTankInventory().getFluid().isEmpty()) {
            return 0.0;
        }

        // Get amount and capacity
        long currentAmount = tank.getTankInventory().getFluid().getAmount();
        long capacity = tank.getTankInventory().getCapacity();

        // Safety check
        if (capacity <= 0) return 0.0;

        // Calculate percentage (0.0 to 1.0)
        return Math.min(1.0, (double) currentAmount / capacity);
    }

    /**
     * Calculate water mass based on fill percentage
     */
    private double calculateWaterMass(double fillPercentage) {
        double waterVolume = MAX_TANK_VOLUME * fillPercentage; // m³
        return waterVolume * WATER_DENSITY; // kg
    }

    // ==================== CONTEXT HELPERS ====================

    /**
     * Calculate current mass with proper context
     * Call this when you need accurate mass (e.g., on fluid change)
     */
    public static double getCurrentMass(Level level, BlockPos pos, BlockState state) {
        // Set context for this call
        CURRENT_CONTEXT.set(new TankContext(level, pos));
        try {
            Double mass = INSTANCE.getBlockStateMass(state);
            return mass != null ? mass : EMPTY_TANK_MASS;
        } finally {
            // Always clean up!
            CURRENT_CONTEXT.remove();
        }
    }

    /**
     * Notify VS2 that tank mass has changed
     * Call this from your BlockEntity when fluid changes
     */
    public static void notifyMassChanged(Level level, BlockPos pos, BlockState state,
                                         double oldMass, double newMass) {
        if (level.isClientSide) return;

        // Only notify if mass changed significantly (> 1kg)
        if (Math.abs(oldMass - newMass) < 1.0) return;

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

    /**
     * Special notification for block removal
     * Correctly subtracts full mass (water + structure)
     */
    public static void notifyBlockRemoved(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide) return;

        // Calculate what the mass WAS before removal
        double oldMass = getCurrentMass(level, pos, state);
        double newMass = 0.0; // Block is gone

        notifyMassChanged(level, pos, state, oldMass, newMass);
    }

    // ==================== REGISTRATION ====================

    public static void register() {
        Registry.register(
                BlockStateInfo.INSTANCE.getREGISTRY(),
                new ResourceLocation(VSMarinePropulsionMod.MOD_ID, "block_weights"),
                INSTANCE
        );
    }
}