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
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.lang3.tuple.Pair;

public class RudderContraption extends Contraption {

    protected int rudderBlocks;
    protected Direction facing;

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
        return tag;
    }

    @Override
    public void readNBT(Level world, CompoundTag tag, boolean spawnData) {
        rudderBlocks = tag.getInt("Rudders");
        facing = Direction.from3DDataValue(tag.getInt("Facing"));
        super.readNBT(world, tag, spawnData);
    }

    public int getRudderBlocks() {
        return rudderBlocks;
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
