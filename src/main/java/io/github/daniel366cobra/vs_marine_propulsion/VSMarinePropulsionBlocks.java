package io.github.daniel366cobra.vs_marine_propulsion;

import com.simibubi.create.content.kinetics.BlockStressDefaults;
import com.simibubi.create.foundation.data.BlockStateGen;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.foundation.data.SharedProperties;
import com.simibubi.create.foundation.item.ItemDescription;
import com.tterrag.registrate.util.entry.BlockEntry;
import io.github.daniel366cobra.vs_marine_propulsion.blocks.auxiliary.SeacockBlock;
import io.github.daniel366cobra.vs_marine_propulsion.blocks.control.engine_order_telegraph.EngineOrderTelegraphBlock;
import io.github.daniel366cobra.vs_marine_propulsion.blocks.control.helm.HelmBlock;
import io.github.daniel366cobra.vs_marine_propulsion.blocks.drivetrain.variator.VariatorBlock;
import io.github.daniel366cobra.vs_marine_propulsion.blocks.propulsion.large_ship_propeller.LargeShipPropellerBlock;
import io.github.daniel366cobra.vs_marine_propulsion.blocks.steering.rudder.RudderBlock;
import io.github.daniel366cobra.vs_marine_propulsion.blocks.steering.rudder_bearing.RudderBearingBlock;
import io.github.daniel366cobra.vs_marine_propulsion.items.EngineOrderTelegraphBlockItem;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.material.MapColor;

import static com.simibubi.create.foundation.data.AssetLookup.partialBaseModel;
import static com.simibubi.create.foundation.data.TagGen.axeOrPickaxe;
import static com.simibubi.create.foundation.data.TagGen.pickaxeOnly;
import static io.github.daniel366cobra.vs_marine_propulsion.blocks.drivetrain.variator.VariatorBlock.LINKED;
import static io.github.daniel366cobra.vs_marine_propulsion.blocks.drivetrain.variator.VariatorBlock.ORDER;

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
            .transform(pickaxeOnly())
            .blockstate((ctx, prov ) -> prov.directionalBlock(ctx.get(), partialBaseModel(ctx, prov)))
            .lang("Large Ship Propeller")
            .onRegisterAfter(Registries.ITEM, v -> ItemDescription.useKey(v, "block." + VSMarinePropulsionMod.MOD_ID + ".large_ship_propeller"))
            .item()
            .model((ctx, prov) -> prov.withExistingParent(ctx.getName(), prov.modLoc("block/" + ctx.getName() + "/item")))
            .build()
            .register();

    public static final BlockEntry<VariatorBlock> VARIATOR = REGISTRATE
            .block("variator", VariatorBlock::new)
            .initialProperties(SharedProperties::stone)
            .properties(p -> p.noOcclusion().mapColor(MapColor.PODZOL))
            .addLayer(() -> RenderType::cutoutMipped)
            .transform(BlockStressDefaults.setNoImpact())
            .transform(axeOrPickaxe())
            .blockstate((ctx, prov) -> prov.directionalBlock(ctx.get(), state ->
                            partialBaseModel(ctx, prov, state.getValue(LINKED) ? "linked" : "unlinked", state.getValue(ORDER).toString())
                    )
            )
            .lang("Variator")
            .onRegisterAfter(Registries.ITEM, v -> ItemDescription.useKey(v, "block." + VSMarinePropulsionMod.MOD_ID + ".variator"))
            .item()
            .model((ctx, prov) -> prov.withExistingParent(ctx.getName(), prov.modLoc("block/" + ctx.getName() + "/item")))
            .build()
            .register();

    public static final BlockEntry<EngineOrderTelegraphBlock> ENGINE_ORDER_TELEGRAPH = REGISTRATE
            .block("engine_order_telegraph", EngineOrderTelegraphBlock::new)
            .initialProperties(SharedProperties::softMetal)
            .properties(p -> p
                    .noOcclusion()
                    .mapColor(MapColor.DEEPSLATE))
            .addLayer(() -> RenderType::cutoutMipped)
            .transform(pickaxeOnly())
            .blockstate((ctx, prov ) -> prov.horizontalFaceBlock(ctx.get(), partialBaseModel(ctx, prov)))
            .lang("Engine Order Telegraph")
            .onRegisterAfter(Registries.ITEM, v -> ItemDescription.useKey(v, "block." + VSMarinePropulsionMod.MOD_ID + ".engine_order_telegraph"))
            .item(EngineOrderTelegraphBlockItem::new)
            .model((ctx, prov) -> prov.withExistingParent(ctx.getName(), prov.modLoc("block/" + ctx.getName() + "/item")))
            .build()
            .register();

    public static final BlockEntry<RudderBearingBlock> RUDDER_BEARING = REGISTRATE
            .block("rudder_bearing", RudderBearingBlock::new)
            .initialProperties(SharedProperties::softMetal)
            .properties(p -> p
                    .noOcclusion()
                    .mapColor(MapColor.DEEPSLATE))
            .addLayer(() -> RenderType::cutoutMipped)
            .transform(pickaxeOnly())
            .blockstate((ctx, prov ) -> prov.directionalBlock(ctx.get(), partialBaseModel(ctx, prov)))
            .lang("Rudder Bearing")
            .onRegisterAfter(Registries.ITEM, v -> ItemDescription.useKey(v, "block." + VSMarinePropulsionMod.MOD_ID + ".rudder_bearing"))
            .item()
            .model((ctx, prov) -> prov.withExistingParent(ctx.getName(), prov.modLoc("block/" + ctx.getName() + "/item")))
            .build()
            .register();

    public static final BlockEntry<RudderBlock> RUDDER = REGISTRATE
            .block("rudder", RudderBlock::new)
            .initialProperties(SharedProperties::softMetal)
            .properties(p -> p
                    .noOcclusion()
                    .mapColor(MapColor.DEEPSLATE))
            .transform(pickaxeOnly())
            .blockstate(BlockStateGen.axisBlockProvider(false))
            .lang("Rudder")
            .onRegisterAfter(Registries.ITEM, v -> ItemDescription.useKey(v, "block." + VSMarinePropulsionMod.MOD_ID + ".rudder"))
            .simpleItem()
            .register();

    public static final BlockEntry<HelmBlock> HELM = REGISTRATE
            .block("helm", HelmBlock::new)
            .initialProperties(SharedProperties::wooden)
            .properties(p -> p
                    .noOcclusion()
                    .mapColor(MapColor.WOOD))
            .transform(axeOrPickaxe())
            .blockstate((ctx, prov ) -> prov.horizontalBlock(ctx.get(), partialBaseModel(ctx, prov)))
            .lang("Helm")
            .onRegisterAfter(Registries.ITEM, v -> ItemDescription.useKey(v, "block." + VSMarinePropulsionMod.MOD_ID + ".helm"))
            .item()
            .model((ctx, prov) -> prov.withExistingParent(ctx.getName(), prov.modLoc("block/" + ctx.getName() + "/item")))
            .build()
            .register();

    public static final BlockEntry<SeacockBlock> SEACOCK = REGISTRATE
            .block("seacock", SeacockBlock::new)
            .initialProperties(SharedProperties::softMetal)
            .transform(pickaxeOnly())
            .blockstate(BlockStateGen.directionalBlockProvider(false))
            .lang("Seacock")
            .onRegisterAfter(Registries.ITEM, v -> ItemDescription.useKey(v, "block." + VSMarinePropulsionMod.MOD_ID + ".seacock"))
            .simpleItem()
            .register();

    public static void register() {
        VSMarinePropulsionMod.LOGGER.info("Registering blocks for " + VSMarinePropulsionMod.NAME);
    }
}
