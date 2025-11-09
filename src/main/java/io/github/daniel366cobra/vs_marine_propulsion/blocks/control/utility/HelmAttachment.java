package io.github.daniel366cobra.vs_marine_propulsion.blocks.control.utility;

import com.fasterxml.jackson.annotation.JsonIgnore;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.joml.Vector3d;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

//FIXME not saving ship forward direction
public class HelmAttachment {

    private String dimensionId = null;

    public Map<BlockPos, HelmData> helms = new ConcurrentHashMap<>();

    private Direction shipForwardDirection = Direction.NORTH; // Default forward

    public HelmAttachment() {}

    public HelmAttachment(String dimensionId) {
        this.dimensionId = dimensionId;
    }

    private static HelmAttachment getOrCreate(ServerShip ship, String dimensionId) {
        HelmAttachment attachment = ship.getAttachment(HelmAttachment.class);
        if (attachment == null) {
            attachment = new HelmAttachment(dimensionId);
            ship.saveAttachment(HelmAttachment.class, attachment);
        }
        return attachment;
    }

    public static HelmAttachment getOrCreate(ServerShip ship) {
        return  getOrCreate(ship, ship.getChunkClaimDimension());
    }

    public static HelmAttachment get(Level level, BlockPos pos) {
        ServerLevel serverLevel = (ServerLevel) level;
        ServerShip ship = VSGameUtilsKt.getShipObjectManagingPos(serverLevel, pos);
        if (ship == null) {
            ship = VSGameUtilsKt.getShipManagingPos(serverLevel, pos);
        }

        return ship != null ? getOrCreate(ship) : null;
    }

    /**
     * Attempts to add a helm to the ship
     * @param helmFacing The direction the helm block is facing
     * @param helmPos The position of the helm block
     * @return true if helm was successfully added, false if invalid facing
     */
    public boolean addHelm(Direction helmFacing, BlockPos helmPos) {
        System.out.println("DEBUG: Adding helm at " + helmPos + " facing " + helmFacing);
        System.out.println("DEBUG: Current helms: " + helms.size());

        // If we already have a captain, use their direction for validation
        Direction requiredDirection = getCaptainDirection();
        System.out.println("DEBUG: Required direction: " + requiredDirection);

        if (requiredDirection == null) {
            // No captain yet - this helm becomes captain and defines ship orientation
            shipForwardDirection = helmFacing;
            helms.put(helmPos, new HelmData(helmFacing, true));
            System.out.println("DEBUG: Set as captain");
            return true;
        } else {
            // Additional helms must face the same direction as captain
            if (helmFacing == requiredDirection) {
                helms.put(helmPos, new HelmData(helmFacing, false));
                System.out.println("DEBUG: Added as regular helm");
                return true;
            } else {
                // Invalid facing for additional helm - DO NOT add to map
                System.out.println("DEBUG: Invalid facing! " + helmFacing + " != " + requiredDirection);
                return false;
            }
        }
    }

    /**
     * Removes a helm from the ship
     */
    public void removeHelm(BlockPos helmPos) {
        HelmData removedHelm = helms.remove(helmPos);
        if (removedHelm != null && removedHelm.isCaptain && !helms.isEmpty()) {
            // Captain was removed - promote the next helm to captain
            Map.Entry<BlockPos, HelmData> nextHelm = helms.entrySet().iterator().next();
            shipForwardDirection = nextHelm.getValue().facing;
            // Update the new captain
            helms.put(nextHelm.getKey(), new HelmData(nextHelm.getValue().facing, true));
        } else if (helms.isEmpty()) {
            // No helms left - reset ship direction
            shipForwardDirection = Direction.NORTH;
        }
    }

    public Direction getShipForwardDirection() { return shipForwardDirection; }

    @JsonIgnore
    public Vector3d getForwardVector() {
        return VectorConversionsMCKt.toJOMLD(shipForwardDirection.getNormal());
    }

    @JsonIgnore
    public BlockPos getCaptainHelmPosition() {
        return helms.entrySet().stream()
                .filter(entry -> entry.getValue().isCaptain)
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }

    @JsonIgnore
    public Direction getCaptainDirection() {
        BlockPos captainPos = getCaptainHelmPosition();
        if (captainPos != null) {
            HelmData captainData = helms.get(captainPos);
            if (captainData != null) {
                return captainData.facing;
            }
        }
        return null; // No captain found
    }

    @JsonIgnore
    public int getTotalHelms() { return helms.size(); }

    @JsonIgnore
    public boolean hasValidOrientation() {
        return !helms.isEmpty();
    }
}
