package io.github.daniel366cobra.vs_marine_propulsion.blocks.propulsion;

import com.jozufozu.flywheel.core.PartialModel;
import com.tterrag.registrate.util.nullness.NonNullFunction;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class ShipPropellerBlockEntityRendererFactory
        implements NonNullFunction<BlockEntityRendererProvider.Context, BlockEntityRenderer<ShipPropellerBlockEntity>> {


    private final Map<Integer, PartialModel> propellerModelsMap;

    public ShipPropellerBlockEntityRendererFactory(PartialModel cwPropellerModel, PartialModel ccwPropellerModel) {
        this.propellerModelsMap = new HashMap<>();
        this.propellerModelsMap.put(1, cwPropellerModel);
        this.propellerModelsMap.put(-1, ccwPropellerModel);
    }

    @Override
    public @NotNull BlockEntityRenderer<ShipPropellerBlockEntity> apply(BlockEntityRendererProvider.@NotNull Context context) {
        return new ShipPropellerBlockEntityRenderer(context, this.propellerModelsMap);
    }
}
