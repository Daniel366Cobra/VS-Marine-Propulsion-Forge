package io.github.daniel366cobra.vs_marine_propulsion.config;

import io.github.daniel366cobra.vs_marine_propulsion.VSMarinePropulsionMod;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import org.valkyrienskies.mod.common.ValkyrienSkiesMod;

public class VSMarinePropulsionConfig {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.ConfigValue<Number> LARGE_SHIP_PROPELLER_MAX_THRUST;
    public static final ForgeConfigSpec.ConfigValue<Number> LARGE_SHIP_PROPELLER_MAX_THRUST_RPM;
    public static final ForgeConfigSpec.ConfigValue<Number> LARGE_SHIP_PROPELLER_CAVITATION_RPM;

    static {
        BUILDER.push("Propulsors");

        LARGE_SHIP_PROPELLER_MAX_THRUST = BUILDER.comment("Large Ship Propeller maximum thrust").define("large_ship_propeller_max_thrust", 40000);
        LARGE_SHIP_PROPELLER_MAX_THRUST_RPM = BUILDER.comment("Large Ship Propeller maximum thrust RPM").define("large_ship_propeller_max_thrust_RPM", 64);
        LARGE_SHIP_PROPELLER_CAVITATION_RPM = BUILDER.comment("Large Ship Propeller cavitation RPM").define("large_ship_propeller_cavitation_RPM", 96);

        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    public static void register(ModLoadingContext context){
        VSMarinePropulsionMod.LOGGER.info("Registering config!");
        context.registerConfig(ModConfig.Type.SERVER, SPEC, "vsmarinepropulsion-config.toml");
    }
}
