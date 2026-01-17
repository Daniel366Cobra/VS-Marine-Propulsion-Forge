package io.github.daniel366cobra.vs_marine_propulsion.ship;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.github.daniel366cobra.vs_marine_propulsion.VSMarinePropulsionPacketHandler;
import io.github.daniel366cobra.vs_marine_propulsion.debug.DebugVectorData;
import io.github.daniel366cobra.vs_marine_propulsion.debug.DebugVectorPacket;
import io.github.daniel366cobra.vs_marine_propulsion.ship.data.ControlSurfaceData;
import io.github.daniel366cobra.vs_marine_propulsion.ship.data.HelmData;
import io.github.daniel366cobra.vs_marine_propulsion.ship.data.PropulsorData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.PacketDistributor;
import org.apache.commons.lang3.tuple.Pair;
import org.joml.Vector3d;
import org.valkyrienskies.core.api.ships.PhysShip;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.api.ships.ShipForcesInducer;
import org.valkyrienskies.core.api.ships.properties.ShipTransform;
import org.valkyrienskies.core.impl.game.ships.PhysShipImpl;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.ValkyrienSkiesMod;
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
    private Direction shipForwardDirection = null;
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
            if (helmFacing != getShipForwardDirection())
                helmFacing = getShipForwardDirection();

            newHelmData = new HelmData(helmPos, helmFacing, false);
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
                shipForwardDirection = null;
            }
        }
    }

    public Direction getShipForwardDirection() {
        return shipForwardDirection;
    }

    public void setShipForwardDirection(Direction direction) {
        this.shipForwardDirection = direction;
    }

    //TODO remove dependence on "forward" for rudder calculations
    @JsonIgnore
    public Vector3d getForwardVector() {
        return hasValidOrientation()? VectorConversionsMCKt.toJOMLD(shipForwardDirection.getNormal()) : new Vector3d(0, 0, 0);
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
        ControlSurfaceData newData = new ControlSurfaceData(pos, data.center, data.normalDirection, data.axisDirection,
                data.rudderBlocks);
        newData.angle = data.angle;
        newData.submergedPercentage = data.submergedPercentage;
        controlSurfaces.add(newData);
    }

    public void removeControlSurface(BlockPos pos) {
        // Create temporary object for removal (uses position-based equality)
        ControlSurfaceData tempForRemoval = new ControlSurfaceData(pos, new Vector3d(), new Vector3d(), new Vector3d(), 0);
        controlSurfaces.remove(tempForRemoval);
    }

    @Nullable
    public ControlSurfaceData getControlSurfaceAtPos(BlockPos pos) {
        // Create temporary object for lookup
        ControlSurfaceData tempForLookup = new ControlSurfaceData(pos, new Vector3d(), new Vector3d(), new Vector3d(), 0);
        for (ControlSurfaceData data : controlSurfaces) {
            if (data.equals(tempForLookup)) {
                return data;
            }
        }
        return null;
    }

    //----------------DEBUG---------------

    private void sendDebugVector(PhysShipImpl physShip, Vector3d worldStart,
                                 Vector3d worldEnd, int color, String label) {
        // Get the Minecraft server
        MinecraftServer server = ValkyrienSkiesMod.getCurrentServer();
        if (server == null) return;

        // Get ship position for distance check
        Vector3d shipPos = new Vector3d(physShip.getTransform().getPositionInShip());

        // Create a consistent ship ID for rendering
        long shipId = physShip.getId();

        // Create the force data
        DebugVectorData forceData = new DebugVectorData(
                shipId, worldStart, worldEnd, color, label, 2 // 2 tick duration
        );

        // Send to all nearby players in ALL dimensions
        // Client will check F3+B itself
        for (ServerLevel level : server.getAllLevels()) {
            for (ServerPlayer player : level.players()) {
                double distanceSq = player.distanceToSqr(shipPos.x, shipPos.y, shipPos.z);
                if (distanceSq < 256 * 256) { // 256 block radius
                    VSMarinePropulsionPacketHandler.CHANNEL.send(
                            PacketDistributor.PLAYER.with(() -> player),
                            new DebugVectorPacket(forceData)
                    );
                }
            }
        }
    }

    //---------------PHYSICS--------------
    @Override
    public void applyForces(PhysShip physicsShip) {
        PhysShipImpl physShip = (PhysShipImpl) physicsShip;

        applyPropulsionForces(physShip);
        applyControlSurfaceForces(physShip);
    }

    /**
     * Calculates and applies propulsion forces.
     * All force calculations done in shipyard coordinates.
     *
     * @param physShip the ship to which forces are applied
     */
    private void applyPropulsionForces(PhysShipImpl physShip) {

        final ShipTransform transform = physShip.getTransform();

        propulsors.forEach(data -> {

            float thrust = data.thrust;
            boolean submerged = data.submerged;

            if (thrust == 0.0f || !submerged) return;

            // Calculate position relative to ship's center of mass in ship coordinates
            Vector3d thrustPosShipyard = VectorConversionsMCKt.toJOMLD(data.getBlockPos())
                    .add(0.5, 0.5, 0.5, new Vector3d());

            Vector3d comPositionShipyard = new Vector3d(transform.getPositionInShip());

            Vector3d thrustRelativePosShipyard = thrustPosShipyard.sub(comPositionShipyard, new Vector3d());

            Vector3d thrustDirShipyard = new Vector3d(data.thrustDirection);

            Vector3d thrustForceShipyard = thrustDirShipyard.mul(thrust, new Vector3d());

            physShip.applyRotDependentForceToPos(thrustForceShipyard, thrustRelativePosShipyard);

            // ===== DEBUG VISUALIZATION in WORLD=====
            Vector3d worldBlockCenter = transform.getShipToWorld().transformPosition(thrustPosShipyard, new Vector3d());

            Vector3d thrustDirWorld = transform.getShipToWorld().transformDirection(thrustForceShipyard, new Vector3d());

            Vector3d worldThrustEnd = new Vector3d(worldBlockCenter).add(new Vector3d(thrustDirWorld).mul(0.001));

            int color = data.submerged ? 0xFF0000FF : 0xFF808080; // Blue if submerged, gray if not

            sendDebugVector(physShip, worldBlockCenter, worldThrustEnd,
                    color,
                    String.format("Thrust: %.1fN", data.thrust));
        });
    }


    /**
     * Calculates and applies hydrodynamic forces to control surfaces.
     * All force calculations done in shipyard coordinates.
     *
     * @param physShip the ship to which forces are applied
     */
    private void applyControlSurfaceForces(PhysShipImpl physShip) {

        // FIXME apply forces to rudder center, not hinge. Also transform the axis
        final ShipTransform transform = physShip.getTransform();

        controlSurfaces.forEach(data -> {
            if (data.submergedPercentage < 0.05f || Math.abs(data.angle) < 0.1f) return;

            // Rudder position in SHIPYARD coordinates
            Vector3d rudderPosShipyard = VectorConversionsMCKt.toJOMLD(data.getBlockPos())
                    .add(0.5, 0.5, 0.5, new Vector3d());

            //TODO finish this
            Vector3d centerPosShipyard = new Vector3d(data.center);

            Vector3d comPositionShipyard = new Vector3d(transform.getPositionInShip());

            // Rudder position in SHIP-CENTRIC coordinates (CoM = zero) for later application of force
            Vector3d rudderRelativePosShipyard = rudderPosShipyard.sub(comPositionShipyard, new Vector3d());

            // Get ship velocity - already in WORLD coordinates (rotated + translated to the ship movement)
            Vector3d linearVelocityWorld = new Vector3d(physShip.getPoseVel().getVel());
            Vector3d angularVelocityWorld = new Vector3d(physShip.getPoseVel().getOmega());

            // Ship velocity transformed to SHIPYARD coordinates
            Vector3d linearVelocityShipyard = transform.getWorldToShip().transformDirection(linearVelocityWorld, new Vector3d());
            Vector3d angularVelocityShipyard = transform.getWorldToShip().transformDirection(angularVelocityWorld, new Vector3d());
            Vector3d rotationalVelocityShipyard = angularVelocityShipyard.cross(rudderRelativePosShipyard, new Vector3d());

            Vector3d totalVelocityShipyard = linearVelocityShipyard.add(rotationalVelocityShipyard, new Vector3d());

            // Calculate water velocity (opposite of ship motion) in SHIPYARD coordinates
            Vector3d waterVelocityShipyard = new Vector3d(totalVelocityShipyard).negate();

            double maxWaterVelocity = 50.0; // 50 m/s
            double waterVelocityMagnitude = waterVelocityShipyard.length();

            if (waterVelocityMagnitude < 0.05) return;

            if (waterVelocityMagnitude > maxWaterVelocity) {
                waterVelocityShipyard.mul(maxWaterVelocity / waterVelocityMagnitude);
            }

            Vector3d waterFlowDirUnitShipyard = new Vector3d(waterVelocityShipyard).normalize();

            // Calculate chordwise unit vector from normal and axis ones
            // By chosen convention, axis and normal vectors are directed at the positive half of respective world axis,
            // set upon assembly of rudder contraption
            Vector3d rudderNeutralNormalUnitShipyard = new Vector3d(data.normalDirection).normalize();
            Vector3d rudderNeutralSpanUnitShipyard = new Vector3d(data.axisDirection).normalize();
            Vector3d rudderNeutralChordUnitShipyard = new Vector3d(rudderNeutralNormalUnitShipyard).cross(rudderNeutralSpanUnitShipyard)
                    .normalize()
                    .negate();

            double rudderDeflection = Math.toRadians(data.angle);

            // Deflected rudder vectors
            Vector3d rudderDeflectedChordUnitShipyard = new Vector3d(rudderNeutralChordUnitShipyard)
                    .rotateAxis(rudderDeflection,
                            rudderNeutralSpanUnitShipyard.x,
                            rudderNeutralSpanUnitShipyard.y,
                            rudderNeutralSpanUnitShipyard.z);

            Vector3d rudderDeflectedNormalUnitShipyard = new Vector3d(rudderNeutralNormalUnitShipyard)
                    .rotateAxis(rudderDeflection,
                            rudderNeutralSpanUnitShipyard.x,
                            rudderNeutralSpanUnitShipyard.y,
                            rudderNeutralSpanUnitShipyard.z);

            // From which side the flow is hitting the rudder
            double sideSign = Math.signum(waterFlowDirUnitShipyard.dot(rudderDeflectedNormalUnitShipyard));

            // From which edge the flow comes at the rudder
            double flowSign = Math.signum(waterFlowDirUnitShipyard.dot(rudderDeflectedChordUnitShipyard));

            // Angle of attack between flow vector and deflected chordwise
            double totalAoA = waterFlowDirUnitShipyard.angleSigned(
                    rudderDeflectedChordUnitShipyard,  // Vector to compare against
                    rudderNeutralSpanUnitShipyard      // Normal/axis for sign determination
            );

            // Calculate hydrodynamic force magnitudes
            Pair<Double, Double> forces = calculateSimpleHydroForces(
                    totalAoA, waterVelocityMagnitude,
                    data.rudderBlocks * data.submergedPercentage
            );

            double liftMagnitude = forces.getLeft();
            double dragMagnitude = forces.getRight();

            // Force directions

            // Lift is perpendicular to both flow direction and spanwise direction
            // Forward/reverse flow flips - cross direction should also flip!
            Vector3d liftDirShipyard = waterFlowDirUnitShipyard.cross(rudderNeutralSpanUnitShipyard, new Vector3d())
                    .normalize();

            // Scale the magnitude
            Vector3d liftForceShipyard = new Vector3d(liftDirShipyard).mul(flowSign * liftMagnitude);

            // Drag opposite to ship velocity (along relative water flow)
            Vector3d dragForceShipyard = new Vector3d(waterVelocityShipyard)
                    .normalize()
                    .mul(dragMagnitude);

            // Combine forces
            Vector3d totalForceShipyard = new Vector3d()
                    .add(liftForceShipyard)
                    .add(dragForceShipyard);

            // Limit maximum force (safety)
            double maxForce = 200000.0; // 200 kN max (big ship rudder)
            double forceMagnitude = totalForceShipyard.length();
            if (forceMagnitude > maxForce) {
                totalForceShipyard.mul(maxForce / forceMagnitude);
            }

            // Apply to ship as rotation dependent
            physShip.applyRotDependentForceToPos(totalForceShipyard, rudderRelativePosShipyard);

            // ===== DEBUG VISUALIZATION in WORLD =====

            // Positions
            Vector3d rudderPosWorld = transform.getShipToWorld().transformPosition(rudderPosShipyard, new Vector3d());

            // Velocities
            Vector3d waterVelocityWorld = transform.getShipToWorld().transformDirection(waterVelocityShipyard);

            // Forces and directions
            Vector3d liftForceWorld = transform.getShipToWorld()
                    .transformDirection(liftForceShipyard, new Vector3d());

            Vector3d dragForceWorld = transform.getShipToWorld()
                    .transformDirection(dragForceShipyard, new Vector3d());

            Vector3d chordUnitDirectionWorld = transform.getShipToWorld()
                    .transformDirection(rudderDeflectedChordUnitShipyard, new Vector3d());

            Vector3d normalUnitDirectionWorld = transform.getShipToWorld()
                    .transformDirection(rudderDeflectedNormalUnitShipyard, new Vector3d());


            // Vector endpoints
            Vector3d worldWaterVelocityEnd = new Vector3d(rudderPosWorld).add(new Vector3d(waterVelocityWorld).mul(5));

            Vector3d worldChordDirectionEnd = new Vector3d(rudderPosWorld).add(new Vector3d(chordUnitDirectionWorld).mul(5));
            Vector3d worldNormalDirectionEnd = new Vector3d(rudderPosWorld).add(new Vector3d(normalUnitDirectionWorld).mul(5));

            Vector3d worldLiftForceEnd = new Vector3d(rudderPosWorld).add(new Vector3d(liftForceWorld).mul(0.001));
            Vector3d worldDragForceEnd = new Vector3d(rudderPosWorld).add(new Vector3d(dragForceWorld).mul(0.001));

            // Vector colors
            int liftColor = 0xFF00FFFF; // Cyan
            int dragColor = 0xFF951529; // Dark red
            int velocityColor = 0xFFFFFF00; // Yellow
            int normalDirectionColor = 0xFFFFFFFF; // White
            int chordDirectionColor = 0xFF00FF00; // Green


            sendDebugVector(physShip, rudderPosWorld, worldLiftForceEnd,
                    liftColor,
                    "Rudder lift");

            sendDebugVector(physShip, rudderPosWorld, worldDragForceEnd,
                    dragColor,
                    "Rudder drag: %.1f° (%.1fN)");

            sendDebugVector(physShip, rudderPosWorld, worldWaterVelocityEnd,
                    velocityColor,
                    "Flow velocity");

            sendDebugVector(physShip, rudderPosWorld, worldNormalDirectionEnd,
                    normalDirectionColor,
                    "Deflected normal");

            sendDebugVector(physShip, rudderPosWorld, worldChordDirectionEnd,
                    chordDirectionColor,
                    "Deflected chord");

        });
    }

    /**
     * Simplified calculation of lift and drag coefficients for a flat plate foil in water.
     *
     * @param angleRad angle of attack (positive or negative from neutral), in radians
     * @param speed flow speed, in m/s
     * @param area foil area, in blocks
     * @return Pair of Cl and Cd. Cl is signed.
     */
    private Pair<Double, Double> calculateSimpleHydroForces(double angleRad, double speed, double area) {

        final double waterDensity = 1000.0;
        final double STALL_START_RAD = Math.toRadians(15.0);      // 0.2618
        final double DEEP_STALL_RAD = Math.toRadians(25.0);       // 0.4363
        final double STALL_RANGE_RAD = DEEP_STALL_RAD - STALL_START_RAD;  // 0.1745

        double dynPressure = 0.5 * waterDensity * speed * speed;

        double angleSign = Math.signum(angleRad);

        double angleAbs = Math.abs(angleRad);
        double sinAlpha = Math.sin(angleAbs);

        double CL, CD;

        if (angleAbs <= STALL_START_RAD) {
            CL = angleAbs / STALL_START_RAD;
            CD = 0.05 + 0.8 * sinAlpha * sinAlpha;
        } else if (angleAbs <= DEEP_STALL_RAD) {
            double fraction = (angleAbs - STALL_START_RAD) / STALL_RANGE_RAD;
            CL = 1.0 - 0.5 * fraction;
            CD = 0.05 + 1.5 * sinAlpha * sinAlpha;
        } else {
            CL = 0.5;
            CD = 0.05 + 2.0 * sinAlpha * sinAlpha;
        }

        // Speed effect: cavitation-like reduction at high speed
        double speedFactor = 1.0;
        if (speed > 15.0) { // ~30 knots
            speedFactor = Math.max(0.3, 1.0 - 0.05 * (speed - 15.0));
        }

        CL *= speedFactor;
        CD *= speedFactor;

        double liftMagnitude = angleSign * dynPressure * area * CL;
        double dragMagnitude = dynPressure * area * CD;

        return Pair.of(liftMagnitude, dragMagnitude);
    }
}
