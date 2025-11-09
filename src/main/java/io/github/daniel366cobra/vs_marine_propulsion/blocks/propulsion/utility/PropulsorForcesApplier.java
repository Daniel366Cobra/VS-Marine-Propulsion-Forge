package io.github.daniel366cobra.vs_marine_propulsion.blocks.propulsion.utility;

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

public class PropulsorForcesApplier implements ShipForcesInducer {

    private String dimensionId = null;

    public Map<BlockPos, PropulsorData> propulsors = new ConcurrentHashMap<>();

    public PropulsorForcesApplier() {}

    public PropulsorForcesApplier(String dimensionId) {
        this.dimensionId = dimensionId;
    }

    public static PropulsorForcesApplier getOrCreate(ServerShip ship, String dimensionId) {
        PropulsorForcesApplier shipControl = ship.getAttachment(PropulsorForcesApplier.class);
        if (shipControl == null) {
            shipControl = new PropulsorForcesApplier(dimensionId);
            ship.saveAttachment(PropulsorForcesApplier.class, shipControl);
        }
        return shipControl;
    }

    public static PropulsorForcesApplier getOrCreate(ServerShip ship) {
        return  getOrCreate(ship, ship.getChunkClaimDimension());
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
        propulsors.put(pos, data);
    }
    public void removePropulsor(BlockPos pos) {
        propulsors.remove(pos);
    }

    @Nullable
    public PropulsorData getPropulsorAtPos(BlockPos pos) {
        return propulsors.get(pos);
    }

    @Override
    public void applyForces(@NotNull PhysShip physicsShip) {
        PhysShipImpl physShip = (PhysShipImpl) physicsShip;
        final ShipTransform transform = physShip.getTransform();

        propulsors.forEach((pos, data) -> {
            float thrust = data.thrust;
            Vector3d dir = data.thrustDirection;
            boolean submerged = data.submerged;

            if (thrust == 0.0f || !submerged) return;

            // Calculate position relative to ship's center of mass in ship coordinates
            Vector3d thrustPos = VectorConversionsMCKt.toJOMLD(pos)
                    .add(0.5, 0.5, 0.5, new Vector3d())
                    .sub(transform.getPositionInShip());

            // Transform thrust direction from ship-local to world coordinates
            Vector3d thrustForce = transform.getShipToWorld().transformDirection(dir, new Vector3d());
            thrustForce.normalize().mul(thrust);

            // Apply force at the specific position - THIS IS THE CRITICAL FIX
            physShip.applyInvariantForceToPos(thrustForce, thrustPos);
        });
    }

}
