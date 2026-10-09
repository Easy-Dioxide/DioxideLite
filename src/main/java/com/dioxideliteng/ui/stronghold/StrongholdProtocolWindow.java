package com.dioxideliteng.ui.stronghold;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

/**
 * Opens the Stronghold Protocol (卫戍协议) web game inside a JavaFX WebView window.
 *
 * <p>The web build at {@link #URL} already supports 1–4 player online co-op, so loading it
 * directly in an embedded WebView gives a playable, multiplayer-capable experience without
 * bundling a separate game runtime. Minecraft runs headless for AWT, so we use a native
 * JavaFX {@link Stage} (same approach as the Microsoft login window).
 */
public final class StrongholdProtocolWindow {

   /** Official deployed web build of Stronghold Protocol. */
   public static final String URL = "https://weishuxieyi.icu/_build/67c6f52a2530-11/";

   private static final int STARTUP_TIMEOUT_SECONDS = 15;

   private static final AtomicBoolean open = new AtomicBoolean();

   private StrongholdProtocolWindow() {
   }

   /**
    * Opens the Stronghold Protocol window. If a window is already open, it is brought to the
    * front instead of opening a second one.
    *
    * @return {@code true} if a new window was opened, {@code false} if an existing one was reused
    */
   public static boolean open() {
      if (!open.compareAndSet(false, true)) {
         Platform.runLater(StrongholdProtocolWindow::toFront);
         return false;
      }
      var window = new GameWindow();
      Thread.ofPlatform().daemon(true).name("dioxideliteng-stronghold-startup").start(window::show);
      return true;
   }

   private static volatile Stage activeStage;

   private static void toFront() {
      var stage = activeStage;
      if (stage != null) {
         stage.setIconified(false);
         stage.toFront();
         stage.requestFocus();
      }
   }

   private static final class GameWindow {
      private final CompletableFuture<Void> ready = new CompletableFuture<>();
      private volatile Stage stage;
      private volatile WebView webView;

      void show() {
         Runnable create = () -> {
            try {
               Platform.setImplicitExit(false);
               stage = new Stage();
               activeStage = stage;
               stage.setTitle("卫戍协议 · Stronghold Protocol");
               webView = new WebView();
               webView.setContextMenuEnabled(false);
               // Keep the real browser user agent so the game serves the desktop build.
               webView.getEngine().setUserAgent(
                  "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/605.1.15 "
                     + "(KHTML, like Gecko) Version/17.0 Safari/605.1.15");
               stage.setScene(new Scene(webView, 1280, 720));
               stage.setMinWidth(960);
               stage.setMinHeight(540);
               stage.setOnCloseRequest(event -> dispose());
               stage.show();
               stage.centerOnScreen();
               stage.toFront();
               stage.requestFocus();
               webView.getEngine().load(URL);
               ready.complete(null);
            } catch (Exception | LinkageError error) {
               ready.completeExceptionally(error);
               dispose();
            }
         };

         try {
            try {
               Platform.startup(create);
            } catch (IllegalStateException alreadyStarted) {
               Platform.runLater(create);
            }
         } catch (Exception | LinkageError error) {
            ready.completeExceptionally(error);
            dispose();
         }

         try {
            ready.get(STARTUP_TIMEOUT_SECONDS, TimeUnit.SECONDS);
         } catch (Exception error) {
            // Best-effort: the window either opened or failed. The caller does not block.
         }
      }

      private void dispose() {
         if (stage != null) {
            if (webView != null) {
               webView.getEngine().getLoadWorker().cancel();
               webView.getEngine().load(null);
               webView = null;
            }
            stage.setOnCloseRequest(null);
            stage.hide();
            stage.setScene(null);
            if (activeStage == stage) activeStage = null;
            stage = null;
         }
         open.set(false);
      }
   }
}
