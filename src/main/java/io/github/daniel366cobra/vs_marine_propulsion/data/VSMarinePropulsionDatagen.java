package io.github.daniel366cobra.vs_marine_propulsion.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.simibubi.create.foundation.utility.FilesHelper;
import com.tterrag.registrate.providers.ProviderType;
import io.github.daniel366cobra.vs_marine_propulsion.VSMarinePropulsionMod;
import net.minecraftforge.data.event.GatherDataEvent;

import java.util.Map;
import java.util.function.BiConsumer;

public class VSMarinePropulsionDatagen {

    public static void gatherData(GatherDataEvent event) {
        addLangData("tooltips");
        addLangData("interface");
    }

    private static void addLangData(String filename) {
        VSMarinePropulsionMod.getRegistrate().addDataGenerator(ProviderType.LANG, provider -> {
            BiConsumer<String, String> langConsumer = provider::add;

            provideDefaultLang(filename, langConsumer);
        });
    }

    private static void provideDefaultLang(String fileName, BiConsumer<String, String> consumer) {
        String path = "assets/" + VSMarinePropulsionMod.MOD_ID + "/lang/default/" + fileName + ".json";
        JsonElement jsonElement = FilesHelper.loadJsonResource(path);
        if (jsonElement == null) {
            throw new IllegalStateException(String.format("Could not find default lang file: %s", path));
        }
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue().getAsString();
            consumer.accept(key, value);
        }
    }
}
