package com.dioxidelite.mixin.client;

import net.minecraft.client.renderer.ShaderManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ShaderManager.class)
public interface ShaderManagerAccessor {
    @Accessor ShaderManager.CompilationCache getCompilationCache();
}
