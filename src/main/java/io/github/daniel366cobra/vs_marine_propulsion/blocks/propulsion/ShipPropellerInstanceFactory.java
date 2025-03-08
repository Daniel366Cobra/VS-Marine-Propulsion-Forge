package io.github.daniel366cobra.vs_marine_propulsion.blocks.propulsion;

import com.jozufozu.flywheel.api.MaterialManager;
import com.jozufozu.flywheel.core.PartialModel;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

public class ShipPropellerInstanceFactory implements BiFunction<MaterialManager, ShipPropellerBlockEntity, ShipPropellerInstance> {

    private final Map<Integer, PartialModel> propellerModelsMap;

    public ShipPropellerInstanceFactory(PartialModel cwPropellerModel, PartialModel ccwPropellerModel) {
        this.propellerModelsMap  = new HashMap<>();
        this.propellerModelsMap.put(1, cwPropellerModel);
        this.propellerModelsMap.put(-1, ccwPropellerModel);
    }

    @Override
    public ShipPropellerInstance apply(MaterialManager materialManager, ShipPropellerBlockEntity blockEntity) {
        return new ShipPropellerInstance(materialManager, blockEntity, this.propellerModelsMap);
    }
}
