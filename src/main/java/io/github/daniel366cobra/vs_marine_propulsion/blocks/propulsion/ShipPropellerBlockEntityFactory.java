package io.github.daniel366cobra.vs_marine_propulsion.blocks.propulsion;

import com.tterrag.registrate.builders.BlockEntityBuilder;
import io.github.daniel366cobra.vs_marine_propulsion.ship_control.PropellerThrustCurve;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class ShipPropellerBlockEntityFactory implements BlockEntityBuilder.BlockEntityFactory<ShipPropellerBlockEntity> {
    private final PropellerThrustCurve thrustCurve;

    public ShipPropellerBlockEntityFactory(PropellerThrustCurve thrustCurve) {
        this.thrustCurve = thrustCurve;
    }

    @Override
    public @NotNull ShipPropellerBlockEntity create(BlockEntityType<ShipPropellerBlockEntity> type, BlockPos pos, BlockState state) {
        ShipPropellerBlockEntity blockEntity = new ShipPropellerBlockEntity(type, pos, state, thrustCurve);
        return blockEntity;
    }
}
