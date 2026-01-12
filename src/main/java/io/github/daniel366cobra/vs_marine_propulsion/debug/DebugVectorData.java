package io.github.daniel366cobra.vs_marine_propulsion.debug;

import org.joml.Vector3d;

public record DebugVectorData(
        long shipID,
        Vector3d worldStart,
        Vector3d worldEnd,
        int color,
        String label,
        int tickDuration
) {

}