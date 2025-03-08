package io.github.daniel366cobra.vs_marine_propulsion.blocks.drivetrain.variator;

import net.minecraft.core.BlockPos;

public interface IEngineOrderTelegraphControllable {

    void setMasterTelegraph(BlockPos blockPos);
    void removeMasterTelegraph();
    void setThrottleOrder(int throttleOrder);
}
