package io.github.daniel366cobra.vs_marine_propulsion;

import com.simibubi.create.foundation.block.connected.AllCTTypes;
import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry;
import com.simibubi.create.foundation.block.connected.CTSpriteShifter;
import com.simibubi.create.foundation.block.connected.CTType;
import net.minecraft.resources.ResourceLocation;

public class VSMarinePropulsionSpriteShifts {

    public static final CTSpriteShiftEntry
            BALLAST_TANK = getCT(AllCTTypes.RECTANGLE, "ballast_tank/block"),
            BALLAST_TANK_TOP = getCT(AllCTTypes.RECTANGLE, "ballast_tank/block_top"),
            BALLAST_TANK_INNER = getCT(AllCTTypes.RECTANGLE, "ballast_tank/block_inner");


    private static CTSpriteShiftEntry getCT(CTType type, String blockTextureName, String connectedTextureName) {
        return CTSpriteShifter.getCT(type, new ResourceLocation(VSMarinePropulsionMod.MOD_ID, "block/" + blockTextureName),
                new ResourceLocation(VSMarinePropulsionMod.MOD_ID, "block/" + connectedTextureName + "_connected"));
    }

    private static CTSpriteShiftEntry getCT(CTType type, String blockTextureName) {
        return getCT(type, blockTextureName, blockTextureName);
    }
}
