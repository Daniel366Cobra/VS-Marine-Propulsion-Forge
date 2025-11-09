package io.github.daniel366cobra.vs_marine_propulsion.blocks.propulsion.utility;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.ANY,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE
)
@JsonIgnoreProperties(ignoreUnknown = true)
public class PropulsorForcesApplier implements ShipForcesInducer {

    private String dimensionId = null;

    // Use Set instead of Map - each PropulsorData contains its own position
    private Set<PropulsorData> propulsors = ConcurrentHashMap.newKeySet();

    public PropulsorForcesApplier() {}

    public PropulsorForcesApplier(String dimensionId) {
        this.dimensionId = dimensionId;
    }

    public static PropulsorForcesApplier getOrCreate(ServerShip ship, String dimensionId) {
        PropulsorForcesApplier propulsorAttachment = ship.getAttachment(PropulsorForcesApplier.class);
        if (propulsorAttachment == null) {
            propulsorAttachment = new PropulsorForcesApplier(dimensionId);
            ship.saveAttachment(PropulsorForcesApplier.class, propulsorAttachment);
        }
        return propulsorAttachment;
    }

    public static PropulsorForcesApplier getOrCreate(ServerShip ship) {
        return getOrCreate(ship, ship.getChunkClaimDimension());
    }

    public static PropulsorForcesApplier get(Level level, BlockPos pos) {
        ServerLevel serverLevel = (ServerLevel) level;
        ServerShip ship = VSGameUtilsKt.getShipObjectManagingPos(serverLevel, pos);
        if (ship == null) {
            ship = VSGameUtilsKt.getShipManagingPos(serverLevel, pos);
        }
        return ship != null ? getOrCreate(ship) : null;
    }

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

    @Override
    public void applyForces(PhysShip physicsShip) {
        PhysShipImpl physShip = (PhysShipImpl) physicsShip;
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

            // Apply force at the specific position
            physShip.applyInvariantForceToPos(thrustForce, thrustPos);
        });
    }

    @JsonIgnore
    public int getTotalPropulsors() {
        return propulsors.size();
    }
}
