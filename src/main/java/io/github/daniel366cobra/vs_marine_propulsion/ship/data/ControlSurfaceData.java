package io.github.daniel366cobra.vs_marine_propulsion.ship.data;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import net.minecraft.core.BlockPos;
import org.joml.Vector3d;

@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.ANY,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE
)
public class ControlSurfaceData {

    public Vector3d position;
    public Vector3d normalDirection;
    public Vector3d axisDirection;
    public int rudderBlocks;
    public volatile float angle;
    public volatile float submergedPercentage;

    public ControlSurfaceData() {
        this.position = new Vector3d(0, 0, 0);
        this.normalDirection = new Vector3d(0, 0, 0);
        this.axisDirection = new Vector3d(0, 0, 0);
        this.rudderBlocks = 0;
        this.angle = 0.0f;
        this.submergedPercentage = 0.0f;
    }

    public ControlSurfaceData(BlockPos pos, Vector3d normalDir, Vector3d axisDir, int rudderBlocks) {
        this.position = new Vector3d(pos.getX(), pos.getY(), pos.getZ());
        this.normalDirection = normalDir;
        this.axisDirection = axisDir;
        this.rudderBlocks = rudderBlocks;
        this.angle = 0.0f;
        this.submergedPercentage = 0.0f;
    }

    public BlockPos getBlockPos() {
        return new BlockPos((int)position.x(), (int)position.y(), (int)position.z());
    }

    // For Set operations if needed later
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ControlSurfaceData that = (ControlSurfaceData) o;
        return position.equals(that.position);
    }

    @Override
    public int hashCode() {
        return position.hashCode();
    }

    public String toString() {
        return "Normal Direction: " + this.normalDirection + ", Axis Direction: " + this.axisDirection + ", Angle: " + this.angle + ", Rudder Blocks: " + this.rudderBlocks + ", Submerged Percentage: " + this.submergedPercentage;
    }

}
