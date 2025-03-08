package io.github.daniel366cobra.vs_marine_propulsion;

import com.simibubi.create.content.kinetics.BlockStressDefaults;
import com.simibubi.create.foundation.data.AssetLookup;
import com.simibubi.create.foundation.data.BlockStateGen;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.foundation.data.SharedProperties;
import com.tterrag.registrate.util.entry.BlockEntry;
import io.github.daniel366cobra.vs_marine_propulsion.blocks.control.engine_order_telegraph.EngineOrderTelegraphBlock;
import io.github.daniel366cobra.vs_marine_propulsion.blocks.drivetrain.variator.VariatorBlock;
import io.github.daniel366cobra.vs_marine_propulsion.blocks.propulsion.large_ship_propeller.LargeShipPropellerBlock;
import io.github.daniel366cobra.vs_marine_propulsion.items.EngineOrderTelegraphBlockItem;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.material.MapColor;

import static com.simibubi.create.foundation.data.ModelGen.customItemModel;
import static com.simibubi.create.foundation.data.TagGen.axeOrPickaxe;

public class VSMarinePropulsionBlocks {

    private static final CreateRegistrate REGISTRATE = VSMarinePropulsionMod.getRegistrate();

    public static final BlockEntry<LargeShipPropellerBlock> LARGE_SHIP_PROPELLER = REGISTRATE
            .block("large_ship_propeller", LargeShipPropellerBlock::new)
            .initialProperties(SharedProperties::softMetal)
            .properties(p -> p
                    .noOcclusion()
                    .mapColor(MapColor.DEEPSLATE)
                    .strength(3.5f))
            .addLayer(() -> RenderType::cutoutMipped)
            .transform(BlockStressDefaults.setImpact(4.0f))
            .transform(axeOrPickaxe())
            .item()
            .transform(customItemModel())
            .register();

    public static final BlockEntry<VariatorBlock> VARIATOR = REGISTRATE
            .block("variator", VariatorBlock::new)
            .initialProperties(SharedProperties::stone)
            .properties(p -> p.noOcclusion().mapColor(MapColor.PODZOL))
            .addLayer(() -> RenderType::cutoutMipped)
            .transform(BlockStressDefaults.setNoImpact())
            .transform(axeOrPickaxe())
            .blockstate((c, p) -> BlockStateGen.axisBlock(c, p, AssetLookup.forPowered(c, p)))
            .item()
            .transform(customItemModel())
            .register();

    public static final BlockEntry<EngineOrderTelegraphBlock> ENGINE_ORDER_TELEGRAPH = REGISTRATE
            .block("engine_order_telegraph", EngineOrderTelegraphBlock::new)
            .initialProperties(SharedProperties::softMetal)
            .properties(p -> p
                    .noOcclusion()
                    .mapColor(MapColor.DEEPSLATE))
            .addLayer(() -> RenderType::cutoutMipped)
            .transform(axeOrPickaxe())
            .item(EngineOrderTelegraphBlockItem::new)
            .transform(customItemModel("_", "engine_order_telegraph"))
            .register();

    public static void register() {
        VSMarinePropulsionMod.LOGGER.info("Registering blocks for " + VSMarinePropulsionMod.NAME);
    }
}
