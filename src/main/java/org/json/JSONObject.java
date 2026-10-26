package org.json;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * {@code org.json.JSONObject} 的兼容实现，底层用 Minecraft 自带的 Gson。
 *
 * <p>保留同名 API 让 KRS 代码零改动；不额外打包 org.json，避免 Fabric 因
 * 其非 semver 版本号拒绝加载嵌套库。</p>
 */
public class JSONObject {

    /** org.json 用一个哨兵表示 JSON 里的 null，这里保持一致。 */
    public static final Object NULL = new Object() {
        @Override
        public boolean equals(Object other) {
            return other == this || other == null;
        }

        @Override
        public int hashCode() {
            return 0;
        }

        @Override
        public String toString() {
            return "null";
        }
    };

    private final JsonObject object;

    public JSONObject() {
        this.object = new JsonObject();
    }

    public JSONObject(String source) {
        this.object = GsonSupport.parseObject(source);
    }

    JSONObject(JsonObject object) {
        this.object = object;
    }

    JsonObject unwrap() {
        return object;
    }

    public int length() {
        return object.size();
    }

    public boolean isEmpty() {
        return object.isEmpty();
    }

    public boolean has(String key) {
        return key != null && object.has(key);
    }

    public boolean isNull(String key) {
        JsonElement element = object.get(key);
        return !object.has(key) || element == null || element.isJsonNull();
    }

    public Object get(String key) {
        if (!object.has(key)) {
            throw new JSONException("Key " + key + " not found");
        }
        return GsonSupport.toJava(object.get(key));
    }

    public Object opt(String key) {
        return key != null && object.has(key) ? GsonSupport.toJava(object.get(key)) : null;
    }

    public JSONObject getJSONObject(String key) {
        JsonElement element = requireElement(key);
        if (!element.isJsonObject()) {
            throw new JSONException("Not a JSON object: " + key);
        }
        return new JSONObject(element.getAsJsonObject());
    }

    public JSONArray getJSONArray(String key) {
        JsonElement element = requireElement(key);
        if (!element.isJsonArray()) {
            throw new JSONException("Not a JSON array: " + key);
        }
        return new JSONArray(element.getAsJsonArray());
    }

    public String getString(String key) {
        JsonElement element = requireElement(key);
        if (!element.isJsonPrimitive()) {
            throw new JSONException("Not a string: " + key);
        }
        return element.getAsString();
    }

    public int getInt(String key) {
        return GsonSupport.asInt(requireElement(key));
    }

    public long getLong(String key) {
        return GsonSupport.asLong(requireElement(key));
    }

    public double getDouble(String key) {
        return GsonSupport.asDouble(requireElement(key));
    }

    public boolean getBoolean(String key) {
        return GsonSupport.asBoolean(requireElement(key));
    }

    private JsonElement requireElement(String key) {
        if (!object.has(key)) {
            throw new JSONException("Key " + key + " not found");
        }
        return object.get(key);
    }

    public JSONObject optJSONObject(String key) {
        JsonElement element = optElement(key);
        return element != null && element.isJsonObject() ? new JSONObject(element.getAsJsonObject()) : null;
    }

    public JSONArray optJSONArray(String key) {
        JsonElement element = optElement(key);
        return element != null && element.isJsonArray() ? new JSONArray(element.getAsJsonArray()) : null;
    }

    public String optString(String key) {
        return optString(key, "");
    }

    public String optString(String key, String fallback) {
        JsonElement element = optElement(key);
        return element == null || element.isJsonNull() ? fallback : element.getAsString();
    }

    public int optInt(String key) {
        return optInt(key, 0);
    }

    public int optInt(String key, int fallback) {
        JsonElement element = optElement(key);
        if (element == null || element.isJsonNull() || !element.isJsonPrimitive()) {
            return fallback;
        }
        try {
            return element.getAsInt();
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    public long optLong(String key, long fallback) {
        JsonElement element = optElement(key);
        if (element == null || element.isJsonNull() || !element.isJsonPrimitive()) {
            return fallback;
        }
        try {
            return element.getAsLong();
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    public double optDouble(String key) {
        return optDouble(key, Double.NaN);
    }

    public double optDouble(String key, double fallback) {
        JsonElement element = optElement(key);
        if (element == null || element.isJsonNull() || !element.isJsonPrimitive()) {
            return fallback;
        }
        try {
            return element.getAsDouble();
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    public boolean optBoolean(String key) {
        return optBoolean(key, false);
    }

    public boolean optBoolean(String key, boolean fallback) {
        JsonElement element = optElement(key);
        if (element == null || element.isJsonNull() || !element.isJsonPrimitive()) {
            return fallback;
        }
        try {
            return element.getAsBoolean();
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private JsonElement optElement(String key) {
        return key != null && object.has(key) ? object.get(key) : null;
    }

    public JSONObject put(String key, Object value) {
        if (key == null) {
            throw new JSONException("Key must not be null");
        }
        object.add(key, GsonSupport.toJson(value));
        return this;
    }

    public JSONObject put(String key, boolean value) {
        object.addProperty(key, value);
        return this;
    }

    public JSONObject put(String key, int value) {
        object.addProperty(key, value);
        return this;
    }

    public JSONObject put(String key, long value) {
        object.addProperty(key, value);
        return this;
    }

    public JSONObject put(String key, double value) {
        object.addProperty(key, value);
        return this;
    }

    public JSONObject put(String key, String value) {
        if (value == null) {
            object.add(key, JsonNull.INSTANCE);
        } else {
            object.addProperty(key, value);
        }
        return this;
    }

    public JSONObject putOpt(String key, Object value) {
        return value == null ? this : put(key, value);
    }

    public Object remove(String key) {
        if (!object.has(key)) {
            return null;
        }
        JsonElement element = object.remove(key);
        return GsonSupport.toJava(element);
    }

    public Set<String> keySet() {
        return new LinkedHashSet<>(object.keySet());
    }

    public Map<String, Object> toMap() {
        return GsonSupport.toMap(object);
    }

    @Override
    public String toString() {
        return object.toString();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof JSONObject other2 && object.equals(other2.object);
    }

    @Override
    public int hashCode() {
        return object.hashCode();
    }
}
