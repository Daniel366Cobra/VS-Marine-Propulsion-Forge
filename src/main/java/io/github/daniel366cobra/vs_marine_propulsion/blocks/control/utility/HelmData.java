package io.github.daniel366cobra.vs_marine_propulsion.blocks.control.utility;

import net.minecraft.core.Direction;

public class HelmData {
    public final Direction facing;
    public volatile boolean isCaptain; // First one placed becomes captain


    public HelmData(Direction facing, boolean isCaptain) {
        this.facing = facing;
        this.isCaptain = isCaptain;
    }

    public String toString() {
        return "Facing: " + this.facing + ", Captain: " + this.isCaptain;
    }
}
