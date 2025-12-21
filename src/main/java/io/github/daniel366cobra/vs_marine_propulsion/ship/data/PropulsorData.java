package io.github.daniel366cobra.vs_marine_propulsion.ship.data;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import net.minecraft.core.BlockPos;
import org.joml.Vector3d;
import org.joml.Vector3dc;

@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.ANY,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE
)
public class PropulsorData {
    public Vector3dc position;  // Store position here
    public Vector3d thrustDirection;
    public volatile float thrust;
    public volatile boolean submerged;

    public PropulsorData() {
        this.position = new Vector3d(0, 0, 0);
        this.thrustDirection = new Vector3d(0, 0, 0);
        this.thrust = 0.0f;
        this.submerged = false;
    }

    public PropulsorData(BlockPos pos, Vector3d dir, float thrust) {
        this.position = new Vector3d(pos.getX(), pos.getY(), pos.getZ());
        this.thrustDirection = dir;
        this.thrust = thrust;
        this.submerged = false;
    }

    public BlockPos getBlockPos() {
        return new BlockPos((int)position.x(), (int)position.y(), (int)position.z());
    }

    // For Set operations if needed later
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PropulsorData that = (PropulsorData) o;
        return position.equals(that.position);
    }

    @Override
    public int hashCode() {
        return position.hashCode();
    }

    @Override
    public String toString() {
        return "Direction: " + this.thrustDirection + ", Thrust: " + this.thrust + ", Submerged: " + this.submerged;
    }
}