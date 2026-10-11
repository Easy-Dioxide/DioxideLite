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
import java.util.Comparator;
import java.util.Locale;
import java.util.PriorityQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundKeepAlivePacket;
import net.minecraft.network.protocol.common.ClientboundPingPacket;

/**
 * Ported from Krs (com.instrumentalist.krs.hacks.features.exploit.FakePinger) and adapted to the client.
 * IntValue/FloatValue/BooleanValue/ListValue -> NumberSetting/BooleanSetting/ModeSetting.
 * RandomUtil.nextInt -> ThreadLocalRandom. PacketUtil.handlePacket is replayed via the packet's handle()
 * method against the client connection listener (raw cast, since Packet<?> cannot be dispatched generically).
 */
public class FakePinger extends Feature {
   private static final String POSITIVE = "Positive";
   private static final String NEGATIVE = "Negative";
   private static final String ZERO = "Zero";
   private static final String NON_ZERO = "Non Zero";
   private static final String FIXED = "Fixed";
   private static final String RANDOM = "Random";
   private static final String FLUSH_OLDEST = "Flush Oldest";
   private static final String DROP_OLDEST = "Drop Oldest";
   private static final String DROP_NEWEST = "Drop Newest";

   private final BooleanSetting delayedKeepAlive = new BooleanSetting("Delayed Keep Alive", this, true);
   private final BooleanSetting delayedPing = new BooleanSetting("Delayed Ping", this, true);
   private final ModeSetting mode = new ModeSetting("Mode", this, POSITIVE,
         new String[]{POSITIVE, NEGATIVE, ZERO, NON_ZERO});
   private final ModeSetting delayMode = new ModeSetting("Delay Mode", this, FIXED, new String[]{FIXED, RANDOM});
   private final NumberSetting delay = new NumberSetting("Delay", this, 2500.0, 0.0, 50000.0, 1.0);
   private final NumberSetting maxDelay = new NumberSetting("Max Delay", this, 3500.0, 0.0, 50000.0, 1.0);
   private final NumberSetting minDelay = new NumberSetting("Min Delay", this, 1500.0, 0.0, 50000.0, 1.0);
   private final BooleanSetting keepOrder = new BooleanSetting("Keep Order", this, true);
   private final NumberSetting packetsPerTick = new NumberSetting("Packets Per Tick", this, 100.0, 1.0, 100.0, 1.0);
   private final NumberSetting queueLimit = new NumberSetting("Queue Limit", this, 128.0, 1.0, 512.0, 1.0);
   private final ModeSetting overflowAction = new ModeSetting("Overflow Action", this, FLUSH_OLDEST,
         new String[]{FLUSH_OLDEST, DROP_OLDEST, DROP_NEWEST});

   private final PriorityQueue<DelayData> packetQueue = new PriorityQueue<>(
         Comparator.comparingLong(DelayData::releaseAtNanos).thenComparingLong(DelayData::sequence));
   private long lastReleaseAtNanos;
   private long sequence;

   public FakePinger() {
      super("FakePinger", Category.MISC);
      this.mode.setVisible(this::delaySettingsVisible);
      this.delayMode.setVisible(this::delaySettingsVisible);
      this.delay.setVisible(() -> delaySettingsVisible() && fixedDelayMode());
      this.maxDelay.setVisible(() -> delaySettingsVisible() && randomDelayMode());
      this.minDelay.setVisible(() -> delaySettingsVisible() && randomDelayMode());
      this.keepOrder.setVisible(() -> delaySettingsVisible() && randomDelayMode());
      this.packetsPerTick.setVisible(this::delaySettingsVisible);
      this.queueLimit.setVisible(this::delaySettingsVisible);
      this.overflowAction.setVisible(this::delaySettingsVisible);
   }

   @Override
   public void onEnable() {
      clearPackets();
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
            synchronized (this.packetQueue) {
               addPacket(packet, now);
            }
         }
      }
   }

   private boolean shouldDelay(Packet<?> packet) {
      if (packet instanceof ClientboundKeepAlivePacket) {
         return this.delayedKeepAlive.getValue();
      }
      if (packet instanceof ClientboundPingPacket pingPacket) {
         return this.delayedPing.getValue() && safeParam(pingPacket.getId());
      }
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
      if (this.packetQueue.size() >= queueLimit()) {
         String action = this.overflowAction.getValue().toLowerCase(Locale.ROOT);
         if (action.equals("drop newest")) return;

         while (this.packetQueue.size() >= queueLimit()) {
            DelayData data = this.packetQueue.poll();
            if (data == null) break;

            if (action.equals("flush oldest")) handlePacket(data.packet());
         }
      }

      long releaseAtNanos = now + TimeUnit.MILLISECONDS.toNanos(nextDelay());
      if (this.keepOrder.getValue() && randomDelayMode()) {
         releaseAtNanos = Math.max(releaseAtNanos, this.lastReleaseAtNanos);
         this.lastReleaseAtNanos = releaseAtNanos;
      }

      this.packetQueue.add(new DelayData(packet, releaseAtNanos, this.sequence++));
   }

   private void sendPackets(boolean forceAll) {
      long now = System.nanoTime();
      int sent = 0;
      int limit = forceAll ? Integer.MAX_VALUE : packetsPerTick();

      synchronized (this.packetQueue) {
         while (!this.packetQueue.isEmpty() && sent < limit) {
            DelayData data = this.packetQueue.peek();
            if (forceAll || data.releaseAtNanos() <= now) {
               handlePacket(data.packet());
               this.packetQueue.poll();
               sent++;
            } else {
               break;
            }
         }
      }
   }

   private void clearPackets() {
      synchronized (this.packetQueue) {
         this.packetQueue.clear();
         this.lastReleaseAtNanos = 0L;
         this.sequence = 0L;
      }
   }

   private long nextDelay() {
      if (fixedDelayMode()) return clampDelay((int) this.delay.getValue());

      int min = clampDelay((int) this.minDelay.getValue());
      int max = clampDelay((int) this.maxDelay.getValue());
      int lower = Math.min(min, max);
      int upper = Math.max(min, max);

      return ThreadLocalRandom.current().nextInt(lower, upper + 1);
   }

   private boolean delaySettingsVisible() {
      return this.delayedKeepAlive.getValue() || this.delayedPing.getValue();
   }

   private boolean fixedDelayMode() {
      return this.delayMode.is(FIXED);
   }

   private boolean randomDelayMode() {
      return this.delayMode.is(RANDOM);
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

   @SuppressWarnings({"unchecked", "rawtypes"})
   private void handlePacket(Packet<?> packet) {
      if (mc.getConnection() == null) return;
      try {
         ((Packet) packet).handle(mc.getConnection());
      } catch (Exception ignored) {
      }
   }

   private record DelayData(Packet<?> packet, long releaseAtNanos, long sequence) {
   }
}
