package org.json;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * {@code org.json.JSONArray} 的兼容实现，底层用 Minecraft 自带的 Gson。
 *
 * <p>为什么不用官方 org.json 包：Fabric 会把 {@code include} 进来的库当作嵌套 mod 解析，
 * 而 org.json 的版本号是纯日期（如 {@code 20250107}），不满足 semver，加载期会直接报错。
 * 这里保留同名 API，让 KRS 代码零改动，同时不引入额外依赖。</p>
 */
public class JSONArray {

    private final JsonArray array;

    public JSONArray() {
        this.array = new JsonArray();
    }

    public JSONArray(String source) {
        this.array = GsonSupport.parseArray(source);
    }

    JSONArray(JsonArray array) {
        this.array = array;
    }

    JsonArray unwrap() {
        return array;
    }

    public int length() {
        return array.size();
    }

    public boolean isEmpty() {
        return array.isEmpty();
    }

    public Object get(int index) {
        return GsonSupport.toJava(getElement(index));
    }

    public Object opt(int index) {
        return index >= 0 && index < array.size() ? GsonSupport.toJava(array.get(index)) : null;
    }

    private JsonElement getElement(int index) {
        if (index < 0 || index >= array.size()) {
            throw new JSONException("Index " + index + " out of range [0," + array.size() + ")");
        }
        return array.get(index);
    }

    public JSONObject getJSONObject(int index) {
        JsonElement element = getElement(index);
        if (!element.isJsonObject()) {
            throw new JSONException("Not a JSON object at index " + index);
        }
        return new JSONObject(element.getAsJsonObject());
    }

    public JSONArray getJSONArray(int index) {
        JsonElement element = getElement(index);
        if (!element.isJsonArray()) {
            throw new JSONException("Not a JSON array at index " + index);
        }
        return new JSONArray(element.getAsJsonArray());
    }

    public String getString(int index) {
        JsonElement element = getElement(index);
        if (!element.isJsonPrimitive()) {
            throw new JSONException("Not a string at index " + index);
        }
        return element.getAsString();
    }

    public int getInt(int index) {
        return GsonSupport.asInt(getElement(index));
    }

    public long getLong(int index) {
        return GsonSupport.asLong(getElement(index));
    }

    public double getDouble(int index) {
        return GsonSupport.asDouble(getElement(index));
    }

    public boolean getBoolean(int index) {
        return GsonSupport.asBoolean(getElement(index));
    }

    public JSONObject optJSONObject(int index) {
        JsonElement element = optElement(index);
        return element != null && element.isJsonObject() ? new JSONObject(element.getAsJsonObject()) : null;
    }

    public JSONArray optJSONArray(int index) {
        JsonElement element = optElement(index);
        return element != null && element.isJsonArray() ? new JSONArray(element.getAsJsonArray()) : null;
    }

    public String optString(int index) {
        return optString(index, "");
    }

    public String optString(int index, String fallback) {
        JsonElement element = optElement(index);
        return element == null || element.isJsonNull() ? fallback : element.getAsString();
    }

    public int optInt(int index) {
        return optInt(index, 0);
    }

    public int optInt(int index, int fallback) {
        JsonElement element = optElement(index);
        if (element == null || element.isJsonNull() || !element.isJsonPrimitive()) {
            return fallback;
        }
        try {
            return element.getAsInt();
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    public double optDouble(int index) {
        return optDouble(index, Double.NaN);
    }

    public double optDouble(int index, double fallback) {
        JsonElement element = optElement(index);
        if (element == null || element.isJsonNull() || !element.isJsonPrimitive()) {
            return fallback;
        }
        try {
            return element.getAsDouble();
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    public boolean optBoolean(int index) {
        return optBoolean(index, false);
    }

    public boolean optBoolean(int index, boolean fallback) {
        JsonElement element = optElement(index);
        if (element == null || element.isJsonNull() || !element.isJsonPrimitive()) {
            return fallback;
        }
        try {
            return element.getAsBoolean();
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private JsonElement optElement(int index) {
        return index >= 0 && index < array.size() ? array.get(index) : null;
    }

    public JSONArray put(Object value) {
        array.add(GsonSupport.toJson(value));
        return this;
    }

    public JSONArray put(int index, Object value) {
        JsonElement element = GsonSupport.toJson(value);
        // 与 org.json 一致：index 越界视为追加。
        if (index < 0 || index > array.size()) {
            array.add(element);
        } else {
            array.set(index, element);
        }
        return this;
    }

    public JSONArray remove(int index) {
        if (index >= 0 && index < array.size()) {
            array.remove(index);
        }
        return this;
    }

    public boolean isNull(int index) {
        JsonElement element = optElement(index);
        return element == null || element.isJsonNull();
    }

    public List<Object> toList() {
        return GsonSupport.toList(array);
    }

    public Set<String> keySet() {
        return new LinkedHashSet<>();
    }

    @Override
    public String toString() {
        return array.toString();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof JSONArray other2 && array.equals(other2.array);
    }

    @Override
    public int hashCode() {
        return array.hashCode();
    }
}
