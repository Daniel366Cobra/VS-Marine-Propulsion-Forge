package io.github.daniel366cobra.vs_marine_propulsion;

import com.simibubi.create.foundation.utility.VoxelShaper;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.function.BiFunction;

import static net.minecraft.core.Direction.*;

public class VSMarinePropulsionShapes {

    public static final VoxelShaper ENGINE_ORDER_TELEGRAPH_FLOOR = shape(4, 0, 0, 12, 5, 16)
            .forHorizontalAxis();

    public static final VoxelShaper ENGINE_ORDER_TELEGRAPH_CEILING = shape(4, 11, 0, 15, 16, 16)
            .forHorizontalAxis();

    public static final VoxelShaper ENGINE_ORDER_TELEGRAPH_WALL = shape(4, 0, 0, 12, 16, 5)
            .forHorizontal(SOUTH);

    public static final VoxelShaper RUDDER = shape(0, 6, 0, 16, 10, 16)
            .forDirectional();

    public static final VoxelShaper HELM = shape(5,0,2,11,16,11)
            .add(0, 5, 0, 16, 21, 2)
            .forHorizontal(SOUTH);

    private static VSMarinePropulsionShapes.Builder shape(VoxelShape shape) {
        return new VSMarinePropulsionShapes.Builder(shape);
    }

    private static VSMarinePropulsionShapes.Builder shape(double x1, double y1, double z1, double x2, double y2, double z2) {
        return shape(cuboid(x1, y1, z1, x2, y2, z2));
    }

    private static VoxelShape cuboid(double x1, double y1, double z1, double x2, double y2, double z2) {
        return Block.box(x1, y1, z1, x2, y2, z2);
    }

    public static class Builder {

        private VoxelShape shape;

        public Builder(VoxelShape shape) {
            this.shape = shape;
        }

        public VSMarinePropulsionShapes.Builder add(VoxelShape shape) {
            this.shape = Shapes.or(this.shape, shape);
            return this;
        }

        public VSMarinePropulsionShapes.Builder add(double x1, double y1, double z1, double x2, double y2, double z2) {
            return add(cuboid(x1, y1, z1, x2, y2, z2));
        }

        public VSMarinePropulsionShapes.Builder erase(double x1, double y1, double z1, double x2, double y2, double z2) {
            this.shape = Shapes.join(shape, cuboid(x1, y1, z1, x2, y2, z2), BooleanOp.ONLY_FIRST);
            return this;
        }

        public VoxelShape build() {
            return shape;
        }

        public VoxelShaper build(BiFunction<VoxelShape, Direction, VoxelShaper> factory, Direction direction) {
            return factory.apply(shape, direction);
        }

        public VoxelShaper build(BiFunction<VoxelShape, Direction.Axis, VoxelShaper> factory, Direction.Axis axis) {
            return factory.apply(shape, axis);
        }

        public VoxelShaper forDirectional(Direction direction) {
            return build(VoxelShaper::forDirectional, direction);
        }

        public VoxelShaper forAxis() {
            return build(VoxelShaper::forAxis, Direction.Axis.Y);
        }

        public VoxelShaper forHorizontalAxis() {
            return build(VoxelShaper::forHorizontalAxis, Direction.Axis.Z);
        }

        public VoxelShaper forHorizontal(Direction direction) {
            return build(VoxelShaper::forHorizontal, direction);
        }

        public VoxelShaper forDirectional() {
            return forDirectional(UP);
        }

    }
}
