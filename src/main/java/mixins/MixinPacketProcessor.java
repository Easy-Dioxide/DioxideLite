package mixins;

import com.dioxideliteng.event.impl.EventPacketReceive;
import com.mojang.logging.LogUtils;
import net.minecraft.network.ClientboundPacketListener;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Krs parity: com.instrumentalist.mixin.injector.PacketProcessorMixin.
 *
 * <p>Dispatches the received-packet event at the packet-HANDLE stage (main thread)
 * instead of the netty read stage, and unwraps {@link ClientboundBundlePacket}
 * recursively so modules observe the individual sub-packets - Hypixel bundles
 * entity motion / movement / player-info packets that Velocity2, Backtrack2,
 * LongJump and AntiBot2 filter on. A cancelled sub-packet is dropped from the
 * bundle; the remaining sub-packets are handed to the vanilla handler exactly
 * like ClientPacketListener#handleBundlePacket does.
 */
@Mixin(targets = "net.minecraft.network.PacketProcessor$ListenerAndPacket")
public abstract class MixinPacketProcessor {
   @Unique
   private static final Logger DIOXIDE_LOGGER = LogUtils.getLogger();
   @Unique
   private static final int DIOXIDE_MAX_BUNDLE_DEPTH = 32;
   @Unique
   private static final int DIOXIDE_MAX_BUNDLE_PACKETS = 4096;
   @Unique
   private static final long DIOXIDE_BUNDLE_WARNING_INTERVAL_NANOS = 10_000_000_000L;
   @Unique
   private static long dioxide$lastBundleWarningNanos;

   @Shadow
   @Final
   private PacketListener listener;
   @Shadow
   @Final
   private Packet<?> packet;

   @Inject(
      method = "handle",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/network/protocol/Packet;handle(Lnet/minecraft/network/PacketListener;)V"
      ),
      cancellable = true
   )
   private void dioxide$dispatchReceivedPacket(CallbackInfo ci) {
      if (!(this.listener instanceof ClientboundPacketListener) || this.packet == null)
         return;

      if (this.packet instanceof ClientboundBundlePacket bundlePacket) {
         ci.cancel();
         int[] remainingPacketBudget = {DIOXIDE_MAX_BUNDLE_PACKETS};
         for (Packet<?> subPacket : bundlePacket.subPackets()) {
            if (!dioxide$handlePacket(subPacket, 1, remainingPacketBudget))
               break;
         }
         return;
      }

      EventPacketReceive event = new EventPacketReceive(this.packet);
      event.call();
      if (event.isCancelled())
         ci.cancel();
   }

   @Unique
   private boolean dioxide$handlePacket(Packet<?> packet, int depth, int[] remainingPacketBudget) {
      if (packet == null)
         return true;

      if (remainingPacketBudget[0]-- <= 0) {
         dioxide$warnInvalidBundle("packet count");
         return false;
      }

      if (packet instanceof ClientboundBundlePacket bundlePacket) {
         if (depth >= DIOXIDE_MAX_BUNDLE_DEPTH) {
            dioxide$warnInvalidBundle("nesting depth");
            return true;
         }

         for (Packet<?> subPacket : bundlePacket.subPackets()) {
            if (!dioxide$handlePacket(subPacket, depth + 1, remainingPacketBudget))
               return false;
         }
         return true;
      }

      EventPacketReceive event = new EventPacketReceive(packet);
      event.call();
      if (!event.isCancelled())
         dioxide$invokePacketHandler(packet);
      return true;
   }

   @Unique
   private void dioxide$warnInvalidBundle(String limit) {
      long now = System.nanoTime();
      if (dioxide$lastBundleWarningNanos != 0L && now - dioxide$lastBundleWarningNanos < DIOXIDE_BUNDLE_WARNING_INTERVAL_NANOS)
         return;

      dioxide$lastBundleWarningNanos = now;
      DIOXIDE_LOGGER.warn("Discarded part of an oversized clientbound packet bundle ({})", limit);
   }

   @Unique
   @SuppressWarnings({"rawtypes", "unchecked"})
   private void dioxide$invokePacketHandler(Packet<?> packet) {
      ((Packet) packet).handle(this.listener);
   }
}
