package io.github.daniel366cobra.vs_marine_propulsion.blocks.propulsion.utility;

import org.joml.Vector3d;

public class PropulsorData {

    public final Vector3d thrustDirection;
    public volatile float thrust;
    public volatile boolean submerged;

    public PropulsorData(Vector3d dir, float thrust) {
        this.thrustDirection = dir;
        this.thrust = thrust;
        this.submerged = false;
    }

    public String toString() {
        return "Direction: " + this.thrustDirection + ", Thrust: " + this.thrust;
    }
}
