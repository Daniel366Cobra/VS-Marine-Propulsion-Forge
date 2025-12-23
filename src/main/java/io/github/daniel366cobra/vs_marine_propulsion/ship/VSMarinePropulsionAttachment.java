package io.github.daniel366cobra.vs_marine_propulsion.ship;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.github.daniel366cobra.vs_marine_propulsion.ship.data.ControlSurfaceData;
import io.github.daniel366cobra.vs_marine_propulsion.ship.data.HelmData;
import io.github.daniel366cobra.vs_marine_propulsion.ship.data.PropulsorData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.apache.commons.lang3.tuple.Pair;
import org.joml.Vector3d;
import org.valkyrienskies.core.api.ships.PhysShip;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.api.ships.ShipForcesInducer;
import org.valkyrienskies.core.api.ships.properties.ShipTransform;
import org.valkyrienskies.core.impl.game.ships.PhysShipImpl;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

import javax.annotation.Nullable;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.ANY,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE
)
@JsonIgnoreProperties(ignoreUnknown = true)
public class VSMarinePropulsionAttachment implements ShipForcesInducer {

    private String dimensionId = null;
    private Direction shipForwardDirection = Direction.NORTH;
    private Set<HelmData> helms = ConcurrentHashMap.newKeySet();
    private Set<PropulsorData> propulsors = ConcurrentHashMap.newKeySet();
    private Set<ControlSurfaceData> controlSurfaces = ConcurrentHashMap.newKeySet();

    public VSMarinePropulsionAttachment() {}
    public VSMarinePropulsionAttachment(String dimensionId) {this.dimensionId = dimensionId;}

    public static VSMarinePropulsionAttachment getOrCreate(ServerShip ship, String dimensionId) {
        VSMarinePropulsionAttachment attachment = ship.getAttachment(VSMarinePropulsionAttachment.class);
        if (attachment == null) {
            attachment = new VSMarinePropulsionAttachment(dimensionId);
            ship.saveAttachment(VSMarinePropulsionAttachment.class, attachment);
        }
        return attachment;
    }

    public static VSMarinePropulsionAttachment getOrCreate(ServerShip ship) {
        return getOrCreate(ship, ship.getChunkClaimDimension());
    }

    public static VSMarinePropulsionAttachment get(Level level, BlockPos pos) {
        ServerLevel serverLevel = (ServerLevel) level;
        ServerShip ship = VSGameUtilsKt.getShipObjectManagingPos(serverLevel, pos);
        if (ship == null) {
            ship = VSGameUtilsKt.getShipManagingPos(serverLevel, pos);
        }
        return ship != null ? getOrCreate(ship) : null;
    }

    //---------------HELMS--------------
    public boolean addHelm(BlockPos helmPos, HelmData data) {

        Direction helmFacing = data.getFacing();

        boolean toCaptain = !hasValidOrientation();
        HelmData newHelmData;

        if (toCaptain) {
            // No captain yet - this helm becomes captain
            setShipForwardDirection(helmFacing);
            newHelmData = new HelmData(helmPos, helmFacing, true);
        } else {
            // Additional helms must face the same direction as captain
            if (data.getFacing() != getShipForwardDirection()) return false;

            newHelmData = new HelmData(helmPos, data.getFacing(), false);
        }

        // Set automatically handles duplicates based on position
        return helms.add(newHelmData);
    }

    public void removeHelm(BlockPos helmPos) {
        HelmData tempForRemoval = new HelmData(helmPos, Direction.NORTH, false);
        boolean removed = helms.remove(tempForRemoval);

        if (removed) {
            // Check if there's still a captain among the remaining helms
            boolean captainRemains = helms.stream()
                    .anyMatch(HelmData::isCaptain);

            if (!captainRemains && !helms.isEmpty()) {
                // We removed the captain - promote a new one
                HelmData newCaptainCandidate = helms.iterator().next();

                // Remove and readd as captain
                helms.remove(newCaptainCandidate);
                helms.add(new HelmData(
                        newCaptainCandidate.getBlockPos(),
                        newCaptainCandidate.getFacing(),
                        true  // This helm is now captain
                ));
                shipForwardDirection = newCaptainCandidate.getFacing();
            } else if (helms.isEmpty()) {
                shipForwardDirection = Direction.NORTH;
            }
        }
    }

    public Direction getShipForwardDirection() {
        return shipForwardDirection;
    }

    public void setShipForwardDirection(Direction direction) {
        this.shipForwardDirection = direction;
    }

    @JsonIgnore
    public Vector3d getForwardVector() {
        return VectorConversionsMCKt.toJOMLD(shipForwardDirection.getNormal());
    }

    @JsonIgnore
    @Nullable
    public BlockPos getCaptainHelmPosition() {
        return helms.stream()
                .filter(HelmData::isCaptain)
                .map(HelmData::getBlockPos)
                .findFirst()
                .orElse(null);
    }

    @JsonIgnore
    public Direction getCaptainDirection() {
        return shipForwardDirection;
    }

    @JsonIgnore
    public HelmData getHelmAtPos(BlockPos pos) {
        // Create temporary object for lookup
        HelmData tempForLookup = new HelmData(pos, Direction.NORTH, false);
        for (HelmData helm : helms) {
            if (helm.equals(tempForLookup)) {
                return helm;
            }
        }
        return null;
    }

    @JsonIgnore
    public int getTotalHelms() {
        return helms.size();
    }

    @JsonIgnore
    public boolean hasValidOrientation() {
        return shipForwardDirection != null && !helms.isEmpty();
    }

    //---------------PROPULSORS--------------
    public void addPropulsor(BlockPos pos, PropulsorData data) {
        // Create new data with the correct position to ensure consistency
        PropulsorData newData = new PropulsorData(pos, data.thrustDirection, data.thrust);
        newData.submerged = data.submerged;
        propulsors.add(newData);
    }

    public void removePropulsor(BlockPos pos) {
        // Create temporary object for removal (uses position-based equality)
        PropulsorData tempForRemoval = new PropulsorData(pos, new Vector3d(), 0.0f);
        propulsors.remove(tempForRemoval);
    }

    @Nullable
    public PropulsorData getPropulsorAtPos(BlockPos pos) {
        // Create temporary object for lookup
        PropulsorData tempForLookup = new PropulsorData(pos, new Vector3d(), 0.0f);
        for (PropulsorData data : propulsors) {
            if (data.equals(tempForLookup)) {
                return data;
            }
        }
        return null;
    }

    @JsonIgnore
    public int getTotalPropulsors() {
        return propulsors.size();
    }

    //---------------CONTROL SURFACES--------------
    public void addControlSurface(BlockPos pos, ControlSurfaceData data) {
        ControlSurfaceData newData = new ControlSurfaceData(pos, data.normalDirection, data.axisDirection,
                data.rudderBlocks);
        newData.angle = data.angle;
        newData.submergedPercentage = data.submergedPercentage;
        controlSurfaces.add(newData);
    }

    public void removeControlSurface(BlockPos pos) {
        // Create temporary object for removal (uses position-based equality)
        ControlSurfaceData tempForRemoval = new ControlSurfaceData(pos, new Vector3d(), new Vector3d(), 0);
        controlSurfaces.remove(tempForRemoval);
    }

    @Nullable
    public ControlSurfaceData getControlSurfaceAtPos(BlockPos pos) {
        // Create temporary object for lookup
        ControlSurfaceData tempForLookup = new ControlSurfaceData(pos, new Vector3d(), new Vector3d(), 0);
        for (ControlSurfaceData data : controlSurfaces) {
            if (data.equals(tempForLookup)) {
                return data;
            }
        }
        return null;
    }

    //---------------PHYSICS--------------
    @Override
    public void applyForces(PhysShip physicsShip) {
        PhysShipImpl physShip = (PhysShipImpl) physicsShip;

        applyPropulsionForces(physShip);
        applyStableRealisticForces(physShip);


    }

    private void applyPropulsionForces(PhysShipImpl physShip) {

        final ShipTransform transform = physShip.getTransform();

        propulsors.forEach(data -> {
            float thrust = data.thrust;
            Vector3d dir = data.thrustDirection;
            boolean submerged = data.submerged;

            if (thrust == 0.0f || !submerged) return;

            // Calculate position relative to ship's center of mass in ship coordinates
            Vector3d thrustPos = VectorConversionsMCKt.toJOMLD(data.getBlockPos())
                    .add(0.5, 0.5, 0.5, new Vector3d())
                    .sub(transform.getPositionInShip());

            // Transform thrust direction from ship-local to world coordinates
            Vector3d thrustForce = transform.getShipToWorld().transformDirection(dir, new Vector3d());
            thrustForce.normalize().mul(thrust);

            physShip.applyInvariantForceToPos(thrustForce, thrustPos);
        });
    }

    //TODO WORKS BUT NEEDS ACC PHYSICS
    private void applyControlForces(PhysShipImpl physShip) {

        final ShipTransform transform = physShip.getTransform();

        controlSurfaces.forEach(data -> {

            // Skip if not submerged
            if (data.submergedPercentage < 0.05f) {
                return;
            }
            // Skip if no angle
            if (Math.abs(data.angle) < 0.1f) {
                return;
            }

            // 2. Simple force calculation
            double baseForce = 5000.0; // Newtons - make this LARGE to see effect
            double forceMagnitude = baseForce *
                    data.angle / 40.0 * // Scale by angle (±40° max)
                    data.submergedPercentage * // Scale by submersion
                    data.rudderBlocks; // Scale by size


            Vector3d forceDirection = new Vector3d(data.normalDirection);

            forceDirection.normalize();

            // 4. Apply force
            Vector3d forceVector = forceDirection.mul(forceMagnitude);

            // 5. Convert to world coordinates and apply
            Vector3d forceWorld = transform.getShipToWorld().transformDirection(forceVector, new Vector3d());
            Vector3d rudderPos = VectorConversionsMCKt.toJOMLD(data.getBlockPos())
                    .add(0.5, 0.5, 0.5, new Vector3d())
                    .sub(transform.getPositionInShip());


            // DEBUG
            System.out.println("Rudder update: angle=" + data.angle);

            physShip.applyInvariantForceToPos(forceWorld, rudderPos);
        });
    }


    private void applyStableRealisticForces(PhysShipImpl physShip) {
        final ShipTransform transform = physShip.getTransform();

        // Get ship's forward direction in SHIP coordinates
        Vector3d shipForwardShip = getForwardVector();

        controlSurfaces.forEach(data -> {
            if (data.submergedPercentage < 0.05f || Math.abs(data.angle) < 0.1f) return;

            // 1. Position in ship coordinates
            Vector3d rudderPosShip = VectorConversionsMCKt.toJOMLD(data.getBlockPos())
                    .add(0.5, 0.5, 0.5, new Vector3d())
                    .sub(transform.getPositionInShip());

            // 2. Get ship velocity in SHIP coordinates (NO angular velocity!)
            Vector3d shipVelocityShip = new Vector3d(physShip.getPoseVel().getVel());

            // 3. Calculate water velocity (simple: opposite of ship motion)
            Vector3d waterVelocity = new Vector3d(shipVelocityShip).negate();
            double waterSpeed = waterVelocity.length();

            // CLAMP water speed to prevent instability
            waterSpeed = Math.min(waterSpeed, 50.0); // Max 50 m/s (~100 knots)
            if (waterSpeed < 0.05) return;


            // b) Rudder deflection in ship coordinates
            double rudderAngleRad = Math.toRadians(data.angle);

            // c) Effective AoA = rudder angle + leeway (but transformed properly!)
            // When ship drifts to port (negative leeway), rudder sees more/less flow
            // Simplified: AoA ≈ rudder angle - leeway (for starboard rudder)
            double effectiveAoARad = rudderAngleRad; // 70% coupling

            // CLAMP AoA to prevent unrealistic values
            effectiveAoARad = Math.max(-Math.toRadians(40.0),
                    Math.min(Math.toRadians(40.0), effectiveAoARad));

            // 5. Calculate hydrodynamic forces (STABLE VERSION)
            Pair<Double, Double> forces = calculateStableHydroForces(
                    effectiveAoARad, waterSpeed,
                    data.rudderBlocks * data.submergedPercentage
            );

            double liftForce = forces.getLeft();
            double dragForce = forces.getRight();

            // 6. Force directions

            // Lift direction: Based on rudder geometry, not water flow (more stable)
            Vector3d liftDir = new Vector3d(data.normalDirection);

            liftDir.normalize();

            // Drag direction: Opposite to ship's velocity (simplified)
            Vector3d dragDir = new Vector3d(shipVelocityShip).negate().normalize();

            // 7. Combine forces
            Vector3d totalForceShip = new Vector3d()
                    .add(liftDir.mul(liftForce))
                    .add(dragDir.mul(dragForce));

            // 9. LIMIT maximum force (safety)
            double maxForce = 200000.0; // 200 kN max (big ship rudder)
            double forceMagnitude = totalForceShip.length();
            if (forceMagnitude > maxForce) {
                totalForceShip.mul(maxForce / forceMagnitude);
            }

            // 10. Convert to world and apply
            Vector3d forceWorld = transform.getShipToWorld()
                    .transformDirection(totalForceShip, new Vector3d());

            physShip.applyInvariantForceToPos(forceWorld, rudderPosShip);
        });
    }

    private Pair<Double, Double> calculateStableHydroForces(double angleRad, double speed, double area) {
        double absAngle = Math.abs(angleRad);
        double sign = Math.signum(angleRad);

        // REALISTIC but STABLE coefficients

        // Lift coefficient - with realistic stall curve
        double cl;
        if (absAngle < Math.toRadians(15.0)) {
            // Linear region: Cl = 2π * α
            cl = 2.0 * Math.PI * angleRad;
        } else if (absAngle < Math.toRadians(30.0)) {
            // Near stall: reduced slope
            double linearCl = 2.0 * Math.PI * Math.toRadians(15.0) * sign;
            double extraAngle = absAngle - Math.toRadians(15.0);
            cl = linearCl + 0.5 * Math.PI * Math.sin(extraAngle) * sign;
        } else {
            // Fully stalled: constant or decreasing
            cl = (1.2 + 0.3 * Math.sin(absAngle - Math.toRadians(30.0))) * sign;
        }

        // Clamp Cl to realistic range
        cl = Math.max(-1.5, Math.min(1.5, cl));

        // Drag coefficient - realistic but bounded
        double cd0 = 0.05; // Base drag
        double inducedDrag = 0.1 * cl * cl; // Induced drag
        double separationDrag = 0.05 * Math.pow(Math.sin(absAngle), 2); // Flow separation

        double cd = cd0 + inducedDrag + separationDrag;
        cd = Math.max(cd0, Math.min(2.0, cd)); // Clamp

        // Force calculation with SPEED DAMPING (critical!)
        double waterDensity = 1000.0;
        double dynamicPressure = 0.5 * waterDensity * speed * speed;

        // DAMPING FACTOR: Forces don't scale quadratically forever
        // At high speeds, flow separates more, reducing effectiveness
        double speedFactor = 1.0 / (1.0 + speed * 0.02); // Reduces above 50 m/s
        double speedFactor2 = Math.min(1.0, 30.0 / speed); // Alternative

        double lift = dynamicPressure * area * cl * speedFactor;
        double drag = dynamicPressure * area * cd * speedFactor;

        return Pair.of(lift, drag);
    }

}
