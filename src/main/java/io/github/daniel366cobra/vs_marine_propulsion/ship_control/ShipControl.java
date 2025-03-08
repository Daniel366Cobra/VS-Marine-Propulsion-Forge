package io.github.daniel366cobra.vs_marine_propulsion.ship_control;

import com.fasterxml.jackson.databind.annotation.JsonAppend;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.valkyrienskies.core.api.ships.PhysShip;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.api.ships.ShipForcesInducer;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.world.ChunkManagement;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ShipControl implements ShipForcesInducer {

    private String dimensionId = null;

    public Map<BlockPos, PropulsorData> propulsors = new ConcurrentHashMap<>();

    public ShipControl() {}

    public ShipControl(String dimensionId) {
        this.dimensionId = dimensionId;
    }

    public static ShipControl getOrCreate(ServerShip ship, String dimensionId) {
        ShipControl shipControl = ship.getAttachment(ShipControl.class);
        if (shipControl == null) {
            shipControl = new ShipControl(dimensionId);
            ship.saveAttachment(ShipControl.class, shipControl);
        }
        return shipControl;
    }

    public static ShipControl getOrCreate(ServerShip ship) {
        return  getOrCreate(ship, ship.getChunkClaimDimension());
    }

    public static ShipControl get(Level level, BlockPos pos) {
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
    public void applyForces(@NotNull PhysShip physShip) {

    }

    private void setLevel(String levelId) {

    }
}
