package io.github.daniel366cobra.vs_marine_propulsion.blocks.control.helm;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.utility.animation.LerpedFloat;
import io.github.daniel366cobra.vs_marine_propulsion.VSMarinePropulsionPacketHandler;
import io.github.daniel366cobra.vs_marine_propulsion.network.WheelAnglePacket;
import io.github.daniel366cobra.vs_marine_propulsion.ship.VSMarinePropulsionAttachment;
import io.github.daniel366cobra.vs_marine_propulsion.ship.data.HelmData;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.valkyrienskies.core.api.ships.LoadedServerShip;
import org.valkyrienskies.mod.api.SeatedControllingPlayer;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.ValkyrienSkiesMod;
import org.valkyrienskies.mod.common.entity.ShipMountingEntity;

import java.util.ArrayList;
import java.util.List;

public class HelmBlockEntity extends SmartBlockEntity {

    private HelmData helmData;
    public static int wheelInterval;
    private List<ShipMountingEntity> seats = new ArrayList<>();

    public LerpedFloat clientWheelAngle;
    public int wheelAngle;
    public static int maxAngle;

    public HelmBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
        wheelAngle = 360;
        maxAngle = 720;
        wheelInterval = 5;
        clientWheelAngle = LerpedFloat.angular();
        clientWheelAngle.setValue(360f);
        helmData = new HelmData(pos, blockState.getValue(HelmBlock.FACING), false);
    }

    @Override
    public void initialize() {
        super.initialize();
        // Sync with helm attachment when block entity loads
        syncWithAttachment();
    }

    /**
     * Sync this block entity with the ship attachment data
     */
    private void syncWithAttachment() {

        if (level == null || level.isClientSide()) return;

        VSMarinePropulsionAttachment shipControl = VSMarinePropulsionAttachment.get(level, worldPosition);
        if (shipControl != null) {
            HelmData existingData = shipControl.getHelmAtPos(worldPosition);
            if (existingData != null) {
                //Pull from attachment
                this.helmData = existingData;
                this.wheelAngle = 360 + (int) (existingData.rudderAngle * 9f);
                this.clientWheelAngle.chase(wheelAngle, 0.2f, LerpedFloat.Chaser.EXP);
            } else {
                shipControl.addHelm(worldPosition, this.helmData);
                // Get the actual object from attachment
                HelmData actualData = shipControl.getHelmAtPos(worldPosition);
                if (actualData != null) {
                    this.helmData = actualData;
                }
            }
        }
    }


    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }

    public boolean startRiding(Player player, boolean force, BlockPos pos, BlockState state, ServerLevel world) {
        // Clean up unused seats
        cleanupSeats();

        ShipMountingEntity seat = spawnSeat(pos, state, world);
        boolean ride = player.startRiding(seat, force);

        if (ride) {
            seats.add(seat);
        } else {
            seat.kill();
        }

        return ride;
    }

    ShipMountingEntity spawnSeat(BlockPos pos, BlockState state, ServerLevel world) {
        Direction facing = state.getValue(HelmBlock.FACING);

        Vector3dc mounterPos;
        if (facing == Direction.NORTH) {
            mounterPos = new Vector3d(pos.getX() + 0.5, pos.getY() + 0.125, pos.getZ() + 1.3125);
        } else if (facing == Direction.SOUTH) {
            mounterPos = new Vector3d(pos.getX() + 0.5, pos.getY() + 0.125, pos.getZ() - 0.3125);
        } else if (facing == Direction.EAST) {
            mounterPos = new Vector3d(pos.getX() - 0.3125, pos.getY() + 0.125, pos.getZ() + .5);
        } else { // WEST
            mounterPos = new Vector3d(pos.getX() + 1.3125, pos.getY() + 0.125, pos.getZ() + .5);
        }

        ShipMountingEntity seatEntity = ValkyrienSkiesMod.SHIP_MOUNTING_ENTITY_TYPE.create(world);
        assert seatEntity != null;
        seatEntity.setPos(mounterPos.x(), mounterPos.y(), mounterPos.z());
        seatEntity.lookAt(EntityAnchorArgument.Anchor.EYES, new Vec3(pos.getX(), pos.getY(), pos.getZ()));
        seatEntity.move(MoverType.SELF, new Vec3(0, 0, 0));
        seatEntity.setController(true);
        world.addFreshEntityWithPassengers(seatEntity);
        return seatEntity;
    }

    public boolean sit(Player player) {
        return startRiding(player, false, this.getBlockPos(), this.getBlockState(), (ServerLevel) level);
    }

    @Override
    public void tick() {
        super.tick();

        if (helmData == null) return;

        Level level = this.getLevel();
        BlockPos blockPos = this.getBlockPos();

        if (!VSGameUtilsKt.isBlockInShipyard(level, blockPos)) return;

        //syncWithAttachment();

        if (!level.isClientSide && !isVirtual()) {

            VSMarinePropulsionAttachment shipControl = VSMarinePropulsionAttachment.get(level, blockPos);
            if (shipControl == null) return;

            LoadedServerShip ship = VSGameUtilsKt.getShipObjectManagingPos((ServerLevel) level, blockPos);
            if (ship == null) return;

            SeatedControllingPlayer playerControl = ship.getAttachment(SeatedControllingPlayer.class);
            HelmData persistentData = shipControl.getHelmAtPos(shipControl.getCaptainHelmPosition());

            if (playerControl != null) {
                if (playerControl.getLeftImpulse() < 0) {
                    this.rotateWheelRight(getBlockState(), (ServerLevel) level, blockPos);
                } else if (playerControl.getLeftImpulse() > 0) {
                    this.rotateWheelLeft(getBlockState(), (ServerLevel) level, blockPos);
                }
            }

            if (persistentData != null) {
                persistentData.rudderAngle = this.getRudderAngle();
                this.helmData = persistentData;

                /*
                Player nearbyPlayer = level.getNearestPlayer(blockPos.getX(), blockPos.getY(), blockPos.getZ(), 10, false);

                nearbyPlayer.displayClientMessage(
                        Component.literal("NEW ANGLE: " + persistentData.rudderAngle),
                        true
                );
                */
            }

            notifyUpdate();

        } else {
            clientWheelAngle.tickChaser();
        }
    }

    @Override
    public void remove() {
        if (level != null && !level.isClientSide) {

            cleanupAttachment();
            cleanupSeats();
        }
        super.remove();
    }

    private void cleanupAttachment() {
        VSMarinePropulsionAttachment shipControl = VSMarinePropulsionAttachment.get(this.getLevel(), this.getBlockPos());
        if (shipControl != null) {
            shipControl.removeHelm(this.getBlockPos());
        }
    }


    private void cleanupSeats() {
        seats.forEach(mountingEntity -> {
            mountingEntity.ejectPassengers();
            mountingEntity.kill();
        });
        seats.clear();
    }

    public boolean rotateWheelRight(BlockState state, ServerLevel world, BlockPos pos) {
        boolean success = false;
        if (wheelAngle - wheelInterval >= 0) {
            wheelAngle -= wheelInterval;
            playWheelSounds(world, pos);
            success = true;
            //clientWheelAngle.chase(wheelAngle, 0.2f, LerpedFloat.Chaser.EXP);
        }

        VSMarinePropulsionPacketHandler.CHANNEL.send(
                PacketDistributor.TRACKING_CHUNK.with(() -> level.getChunkAt(pos)),
                new WheelAnglePacket(wheelAngle, pos)
        );

        //notifyUpdate();
        return success;
    }

    public boolean rotateWheelLeft(BlockState state, ServerLevel world, BlockPos pos) {
        boolean success = false;
        if (wheelAngle + wheelInterval <= 720) {
            wheelAngle += wheelInterval;
            playWheelSounds(world, pos);
            success = true;
            //clientWheelAngle.chase(wheelAngle, 0.2f, LerpedFloat.Chaser.EXP);
        }

        VSMarinePropulsionPacketHandler.CHANNEL.send(
                PacketDistributor.TRACKING_CHUNK.with(() -> level.getChunkAt(pos)),
                new WheelAnglePacket(wheelAngle, pos)
        );

        //notifyUpdate();
        return success;
    }

    public float getRudderAngle() {
        return (wheelAngle - 360f) / 9f;
    }

    private void playWheelSounds(Level world, BlockPos pos) {
        if ((double) wheelAngle / maxAngle == 0.5) {
            //Helm dead center
            world.playSound(null, pos.below(), SoundEvents.BAMBOO_WOOD_BUTTON_CLICK_ON,
                    SoundSource.BLOCKS, 1.5f, world.getRandom().nextFloat() * 0.1F + 0.9F);
        } else if (wheelAngle == maxAngle || wheelAngle == 0) {
            //Helm angle limit
            world.playSound(null, pos.below(), SoundEvents.WOODEN_TRAPDOOR_CLOSE,
                    SoundSource.BLOCKS, 1.5f, world.getRandom().nextFloat() * 0.1F + 0.9F);
        }
        if (wheelAngle % 20 == 0) {
            world.playSound(null, pos.below(), SoundEvents.WOODEN_BUTTON_CLICK_ON,
                    SoundSource.BLOCKS, 0.5f, world.getRandom().nextFloat() * 0.2F + 0.8F);//Helm step click
        }
    }

    @Override
    protected void read(CompoundTag tag, boolean clientPacket) {
        super.read(tag, clientPacket);
        wheelAngle = tag.getInt("WheelAngle");
        clientWheelAngle.chase(wheelAngle, 0.2f, LerpedFloat.Chaser.EXP);
    }

    @Override
    public void write(CompoundTag tag, boolean clientPacket) {
        super.write(tag, clientPacket);
        tag.putInt("WheelAngle", wheelAngle);
    }

    public int getWheelAngle() {
        return wheelAngle;
    }

    public float getRenderWheelAngle(float partialTicks) {
        if (level != null && level.isClientSide()) {
            return clientWheelAngle.getValue(partialTicks);
        }
        return wheelAngle;
    }

    public boolean isCaptain() {
        return this.helmData.isCaptain();
    }
}