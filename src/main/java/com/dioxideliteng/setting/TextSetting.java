package com.dioxideliteng.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.dioxideliteng.module.Feature;

/**
 * String setting, ported to adapt Krs {@code utils/value/TextValue}. Persisted through configs like every
 * other setting; ClickGui text editing is not part of the render layer and is therefore not wired here.
 */
public class TextSetting extends Setting {
   private String value;
   private String defaultValue;

   public TextSetting(String name, Feature feature, String defaultValue) {
      super(name, feature);
      this.defaultValue = defaultValue;
      this.value = defaultValue;
   }

   public String getValue() {
      return this.value;
   }

   public String getDefaultValue() {
      return this.defaultValue;
   }

   public void setValue(String value) {
      this.value = value;
   }

   @Override public JsonElement snapshot(boolean defaults) {
      return new JsonPrimitive(defaults ? this.defaultValue : this.value);
   }

   @Override protected Runnable prepareValue(JsonElement value) {
      String desired = value.getAsString();
      return () -> setValue(desired);
   }
}
