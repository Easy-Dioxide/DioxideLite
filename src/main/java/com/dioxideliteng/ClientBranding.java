package com.dioxideliteng;

import java.io.IOException;
import java.util.Locale;
import java.util.Properties;

/** The displayed version and Fabric metadata come from the same Gradle version. */
public final class ClientBranding {
   public static final String MOD_ID = "dioxideliteng";
   public static final String NAME = "DioxideLiteNG";
   public static final String VERSION = readVersion();
   public static final String DISPLAY_VERSION = VERSION.toUpperCase(Locale.ROOT);
   public static final String WINDOW_TITLE = NAME;
   public static final String TERMINAL_LABEL = NAME;

   private ClientBranding() { }

   private static String readVersion() {
      try (var input = ClientBranding.class.getResourceAsStream("/dioxideliteng-version.properties")) {
         if (input == null) throw new IOException("Missing DioxideLiteNG build version");
         var properties = new Properties();
         properties.load(input);
         var version = properties.getProperty("version");
         if (version == null || version.isBlank() || version.contains("${"))
            throw new IOException("Invalid DioxideLiteNG build version");
         return version.strip();
      } catch (IOException error) {
         throw new ExceptionInInitializerError(error);
      }
   }
}
