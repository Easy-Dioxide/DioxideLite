package mixins;

import com.dioxideliteng.util.krs.PacketUtil;
import com.dioxideliteng.util.krs.RotationManager;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.network.protocol.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientCommonPacketListenerImpl.class)
public class MixinClientCommonPacketListener {
   /**
    * Krs 反作弊绕过核心：拦截每个 outgoing packet，对携带旋转的包
    *（ServerboundMovePlayerPacket / ServerboundUseItemPacket）调用
    * RotationManager.INSTANCE 归一化旋转字段，使服务器看到的旋转与
    * RotationManager 维护的服务器旋转状态一致。
    *
    * 注意：MC 26.3 起 send(Packet) 从 ClientPacketListener 上移到
    * ClientCommonPacketListenerImpl，因此该钩子挂在本类上。
    *
    * 递归防护：PacketUtil.sendPacketAsSilent 会先把包加入 SILENT_PACKETS 标记，
    * 然后调用 send。Mixin 钩子检测到 silent 标记后 consumeSilentPacket 消费它，
    * 记录旋转并放行（不归一化），避免无限递归。
    */
   @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;)V", at = @At("HEAD"), cancellable = true)
   private void dioxideliteng$normalizeOutgoingRotation(Packet<?> packet, CallbackInfo callback) {
      if (packet == null) return;

      // PacketUtil 已标记为 silent 的包：只记录旋转后放行。silent marker 不在这里
      // 消费——由更底层的 MixinConnection 发送钩子消费并跳过事件派发，与 Krs 的
      // Connection.send 1 参钩子语义一致（silent 包不发 SendPacketEvent）。
      if (PacketUtil.isSilentPacket(packet)) {
         RotationManager.INSTANCE.recordOutgoingRotation(packet);
         return;
      }

      Packet<?> normalized = RotationManager.INSTANCE.normalizeOutgoingPacket(packet);
      if (normalized == packet) {
         // 无需替换（非旋转包或未初始化）：只记录旋转状态
         RotationManager.INSTANCE.recordOutgoingRotation(packet);
         return;
      }

      // 替换为归一化后的包：取消原发送，用 PacketUtil.sendPacketAsSilent 重发
      //（已标记 silent，Mixin 钩子会 consumeSilentPacket 后放行，避免递归）
      callback.cancel();
      PacketUtil.sendPacketAsSilent(normalized);
      RotationManager.INSTANCE.recordOutgoingRotation(normalized);
   }
}
