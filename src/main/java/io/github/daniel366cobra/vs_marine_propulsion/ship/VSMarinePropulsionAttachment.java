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
    public boolean addHelm(Direction helmFacing, BlockPos helmPos) {
        Direction requiredDirection = getCaptainDirection();
        HelmData newHelmData;

        if (requiredDirection == null) {
            // No captain yet - this helm becomes captain
            shipForwardDirection = helmFacing;
            newHelmData = new HelmData(helmPos, helmFacing, true);
        } else {
            // Additional helms must face the same direction as captain
            if (helmFacing == requiredDirection) {
                newHelmData = new HelmData(helmPos, helmFacing, false);
            } else {
                return false;
            }
        }

        // Set automatically handles duplicates based on position
        return helms.add(newHelmData);
    }

    public void removeHelm(BlockPos helmPos) {
        // Create a temporary object for removal (uses position-based equality)
        HelmData tempForRemoval = new HelmData(helmPos, Direction.NORTH, false);
        boolean removed = helms.remove(tempForRemoval);

        if (removed) {
            // Check if we removed the captain and need to promote a new one
            HelmData removedCaptain = helms.stream()
                    .filter(HelmData::isCaptain)
                    .findFirst()
                    .orElse(null);

            if (removedCaptain == null && !helms.isEmpty()) {
                // No captain found - promote the first available helm
                HelmData newCaptain = helms.iterator().next();
                newCaptain.setCaptain(true);
                shipForwardDirection = newCaptain.getFacing();
            } else if (helms.isEmpty()) {
                shipForwardDirection = Direction.NORTH;
            }
        }
    }

    public Direction getShipForwardDirection() {
        return shipForwardDirection;
    }

    @JsonIgnore
    public Vector3d getForwardVector() {
        return VectorConversionsMCKt.toJOMLD(shipForwardDirection.getNormal());
    }

    @JsonIgnore
    public BlockPos getCaptainHelmPosition() {
        return helms.stream()
                .filter(HelmData::isCaptain)
                .map(HelmData::getBlockPos)
                .findFirst()
                .orElse(null);
    }

    @JsonIgnore
    public Direction getCaptainDirection() {
        return helms.stream()
                .filter(HelmData::isCaptain)
                .map(HelmData::getFacing)
                .findFirst()
                .orElse(null);
    }

    @JsonIgnore
    public HelmData getHelmData(BlockPos pos) {
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
        return !helms.isEmpty();
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
        applyControlForces(physShip);


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

    private double calculateLiftForce(double angleRadians, double shipSpeed, int rudderBlocks, float submergedPercentage) {
        // Basic hydrodynamic lift formula for control surfaces
        double density = 1000.0; // Water density kg/m³
        double areaPerBlock = 1.0; // m² per rudder block
        double totalArea = areaPerBlock * rudderBlocks * submergedPercentage;

        // Lift coefficient for symmetric foils at small angles
        // Cl = 2π * sin(α) ≈ 2π * α for small angles
        double effectiveAngle = Math.abs(angleRadians);
        double liftCoefficient = 2 * Math.PI * Math.sin(effectiveAngle);

        // Stall reduction at high angles (>20°)
        if (effectiveAngle > Math.toRadians(20)) {
            double stallFactor = 1.0 - (effectiveAngle - Math.toRadians(20)) / Math.toRadians(20);
            liftCoefficient *= Math.max(0.1, stallFactor);
        }

        // Lift force: F = 0.5 * ρ * v² * A * Cl
        double force = 0.5 * density * shipSpeed * shipSpeed * totalArea * liftCoefficient;

        // Apply sign based on angle direction
        return force * Math.signum(angleRadians);
    }

}
