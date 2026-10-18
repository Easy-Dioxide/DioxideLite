package mixins;

import com.dioxideliteng.event.impl.EventModifyPacket;
import com.dioxideliteng.event.impl.EventPacketSend;
import com.dioxideliteng.util.PacketBlinkQueue;
import com.dioxideliteng.util.PacketFilter;
import com.dioxideliteng.util.krs.PacketUtil;
import com.dioxideliteng.util.krs.RotationManager;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundDisconnectPacket;
import net.minecraft.network.protocol.game.ClientboundStartConfigurationPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Connection.class})
public class MixinConnection {
   @Inject(
      method = {"send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;Z)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void dioxideliteng$dispatchPacketSend(Packet packet, ChannelFutureListener listener, boolean flush, CallbackInfo callback) {
      // Krs parity: PacketUtil.sendPacketAsSilent marks the packet so the send hook
      // skips the SendPacketEvent dispatch (and the blink/filter pipelines that hang
      // off it) - the packet itself still goes out, exactly like the Krs
      // Connection#send(Packet) hook. The marker is consumed here because this is the
      // last hook before the real send.
      boolean silent = PacketUtil.consumeSilentPacket(packet);
      // Krs parity: ModifyPacketEvent runs first and may replace the outgoing packet.
      EventModifyPacket modifyEvent = new EventModifyPacket(packet);
      modifyEvent.call();
      if (modifyEvent.isCancelled() || modifyEvent.getPacket() == null) {
         callback.cancel();
         return;
      }

      packet = modifyEvent.getPacket();
      if (silent) {
         RotationManager.INSTANCE.recordOutgoingRotation(packet);
         return;
      }

      EventPacketSend packetSendEvent = new EventPacketSend(packet);
      packetSendEvent.call();
      if (packetSendEvent.isCancelled()) {
         callback.cancel();
      } else {
         if (PacketBlinkQueue.enqueue(packetSendEvent.getPacket())) {
            callback.cancel();
         }

         if (PacketFilter.isRedundant(packetSendEvent.getPacket())) {
            callback.cancel();
         }
         if (!callback.isCancelled() && com.dioxideliteng.module.FeatureManager.autoRod != null) {
            com.dioxideliteng.module.FeatureManager.autoRod.observePacket(packetSendEvent.getPacket());
         }
      }
   }

   @Inject(
      method = {"channelRead0"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void dioxideliteng$dispatchPacketReceive(ChannelHandlerContext channelContext, Packet packet, CallbackInfo callback) {
      // Krs parity: the ReceivedPacketEvent dispatch itself lives at the packet-handle
      // stage (MixinPacketProcessor, on PacketProcessor$ListenerAndPacket#handle), which
      // runs on the main thread and unwraps ClientboundBundlePacket sub-packets.
      // channelRead0 runs on the netty thread; only the blink-queue safety checks stay
      // here (they are this port's own guard, not a Krs event dispatch).
      if (packet instanceof ClientboundDisconnectPacket || packet instanceof ClientboundStartConfigurationPacket) {
         PacketBlinkQueue.disable();
      }
   }
}
