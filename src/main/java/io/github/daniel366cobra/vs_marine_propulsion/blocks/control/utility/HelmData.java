package io.github.daniel366cobra.vs_marine_propulsion.blocks.control.utility;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.joml.Vector3d;
import org.joml.Vector3dc;

@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.ANY,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE
)
public class HelmData {
    public Vector3dc position;  // Use Vector3dc instead of BlockPos
    public Direction facing;
    public boolean isCaptain;

    public HelmData() {
        this.position = new Vector3d(0, 0, 0);
        this.facing = Direction.NORTH;
        this.isCaptain = false;
    }

    public HelmData(BlockPos pos, Direction facing, boolean isCaptain) {
        this.position = new Vector3d(pos.getX(), pos.getY(), pos.getZ());
        this.facing = facing;
        this.isCaptain = isCaptain;
    }

    public boolean isCaptain() {
        return isCaptain;
    }

    public void setCaptain(boolean captain) {
        this.isCaptain = captain;
    }

    public Direction getFacing() {
        return this.facing;
    }

    public BlockPos getBlockPos() {
        return new BlockPos((int)position.x(), (int)position.y(), (int)position.z());
    }

    // Critical for Set operations - equality based only on position
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HelmData helmData = (HelmData) o;
        return position.equals(helmData.position); // Only compare positions
    }

    // Critical for Set operations - hash based only on position
    @Override
    public int hashCode() {
        return position.hashCode(); // Only hash the position
    }

    @Override
    public String toString() {
        return "HelmData{position=" + position + ", facing=" + facing + ", isCaptain=" + isCaptain + '}';
    }
}