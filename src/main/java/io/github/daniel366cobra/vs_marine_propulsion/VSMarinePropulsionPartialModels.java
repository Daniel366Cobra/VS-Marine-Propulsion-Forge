package io.github.daniel366cobra.vs_marine_propulsion;

import com.jozufozu.flywheel.core.PartialModel;

public class VSMarinePropulsionPartialModels {

    public static final PartialModel LARGE_SHIP_PROPELLER_CLOCKWISE = block("large_ship_propeller/blades_clockwise");
    public static final PartialModel LARGE_SHIP_PROPELLER_COUNTERCLOCKWISE = block("large_ship_propeller/blades_counterclockwise");

    public static final PartialModel ENGINE_ORDER_TELEGRAPH_LEVER = block("engine_order_telegraph/lever");

    public static final PartialModel RUDDER_BEARING_ROTATOR = block("rudder_bearing/top");

    public static final PartialModel HELM_WHEEL = block("helm/wheel");
    public static final PartialModel DIVING_PLANE_STATION_WHEEL = block("diving_plane_station/wheel");

    private static PartialModel block(String path) {
        return new PartialModel(VSMarinePropulsionMod.resourceLocationFromPath("block/" + path));
    }

    private static PartialModel entity(String path) {
        return new PartialModel(VSMarinePropulsionMod.resourceLocationFromPath("entity/" + path));
    }

    public static void init() {
        VSMarinePropulsionMod.LOGGER.info("Initializing Partial Models");
    }
}
