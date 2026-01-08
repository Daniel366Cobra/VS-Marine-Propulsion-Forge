package io.github.daniel366cobra.vs_marine_propulsion.client;

import io.github.daniel366cobra.vs_marine_propulsion.items.EngineOrderTelegraphBlockItem;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(Dist.CLIENT)
public class VSMarinePropulsionModClientEvents {

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent event) {
        EngineOrderTelegraphBlockItem.clientTick();
    }

    /*
    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
        if (!Minecraft.getInstance().options.renderDebug) return; // Only if F3 debug is on

        LineRenderer.renderLines(event);

    }

     */
}
