package io.github.daniel366cobra.vs_marine_propulsion.ship_control;

import net.minecraft.util.StringRepresentable;
import org.joml.Vector3d;

public class PropulsorData {

    public final Vector3d dir;
    public volatile float thrust;

    public PropulsorData(Vector3d dir, float thrust) {
        this.dir = dir;
        this.thrust = thrust;
    }

    public String toString() {
        return "Direction: " + this.dir + ", Thrust: " + this.thrust;
    }
}
