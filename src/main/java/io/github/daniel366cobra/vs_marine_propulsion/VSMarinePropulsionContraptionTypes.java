package io.github.daniel366cobra.vs_marine_propulsion;

import com.simibubi.create.content.contraptions.ContraptionType;
import io.github.daniel366cobra.vs_marine_propulsion.blocks.steering.RudderContraption;

public class VSMarinePropulsionContraptionTypes {

    public static final ContraptionType RUDDER = ContraptionType.register("rudder", RudderContraption::new);

    public static void init() {
        VSMarinePropulsionMod.LOGGER.info("Registering contraption types for " + VSMarinePropulsionMod.NAME);
    }
}
