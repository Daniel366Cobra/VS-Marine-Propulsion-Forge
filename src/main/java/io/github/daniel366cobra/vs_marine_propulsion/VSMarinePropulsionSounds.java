package io.github.daniel366cobra.vs_marine_propulsion;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class VSMarinePropulsionSounds {
    private static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(Registries.SOUND_EVENT, VSMarinePropulsionMod.MOD_ID);

    public static final RegistryObject<SoundEvent> ENGINE_ORDER_TELEGRAPH_DING = SOUND_EVENTS.register(
            "engine_order_telegraph_ding",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(VSMarinePropulsionMod.MOD_ID, "engine_order_telegraph_ding"))
    );

    public static final RegistryObject<SoundEvent> ENGINE_ORDER_TELEGRAPH_BELL = SOUND_EVENTS.register(
            "engine_order_telegraph_bell",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(VSMarinePropulsionMod.MOD_ID, "engine_order_telegraph_bell"))
    );

    public static final RegistryObject<SoundEvent> HELM_PROMOTION = SOUND_EVENTS.register(
            "helm_promotion",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(VSMarinePropulsionMod.MOD_ID, "helm_promotion"))
    );


    public static void register(IEventBus eventBus) {
        VSMarinePropulsionMod.LOGGER.info("Registering sound events for " + VSMarinePropulsionMod.NAME);
        SOUND_EVENTS.register(eventBus);
    }
}
