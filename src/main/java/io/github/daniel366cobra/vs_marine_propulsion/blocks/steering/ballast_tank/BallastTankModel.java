package io.github.daniel366cobra.vs_marine_propulsion.blocks.steering.ballast_tank;

import com.simibubi.create.api.connectivity.ConnectivityHandler;
import com.simibubi.create.content.fluids.tank.FluidTankCTBehaviour;
import com.simibubi.create.foundation.block.connected.CTModel;
import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry;
import com.simibubi.create.foundation.block.connected.ConnectedTextureBehaviour;
import com.simibubi.create.foundation.utility.Iterate;
import io.github.daniel366cobra.vs_marine_propulsion.VSMarinePropulsionSpriteShifts;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static io.github.daniel366cobra.vs_marine_propulsion.blocks.steering.ballast_tank.BallastTankHorizontalBlock.AXIS;

public class BallastTankModel extends CTModel {

    private final boolean horizontal;

    protected static final ModelProperty<CullData> CULL_PROPERTY = new ModelProperty<>();

    // Factory methods
    public static BallastTankModel horizontal(BakedModel originalModel) {
        return new BallastTankModel(originalModel, true);
    }

    public static BallastTankModel vertical(BakedModel originalModel) {
        return new BallastTankModel(originalModel, false);
    }

    private BallastTankModel(BakedModel originalModel, boolean horizontal) {
        super(originalModel, createCTBehavior(horizontal));
        this.horizontal = horizontal;
    }

    private static CTSpriteShiftEntry getSideShift() {
        return VSMarinePropulsionSpriteShifts.BALLAST_TANK;
    }

    private static CTSpriteShiftEntry getTopShift() {
        return VSMarinePropulsionSpriteShifts.BALLAST_TANK_TOP;
    }

    private static CTSpriteShiftEntry getInnerShift() {
        return VSMarinePropulsionSpriteShifts.BALLAST_TANK_INNER;
    }

    private static ConnectedTextureBehaviour createCTBehavior(boolean horizontal) {
        if (horizontal) {
            return new BallastTankHorizontalCTBehavior(getSideShift(), getTopShift(), getInnerShift());
        } else {
            return new FluidTankCTBehaviour(getSideShift(), getTopShift(), getInnerShift());
        }
    }

    @Override
    protected ModelData.Builder gatherModelData(ModelData.Builder builder, BlockAndTintGetter world,
                                                BlockPos pos, BlockState state, ModelData blockEntityData) {
        super.gatherModelData(builder, world, pos, state, blockEntityData);

        CullData cullData = horizontal ? new HorizontalCullData() : new VerticalCullData();

        if (horizontal) {
            Direction.Axis axis = state.getValue(AXIS);
            for (Direction d : Iterate.directions) {
                if (d.getAxis() == axis) continue;
                cullData.setCulled(d, ConnectivityHandler.isConnected(world, pos, pos.relative(d)));
            }
        } else {
            for (Direction d : Iterate.horizontalDirections) {
                cullData.setCulled(d, ConnectivityHandler.isConnected(world, pos, pos.relative(d)));
            }
        }

        return builder.with(CULL_PROPERTY, cullData);
    }

    @Override
    public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource rand,
                                    ModelData extraData, RenderType renderType) {
        if (side != null) {
            return Collections.emptyList();
        }

        List<BakedQuad> quads = new ArrayList<>();
        for (Direction d : Iterate.directions) {
            if (extraData.has(CULL_PROPERTY) && extraData.get(CULL_PROPERTY).isCulled(d)) {
                continue;
            }
            quads.addAll(super.getQuads(state, d, rand, extraData, renderType));
        }
        quads.addAll(super.getQuads(state, null, rand, extraData, renderType));
        return quads;
    }

    // Abstract CullData base class
    protected abstract static class CullData {
        abstract void setCulled(Direction face, boolean cull);
        abstract boolean isCulled(Direction face);
    }

    // Horizontal implementation
    protected static class HorizontalCullData extends CullData {
        private final boolean[] culledFaces = new boolean[6];

        public HorizontalCullData() {
            Arrays.fill(culledFaces, false);
        }

        @Override
        void setCulled(Direction face, boolean cull) {
            culledFaces[face.get3DDataValue()] = cull;
        }

        @Override
        boolean isCulled(Direction face) {
            return culledFaces[face.get3DDataValue()];
        }
    }

    // Vertical implementation
    protected static class VerticalCullData extends CullData {
        private final boolean[] culledFaces = new boolean[4];

        public VerticalCullData() {
            Arrays.fill(culledFaces, false);
        }

        @Override
        void setCulled(Direction face, boolean cull) {
            if (face.getAxis().isVertical()) return;
            culledFaces[face.get2DDataValue()] = cull;
        }

        @Override
        boolean isCulled(Direction face) {
            if (face.getAxis().isVertical()) return false;
            return culledFaces[face.get2DDataValue()];
        }
    }
}
