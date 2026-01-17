package io.github.daniel366cobra.vs_marine_propulsion.client;

import io.github.daniel366cobra.vs_marine_propulsion.VSMarinePropulsionMod;
import io.github.daniel366cobra.vs_marine_propulsion.VSMarinePropulsionParticleTypes;
import io.github.daniel366cobra.vs_marine_propulsion.items.EngineOrderTelegraphBlockItem;
import io.github.daniel366cobra.vs_marine_propulsion.particle.PromotionParticle;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

public class VSMarinePropulsionModClientEvents {

    // MOD bus events
    @Mod.EventBusSubscriber(modid = VSMarinePropulsionMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
            VSMarinePropulsionMod.LOGGER.info("Registering particle providers for " + VSMarinePropulsionMod.NAME);
            event.registerSpriteSet(VSMarinePropulsionParticleTypes.PROMOTION_PARTICLE.get(), PromotionParticle.Provider::new);
        }
    }

    // FORGE bus events
    @Mod.EventBusSubscriber(modid = VSMarinePropulsionMod.MOD_ID, value = Dist.CLIENT)
    public static class ClientForgeEvents {
        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            EngineOrderTelegraphBlockItem.clientTick();
        }
    }

}
