package io.github.daniel366cobra.vs_marine_propulsion.blocks.auxiliary;

import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.block.WrenchableDirectionalBlock;
import io.github.daniel366cobra.vs_marine_propulsion.VSMarinePropulsionEntities;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class SeacockBlock extends WrenchableDirectionalBlock implements IBE<SeacockBlockEntity> {

    public SeacockBlock(Properties pProperties) {
        super(pProperties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.UP));
    }

    @Override
    public Class<SeacockBlockEntity> getBlockEntityClass() {
        return SeacockBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends SeacockBlockEntity> getBlockEntityType() {
        return VSMarinePropulsionEntities.SEACOCK_BLOCK_ENTITY.get();
    }

}
