package io.github.daniel366cobra.vs_marine_propulsion;

import com.simibubi.create.content.kinetics.transmission.SplitShaftInstance;
import com.simibubi.create.content.kinetics.transmission.SplitShaftRenderer;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.util.entry.BlockEntityEntry;
import io.github.daniel366cobra.vs_marine_propulsion.blocks.auxiliary.SeacockBlockEntity;
import io.github.daniel366cobra.vs_marine_propulsion.blocks.control.engine_order_telegraph.EngineOrderTelegraphBlockEntity;
import io.github.daniel366cobra.vs_marine_propulsion.blocks.control.engine_order_telegraph.EngineOrderTelegraphBlockEntityRenderer;
import io.github.daniel366cobra.vs_marine_propulsion.blocks.control.engine_order_telegraph.EngineOrderTelegraphInstance;
import io.github.daniel366cobra.vs_marine_propulsion.blocks.control.helm.HelmBlockEntity;
import io.github.daniel366cobra.vs_marine_propulsion.blocks.control.helm.HelmBlockEntityRenderer;
import io.github.daniel366cobra.vs_marine_propulsion.blocks.control.helm.HelmInstance;
import io.github.daniel366cobra.vs_marine_propulsion.blocks.drivetrain.variator.VariatorBlockEntity;
import io.github.daniel366cobra.vs_marine_propulsion.blocks.propulsion.ShipPropellerBlockEntity;
import io.github.daniel366cobra.vs_marine_propulsion.blocks.propulsion.ShipPropellerBlockEntityFactory;
import io.github.daniel366cobra.vs_marine_propulsion.blocks.propulsion.ShipPropellerBlockEntityRendererFactory;
import io.github.daniel366cobra.vs_marine_propulsion.blocks.propulsion.ShipPropellerInstanceFactory;
import io.github.daniel366cobra.vs_marine_propulsion.blocks.propulsion.utility.PropellerThrustCalculator;
import io.github.daniel366cobra.vs_marine_propulsion.blocks.steering.rudder_bearing.RudderBearingBlockEntity;
import io.github.daniel366cobra.vs_marine_propulsion.blocks.steering.rudder_bearing.RudderBearingBlockEntityRenderer;
import io.github.daniel366cobra.vs_marine_propulsion.blocks.steering.rudder_bearing.RudderBearingInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class VSMarinePropulsionEntities {
    private static final CreateRegistrate REGISTRATE = VSMarinePropulsionMod.getRegistrate();


    public static final BlockEntityEntry<ShipPropellerBlockEntity> LARGE_SHIP_PROPELLER_BLOCK_ENTITY = REGISTRATE
            .blockEntity("large_ship_propeller_entity", (BlockEntityType<ShipPropellerBlockEntity> type, BlockPos pos, BlockState state) ->
                    new ShipPropellerBlockEntityFactory(
                            new PropellerThrustCalculator(1.007f, 4, 3.0f, 22.5f))
                            .create(type, pos, state))
            .instance(() -> (materialManager, blockEntity) ->
                    new ShipPropellerInstanceFactory(VSMarinePropulsionPartialModels.LARGE_SHIP_PROPELLER_CLOCKWISE,
                                                    VSMarinePropulsionPartialModels.LARGE_SHIP_PROPELLER_COUNTERCLOCKWISE)
                            .apply(materialManager, blockEntity), false)
            .validBlocks(VSMarinePropulsionBlocks.LARGE_SHIP_PROPELLER)
            .renderer(() -> context ->
                    new ShipPropellerBlockEntityRendererFactory(VSMarinePropulsionPartialModels.LARGE_SHIP_PROPELLER_CLOCKWISE,
                                                                VSMarinePropulsionPartialModels.LARGE_SHIP_PROPELLER_COUNTERCLOCKWISE)
                            .apply(context))
            .register();

    public static final BlockEntityEntry<VariatorBlockEntity> VARIATOR_BLOCK_ENTITY = REGISTRATE
            .blockEntity("variator_entity", VariatorBlockEntity::new)
            .instance(() -> SplitShaftInstance::new, false)
            .validBlocks(VSMarinePropulsionBlocks.VARIATOR)
            .renderer(() -> SplitShaftRenderer::new)
            .register();

    public static final BlockEntityEntry<EngineOrderTelegraphBlockEntity> ENGINE_ORDER_TELEGRAPH_BLOCK_ENTITY = REGISTRATE
            .blockEntity("engine_order_telegraph_entity", EngineOrderTelegraphBlockEntity::new)
            .instance(() -> EngineOrderTelegraphInstance::new, false)
            .validBlocks(VSMarinePropulsionBlocks.ENGINE_ORDER_TELEGRAPH)
            .renderer(() -> EngineOrderTelegraphBlockEntityRenderer::new)
            .register();

    public static final BlockEntityEntry<RudderBearingBlockEntity> RUDDER_BEARING_BLOCK_ENTITY = REGISTRATE
            .blockEntity("rudder_bearing_entity", RudderBearingBlockEntity::new)
            .instance(() -> RudderBearingInstance::new, false)
            .validBlocks(VSMarinePropulsionBlocks.RUDDER_BEARING)
            .renderer(() -> RudderBearingBlockEntityRenderer::new)
            .register();

    public static final BlockEntityEntry<HelmBlockEntity> HELM_BLOCK_ENTITY = REGISTRATE
            .blockEntity("helm_entity", HelmBlockEntity::new)
            .instance(() -> HelmInstance::new, false)
            .validBlocks(VSMarinePropulsionBlocks.HELM)
            .renderer(() -> HelmBlockEntityRenderer::new)
            .register();

    public static final BlockEntityEntry<SeacockBlockEntity> SEACOCK_BLOCK_ENTITY = REGISTRATE
            .blockEntity("seacock_entity", SeacockBlockEntity::new)
            .validBlocks(VSMarinePropulsionBlocks.SEACOCK)
            .register();


    public static void register() {
        VSMarinePropulsionMod.LOGGER.info("Registering entities for " + VSMarinePropulsionMod.NAME);
    }
}
