/*
 * Copyright © Wynntils 2023.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.core.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Manager;
import com.wynntils.core.persisted.config.NullableConfig;
import com.wynntils.utils.EnumUtils;
import com.wynntils.utils.FileUtils;
import com.wynntils.utils.colors.CustomColor;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.apache.commons.lang3.RandomStringUtils;

public final class JsonManager extends Manager {
    public static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(CustomColor.class, new CustomColor.CustomColorSerializer())
            .registerTypeAdapterFactory(new EnumTypeAdapterFactory<>())
            .enableComplexMapKeySerialization()
            .setPrettyPrinting()
            .serializeNulls()
            .create();

    public JsonManager() {
        super(List.of());
    }

    public Type getJsonValueType(Field field) {
        ParameterizedType genericType = (ParameterizedType) field.getGenericType();
        return JsonTypeWrapper.wrap(genericType.getActualTypeArguments()[0]);
    }

    public <T> T deepCopy(T value, Type fieldType) {
        return GSON.fromJson(GSON.toJson(value), fieldType);
    }

    /**
     * Write a json object to a file, taking care to preserve the file against corruption since
     * it contains precious data.
     */
    public void savePreciousJson(File jsonFile, JsonObject jsonObject) {
        FileUtils.mkdir(jsonFile.getParentFile());

        if (jsonFile.exists()) {
            File backupFile = new File(jsonFile.getPath() + ".bak");
            // Remove old backup (if any), and move current json file to backup
            FileUtils.deleteFile(backupFile);
            FileUtils.moveFile(jsonFile, backupFile);
        }

        try (OutputStreamWriter fileWriter =
                new OutputStreamWriter(new FileOutputStream(jsonFile), StandardCharsets.UTF_8)) {
            GSON.toJson(jsonObject, fileWriter);
        } catch (IOException e) {
            WynntilsMod.error("Failed to save json file " + jsonFile, e);
        }
    }

    /**
     * Load a json object from a file. If the file is broken it is replaced with an empty file, taking care
     * to preserve the broken file since it contains precious data.
     */
    public JsonObject loadPreciousJson(File jsonFile) {
        FileUtils.mkdir(jsonFile.getParentFile());

        if (!jsonFile.exists()) {
            return createEmptyFile(jsonFile);
        }

        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(jsonFile), StandardCharsets.UTF_8)) {
            JsonElement fileElement = JsonParser.parseReader(new JsonReader(reader));

            if (fileElement.isJsonObject()) {
                // success
                return fileElement.getAsJsonObject();
            }

            // invalid json file; fall through to error case
            WynntilsMod.error("Error in json file " + jsonFile.getPath());
        } catch (JsonParseException | IOException e) {
            // invalid or unreadable json file; fall through to error case
            WynntilsMod.error("Failed to load or parse json file " + jsonFile.getPath(), e);
        }

        handleInvalidFile(jsonFile);
        return createEmptyFile(jsonFile);
    }

    private JsonObject createEmptyFile(File jsonFile) {
        JsonObject storageJson = new JsonObject();
        savePreciousJson(jsonFile, storageJson);
        return storageJson;
    }

    private void handleInvalidFile(File jsonFile) {
        File dir = jsonFile.getParentFile();

        // Copy old file to a backup, with a random part in the name to make sure we do not overwrite it
        FileUtils.tryCopyFile(
                jsonFile,
                new File(
                        dir,
                        "invalid_" + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "_"
                                + RandomStringUtils.randomAlphanumeric(5) + "_" + jsonFile.getName()));
        FileUtils.deleteFile(jsonFile);
    }

    public static final class EnumTypeAdapterFactory<E extends Enum<E>> implements TypeAdapterFactory {
        @Override
        public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
            if (!type.getRawType().isEnum()) return null;

            Class<E> enumClazz = (Class<E>) type.getRawType();
            return (TypeAdapter<T>) new EnumTypeAdapter<>(enumClazz);
        }
    }

    private static final class EnumTypeAdapter<T extends Enum<T>> extends TypeAdapter<T> {
        private final Class<T> enumClazz;
        private final boolean nullAllowed;

        private EnumTypeAdapter(Class<T> enumClazz) {
            this.enumClazz = enumClazz;
            nullAllowed = NullableConfig.class.isAssignableFrom(enumClazz);
        }

        @Override
        public void write(JsonWriter out, T value) throws IOException {
            if (value != null) {
                out.value(EnumUtils.toJsonFormat((Enum<?>) value));
            } else {
                if (!nullAllowed) {
                    WynntilsMod.warn("Writing null enum value to json for " + enumClazz.getSimpleName());
                }
                out.nullValue();
            }
        }

        @Override
        public T read(JsonReader in) throws IOException {
            if (in.peek() == JsonToken.STRING) {
                String jsonString = in.nextString();

                T value = EnumUtils.fromJsonFormat(enumClazz, jsonString);
                if (value == null) {
                    WynntilsMod.warn("Illegal enum value: " + jsonString + " for type " + enumClazz.getName());
                    return replacement();
                }

                return value;
            } else if (in.peek() == JsonToken.NULL) {
                in.nextNull();

                if (!nullAllowed) {
                    WynntilsMod.warn("Null enum value for type " + enumClazz.getName());
                    return replacement();
                }

                return null;
            } else {
                WynntilsMod.warn("Invalid json type " + in.peek() + " for enum " + enumClazz.getName());
                return replacement();
            }
        }

        private T replacement() {
            Enum<?> firstValue = enumClazz.getEnumConstants()[0];
            WynntilsMod.warn("Will replace with first enum value: " + firstValue.name());
            return (T) firstValue;
        }
    }
}
