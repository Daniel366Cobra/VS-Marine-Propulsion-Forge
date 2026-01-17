package io.github.daniel366cobra.vs_marine_propulsion.items;

import com.simibubi.create.CreateClient;
import com.simibubi.create.foundation.utility.LangBuilder;
import io.github.daniel366cobra.vs_marine_propulsion.VSMarinePropulsionBlocks;
import io.github.daniel366cobra.vs_marine_propulsion.VSMarinePropulsionMod;
import io.github.daniel366cobra.vs_marine_propulsion.VSMarinePropulsionParticleTypes;
import io.github.daniel366cobra.vs_marine_propulsion.blocks.drivetrain.variator.VariatorBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;

public class EngineOrderTelegraphBlockItem extends BlockItem {

    public EngineOrderTelegraphBlockItem (Block block, Item.Properties properties){
        super(block, properties);
    }

    @SubscribeEvent
    public static void gathererItemAlwaysPlacesWhenUsed(PlayerInteractEvent.RightClickBlock event) {
        ItemStack usedItem = event.getItemStack();
        if (usedItem.getItem() instanceof EngineOrderTelegraphBlockItem) {
            if (VSMarinePropulsionBlocks.ENGINE_ORDER_TELEGRAPH.has(event.getLevel().getBlockState(event.getPos())))
                return;
            event.setUseBlock(Event.Result.DENY);
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext pContext) {
        LangBuilder langBuilder = new LangBuilder(VSMarinePropulsionMod.MOD_ID);
        ItemStack stack = pContext.getItemInHand();
        BlockPos pos = pContext.getClickedPos();
        Level level = pContext.getLevel();
        BlockState state = level.getBlockState(pos);
        Player player = pContext.getPlayer();

        boolean clickedVariator = VSMarinePropulsionBlocks.VARIATOR.has(state);

        if (player == null)
            return InteractionResult.FAIL;

        if (player.isShiftKeyDown() && stack.hasTag()) {
            if (level.isClientSide) return InteractionResult.SUCCESS;
            player.displayClientMessage(langBuilder.translate("engine_order_telegraph.clear").component()
                    .withStyle(ChatFormatting.WHITE), true);
            stack.setTag(null);
            return InteractionResult.SUCCESS;
        }

        if (clickedVariator) {
            if (level.isClientSide) return InteractionResult.SUCCESS;

            //reject already-connected variators
            VariatorBlockEntity vbe = (VariatorBlockEntity)level.getBlockEntity(pos);
            if (vbe.hasMasterTelegraph()) {
                player.displayClientMessage(langBuilder.translate("engine_order_telegraph.failure").component()
                        .withStyle(ChatFormatting.RED), true);
                return InteractionResult.SUCCESS;
            }

            CompoundTag stackTag = stack.getOrCreateTag();

            ListTag savedPositions = stackTag.getList("LinkedVariatorsPos", Tag.TAG_COMPOUND);

            boolean alreadySaved = false;

            for (Tag tag : savedPositions) {
                BlockPos savedPos = NbtUtils.readBlockPos((CompoundTag) tag);
                if (savedPos.equals(pos)) {
                    alreadySaved = true;
                    break;
                }
            }

            //remove if already on the list
            if (alreadySaved) {
                ListTag newSavedPositions = new ListTag();
                for (Tag tag : savedPositions) {
                    BlockPos savedPos = NbtUtils.readBlockPos((CompoundTag) tag);
                    if (!savedPos.equals(pos)) {
                        newSavedPositions.add(tag);
                    }
                }
                stackTag.put("LinkedVariatorsPos", newSavedPositions);
                player.displayClientMessage(langBuilder.translate("engine_order_telegraph.remove").component()
                        .withStyle(ChatFormatting.WHITE), true);
            }
            else //add if not on the list
            {
                savedPositions.add(NbtUtils.writeBlockPos(pos));
                stackTag.put("LinkedVariatorsPos", savedPositions);
                player.displayClientMessage(langBuilder.translate("engine_order_telegraph.add").component()
                        .withStyle(ChatFormatting.WHITE), true);

            }

            stack.setTag(stackTag);
            return InteractionResult.SUCCESS;

        }
        else //clicking on a non-controllable block
        {

            CompoundTag stackTag = stack.getTag();

            if (stackTag != null) {
                stackTag.put("BlockEntityTag", stackTag.copy());
            }

            InteractionResult useOn = super.useOn(pContext);
            if (level.isClientSide || useOn == InteractionResult.FAIL)
                return useOn;


            ItemStack itemInHand = player.getItemInHand(pContext.getHand());
            if (!itemInHand.isEmpty())
                itemInHand.setTag(null);
            player.displayClientMessage(langBuilder.translate("engine_order_telegraph.success").component()
                    .withStyle(ChatFormatting.GREEN), true);

            return useOn;
        }
    }

    private static List<BlockPos> lastShownPosList = new ArrayList<>();

    @OnlyIn(Dist.CLIENT)
    public static void clientTick() {
        Player player = Minecraft.getInstance().player;
        if (player == null)
            return;
        ItemStack heldItemMainhand = player.getMainHandItem();
        if (!(heldItemMainhand.getItem() instanceof EngineOrderTelegraphBlockItem))
            return;
        if (!heldItemMainhand.hasTag())
            return;
        CompoundTag stackTag = heldItemMainhand.getOrCreateTag();
        if (!stackTag.contains("LinkedVariatorsPos"))
            return;

        ListTag selectedPosListTag = stackTag.getList("LinkedVariatorsPos", Tag.TAG_COMPOUND);
        List<BlockPos> selectedPosList = new ArrayList<>();

        //a List of all selected BlockPos
        selectedPosListTag.forEach(tag -> selectedPosList
                .add(NbtUtils.readBlockPos((CompoundTag) tag)));

        if (!selectedPosList.equals(lastShownPosList)) {
            lastShownPosList = selectedPosList;
        }

        CreateClient.OUTLINER.showCluster("target", lastShownPosList).colored(0xffcb74).lineWidth(1 / 16f);
    }

    @OnlyIn(Dist.CLIENT)
    protected static AABB getBounds(BlockPos pos) {
        Level world = Minecraft.getInstance().level;

        BlockState state = world.getBlockState(pos);
        VoxelShape shape = state.getShape(world, pos);
        return shape.isEmpty() ? new AABB(BlockPos.ZERO)
                : shape.bounds()
                .move(pos);
    }
}
