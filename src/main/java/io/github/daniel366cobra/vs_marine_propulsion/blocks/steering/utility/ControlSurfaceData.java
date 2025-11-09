package io.github.daniel366cobra.vs_marine_propulsion.blocks.steering.utility;

import org.joml.Vector3d;

public class ControlSurfaceData {

    public final Vector3d normalDirection;
    public final Vector3d axisDirection;
    public int rudderBlocks;
    public volatile float angle;
    public volatile float submergedPercentage;

    public ControlSurfaceData(Vector3d normalDirection, Vector3d axisDirection, float angle, int rudderBlocks) {
        this.normalDirection = normalDirection;
        this.axisDirection = axisDirection;
        this.rudderBlocks = rudderBlocks;
        this.submergedPercentage = 0.0f;
    }

    public String toString() {
        return "Normal Direction: " + this.normalDirection + ", Axis Direction: " + this.axisDirection + ", Angle: " + this.angle + ", Rudder Blocks: " + this.rudderBlocks + ", Submerged Percentage: " + this.submergedPercentage;
    }

}
