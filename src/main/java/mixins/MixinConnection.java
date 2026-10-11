package mixins;

import com.dioxideliteng.event.impl.EventPacketReceive;
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
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Connection.class})
public class MixinConnection {
   @Inject(
      method = {"send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;Z)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void dioxideliteng$dispatchPacketSend(Packet packet, ChannelFutureListener listener, boolean flush, CallbackInfo callback) {
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

   @ModifyVariable(
      method = "send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;Z)V",
      at = @At("HEAD"),
      argsOnly = true,
      index = 1
   )
   private Packet<?> dioxideliteng$normalizePacket(Packet<?> packet) {
      if (packet == null) {
         return packet;
      }

      if (PacketUtil.consumeSilentPacket(packet)) {
         RotationManager.INSTANCE.recordOutgoingRotation(packet);
         return packet;
      }

      Packet<?> normalized = RotationManager.INSTANCE.normalizeOutgoingPacket(packet);
      RotationManager.INSTANCE.recordOutgoingRotation(normalized);
      return normalized;
   }

   @Inject(
      method = {"channelRead0"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void dioxideliteng$dispatchPacketReceive(ChannelHandlerContext channelContext, Packet packet, CallbackInfo callback) {
      EventPacketReceive packetReceiveEvent = new EventPacketReceive(packet);
      packetReceiveEvent.call();
      if (packet instanceof ClientboundDisconnectPacket || packet instanceof ClientboundStartConfigurationPacket) {
         PacketBlinkQueue.disable();
      }

      if (packetReceiveEvent.isCancelled()) {
         callback.cancel();
      }
   }
}
