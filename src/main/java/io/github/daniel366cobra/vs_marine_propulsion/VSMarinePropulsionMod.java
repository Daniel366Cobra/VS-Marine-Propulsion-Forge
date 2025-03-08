package io.github.daniel366cobra.vs_marine_propulsion;

import com.simibubi.create.Create;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.foundation.item.ItemDescription;
import com.simibubi.create.foundation.item.KineticStats;
import com.simibubi.create.foundation.item.TooltipHelper;
import com.simibubi.create.foundation.item.TooltipModifier;
import io.github.daniel366cobra.vs_marine_propulsion.config.VSMarinePropulsionConfig;
import io.github.daniel366cobra.vs_marine_propulsion.creative_tabs.VSMarinePropulsionCreativeTab;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.valkyrienskies.core.impl.hooks.VSEvents;
import org.valkyrienskies.mod.common.ValkyrienSkiesMod;

/**
 * Temporary borrows:
 * Ship propeller model & texture - VS Tournament
 * Stepped Lever model & texture - Design N Decor
 * Parts of the Freewheel clutch texture - Create Connected
 */

@Mod(VSMarinePropulsionMod.MOD_ID)
public class VSMarinePropulsionMod {
    public static final String MOD_ID = "vs_marine_propulsion";


    public static final String NAME = "VS Marine Propulsion";
    public static final Logger LOGGER = LoggerFactory.getLogger(NAME);
    public static final CreateRegistrate REGISTRATE = CreateRegistrate.create(VSMarinePropulsionMod.MOD_ID);

    static {
        REGISTRATE.setTooltipModifierFactory(
                item -> new ItemDescription.Modifier(item, TooltipHelper.Palette.STANDARD_CREATE)
                        .andThen(TooltipModifier.mapNull(KineticStats.create(item))));
    }

    public VSMarinePropulsionMod() {
        IEventBus eventBus = FMLJavaModLoadingContext.get().getModEventBus();
        VSMarinePropulsionMod.REGISTRATE.registerEventListeners(eventBus);
        LOGGER.info("{} initializing!", NAME);

        VSMarinePropulsionBlocks.register();
        VSMarinePropulsionBlockEntities.register();
        VSMarinePropulsionConfig.register(ModLoadingContext.get());

        VSMarinePropulsionSounds.register(eventBus);
        VSMarinePropulsionPartialModels.init();
        VSMarinePropulsionCreativeTab.register(eventBus);

        VSEvents.ShipLoadEvent.Companion.on(e -> {

        });
    }


    public static ResourceLocation resourceLocationFromPath(String path) {
        return new ResourceLocation(MOD_ID, path);
    }

    public static CreateRegistrate getRegistrate() {
        return REGISTRATE;
    }
}
