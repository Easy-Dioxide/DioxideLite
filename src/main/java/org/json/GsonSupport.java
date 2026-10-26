package org.json;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * {@code org.json} 与 Gson 之间的类型转换工具。
 *
 * <p>KRS 原本依赖官方 org.json 库，但它的版本号不满足 semver，Fabric 会在加载嵌套库时失败。
 * 这里用 Minecraft 自带的 Gson 实现同名 API，行为对齐 org.json 的常用语义。</p>
 */
final class GsonSupport {

    private GsonSupport() {
    }

    static JsonArray parseArray(String source) {
        try {
            JsonElement element = JsonParser.parseString(source);
            if (element.isJsonArray()) {
                return element.getAsJsonArray();
            }
            throw new JSONException("Not a JSON array: " + source);
        } catch (JsonSyntaxException | IllegalStateException failure) {
            throw new JSONException("Invalid JSON array", failure);
        }
    }

    static JsonObject parseObject(String source) {
        try {
            JsonElement element = JsonParser.parseString(source);
            if (element.isJsonObject()) {
                return element.getAsJsonObject();
            }
            throw new JSONException("Not a JSON object: " + source);
        } catch (JsonSyntaxException | IllegalStateException failure) {
            throw new JSONException("Invalid JSON object", failure);
        }
    }

    static int asInt(JsonElement element) {
        if (element == null || element.isJsonNull() || !element.isJsonPrimitive()) {
            throw new JSONException("Not a number");
        }
        try {
            return element.getAsInt();
        } catch (RuntimeException failure) {
            throw new JSONException("Not a number: " + element, failure);
        }
    }

    static long asLong(JsonElement element) {
        if (element == null || element.isJsonNull() || !element.isJsonPrimitive()) {
            throw new JSONException("Not a number");
        }
        try {
            return element.getAsLong();
        } catch (RuntimeException failure) {
            throw new JSONException("Not a number: " + element, failure);
        }
    }

    static double asDouble(JsonElement element) {
        if (element == null || element.isJsonNull() || !element.isJsonPrimitive()) {
            throw new JSONException("Not a number");
        }
        try {
            return element.getAsDouble();
        } catch (RuntimeException failure) {
            throw new JSONException("Not a number: " + element, failure);
        }
    }

    static boolean asBoolean(JsonElement element) {
        if (element == null || element.isJsonNull() || !element.isJsonPrimitive()) {
            throw new JSONException("Not a boolean");
        }
        try {
            return element.getAsBoolean();
        } catch (RuntimeException failure) {
            throw new JSONException("Not a boolean: " + element, failure);
        }
    }

    /** JsonElement → Java 值，语义对齐 org.json 的 Object 返回。 */
    static Object toJava(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return JSONObject.NULL;
        }
        if (element.isJsonObject()) {
            return new JSONObject(element.getAsJsonObject());
        }
        if (element.isJsonArray()) {
            return new JSONArray(element.getAsJsonArray());
        }
        JsonPrimitive primitive = element.getAsJsonPrimitive();
        if (primitive.isBoolean()) {
            return primitive.getAsBoolean();
        }
        if (primitive.isNumber()) {
            BigDecimal number = primitive.getAsBigDecimal();
            try {
                if (number.stripTrailingZeros().scale() <= 0) {
                    long asLong = number.longValueExact();
                    return asLong >= Integer.MIN_VALUE && asLong <= Integer.MAX_VALUE ? (Object) (int) asLong : (Object) asLong;
                }
            } catch (ArithmeticException ignored) {
                // 非整数：走 double。
            }
            return primitive.getAsDouble();
        }
        return primitive.getAsString();
    }

    /** Java 值 → JsonElement。 */
    static JsonElement toJson(Object value) {
        if (value == null || value == JSONObject.NULL || value == JsonNull.INSTANCE) {
            return JsonNull.INSTANCE;
        }
        if (value instanceof JSONObject object) {
            return object.unwrap();
        }
        if (value instanceof JSONArray array) {
            return array.unwrap();
        }
        if (value instanceof JsonElement element) {
            return element;
        }
        if (value instanceof Map<?, ?> map) {
            JsonObject object = new JsonObject();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                object.add(String.valueOf(entry.getKey()), toJson(entry.getValue()));
            }
            return object;
        }
        if (value instanceof Iterable<?> iterable) {
            JsonArray array = new JsonArray();
            for (Object item : iterable) {
                array.add(toJson(item));
            }
            return array;
        }
        if (value instanceof Object[] items) {
            JsonArray array = new JsonArray();
            for (Object item : items) {
                array.add(toJson(item));
            }
            return array;
        }
        if (value instanceof Number number) {
            return new JsonPrimitive(number);
        }
        if (value instanceof Boolean bool) {
            return new JsonPrimitive(bool);
        }
        if (value instanceof Character character) {
            return new JsonPrimitive(character);
        }
        return new JsonPrimitive(String.valueOf(value));
    }

    /** 把 JsonObject 转成有序 Map，供 keySet/entrySet 使用。 */
    static Map<String, Object> toMap(JsonObject object) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            map.put(entry.getKey(), toJava(entry.getValue()));
        }
        return map;
    }

    static List<Object> toList(JsonArray array) {
        List<Object> list = new ArrayList<>(array.size());
        for (JsonElement element : array) {
            list.add(toJava(element));
        }
        return list;
    }
}
