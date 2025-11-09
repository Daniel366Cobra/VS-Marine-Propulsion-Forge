package io.github.daniel366cobra.vs_marine_propulsion.blocks.control.utility;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.joml.Vector3d;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.ANY,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE
)
@JsonIgnoreProperties(ignoreUnknown = true)
public class HelmAttachment {
    private String dimensionId = null;

    // Use Set instead of Map - each HelmData contains its own position
    private Set<HelmData> helms = ConcurrentHashMap.newKeySet();

    private Direction shipForwardDirection = Direction.NORTH;

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
        return getOrCreate(ship, ship.getChunkClaimDimension());
    }

    public static HelmAttachment get(Level level, BlockPos pos) {
        ServerLevel serverLevel = (ServerLevel) level;
        ServerShip ship = VSGameUtilsKt.getShipObjectManagingPos(serverLevel, pos);
        if (ship == null) {
            ship = VSGameUtilsKt.getShipManagingPos(serverLevel, pos);
        }
        return ship != null ? getOrCreate(ship) : null;
    }

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
}