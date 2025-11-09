package io.github.daniel366cobra.vs_marine_propulsion.blocks.control.helm;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.utility.animation.LerpedFloat;
import io.github.daniel366cobra.vs_marine_propulsion.VSMarinePropulsionPacketHandler;
import io.github.daniel366cobra.vs_marine_propulsion.blocks.control.utility.HelmAttachment;
import io.github.daniel366cobra.vs_marine_propulsion.network.WheelAnglePacket;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.valkyrienskies.core.api.ships.LoadedServerShip;
import org.valkyrienskies.mod.api.SeatedControllingPlayer;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.ValkyrienSkiesMod;
import org.valkyrienskies.mod.common.entity.ShipMountingEntity;

import java.util.ArrayList;
import java.util.List;

public class HelmBlockEntity extends SmartBlockEntity {

    public static final Logger LOGGER = LoggerFactory.getLogger("base_helm_entity");

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
    }

    @Override
    public void initialize() {
        super.initialize();
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }

    public boolean startRiding(Player player, boolean force, BlockPos pos, BlockState state, ServerLevel world) {

        for (int i = seats.size() - 1; i > 0; i--) {
            if (!seats.get(i).isPassenger()) {
                seats.get(i).kill();
                seats.remove(i);
            } else if (!seats.get(i).isAlive()) {
                seats.remove(i);
            }
        }

        ShipMountingEntity seat = spawnSeat(pos, state, world);

        boolean ride = player.startRiding(seat, force);

        if (ride) seats.add(seat);

        return ride;
    }

    ShipMountingEntity spawnSeat(BlockPos pos, BlockState state, ServerLevel world) {
        // USE THE CORRECT FACING PROPERTY
        Direction facing = state.getValue(HelmBlock.FACING);
        BlockPos newPos = pos.relative(facing.getOpposite());

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

        Level level = this.getLevel();
        BlockPos blockPos = this.getBlockPos();
        BlockState blockState = level.getBlockState(blockPos);

        if (!level.isClientSide) {

            if (VSGameUtilsKt.isBlockInShipyard(level, blockPos)) {
                ChunkPos chunkPos = level.getChunk(blockPos).getPos();
                LoadedServerShip ship = VSGameUtilsKt.getShipObjectManagingPos((ServerLevel) level, chunkPos);

                if (ship != null) {
                    SeatedControllingPlayer playerControl = ship.getAttachment(SeatedControllingPlayer.class);

                    BlockEntity be = level.getBlockEntity(blockPos);
                    if (be instanceof HelmBlockEntity blockEntity) {
                        if (playerControl != null) {
                            if (playerControl.getLeftImpulse() < 0) {
                                blockEntity.rotateWheelRight(blockState, (ServerLevel)level, blockPos);
                            } else if (playerControl.getLeftImpulse() > 0) {
                                blockEntity.rotateWheelLeft(blockState, (ServerLevel)level, blockPos);
                            }
                        }

                        //Matrix3dc moiTensor = ship.getInertiaData().getMomentOfInertiaTensor();

                    }
                    notifyUpdate();
                }
            }
        }
        else
        {
            clientWheelAngle.tickChaser();
        }
    }

    @Override
    public void remove() {

        assert level != null;

        if (!level.isClientSide) {

            HelmAttachment helmAttachment = HelmAttachment.get(this.getLevel(), this.getBlockPos());
            if (helmAttachment != null)
                helmAttachment.removeHelm(this.getBlockPos());


            seats.forEach((mountingEntity) -> {
                mountingEntity.ejectPassengers();
                mountingEntity.kill();
            });

            seats.clear();

        }
        super.remove();
    }

    public float getRenderWheelAngle(float partialTicks) {
        if (level != null && level.isClientSide()) {
            // Use LerpedFloat for smooth rendering
            return clientWheelAngle.getValue(partialTicks);
        }
        return wheelAngle; // Fallback
    }


    public boolean rotateWheelRight(BlockState state, ServerLevel world, BlockPos pos) {
        boolean success = false;
        if (wheelAngle-wheelInterval >= 0) {
            wheelAngle-=wheelInterval;
            playWheelSounds(world, pos);
            success = true;

            clientWheelAngle.chase(wheelAngle, 0.2f, LerpedFloat.Chaser.EXP);
        }

        VSMarinePropulsionPacketHandler.CHANNEL.send(
                PacketDistributor.TRACKING_CHUNK.with(() -> level.getChunkAt(pos)),
                new WheelAnglePacket(wheelAngle, pos)
        );

        notifyUpdate();
        return success;
    }

    public boolean rotateWheelLeft(BlockState state, ServerLevel world, BlockPos pos) {
        boolean success = false;
        if (wheelAngle+wheelInterval <= 720) {
            wheelAngle+=wheelInterval;
            playWheelSounds(world, pos);
            success = true;

            clientWheelAngle.chase(wheelAngle, 0.2f, LerpedFloat.Chaser.EXP);
        }

        VSMarinePropulsionPacketHandler.CHANNEL.send(
                PacketDistributor.TRACKING_CHUNK.with(() -> level.getChunkAt(pos)),
                new WheelAnglePacket(wheelAngle, pos)
        );

        notifyUpdate();
        return success;
    }

    public float getRudderAngle() {
        // Map wheel angle (0-720) to rudder angle (-40° to +40°)
        // 360 = neutral (0° rudder)
        // 0 = full left (-40° rudder)
        // 720 = full right (+40° rudder)
        return (wheelAngle - 360f) / 9f; // 360° wheel range = 40° rudder range
    }

    private void playWheelSounds(Level world, BlockPos pos) {
        if ((double)wheelAngle/ HelmBlockEntity.maxAngle == 0.5) {
            world.playSound(null, pos.below(), SoundEvents.BAMBOO_WOOD_BUTTON_CLICK_ON,
                    SoundSource.BLOCKS, 1.5f, world.getRandom().nextFloat() * 0.1F + 0.9F);
            world.playSound(null, pos.below(), SoundEvents.ARMOR_EQUIP_CHAIN,
                    SoundSource.BLOCKS, 0.6f, world.getRandom().nextFloat() * 0.1F + 0.9F);
        } else if (wheelAngle == HelmBlockEntity.maxAngle || wheelAngle == 0) {
            world.playSound(null, pos.below(), SoundEvents.BAMBOO_WOOD_BUTTON_CLICK_ON,
                    SoundSource.BLOCKS, 1.5f, world.getRandom().nextFloat() * 0.1F + 0.9F);
        }
    }

    @Override
    protected void read(CompoundTag tag, boolean clientPacket) {
        //VSMarinePropulsionMod.LOGGER.info("Reading EOT Block Entity!");
        super.read(tag, clientPacket);
        wheelAngle = tag.getInt("WheelAngle");

        clientWheelAngle.chase(wheelAngle, 0.2f, LerpedFloat.Chaser.EXP);

    }

    @Override
    public void write(CompoundTag tag, boolean clientPacket) {
        //VSMarinePropulsionMod.LOGGER.info("Writing EOT Block Entity!");
        super.write(tag, clientPacket);

        tag.putInt("WheelAngle", wheelAngle);
    }

    public int getWheelAngle() {
        return wheelAngle;
    }

}