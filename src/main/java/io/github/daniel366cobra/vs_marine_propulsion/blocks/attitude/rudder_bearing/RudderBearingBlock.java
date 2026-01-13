package io.github.daniel366cobra.vs_marine_propulsion.blocks.attitude.rudder_bearing;

import com.simibubi.create.content.contraptions.bearing.BearingBlock;
import com.simibubi.create.foundation.block.IBE;
import io.github.daniel366cobra.vs_marine_propulsion.VSMarinePropulsionEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
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
                if (player.getItemInHand(handIn).isEmpty()) {
                        if (!worldIn.isClientSide) {

                                withBlockEntityDo(worldIn, pos, be -> {
                                        if (be.running) {
                                                be.disassemble();
                                                return;
                                        }
                                        be.assembleNextTick = true;
                                });
                        }
                        return InteractionResult.SUCCESS;
                }
                return InteractionResult.PASS;
        }

        @Override
        public InteractionResult onWrenched(BlockState state, UseOnContext context) {
                InteractionResult resultType = super.onWrenched(state, context);
                if (!context.getLevel().isClientSide && resultType.consumesAction())
                        withBlockEntityDo(context.getLevel(), context.getClickedPos(), RudderBearingBlockEntity::disassemble);
                return resultType;
        }

        @Override
        public Class<RudderBearingBlockEntity> getBlockEntityClass() {
                return RudderBearingBlockEntity.class;
        }

        @Override
        public BlockEntityType<? extends RudderBearingBlockEntity> getBlockEntityType() {
                return VSMarinePropulsionEntities.RUDDER_BEARING_BLOCK_ENTITY.get();
        }

}
