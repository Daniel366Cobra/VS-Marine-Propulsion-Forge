package io.github.daniel366cobra.vs_marine_propulsion.blocks.steering;

import com.simibubi.create.content.contraptions.bearing.BearingBlock;
import com.simibubi.create.foundation.block.IBE;
import io.github.daniel366cobra.vs_marine_propulsion.VSMarinePropulsionBlockEntities;
import io.github.daniel366cobra.vs_marine_propulsion.VSMarinePropulsionMod;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class RudderBearingBlock extends BearingBlock implements IBE<RudderBearingBlockEntity> {

        public RudderBearingBlock(Properties properties) {
                super(properties);
        }

        @Override
        public InteractionResult use(BlockState state, Level worldIn, BlockPos pos, Player player, InteractionHand handIn, BlockHitResult hit) {
                if (!player.mayBuild())
                        return InteractionResult.FAIL;
                if (player.isShiftKeyDown())
                        return InteractionResult.FAIL;
                if (player.getItemInHand(handIn)
                        .isEmpty()) {
                        if (worldIn.isClientSide)
                                return InteractionResult.SUCCESS;
                        withBlockEntityDo(worldIn, pos, be -> {
                                if (be.isRunning()) {
                                        be.disassemble();
                                        return;
                                }
                                be.assemble();
                        });
                        return InteractionResult.SUCCESS;
                }
                return InteractionResult.PASS;
        }

        @Override
        public Class<RudderBearingBlockEntity> getBlockEntityClass() {
                return RudderBearingBlockEntity.class;
        }

        @Override
        public BlockEntityType<? extends RudderBearingBlockEntity> getBlockEntityType() {
                return VSMarinePropulsionBlockEntities.RUDDER_BEARING_BLOCK_ENTITY.get();
        }

}
