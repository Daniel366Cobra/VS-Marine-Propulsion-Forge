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
import net.minecraft.util.Mth;
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

    private void applyPropulsionForces(PhysShipImpl physShip) {

        final ShipTransform transform = physShip.getTransform();

        propulsors.forEach(data -> {

            float thrust = data.thrust;
            Vector3d thrustDir = data.thrustDirection;
            boolean submerged = data.submerged;

            if (thrust == 0.0f || !submerged) return;

            // Calculate position relative to ship's center of mass in ship coordinates
            Vector3d thrustPos = VectorConversionsMCKt.toJOMLD(data.getBlockPos())
                    .add(0.5, 0.5, 0.5, new Vector3d());

            Vector3d thrustPosShip = thrustPos.sub(transform.getPositionInShip(), new Vector3d());

            Vector3d thrustDirShip = thrustDir.mul(thrust, new Vector3d());

            physShip.applyRotDependentForceToPos(thrustDirShip, thrustPosShip);

            // ===== DEBUG VISUALIZATION in WORLD=====
            Vector3d worldBlockCenter = transform.getShipToWorld().transformPosition(thrustPos, new Vector3d());

            Vector3d thrustDirWorld = transform.getShipToWorld().transformDirection(thrustDirShip, new Vector3d());
            thrustDirWorld.normalize().mul(thrust);

            Vector3d worldThrustEnd = new Vector3d(worldBlockCenter).add(new Vector3d(thrustDirWorld).mul(0.001));

            int color = data.submerged ? 0xFF0000FF : 0xFF808080; // Blue if submerged, gray if not

            sendDebugVector(physShip, worldBlockCenter, worldThrustEnd,
                    color,
                    String.format("Thrust: %.1fN", data.thrust));
        });
    }


    //TODO verify all sailing directions
    private void applyControlSurfaceForces(PhysShipImpl physShip) {

        final ShipTransform transform = physShip.getTransform();

        controlSurfaces.forEach(data -> {
            if (data.submergedPercentage < 0.05f || Math.abs(data.angle) < 0.1f) return;

            // Rudder position in shipyard coordinates
            Vector3d rudderPos = VectorConversionsMCKt.toJOMLD(data.getBlockPos())
                    .add(0.5, 0.5, 0.5, new Vector3d());

            // Rudder position in ship-centric coordinates
            Vector3d rudderPosShip = rudderPos.sub(transform.getPositionInShip(), new Vector3d());

            // Get ship's forward direction in shipyard coordinates
            Vector3d shipForwardShip = getForwardVector();

            // Get ship velocity - already in world coordinates
            Vector3d shipVelocityWorld = new Vector3d(physShip.getPoseVel().getVel());

            // Ship velocity transformed to shipyard coordinates
            Vector3d shipVelocityShip = transform.getWorldToShip().transformDirection(shipVelocityWorld, new Vector3d());

            // Calculate water velocity (opposite of ship motion)
            Vector3d waterVelocityShip = new Vector3d(shipVelocityShip).negate();
            double waterSpeed = waterVelocityShip.length();

            Vector3d waterFlowDir = new Vector3d(waterVelocityShip).normalize();

            double directionModifier = Math.signum(waterFlowDir.dot(shipForwardShip));

            waterSpeed = Math.min(waterSpeed, 50.0); // Max 50 m/s (~100 knots)
            if (waterSpeed < 0.05) return;

            // Rudder deflection in ship coordinates
            double rudderAngleRad = Mth.clamp(Math.toRadians(data.angle * directionModifier),
                    Math.toRadians(-40.0),
                    Math.toRadians(40.0));

            // Calculate hydrodynamic force magnitudes
            Pair<Double, Double> forces = calculateHydrodynamicForces(
                    rudderAngleRad, waterSpeed,
                    data.rudderBlocks * data.submergedPercentage
            );

            double liftMagnitude = forces.getLeft();
            double dragMagnitude = forces.getRight();

            // Force directions
            // Rudder normal direction in shipyard coordinates
            Vector3d rudderNormalShip = new Vector3d(data.normalDirection).normalize();

            // Lift direction along rudder normal (perpendicular to chord)
            Vector3d liftForceShip = new Vector3d(rudderNormalShip)
                    .normalize()
                    .mul(liftMagnitude);

            // Drag opposite to ship velocity
            Vector3d dragForceShip = new Vector3d(shipVelocityShip)
                    .negate()
                    .normalize()
                    .mul(dragMagnitude);

            // Combine forces
            Vector3d totalForceShip = new Vector3d()
                    .add(liftForceShip)
                    .add(dragForceShip);

            // Limit maximum force (safety)
            double maxForce = 200000.0; // 200 kN max (big ship rudder)
            double forceMagnitude = totalForceShip.length();
            if (forceMagnitude > maxForce) {
                totalForceShip.mul(maxForce / forceMagnitude);
            }

            // Apply to ship as rotation dependent
            physShip.applyRotDependentForceToPos(totalForceShip, rudderPosShip);

            // ===== DEBUG VISUALIZATION in WORLD =====
            Vector3d worldBlockCenter = transform.getShipToWorld().transformPosition(rudderPos, new Vector3d());

            Vector3d liftForceWorld = transform.getShipToWorld()
                    .transformDirection(liftForceShip, new Vector3d());

            Vector3d dragForceWorld = transform.getShipToWorld()
                    .transformDirection(dragForceShip, new Vector3d());

            Vector3d worldLiftForceEnd = new Vector3d(worldBlockCenter).add(new Vector3d(liftForceWorld).mul(0.001));
            Vector3d worldDragForceEnd = new Vector3d(worldBlockCenter).add(new Vector3d(dragForceWorld).mul(0.001));

            int liftColor = data.angle * directionModifier > 0 ? 0xFF00FF00 : 0xFFFF0000; // Green for starboard, red for port
            int dragColor = 0xFF951529;

            sendDebugVector(physShip, worldBlockCenter, worldLiftForceEnd,
                    liftColor,
                    String.format("Rudder lift: %.1f° (%.1fN)", data.angle, liftForceWorld.length()));

            sendDebugVector(physShip, worldBlockCenter, worldDragForceEnd,
                    dragColor,
                    String.format("Rudder drag: %.1f° (%.1fN)", data.angle, dragForceWorld.length()));
        });
    }

    private Pair<Double, Double> calculateHydrodynamicForces(double angleRad, double speed, double area) {

        double absAngle = Math.abs(angleRad);
        double sign = Math.signum(angleRad);

        // Critical angles (in radians)
        double linearLimit = Math.toRadians(12.0);  // Reduced from 15° - stall starts earlier
        double fullStallAngle = Math.toRadians(35.0); // Stall is more gradual
        double maxClAngle = Math.toRadians(15.0);    // Max Cl occurs after linear region ends

        // Lift coefficient with more realistic stall
        double cl;

        if (absAngle <= linearLimit) {
            // Linear region: Cl = 2π * α (thin airfoil theory)
            cl = 2.0 * Math.PI * absAngle;
        } else if (absAngle <= maxClAngle) {
            // Post-linear, pre-stall: Cl continues to increase but slower
            double linearCl = 2.0 * Math.PI * linearLimit;
            double theta = (absAngle - linearLimit) / (maxClAngle - linearLimit);
            // Cubic interpolation for smooth transition
            cl = linearCl + (0.1 * Math.PI) * (3 * theta * theta - 2 * theta * theta * theta);
        } else if (absAngle <= fullStallAngle) {
            // Stall region: Cl decreases from max value
            double maxCl = 2.0 * Math.PI * linearLimit + 0.1 * Math.PI; // Max Cl value
            double stallProgress = (absAngle - maxClAngle) / (fullStallAngle - maxClAngle);
            // Drop to about 60% of max Cl at full stall
            cl = maxCl * (1.0 - 0.4 * stallProgress * stallProgress);
        } else {
            // Deep stall: Cl stabilizes at lower value with some oscillation
            double deepStallCl = 0.6 * (2.0 * Math.PI * linearLimit); // ~60% of max
            // Add some variation but much less than your version
            cl = deepStallCl * (0.9 + 0.1 * Math.sin(2.0 * (absAngle - fullStallAngle)));
        }

        // Apply sign and realistic bounds
        cl *= sign;
        cl = Math.max(-1.8, Math.min(1.8, cl));  // Slightly higher max for foil sections

        // Drag coefficient - improved modeling
        double cd0 = 0.03;  // Lower base drag for streamlined foil
        double clForDrag = Math.abs(cl);

        // Induced drag (proportional to Cl²)
        double aspectRatio = 2.0;  // Typical for rudders
        double inducedDragFactor = (clForDrag * clForDrag) / (Math.PI * aspectRatio * 0.9);

        // Separation drag - increases dramatically post-stall
        double separationFactor;
        if (absAngle <= maxClAngle) {
            separationFactor = 0.5 * Math.sin(2.0 * absAngle);  // Sin² approximation
        } else {
            // Post-stall: rapid increase in drag
            double stallSeverity = (absAngle - maxClAngle) / (fullStallAngle - maxClAngle);
            separationFactor = 0.5 + 1.5 * stallSeverity * stallSeverity;
        }

        double cd = cd0 + inducedDragFactor + separationFactor;
        cd = Math.max(cd0, Math.min(3.0, cd));  // Higher max drag in stall

        // Force calculation with improved damping
        double waterDensity = 1025.0;  // Seawater density
        double dynamicPressure = 0.5 * waterDensity * speed * speed;

        // Reynolds number effect (simplified)
        double reynoldsFactor = Math.log10(1.0 + speed * 5.0) / Math.log10(11.0);
        reynoldsFactor = Math.max(0.7, Math.min(1.3, reynoldsFactor));

        // Cavitation/stall effect at high angles and speeds
        double cavitationFactor = 1.0;
        if (absAngle > Math.toRadians(20.0) && speed > 5.0) {
            double cavitationSeverity = (speed - 5.0) / 10.0 * (absAngle - Math.toRadians(20.0)) / Math.toRadians(15.0);
            cavitationFactor = 1.0 / (1.0 + Math.max(0, cavitationSeverity));
        }

        double lift = dynamicPressure * area * cl * reynoldsFactor * cavitationFactor;
        double drag = dynamicPressure * area * cd * reynoldsFactor * cavitationFactor;

        return Pair.of(lift, drag);
    }
}
