package mixins;

import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.authlib.services.ProfileResult;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.client.multiplayer.ProfileKeyPairManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Minecraft.class)
public interface MinecraftSessionAccessor {
   @Mutable @Accessor("services") void dioxideliteng$setServices(net.minecraft.server.Services services);
   @Mutable @Accessor("user") void dioxideliteng$setUser(User user);
   @Accessor("userApiService") UserApiService dioxideliteng$getUserApiService();
   @Mutable @Accessor("userApiService") void dioxideliteng$setUserApiService(UserApiService service);
   @Accessor("profileFuture") CompletableFuture<ProfileResult> dioxideliteng$getProfile();
   @Mutable @Accessor("profileFuture") void dioxideliteng$setProfile(CompletableFuture<ProfileResult> profile);
   @Accessor("userPropertiesFuture") CompletableFuture<UserApiService.UserProperties> dioxideliteng$getProperties();
   @Mutable @Accessor("userPropertiesFuture") void dioxideliteng$setProperties(CompletableFuture<UserApiService.UserProperties> properties);
   @Mutable @Accessor("profileKeyPairManager") void dioxideliteng$setKeys(ProfileKeyPairManager keys);
}
