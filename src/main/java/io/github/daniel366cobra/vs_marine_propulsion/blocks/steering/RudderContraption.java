package io.github.daniel366cobra.vs_marine_propulsion.blocks.steering;

import com.simibubi.create.content.contraptions.AssemblyException;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.ContraptionType;
import com.simibubi.create.content.contraptions.bearing.AnchoredLighter;
import com.simibubi.create.content.contraptions.render.ContraptionLighter;
import io.github.daniel366cobra.vs_marine_propulsion.VSMarinePropulsionBlocks;
import io.github.daniel366cobra.vs_marine_propulsion.VSMarinePropulsionContraptionTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.lang3.tuple.Pair;
import org.joml.Vector3d;

import java.util.List;
import java.util.stream.Collectors;

import static net.minecraft.world.level.block.state.properties.BlockStateProperties.AXIS;

public class RudderContraption extends Contraption {

    protected int rudderBlocks;
    protected Direction facing;
    protected Axis normalAxis;

    public RudderContraption() {}

    public RudderContraption(Direction facing) {
        this.facing = facing;
    }

    @Override
    public boolean assemble(Level world, BlockPos pos) throws AssemblyException {
        BlockPos offset = pos.relative(facing);
        if (!searchMovedStructure(world, offset, null))
            return false;
        startMoving(world);
        expandBoundsAroundAxis(facing.getAxis());

        if (rudderBlocks == 0)
            throw new AssemblyException(Component.translatable("gui.assembly.exception.no_rudders"));

        //Check for mismatched rudder block placement
        List<StructureTemplate.StructureBlockInfo> rudderBlocks = this.getBlocks()
                .values()
                .stream()
                .filter(blockInfo -> blockInfo.state().is(VSMarinePropulsionBlocks.RUDDER.get())).collect(Collectors.toList());

        Axis firstRudderAxis = rudderBlocks.get(0).state().getValue(AXIS);
        boolean mismatchedRudders = rudderBlocks
                .stream()
                .anyMatch(blockInfo -> blockInfo.state().getValue(AXIS) != firstRudderAxis);

        if (mismatchedRudders)
            throw new AssemblyException(Component.translatable("gui.assembly.exception.mismatched_rudders"));

        this.normalAxis = firstRudderAxis;
        return !blocks.isEmpty();
    }

    @Override
    public boolean canBeStabilized(Direction facing, BlockPos localPos) {
        if (facing.getOpposite() == this.facing && BlockPos.ZERO.equals(localPos))
            return false;
        return facing.getAxis() == this.facing.getAxis();
    }

    @Override
    public ContraptionType getType() {
        return VSMarinePropulsionContraptionTypes.RUDDER;
    }

    @Override
    protected boolean isAnchoringBlockAt(BlockPos pos) {
        return pos.equals(anchor.relative(facing.getOpposite()));
    }

    @Override
    public void addBlock(BlockPos pos, Pair<StructureTemplate.StructureBlockInfo, BlockEntity> capture) {
        BlockPos localPos = pos.subtract(anchor);
        if (!getBlocks().containsKey(localPos) && capture.getKey().state().is(VSMarinePropulsionBlocks.RUDDER.get()))
            rudderBlocks++;
        super.addBlock(pos, capture);
    }

    @Override
    public CompoundTag writeNBT(boolean spawnPacket) {
        CompoundTag tag = super.writeNBT(spawnPacket);
        tag.putInt("Rudders", rudderBlocks);
        tag.putString("NormalAxis", normalAxis.getName());
        return tag;
    }

    @Override
    public void readNBT(Level world, CompoundTag tag, boolean spawnData) {
        rudderBlocks = tag.getInt("Rudders");
        facing = Direction.from3DDataValue(tag.getInt("Facing"));
        normalAxis = Axis.byName(tag.getString("NormalAxis"));
        super.readNBT(world, tag, spawnData);
    }

    public int getRudderBlocks() {
        return rudderBlocks;
    }

    /**
     * @return a new Vector3d pointing in the positive direction of this contraption's normal axis.
     */
    public Vector3d getNormalVector() {
        return switch (normalAxis) {
            case X -> new Vector3d(1, 0, 0);
            case Y -> new Vector3d(0, 1, 0);
            default -> new Vector3d(0, 0, 1);
        };
    }

    /**
     * @return a new Vector3d pointing in the direction of this contraption's axis of rotation.
     */
    public Vector3d getRotationAxisVector() {
        return new Vector3d(facing.getStepX(), facing.getStepY(), facing.getStepZ());
    }

    public Direction getFacing() {
        return facing;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public ContraptionLighter<?> makeLighter() {
        return new AnchoredLighter(this);
    }

}
