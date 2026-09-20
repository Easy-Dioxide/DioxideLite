package com.dioxidelite.mixin;

// ---------------------------------------------------------------------------
// 移植来源：SetsunaClient（上游开源版）com/setsuna/mixin/InventoryMixin.java
// 变更：包名/导入 com.setsuna.* -> com.dioxidelite.*，mixin 方法前缀
//       setsuna$ -> dioxidelite$，字符串中的 setsuna -> dioxidelite。
//       逻辑逐行保留，未做功能改动。
// ---------------------------------------------------------------------------


import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.dioxidelite.module.modules.movement.Scaffold;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Inventory.class)
public abstract class InventoryMixin {

    @Shadow
    @Final
    public Player player;

    @ModifyExpressionValue(
            method = {"removeFromSelected", "tick", "getSelectedItem", "setSelectedItem"},
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/world/entity/player/Inventory;selected:I",
                    opcode = Opcodes.GETFIELD))
    private int dioxidelite$scaffoldSilentSlot(int original) {
        if (player == Minecraft.getInstance().player) {
            return Scaffold.INSTANCE.modifyServerSelectedSlot(original);
        }
        return original;
    }
}
