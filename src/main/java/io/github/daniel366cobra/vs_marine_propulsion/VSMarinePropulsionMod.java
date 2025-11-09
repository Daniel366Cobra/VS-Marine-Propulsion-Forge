package io.github.daniel366cobra.vs_marine_propulsion;

import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.foundation.item.ItemDescription;
import com.simibubi.create.foundation.item.KineticStats;
import com.simibubi.create.foundation.item.TooltipHelper;
import com.simibubi.create.foundation.item.TooltipModifier;
import com.tterrag.registrate.util.nullness.NonNullSupplier;
import io.github.daniel366cobra.vs_marine_propulsion.config.VSMarinePropulsionConfig;
import io.github.daniel366cobra.vs_marine_propulsion.creative_tabs.VSMarinePropulsionCreativeTab;
import io.github.daniel366cobra.vs_marine_propulsion.data.VSMarinePropulsionDatagen;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Temporary borrows:
 * Ship propeller model & texture - VS Tournament
 * Parts of the Freewheel clutch texture - Create Connected
 */

@Mod(VSMarinePropulsionMod.MOD_ID)
public class VSMarinePropulsionMod {
    public static final String MOD_ID = "vs_marine_propulsion";

    public static final String NAME = "VS Marine Propulsion";
    public static final Logger LOGGER = LoggerFactory.getLogger(NAME);
    public static final NonNullSupplier<CreateRegistrate> REGISTRATE = NonNullSupplier.lazy(() -> CreateRegistrate.create(VSMarinePropulsionMod.MOD_ID));


    static {
        REGISTRATE.get().setTooltipModifierFactory(
                item -> new ItemDescription.Modifier(item, TooltipHelper.Palette.STANDARD_CREATE)
                        .andThen(TooltipModifier.mapNull(KineticStats.create(item))));
    }

    public VSMarinePropulsionMod() {
        IEventBus eventBus = FMLJavaModLoadingContext.get().getModEventBus();

        LOGGER.info("{} initializing!", NAME);

        eventBus.addListener(EventPriority.LOWEST, VSMarinePropulsionDatagen::gatherData);
        eventBus.addListener(this::onCommonSetup);

        VSMarinePropulsionMod.REGISTRATE.get().registerEventListeners(eventBus);

        VSMarinePropulsionConfig.register(ModLoadingContext.get());
        VSMarinePropulsionCreativeTab.register(eventBus);
        VSMarinePropulsionBlocks.register();
        VSMarinePropulsionEntities.register();
        VSMarinePropulsionSounds.register(eventBus);
        VSMarinePropulsionPartialModels.init();
        VSMarinePropulsionContraptionTypes.init();

    }

    public void onCommonSetup(FMLCommonSetupEvent event) {
        VSMarinePropulsionPacketHandler.register();
    }


    public static ResourceLocation resourceLocationFromPath(String path) {
        return new ResourceLocation(MOD_ID, path);
    }

    public static CreateRegistrate getRegistrate() {
        return REGISTRATE.get();
    }
}
