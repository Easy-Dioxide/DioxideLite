package mixins;

import com.dioxideliteng.module.visual.NameTags;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(EntityRenderState.class)
public class MixinNameTagRenderState implements NameTags.ReplacementState {
   @Unique private boolean dioxideliteng$replace;
   @Override public boolean dioxideliteng$replaceNameTag() { return dioxideliteng$replace; }
   @Override public void dioxideliteng$replaceNameTag(boolean replace) { dioxideliteng$replace = replace; }
}
