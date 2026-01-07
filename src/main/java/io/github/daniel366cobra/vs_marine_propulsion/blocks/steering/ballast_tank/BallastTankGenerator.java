package io.github.daniel366cobra.vs_marine_propulsion.blocks.steering.ballast_tank;

import com.simibubi.create.foundation.data.AssetLookup;
import com.simibubi.create.foundation.data.SpecialBlockStateGen;
import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateBlockstateProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.generators.ModelFile;

import static io.github.daniel366cobra.vs_marine_propulsion.blocks.steering.ballast_tank.BallastTankHorizontalBlock.*;
import static io.github.daniel366cobra.vs_marine_propulsion.blocks.steering.ballast_tank.BallastTankVerticalBlock.BOTTOM;
import static io.github.daniel366cobra.vs_marine_propulsion.blocks.steering.ballast_tank.BallastTankVerticalBlock.TOP;

public class BallastTankGenerator extends SpecialBlockStateGen {

    private boolean horizontal;

    public static BallastTankGenerator horizontal() {
        return new BallastTankGenerator(true);
    }

    public static BallastTankGenerator vertical() {
        return new BallastTankGenerator(false);
    }

    private BallastTankGenerator(boolean horizontal) {this.horizontal = horizontal;}

    @Override
    protected int getXRotation(BlockState state) {
        return 0;
    }

    @Override
    protected int getYRotation(BlockState state) {
        return 0;
    }

    @Override
    public <T extends Block> ModelFile getModel(DataGenContext<Block, T> ctx, RegistrateBlockstateProvider prov, BlockState state) {

        String shapeName = "middle";
        String modelName;

        if (horizontal) {
            Boolean positive = state.getValue(POSITIVE);
            Boolean negative = state.getValue(NEGATIVE);
            Direction.Axis axis = state.getValue(AXIS);

            if (positive && negative)
                shapeName = "single";
            else if (positive)
                shapeName = "positive";
            else if (negative)
                shapeName = "negative";

            modelName = (axis == Direction.Axis.X ? "x" : "z") + "_" + shapeName;

        } else {
            Boolean top = state.getValue(TOP);
            Boolean bottom = state.getValue(BOTTOM);

            if (top && bottom)
                shapeName = "single";
            else if (top)
                shapeName = "top";
            else if (bottom)
                shapeName = "bottom";

            modelName = shapeName;
        }

        return AssetLookup.partialBaseModel(ctx, prov, modelName);
    }
}