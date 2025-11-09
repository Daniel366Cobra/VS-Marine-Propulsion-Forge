package io.github.daniel366cobra.vs_marine_propulsion.debug;

import org.joml.Vector3d;

public record ForceVectorData(
        Vector3d worldPos,
        Vector3d force,
        int tickDuration // Optional: auto-expire old data
) {
    // Helper method to return a new instance with decremented duration
    public ForceVectorData withDecrementedDuration() {
        return new ForceVectorData(
                this.worldPos,
                this.force,
                this.tickDuration - 1
        );
    }
}
