package io.github.daniel366cobra.vs_marine_propulsion.ship_control;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3d;
import org.valkyrienskies.core.api.ships.PhysShip;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.api.ships.ShipForcesInducer;
import org.valkyrienskies.core.impl.game.ships.PhysShipImpl;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ShipForcesApplier implements ShipForcesInducer {

    private String dimensionId = null;

    public Map<BlockPos, PropulsorData> propulsors = new ConcurrentHashMap<>();

    public ShipForcesApplier() {}

    public ShipForcesApplier(String dimensionId) {
        this.dimensionId = dimensionId;
    }

    public static ShipForcesApplier getOrCreate(ServerShip ship, String dimensionId) {
        ShipForcesApplier shipControl = ship.getAttachment(ShipForcesApplier.class);
        if (shipControl == null) {
            shipControl = new ShipForcesApplier(dimensionId);
            ship.saveAttachment(ShipForcesApplier.class, shipControl);
        }
        return shipControl;
    }

    public static ShipForcesApplier getOrCreate(ServerShip ship) {
        return  getOrCreate(ship, ship.getChunkClaimDimension());
    }

    public static ShipForcesApplier get(Level level, BlockPos pos) {
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

        propulsors.forEach((pos, data) -> {
            float thrust = data.thrust;
            Vector3d dir = data.dir;
            boolean submerged = data.submerged;

            if (thrust == 0.0f || !submerged) return;

            Vector3d thrustPos = VectorConversionsMCKt.toJOMLD(pos)
                    .add(0.5, 0.5, 0.5, new Vector3d())
                    .sub(physShip.getTransform().getPositionInShip());


            Vector3d thrustForce = physShip.getTransform().getShipToWorldRotation().transform(dir, new Vector3d());

            //VSMarinePropulsionMod.LOGGER.info("DIR: " + dir.toString() + ", THRUST_FORCE: " + thrustForce.toString() + ", THRUST: " + thrust);

            thrustForce.mul(thrust);

            physShip.applyInvariantForceToPos(thrustForce, thrustPos);

        });

    }

    private void setLevel(String levelId) {

    }
}
