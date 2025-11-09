package io.github.daniel366cobra.vs_marine_propulsion.blocks.propulsion;

import com.tterrag.registrate.builders.BlockEntityBuilder;
import io.github.daniel366cobra.vs_marine_propulsion.blocks.propulsion.utility.PropellerThrustCalculator;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class ShipPropellerBlockEntityFactory implements BlockEntityBuilder.BlockEntityFactory<ShipPropellerBlockEntity> {
    private final PropellerThrustCalculator thrustCalculator;

    public ShipPropellerBlockEntityFactory(PropellerThrustCalculator thrustCalculator) {
        this.thrustCalculator = thrustCalculator;
    }

    @Override
    public @NotNull ShipPropellerBlockEntity create(BlockEntityType<ShipPropellerBlockEntity> type, BlockPos pos, BlockState state) {
        ShipPropellerBlockEntity blockEntity = new ShipPropellerBlockEntity(type, pos, state, thrustCalculator);
        return blockEntity;
    }
}
