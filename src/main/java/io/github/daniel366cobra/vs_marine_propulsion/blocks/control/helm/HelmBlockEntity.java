package io.github.daniel366cobra.vs_marine_propulsion.blocks.control.helm;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.utility.animation.LerpedFloat;
import io.github.daniel366cobra.vs_marine_propulsion.VSMarinePropulsionPacketHandler;
import io.github.daniel366cobra.vs_marine_propulsion.VSMarinePropulsionSounds;
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
        if (!canPlayerUseHelm(player)) {
            return false;
        }
        return startRiding(player, false, this.getBlockPos(), this.getBlockState(), (ServerLevel) level);
    }

    private void updateBlockFacing(Direction newFacing) {
        if (level == null || level.isClientSide) return;

        BlockState currentState = getBlockState();
        Direction currentFacing = currentState.getValue(HelmBlock.FACING);

        if (currentFacing != newFacing) {
            level.setBlock(worldPosition, currentState.setValue(HelmBlock.FACING, newFacing), 3);
            setChanged();
        }
    }

    /**
     * Sync this block entity with the ship attachment data
     */
    private void syncWithAttachment() {

        if (level == null || level.isClientSide()) return;

        VSMarinePropulsionAttachment shipControl = VSMarinePropulsionAttachment.get(level, worldPosition);
        if (shipControl == null) return;

        // Get or create helm data in attachment
        HelmData existingData = shipControl.getHelmAtPos(worldPosition);

        boolean wasPromoted = false;

        if (existingData == null) {
            // New helm added

            boolean isFirstHelm = !shipControl.hasValidOrientation();

            if (!isFirstHelm) {
                // Ship already has captain helm
                Direction requiredFacing = shipControl.getShipForwardDirection();
                Direction currentFacing = getBlockState().getValue(HelmBlock.FACING);

                if (currentFacing != requiredFacing) {
                    updateBlockFacing(requiredFacing);
                }

                // Create non-captain helm data with correct facing
                this.helmData = new HelmData(worldPosition, requiredFacing, false);

            }

            // Handles captaincy automatically for first helm
            shipControl.addHelm(worldPosition, this.helmData);

            // Will have isCaptain == true for first helm
            // Local this.helmData.isCaptain will stay false yet
            existingData = shipControl.getHelmAtPos(worldPosition);

            if (existingData != null && existingData.isCaptain) {
                // Play sound
                level.playSound(null, getBlockPos(), VSMarinePropulsionSounds.HELM_PROMOTION.get(),
                        SoundSource.BLOCKS, 0.5f, 1.0f);

                // Show message
                if (isFirstHelm) {
                    sendMessageToClosestPlayer(Component.translatable("vs_marine_propulsion.helm.set_captain")
                            .append(" " + existingData.getFacing()));
                } else {
                    sendMessageToClosestPlayer(Component.translatable("vs_marine_propulsion.helm.update_captain")
                            .append(" " + existingData.getFacing()));
                }
            }

        } else {
            // Existing helm updated
            if (!this.helmData.isCaptain && existingData.isCaptain) {
                // Play sound
                level.playSound(null, getBlockPos(), VSMarinePropulsionSounds.HELM_PROMOTION.get(),
                        SoundSource.BLOCKS, 0.5f, 1.0f);

                // Show message (helm was promoted due to captain removal)
                sendMessageToClosestPlayer(Component.translatable("vs_marine_propulsion.helm.update_captain")
                        .append(" " + existingData.getFacing()));
            }
        }

        if (existingData != null) {
            // Sync data
            this.helmData = existingData;
            this.wheelAngle = 360 + (int) (existingData.rudderAngle * 9f);
            this.clientWheelAngle.chase(wheelAngle, 0.2f, LerpedFloat.Chaser.EXP);
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (helmData == null) return;

        Level level = this.getLevel();
        BlockPos blockPos = this.getBlockPos();

        if (!VSGameUtilsKt.isBlockInShipyard(level, blockPos)) return;

        if (!level.isClientSide && !isVirtual()) {

            syncWithAttachment();

            VSMarinePropulsionAttachment shipControl = VSMarinePropulsionAttachment.get(level, blockPos);
            if (shipControl == null) return;

            LoadedServerShip ship = VSGameUtilsKt.getShipObjectManagingPos((ServerLevel) level, blockPos);
            if (ship == null) return;

            SeatedControllingPlayer playerControl = ship.getAttachment(SeatedControllingPlayer.class);

            if (playerControl != null) {
                if (playerControl.getLeftImpulse() < 0) {
                    this.rotateWheelRight((ServerLevel) level, blockPos);
                } else if (playerControl.getLeftImpulse() > 0) {
                    this.rotateWheelLeft((ServerLevel) level, blockPos);
                }
            }

            HelmData persistentData = shipControl.getHelmAtPos(blockPos);

            if (persistentData != null) {
                persistentData.rudderAngle = this.getRudderAngle();
                // Ensure our local reference is up-to-date
                this.helmData = persistentData;
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

    private boolean canPlayerUseHelm(Player player) {
        if (level == null || level.isClientSide) return false;

        VSMarinePropulsionAttachment shipControl = VSMarinePropulsionAttachment.get(level, worldPosition);
        if (shipControl == null) {
            sendMessageToClosestPlayer(Component.translatable("vs_marine_propulsion.helm.no_ship_found"));
            return false;
        }

        return true;
    }

    private void sendMessageToClosestPlayer(Component message) {
        BlockPos blockPos = this.getBlockPos();
        Player nearbyPlayer = level.getNearestPlayer(blockPos.getX(), blockPos.getY(), blockPos.getZ(), 10, false);
        if (nearbyPlayer != null) {
            nearbyPlayer.displayClientMessage(message,true);
        }
    }

    public void rotateWheelRight(ServerLevel world, BlockPos pos) {
        if (wheelAngle - wheelInterval >= 0) {
            wheelAngle -= wheelInterval;
            playWheelSounds(world, pos);
        }

        VSMarinePropulsionPacketHandler.CHANNEL.send(
                PacketDistributor.TRACKING_CHUNK.with(() -> level.getChunkAt(pos)),
                new WheelAnglePacket(wheelAngle, pos)
        );
    }

    public void rotateWheelLeft(ServerLevel world, BlockPos pos) {
        if (wheelAngle + wheelInterval <= 720) {
            wheelAngle += wheelInterval;
            playWheelSounds(world, pos);
        }

        VSMarinePropulsionPacketHandler.CHANNEL.send(
                PacketDistributor.TRACKING_CHUNK.with(() -> level.getChunkAt(pos)),
                new WheelAnglePacket(wheelAngle, pos)
        );
    }

    public float getRudderAngle() {
        return (360f - wheelAngle) / 9f;
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