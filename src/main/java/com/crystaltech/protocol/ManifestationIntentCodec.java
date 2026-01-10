package com.crystaltech.protocol;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

/**
 * Handles JSON encoding and decoding of {@link ManifestationIntent} objects.
 */
public final class ManifestationIntentCodec {
    private static final Gson GSON = new Gson();

    private ManifestationIntentCodec() {
    }

    public static ManifestationIntent decode(String json) {
        JsonElement root = JsonParser.parseString(json);
        if (!root.isJsonObject()) {
            throw new JsonParseException("Manifestation intent payload must be a JSON object");
        }

        JsonObject object = root.getAsJsonObject();
        String intentId = requiredString(object, "intent_id");
        String schemaVersion = requiredString(object, "schema_version");
        String scenarioId = requiredString(object, "scenario_id");
        String scenarioVersion = optionalString(object, "scenario_version");
        int allowedStage = requiredInt(object, "allowed_stage");
        String confidenceLevel = requiredString(object, "confidence_level");
        List<String> constraints = readStringList(object, "constraints");
        List<String> contextNotes = readStringList(object, "context_notes");
        Instant issuedAt = requiredInstant(object, "issued_at");
        Instant expiresAt = optionalInstant(object, "expires_at");
        String signature = requiredString(object, "signature");

        return new ManifestationIntent(intentId, schemaVersion, scenarioId, scenarioVersion, allowedStage, confidenceLevel, constraints,
                contextNotes, issuedAt, expiresAt, signature);
    }

    public static String encode(ManifestationIntent intent) {
        JsonObject object = new JsonObject();
        object.addProperty("intent_id", intent.intentId());
        object.addProperty("schema_version", intent.schemaVersion());
        object.addProperty("scenario_id", intent.scenarioId());
        if (intent.scenarioVersion() != null) {
            object.addProperty("scenario_version", intent.scenarioVersion());
        }
        object.addProperty("allowed_stage", intent.allowedStage());
        object.addProperty("confidence_level", intent.confidenceLevel());
        if (!intent.constraints().isEmpty()) {
            object.add("constraints", writeStringList(intent.constraints()));
        }
        if (!intent.contextNotes().isEmpty()) {
            object.add("context_notes", writeStringList(intent.contextNotes()));
        }
        object.addProperty("issued_at", intent.issuedAt().toString());
        if (intent.expiresAt() != null) {
            object.addProperty("expires_at", intent.expiresAt().toString());
        }
        object.addProperty("signature", intent.signature());
        return GSON.toJson(object);
    }

    private static String requiredString(JsonObject object, String key) {
        if (!object.has(key)) {
            throw new JsonParseException("Missing required field: " + key);
        }
        JsonElement element = object.get(key);
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
            throw new JsonParseException("Expected string for field: " + key);
        }
        return element.getAsString();
    }

    private static int requiredInt(JsonObject object, String key) {
        if (!object.has(key)) {
            throw new JsonParseException("Missing required field: " + key);
        }
        JsonElement element = object.get(key);
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
            throw new JsonParseException("Expected integer for field: " + key);
        }
        return element.getAsInt();
    }

    private static String optionalString(JsonObject object, String key) {
        return Optional.ofNullable(object.get(key))
                .filter(JsonElement::isJsonPrimitive)
                .map(JsonElement::getAsString)
                .orElse(null);
    }

    private static Instant optionalInstant(JsonObject object, String key) {
        JsonElement element = object.get(key);
        if (element == null) {
            return null;
        }
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
            throw new JsonParseException("Expected ISO-8601 timestamp for field: " + key);
        }
        try {
            return Instant.parse(element.getAsString());
        } catch (Exception ex) {
            throw new JsonParseException("Invalid timestamp format for field: " + key, ex);
        }
    }

    private static Instant requiredInstant(JsonObject object, String key) {
        Instant value = optionalInstant(object, key);
        if (value == null) {
            throw new JsonParseException("Missing required field: " + key);
        }
        return value;
    }

    private static List<String> readStringList(JsonObject object, String key) {
        JsonElement element = object.get(key);
        if (element == null) {
            return List.of();
        }
        if (!element.isJsonArray()) {
            throw new JsonParseException("Expected array for field: " + key);
        }
        JsonArray array = element.getAsJsonArray();
        List<String> values = new ArrayList<>(array.size());
        for (JsonElement entry : array) {
            if (!entry.isJsonPrimitive() || !entry.getAsJsonPrimitive().isString()) {
                throw new JsonParseException("Array " + key + " must contain only strings");
            }
            values.add(entry.getAsString());
        }
        return values;
    }

    private static JsonArray writeStringList(List<String> values) {
        JsonArray array = new JsonArray();
        for (String value : values) {
            array.add(value);
        }
        return array;
    }
}
