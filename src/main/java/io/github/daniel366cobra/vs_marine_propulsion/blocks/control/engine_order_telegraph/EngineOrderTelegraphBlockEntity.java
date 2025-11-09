package io.github.daniel366cobra.vs_marine_propulsion.blocks.control.engine_order_telegraph;

import com.simibubi.create.content.equipment.goggles.IHaveHoveringInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.utility.LangBuilder;
import com.simibubi.create.foundation.utility.animation.LerpedFloat;
import io.github.daniel366cobra.vs_marine_propulsion.VSMarinePropulsionMod;
import io.github.daniel366cobra.vs_marine_propulsion.VSMarinePropulsionSounds;
import io.github.daniel366cobra.vs_marine_propulsion.blocks.drivetrain.variator.VariatorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;
import java.util.stream.Collectors;

public class EngineOrderTelegraphBlockEntity extends SmartBlockEntity implements IHaveHoveringInformation {

    int throttleOrder = 0;
    int changeTimer;
    boolean variatorsRelinkNeeded = true;
    LerpedFloat clientLeverState;

    private Set<BlockPos> linkedVariatorBlockPos = new HashSet<>();

    public EngineOrderTelegraphBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        clientLeverState = LerpedFloat.linear();
    }

    @Override
    public void write(CompoundTag tag, boolean clientPacket) {
        //VSMarinePropulsionMod.LOGGER.info("Writing EOT Block Entity!");

        super.write(tag, clientPacket);

        tag.putInt("ThrottleOrder", throttleOrder);
        tag.putInt("ChangeTimer", changeTimer);

        ListTag linkedVariatorsTag = new ListTag();
        linkedVariatorBlockPos.forEach(blockPos -> linkedVariatorsTag.add(NbtUtils.writeBlockPos(blockPos)));
        tag.put("LinkedVariatorsPos", linkedVariatorsTag);
    }

    @Override
    protected void read(CompoundTag tag, boolean clientPacket) {
        //VSMarinePropulsionMod.LOGGER.info("Reading EOT Block Entity!");
        super.read(tag, clientPacket);
        throttleOrder = tag.getInt("ThrottleOrder");
        changeTimer = tag.getInt("ChangeTimer");

        ListTag linkedVariatorsTag = tag.getList("LinkedVariatorsPos", Tag.TAG_COMPOUND);

        linkedVariatorsTag.forEach(variatorTag -> linkedVariatorBlockPos
                .add(NbtUtils.readBlockPos((CompoundTag) variatorTag)));
        variatorsRelinkNeeded = true;
        clientLeverState.chase(throttleOrder, 0.2f, LerpedFloat.Chaser.EXP);

    }

    @Override
    public void tick() {
        super.tick();

        if (changeTimer >= 0) changeTimer--;

        if (changeTimer == 0 || variatorsRelinkNeeded) {
            updateVariators(variatorsRelinkNeeded);
            level.playSound(null, worldPosition, VSMarinePropulsionSounds.ENGINE_ORDER_TELEGRAPH_BELL.get(), SoundSource.BLOCKS, 0.2F, 1.0f);
        }

        if (level.isClientSide)
            clientLeverState.tickChaser();
    }

    @Override
    public void initialize() {
        super.initialize();
    }

    private void updateVariators(boolean relinkNeeded) {

        if (level == null || !level.isLoaded(this.worldPosition)) return;

        if (level.isClientSide()) return;

        if (relinkNeeded) {
            linkedVariatorBlockPos.forEach(blockPos -> {
                if (getLevel().getBlockEntity(blockPos) instanceof VariatorBlockEntity vbe) {
                    vbe.setMasterTelegraph(this.getBlockPos());
                }
            });
            variatorsRelinkNeeded = false;
        }

        linkedVariatorBlockPos
                .stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toSet())
                .forEach(blockPos ->
                {
                    VariatorBlockEntity vbe = (VariatorBlockEntity) level.getBlockEntity(blockPos);
                    //VSMarinePropulsionMod.LOGGER.info("Updating throttle on Variator at " + blockPos.toShortString());
                    vbe.setThrottleOrder(this.throttleOrder);
                });

    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }

    public void changeState(boolean back) {
        int prevState = throttleOrder;
        throttleOrder += back ? -1 : 1;
        throttleOrder = Mth.clamp(throttleOrder, -3, 3);
        if (prevState != throttleOrder) {
            changeTimer = 15;
            level.playSound(null, getBlockPos(), VSMarinePropulsionSounds.ENGINE_ORDER_TELEGRAPH_DING.get(), SoundSource.BLOCKS, 1.0f, 1.0f);
        }

        notifyUpdate();
    }

    @Override
    public void remove() {
        super.remove();

        if (this.getLevel().isClientSide()) return;
        if (this.linkedVariatorBlockPos.isEmpty()) return;

        List<VariatorBlockEntity> linkedVariatorBlockEntities = new ArrayList<>();

        linkedVariatorBlockPos
                .stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toSet())
                .forEach(blockPos -> {
                    VariatorBlockEntity vbe = (VariatorBlockEntity) level.getBlockEntity(blockPos);
                    vbe.removeMasterTelegraph();
                });
    }

    public void unlinkVariatorAt(BlockPos blockPos) {
        //VSMarinePropulsionMod.LOGGER.info("Unlinking Variator at: " + blockPos.toShortString());
        this.linkedVariatorBlockPos.remove(blockPos);
    }

    @Override
    public boolean addToTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        String modId = VSMarinePropulsionMod.MOD_ID;
        LangBuilder langBuilder = new LangBuilder(modId);

        langBuilder.add(Component.translatable("block." + modId + ".engine_order_telegraph.tooltip.current_order"))
                .add(Component.translatable(EngineOrder.throttleDescription(this.throttleOrder)))
                .forGoggles(tooltip);

        return true;
    }

    public int getThrottleOrder() {
        return throttleOrder;
    }

    public enum EngineOrder {
        FULL_ASTERN(-3, "full_astern"),
        HALF_ASTERN(-2, "half_astern"),
        SLOW_ASTERN(-1, "slow_astern"),
        STOP(0, "stop"),
        SLOW_AHEAD(1, "slow_ahead"),
        HALF_AHEAD(2, "half_ahead"),
        FULL_AHEAD(3, "full_ahead");

        private final int orderThrottle;
        private final String orderDescription;

        EngineOrder(int orderThrottle, String orderDescription) {
            this.orderThrottle = orderThrottle;
            this.orderDescription = orderDescription;
        }

        public int getThrottle() {
            return this.orderThrottle;
        }

        public static String throttleDescription(int order) {
            String modId = VSMarinePropulsionMod.MOD_ID;
            for (EngineOrder eo : EngineOrder.values()) {
                if (eo.orderThrottle == order) {
                    return "block." + modId + ".engine_order_telegraph.tooltip." + eo.orderDescription;
                }
            }
            throw new IllegalArgumentException("Invalid engine order: " + order);
        }
    }

}
