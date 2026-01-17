package io.github.daniel366cobra.vs_marine_propulsion;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class VSMarinePropulsionParticleTypes {
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, VSMarinePropulsionMod.MOD_ID);

    public static final RegistryObject<SimpleParticleType> PROMOTION_PARTICLE = PARTICLE_TYPES.register(
            "promotion_particle",
            () -> new SimpleParticleType(true)
    );

    public static void register(IEventBus eventBus) {
        VSMarinePropulsionMod.LOGGER.info("Registering particles for " + VSMarinePropulsionMod.NAME);
        PARTICLE_TYPES.register(eventBus);
    }
}
