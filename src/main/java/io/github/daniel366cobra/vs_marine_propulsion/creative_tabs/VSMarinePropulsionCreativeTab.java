package io.github.daniel366cobra.vs_marine_propulsion.creative_tabs;

import com.simibubi.create.AllCreativeModeTabs;
import com.tterrag.registrate.util.entry.ItemProviderEntry;
import io.github.daniel366cobra.vs_marine_propulsion.VSMarinePropulsionBlocks;
import io.github.daniel366cobra.vs_marine_propulsion.VSMarinePropulsionMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class VSMarinePropulsionCreativeTab {
    private static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister
            .create(Registries.CREATIVE_MODE_TAB, VSMarinePropulsionMod.MOD_ID);

    public static final List<ItemProviderEntry<?>> ITEMS = List.of(
            VSMarinePropulsionBlocks.LARGE_SHIP_PROPELLER,
            VSMarinePropulsionBlocks.ENGINE_ORDER_TELEGRAPH,
            VSMarinePropulsionBlocks.VARIATOR,
            VSMarinePropulsionBlocks.RUDDER_BEARING,
            VSMarinePropulsionBlocks.RUDDER,
            VSMarinePropulsionBlocks.HELM_BLOCK
    );

    public static final RegistryObject<CreativeModeTab> MAIN = CREATIVE_MODE_TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.vs_marine_propulsion.main"))
                    .withTabsBefore(AllCreativeModeTabs.PALETTES_CREATIVE_TAB.getKey())
                    .icon(VSMarinePropulsionBlocks.LARGE_SHIP_PROPELLER::asStack)
                    .displayItems(new DisplayItemsGenerator(ITEMS))
                    .build());

    public static void register(IEventBus modEventBus) {
        CREATIVE_MODE_TABS.register(modEventBus);
    }

    private record DisplayItemsGenerator(
            List<ItemProviderEntry<?>> items) implements CreativeModeTab.DisplayItemsGenerator {
        @Override
        public void accept(@NotNull CreativeModeTab.ItemDisplayParameters params,
                           @NotNull CreativeModeTab.Output output) {
            for (ItemProviderEntry<?> item : items) {
                output.accept(item);
            }
        }
    }
}
