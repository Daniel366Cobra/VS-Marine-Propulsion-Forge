package io.github.daniel366cobra.vs_marine_propulsion.debug;

import org.joml.Vector3d;

import java.util.UUID;

public record ForceVectorData(
        long shipID,
        Vector3d worldStart,
        Vector3d worldEnd,
        int color,
        String label,
        int tickDuration
) {

}