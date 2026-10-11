package mixins;

import com.dioxideliteng.ui.dynamicIsland.DynamicIslandLatency;
import com.dioxideliteng.util.krs.PacketUtil;
import com.dioxideliteng.util.krs.RotationManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.ping.ClientboundPongResponsePacket;
import net.minecraft.network.protocol.ping.ServerboundPingRequestPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class MixinClientPacketListener implements DynamicIslandLatency.Source {
   /**
    * Krs 反作弊绕过核心：在 ClientPacketListener.send(Packet) 链路上拦截每个
    * outgoing packet，对携带旋转的包（ServerboundMovePlayerPacket /
    * ServerboundUseItemPacket）调用 RotationManager.INSTANCE 归一化旋转字段，
    * 使服务器看到的旋转与 RotationManager 维护的服务器旋转状态一致。
    *
    * 递归防护：PacketUtil.sendPacketAsSilent 会先把包加入 SILENT_PACKETS 标记，
    * 然后调用 send。Mixin 钩子检测到 silent 标记后 consumeSilentPacket 消费它，
    * 记录旋转并放行（不归一化），避免无限递归。
    */
   @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;)V", at = @At("HEAD"), cancellable = true)
   private void dioxideliteng$normalizeOutgoingRotation(Packet<?> packet, CallbackInfo callback) {
      if (packet == null) return;

      // PacketUtil 已标记为 silent 的包：记录旋转后放行（不重复归一化）
      if (PacketUtil.consumeSilentPacket(packet)) {
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

   @Inject(method = "handleMoveEntity", at = @At("TAIL"))
   private void dioxideliteng$predictMovedPlayer(net.minecraft.network.protocol.game.ClientboundMoveEntityPacket packet, CallbackInfo callback) {
      var mc = Minecraft.getInstance();
      var aura = com.dioxideliteng.module.FeatureManager.killAura;
      if (aura != null && mc.level != null && mc.getConnection() == (ClientPacketListener)(Object)this) {
         aura.onPredictEntityUpdate(packet.getEntity(mc.level), packet.hasRotation(), packet.hasPosition(), false);
      }
   }

   @Inject(method = "handleRotateMob", at = @At("TAIL"))
   private void dioxideliteng$predictPlayerHead(net.minecraft.network.protocol.game.ClientboundRotateHeadPacket packet, CallbackInfo callback) {
      var mc = Minecraft.getInstance();
      var aura = com.dioxideliteng.module.FeatureManager.killAura;
      if (aura != null && mc.level != null && mc.getConnection() == (ClientPacketListener)(Object)this) {
         aura.onPredictHeadUpdate(packet.getEntity(mc.level), packet.getYHeadRot());
      }
   }

   @Inject(method = "handleEntityPositionSync", at = @At("TAIL"))
   private void dioxideliteng$predictSyncedPlayer(net.minecraft.network.protocol.game.ClientboundEntityPositionSyncPacket packet, CallbackInfo callback) {
      var mc = Minecraft.getInstance();
      var aura = com.dioxideliteng.module.FeatureManager.killAura;
      if (aura != null && mc.level != null && mc.getConnection() == (ClientPacketListener)(Object)this) {
         aura.onPredictEntityUpdate(mc.level.getEntity(packet.id()), true, true, false);
      }
   }

   @Inject(method = "handleTeleportEntity", at = @At("TAIL"))
   private void dioxideliteng$predictTeleportedPlayer(net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket packet, CallbackInfo callback) {
      var mc = Minecraft.getInstance();
      var aura = com.dioxideliteng.module.FeatureManager.killAura;
      if (aura != null && mc.level != null && mc.getConnection() == (ClientPacketListener)(Object)this) {
         aura.onPredictEntityUpdate(mc.level.getEntity(packet.id()), true, true, true);
      }
   }

   @Inject(method = "handleDamageEvent", at = @At("TAIL"))
   private void dioxideliteng$recordVelocityDamage(net.minecraft.network.protocol.game.ClientboundDamageEventPacket packet, CallbackInfo callback) {
      var mc = Minecraft.getInstance();
      var velocity = com.dioxideliteng.module.FeatureManager.velocity;
      if (velocity != null && mc.getConnection() == (ClientPacketListener)(Object)this) velocity.recordDamage(packet);
   }
   @Inject(method = "handleLogin", at = @At("TAIL"))
   private void dioxideliteng$initializeRestoredModules(net.minecraft.network.protocol.game.ClientboundLoginPacket packet, CallbackInfo callback) {
      com.dioxideliteng.module.FeatureManager.getModules().forEach(com.dioxideliteng.module.Feature::initializeWorldState);
   }
   @Inject(method = "setTitleText", at = @At("TAIL"))
   private void dioxideliteng$recordVictory(net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket packet, CallbackInfo callback) {
      com.dioxideliteng.module.visual.Hud.SessionHud.SessionTracker.title(packet.text().getString());
   }
   @Inject(method = "handleEntityEvent", at = @At("TAIL"))
   private void dioxideliteng$recordDeath(net.minecraft.network.protocol.game.ClientboundEntityEventPacket packet, CallbackInfo callback) {
      var level = Minecraft.getInstance().level;
      if (level != null && packet.getEventId() == 3) com.dioxideliteng.module.visual.Hud.SessionHud.SessionTracker.death(packet.getEntity(level));
   }
   @Unique
   private final DynamicIslandLatency dioxideliteng$islandLatency = new DynamicIslandLatency();

   @Inject(method = "tick", at = @At("TAIL"))
   private void dioxideliteng$sampleIslandLatency(CallbackInfo callback) {
      Minecraft mc = Minecraft.getInstance();
      ClientPacketListener listener = (ClientPacketListener)(Object)this;
      if (mc.getConnection() != listener || mc.player == null || mc.hasSingleplayerServer()) {
         this.dioxideliteng$islandLatency.reset();
         return;
      }
      long token = this.dioxideliteng$islandLatency.request(System.nanoTime() / 1_000_000L);
      if (token != 0) listener.send(new ServerboundPingRequestPacket(token));
   }

   @Inject(method = "handlePongResponse", at = @At("HEAD"), cancellable = true)
   private void dioxideliteng$receiveIslandLatency(ClientboundPongResponsePacket packet, CallbackInfo callback) {
      if (this.dioxideliteng$islandLatency.receive(packet.time(), System.nanoTime() / 1_000_000L)) {
         // Our opaque IDs are not vanilla wall-clock timestamps; do not log them in the F3 chart.
         callback.cancel();
      }
   }

   @Override
   public int dioxideliteng$getLivePing(long now, int fallback) {
      return this.dioxideliteng$islandLatency.latency(now, fallback);
   }
}
