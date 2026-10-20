package com.dioxideliteng.module.misc;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.Events;
import com.dioxideliteng.event.impl.EventPacketReceive;
import com.dioxideliteng.event.impl.EventWorld;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import com.dioxideliteng.setting.BooleanSetting;
import com.dioxideliteng.setting.ModeSetting;
import com.dioxideliteng.setting.NumberSetting;
import com.dioxideliteng.util.krs.PacketUtil;
import com.dioxideliteng.util.krs.RandomUtil;
import java.util.Comparator;
import java.util.Locale;
import java.util.PriorityQueue;
import java.util.concurrent.TimeUnit;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundKeepAlivePacket;
import net.minecraft.network.protocol.common.ClientboundPingPacket;

/**
 * Ported from Krs (com.instrumentalist.krs.hacks.features.exploit.FakePinger).
 * Krs parity: incoming keep-alive/ping packets are cancelled and queued, then re-handled
 * after the configured delay (fixed or random, optionally order-preserving), which makes
 * the client look laggy to the server. The queue is priority-ordered by release time with
 * a sequence tiebreaker; overflow flushes/drops the oldest or drops the newest; disable
 * and world changes flush everything.
 */
public class FakePinger extends Feature {

   private final BooleanSetting delayedKeepAlive = new BooleanSetting("Delayed Keep Alive", this, true);
   private final BooleanSetting delayedPing = new BooleanSetting("Delayed Ping", this, true);
   private final ModeSetting mode = new ModeSetting("Mode", this, "Positive",
         new String[]{"Positive", "Negative", "Zero", "Non Zero"});
   private final ModeSetting delayMode = new ModeSetting("Delay Mode", this, "Fixed",
         new String[]{"Fixed", "Random"});
   private final NumberSetting delay = new NumberSetting("Delay", this, 2500.0, 0.0, 50000.0, 1.0);
   private final NumberSetting maxDelay = new NumberSetting("Max Delay", this, 3500.0, 0.0, 50000.0, 1.0);
   private final NumberSetting minDelay = new NumberSetting("Min Delay", this, 1500.0, 0.0, 50000.0, 1.0);
   private final BooleanSetting keepOrder = new BooleanSetting("Keep Order", this, true);
   private final NumberSetting packetsPerTick = new NumberSetting("Packets Per Tick", this, 100.0, 1.0, 100.0, 1.0);
   private final NumberSetting queueLimit = new NumberSetting("Queue Limit", this, 128.0, 1.0, 512.0, 1.0);
   private final ModeSetting overflowAction = new ModeSetting("Overflow Action", this, "Flush Oldest",
         new String[]{"Flush Oldest", "Drop Oldest", "Drop Newest"});

   private final PriorityQueue<DelayData> packetQueue = new PriorityQueue<>(
         Comparator.comparingLong(DelayData::releaseAtNanos).thenComparingLong(DelayData::sequence)
   );
   private long lastReleaseAtNanos;
   private long sequence;

   public FakePinger() {
      super("Fake Pinger", Category.MISC);
      this.mode.setVisible(this.delayedPing::getValue);
      this.delayMode.setVisible(this::delaySettingsVisible);
      this.delay.setVisible(() -> this.delaySettingsVisible() && this.fixedDelayMode());
      this.maxDelay.setVisible(() -> this.delaySettingsVisible() && this.randomDelayMode());
      this.minDelay.setVisible(() -> this.delaySettingsVisible() && this.randomDelayMode());
      this.keepOrder.setVisible(() -> this.delaySettingsVisible() && this.randomDelayMode());
      this.packetsPerTick.setVisible(this::delaySettingsVisible);
      this.queueLimit.setVisible(this::delaySettingsVisible);
      this.overflowAction.setVisible(this::delaySettingsVisible);
   }

   @Override
   public void onDisable() {
      if (mc.level != null) {
         sendPackets(true);
      } else {
         clearPackets();
      }
   }

   @Override
   public void onEnable() {
      clearPackets();
   }

   @Override
   public void onEvent(Event event) {
      if (event instanceof EventWorld) {
         if (mc.level != null) {
            sendPackets(true);
         } else {
            clearPackets();
         }
         return;
      }
      if (event == Events.TICK) {
         this.setSuffix(this.mode.getValue());
         if (mc.level == null || mc.isLocalServer()) return;
         sendPackets(false);
         return;
      }
      if (event instanceof EventPacketReceive receiving && !receiving.isCancelled()) {
         if (mc.level == null || mc.isLocalServer()) return;

         Packet<?> packet = receiving.getPacket();
         if (shouldDelay(packet)) {
            receiving.setCancelled(true);
            long now = System.nanoTime();

            synchronized (packetQueue) {
               addPacket(packet, now);
            }
         }
      }
   }

   private boolean shouldDelay(Packet<?> packet) {
      if (packet instanceof ClientboundKeepAlivePacket)
         return delayedKeepAlive.getValue();

      if (packet instanceof ClientboundPingPacket pingPacket)
         return delayedPing.getValue() && safeParam(pingPacket.getId());

      return false;
   }

   private boolean safeParam(int parameter) {
      return switch (this.mode.getValue().toLowerCase(Locale.ROOT)) {
         case "positive" -> parameter > 0;
         case "negative" -> parameter < 0;
         case "zero" -> parameter == 0;
         case "non zero" -> parameter != 0;
         default -> true;
      };
   }

   private void addPacket(Packet<?> packet, long now) {
      if (packetQueue.size() >= queueLimit()) {
         String action = this.overflowAction.getValue().toLowerCase(Locale.ROOT);

         if (action.equals("drop newest"))
            return;

         while (packetQueue.size() >= queueLimit()) {
            DelayData data = packetQueue.poll();
            if (data == null) break;

            if (action.equals("flush oldest"))
               PacketUtil.handlePacket(data.packet());
         }
      }

      long releaseAtNanos = now + TimeUnit.MILLISECONDS.toNanos(nextDelay());
      if (this.keepOrder.getValue() && randomDelayMode()) {
         releaseAtNanos = Math.max(releaseAtNanos, lastReleaseAtNanos);
         lastReleaseAtNanos = releaseAtNanos;
      }

      packetQueue.add(new DelayData(packet, releaseAtNanos, sequence++));
   }

   private void sendPackets(boolean forceAll) {
      long now = System.nanoTime();
      int sent = 0;
      int limit = forceAll ? Integer.MAX_VALUE : packetsPerTick();

      synchronized (packetQueue) {
         while (!packetQueue.isEmpty() && sent < limit) {
            DelayData data = packetQueue.peek();
            if (forceAll || data.releaseAtNanos() <= now) {
               PacketUtil.handlePacket(data.packet());
               packetQueue.poll();
               sent++;
            } else {
               break;
            }
         }
      }
   }

   private void clearPackets() {
      synchronized (packetQueue) {
         packetQueue.clear();
         lastReleaseAtNanos = 0L;
         sequence = 0L;
      }
   }

   private long nextDelay() {
      if (fixedDelayMode())
         return clampDelay((int) this.delay.getValue());

      int min = clampDelay((int) this.minDelay.getValue());
      int max = clampDelay((int) this.maxDelay.getValue());
      int lower = Math.min(min, max);
      int upper = Math.max(min, max);

      return RandomUtil.nextInt(lower, upper + 1);
   }

   private boolean delaySettingsVisible() {
      return this.delayedKeepAlive.getValue() || this.delayedPing.getValue();
   }

   private boolean fixedDelayMode() {
      return this.delayMode.is("Fixed");
   }

   private boolean randomDelayMode() {
      return this.delayMode.is("Random");
   }

   private int packetsPerTick() {
      return Math.max(1, Math.min(100, (int) this.packetsPerTick.getValue()));
   }

   private int queueLimit() {
      return Math.max(1, Math.min(512, (int) this.queueLimit.getValue()));
   }

   private int clampDelay(int value) {
      return Math.max(0, Math.min(10000, value));
   }

   private record DelayData(Packet<?> packet, long releaseAtNanos, long sequence) {
   }
}
