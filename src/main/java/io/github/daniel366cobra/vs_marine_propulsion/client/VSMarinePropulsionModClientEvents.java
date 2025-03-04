package io.github.daniel366cobra.vs_marine_propulsion.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(Dist.CLIENT)
public class VSMarinePropulsionModClientEvents {
    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent event) {

        //EngineOrderTelegraphBlockItemForge.clientTick();
    }
}
