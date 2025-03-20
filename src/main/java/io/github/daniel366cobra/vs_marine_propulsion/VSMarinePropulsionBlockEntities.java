package io.github.daniel366cobra.vs_marine_propulsion;

import com.simibubi.create.content.contraptions.bearing.BearingInstance;
import com.simibubi.create.content.contraptions.bearing.BearingRenderer;
import com.simibubi.create.content.kinetics.transmission.SplitShaftInstance;
import com.simibubi.create.content.kinetics.transmission.SplitShaftRenderer;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.util.entry.BlockEntityEntry;
import io.github.daniel366cobra.vs_marine_propulsion.blocks.control.engine_order_telegraph.EngineOrderTelegraphBlockEntity;
import io.github.daniel366cobra.vs_marine_propulsion.blocks.control.engine_order_telegraph.EngineOrderTelegraphBlockEntityRenderer;
import io.github.daniel366cobra.vs_marine_propulsion.blocks.control.engine_order_telegraph.EngineOrderTelegraphInstance;
import io.github.daniel366cobra.vs_marine_propulsion.blocks.drivetrain.variator.VariatorBlockEntity;
import io.github.daniel366cobra.vs_marine_propulsion.blocks.propulsion.*;
import io.github.daniel366cobra.vs_marine_propulsion.blocks.steering.RudderBearingBlockEntity;
import io.github.daniel366cobra.vs_marine_propulsion.ship_control.PropellerThrustCalculator;
import io.github.daniel366cobra.vs_marine_propulsion.ship_control.PropellerThrustCurve;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class VSMarinePropulsionBlockEntities {
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
            .instance(() -> BearingInstance::new, false)
            .validBlocks(VSMarinePropulsionBlocks.ENGINE_ORDER_TELEGRAPH)
            .renderer(() -> BearingRenderer::new)
            .register();


    public static void register() {
        VSMarinePropulsionMod.LOGGER.info("Registering block entities for " + VSMarinePropulsionMod.NAME);
    }
}
