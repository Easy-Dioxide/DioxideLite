package org.json;

/** 兼容层：org.json 的异常类型，保留包名以便 KRS 代码零改动。 */
public class JSONException extends RuntimeException {

    public JSONException(String message) {
        super(message);
    }

    public JSONException(String message, Throwable cause) {
        super(message, cause);
    }
}
