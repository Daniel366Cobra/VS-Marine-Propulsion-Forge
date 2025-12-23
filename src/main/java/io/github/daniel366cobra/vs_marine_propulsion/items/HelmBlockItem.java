package io.github.daniel366cobra.vs_marine_propulsion.items;
import io.github.daniel366cobra.vs_marine_propulsion.ship.VSMarinePropulsionAttachment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.valkyrienskies.core.api.ships.LoadedServerShip;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

public class HelmBlockItem extends BlockItem {

    public HelmBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    /*@Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos clickedPos = context.getClickedPos(); // The block you clicked on
        BlockPos placementPos = clickedPos.relative(context.getClickedFace()); // Where block will be placed
        Player player = context.getPlayer();

        if (level.isClientSide) return InteractionResult.FAIL;
        // First line of defense: prevent invalid placement during normal item use

        // Check if we're clicking on a ship block or placing on a ship

        if (VSGameUtilsKt.isBlockInShipyard(level, clickedPos)
            || VSGameUtilsKt.isBlockInShipyard(level, placementPos)) {

            LoadedServerShip ship = VSGameUtilsKt.getShipObjectManagingPos((ServerLevel) level, clickedPos);

            if (ship != null) {

                VSMarinePropulsionAttachment shipControl = VSMarinePropulsionAttachment.getOrCreate(ship);

                Direction intendedFacing = context.getHorizontalDirection();

                // Check if there's already a captain
                Direction requiredDirection = shipControl.getCaptainDirection();

                if (requiredDirection != null) {
                    // There's a captain - validate facing
                    if (intendedFacing != requiredDirection) {
                        if (player != null) {
                            player.displayClientMessage(
                                    Component.translatable("vs_marine_propulsion.helm.mismatched_facing")
                                            .append(" " + requiredDirection),
                                    true
                            );
                        }
                        return InteractionResult.FAIL; // Prevent placement
                    }
                }
            }
        }

        return super.useOn(context);
    }*/
}