package io.github.daniel366cobra.vs_marine_propulsion.blocks.steering.utility;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.antlr.v4.runtime.misc.NotNull;
import org.joml.Vector3d;
import org.valkyrienskies.core.api.ships.PhysShip;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.api.ships.ShipForcesInducer;
import org.valkyrienskies.core.api.ships.properties.ShipTransform;
import org.valkyrienskies.core.impl.game.ships.PhysShipImpl;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ControlSurfaceForcesApplier implements ShipForcesInducer {

    private String dimensionId = null;

    public Map<BlockPos, ControlSurfaceData> controlSurfaces = new ConcurrentHashMap<>();

    public ControlSurfaceForcesApplier() {}

    public ControlSurfaceForcesApplier(String dimensionId) {
        this.dimensionId = dimensionId;
    }

    public static ControlSurfaceForcesApplier getOrCreate(ServerShip ship, String dimensionId) {
        ControlSurfaceForcesApplier shipControl = ship.getAttachment(ControlSurfaceForcesApplier.class);
        if (shipControl == null) {
            shipControl = new ControlSurfaceForcesApplier(dimensionId);
            ship.saveAttachment(ControlSurfaceForcesApplier.class, shipControl);
        }
        return shipControl;
    }

    public static ControlSurfaceForcesApplier getOrCreate(ServerShip ship) {
        return  getOrCreate(ship, ship.getChunkClaimDimension());
    }

    public static ControlSurfaceForcesApplier get(Level level, BlockPos pos) {
        ServerLevel serverLevel = (ServerLevel) level;
        ServerShip ship = VSGameUtilsKt.getShipObjectManagingPos(serverLevel, pos);
        if (ship == null) {
            ship = VSGameUtilsKt.getShipManagingPos(serverLevel, pos);
        }

        return ship != null ? getOrCreate(ship) : null;
    }

    public void addControlSurface(BlockPos pos, ControlSurfaceData data) {
        controlSurfaces.put(pos, data);
    }
    public void removeControlSurface(BlockPos pos) {
        controlSurfaces.remove(pos);
    }

    @Nullable
    public ControlSurfaceData getControlSurfaceAtPos(BlockPos pos) {
        return controlSurfaces.get(pos);
    }

    @Override
    public void applyForces(@NotNull PhysShip physicsShip) {
        PhysShipImpl physShip = (PhysShipImpl) physicsShip;
        final ShipTransform transform = physShip.getTransform();

        controlSurfaces.forEach((pos, data) -> {
            Vector3d normalDir = data.normalDirection;
            Vector3d axisDir = data.axisDirection;
            float angleDegrees = data.angle; // Your clamped -40 to 40 degrees
            float submergedPercentage = data.submergedPercentage;
            int rudderBlocks = data.rudderBlocks;

            if (submergedPercentage < 0.05f) return;

            // Calculate position relative to ship's center of mass in ship coordinates
            Vector3d thrustPos = VectorConversionsMCKt.toJOMLD(pos)
                    .add(0.5, 0.5, 0.5, new Vector3d())
                    .sub(transform.getPositionInShip());

            // Convert angle to radians for calculations
            double angleRadians = Math.toRadians(angleDegrees);

            // 1. Calculate the effective control surface normal after rotation
            Vector3d effectiveNormal = new Vector3d(normalDir);
            effectiveNormal.rotateAxis(angleRadians, axisDir.x, axisDir.y, axisDir.z);

            // 2. Get ship velocity at this control surface position
            Vector3d shipVelocity = new Vector3d(physShip.getPoseVel().getVel());
            Vector3d waterFlow = new Vector3d(shipVelocity).negate();

            double waterFlowMagnitude = waterFlow.length();

            // Skip if ship is barely moving
            if (waterFlowMagnitude < 0.1) return;

            // 3. Calculate the TRUE angle of attack
            // This is the angle between water flow and the DEFLECTED rudder surface
            Vector3d waterFlowNormalized = new Vector3d(waterFlow).normalize();
            double angleOfAttack = angleRadians; // For symmetric foils, AoA ≈ deflection angle

            // Adjust AoA based on actual geometric relationship
            double dot = waterFlowNormalized.dot(effectiveNormal);
            angleOfAttack = Math.asin(dot); // More accurate AoA calculation

            // 4. Calculate lift direction based on AXIS, not normal
            Vector3d liftDirection = new Vector3d();
            axisDir.cross(waterFlowNormalized, liftDirection);
            liftDirection.normalize();

            // 5. Apply the correct sign to the lift direction
            double liftMagnitude = calculateLiftForce(Math.abs(angleOfAttack), waterFlowMagnitude, rudderBlocks, submergedPercentage);

            // For symmetric foils, lift direction depends on deflection angle sign
            if (angleDegrees < 0) {
                liftDirection.negate();
            }

            // 6. Apply the force
            if (liftMagnitude > 0.001) {
                Vector3d liftForce = new Vector3d(liftDirection).mul(liftMagnitude);
                physShip.applyInvariantForceToPos(liftForce, thrustPos);

                // DEBUG: Remove after testing
                System.out.println("Rudder: angle=" + angleDegrees + "°, AoA=" + Math.toDegrees(angleOfAttack) +
                        "°, flow=" + waterFlowMagnitude + ", force=" + liftMagnitude);
            }
        });
    }

    private double calculateLiftForce(double angleOfAttack, double flowSpeed, int rudderBlocks, float submergedPercentage) {
        // Improved lift formula that scales with rudder size
        double density = 1000.0; // Water density kg/m³
        double baseArea = 1.0; // Area per rudder block
        double totalArea = baseArea * rudderBlocks;

        // Lift coefficient for symmetric foils
        double liftCoefficient = 2 * Math.PI * Math.sin(angleOfAttack); // More accurate

        // Stall behavior - reduce lift at high angles
        if (Math.abs(angleOfAttack) > Math.toRadians(20)) {
            double stallReduction = 1.0 - (Math.abs(angleOfAttack) - Math.toRadians(20)) / Math.toRadians(20);
            liftCoefficient *= Math.max(0.1, stallReduction);
        }

        double force = 0.5 * density * flowSpeed * flowSpeed * totalArea * liftCoefficient;

        // Scale by submerged percentage
        return force * submergedPercentage;
    }
}
