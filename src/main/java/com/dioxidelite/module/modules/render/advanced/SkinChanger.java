package com.dioxidelite.module.modules.render.advanced;

import com.dioxidelite.DioxideLite;
import com.dioxidelite.module.Category;
import com.dioxidelite.module.Module;
import com.dioxidelite.notification.NotificationManager;
import com.dioxidelite.notification.NotificationType;
import com.dioxidelite.setting.settings.ButtonSetting;
import com.dioxidelite.setting.settings.EnumSetting;
import com.dioxidelite.setting.settings.StringSetting;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerModelType;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 本地皮肤替换（移植自 来源客户端 {@code features/render/SkinChanger}：
 * "Overrides your own skin texture locally"）。
 *
 * <p>LOCAL 源：读磁盘 PNG，按 来源的校验（必须 64x64 或 64x32）与
 * {@code ImageBufferDownload#parseUserSkin} 的旧皮转换（64x32 -> 64x64，镜像补左臂/左腿），
 * 再由 {@code TextureManager#register} 注册成 {@code dioxide-lite:custom/skins/local}。</p>
 *
 * <p>PLAYER 源：走 来源 {@code SkinSupport_072} 的两段 HTTP ——
 * {@code api.mojang.com/users/profiles/minecraft/<name>} 拿 32 位 UUID，
 * {@code sessionserver.mojang.com/session/minecraft/profile/<uuid>} 拿 base64 的 textures 属性，
 * 解析出皮肤 PNG 地址与 {@code metadata.model}（slim/default），下载后用同一条上传链路。</p>
 *
 * <p>读盘/HTTP/解码都在 来源的 {@code custom-skin-loader} 式 daemon 线程上，贴图注册走渲染线程
 * （{@code DynamicTexture} 会申请 GPU 贴图）。</p>
 */
// PORT-NOTE: 需要 AbstractClientPlayer#getSkin()（或 PlayerInfo#getSkin）的 mixin hook 才能真正替换本地皮肤；
// 本端口只实现了 1:1 设置面、64x64/64x32 校验与旧皮转换、PNG/在线皮肤下载、贴图注册（dioxide-lite:custom/skins/local）
// 以及 appliedSkinTexture()/appliedSkinModel() 只读入口。
public final class SkinChanger extends Module {

    public static final SkinChanger INSTANCE = new SkinChanger();

    /** 来源 {@code FeatureMode_281}：Source —— 本地 PNG 或按玩家名在线拉取。 */
    public enum Source {
        LOCAL,
        PLAYER
    }

    /** 来源 {@code FeatureMode_285}：Arm width —— 默认（Steve）/ 细（Alex），裸 PNG 看不出模型。 */
    public enum ArmWidth {
        DEFAULT,
        SLIM
    }

    /** 本地皮肤贴图 id（来源: {@code custom:skins/local}）。 */
    private static final Identifier SKIN_TEXTURE = Identifier.fromNamespaceAndPath(DioxideLite.MOD_ID, "custom/skins/local");

    /** 来源 常量 {@code H = 15000}：HTTP 超时 15s。 */
    private static final int TIMEOUT_SECONDS = 15;
    private static final String PROFILE_ENDPOINT = "https://api.mojang.com/users/profiles/minecraft/";
    private static final String SESSION_ENDPOINT = "https://sessionserver.mojang.com/session/minecraft/profile/";

    /** 来源的 {@code custom-skin-loader}：单线程 daemon，只做读盘 / HTTP / 解码。 */
    private static final ExecutorService LOADER = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "custom-skin-loader");
        thread.setDaemon(true);
        return thread;
    });

    /** 懒加载的 HTTP 客户端（来源 只在真正查询时才建连接）。 */
    private static final class ClientHolder {
        private static final HttpClient CLIENT = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public final EnumSetting<Source> source = add(new EnumSetting<>("Source", Source.LOCAL));

    /** 来源 "Skin file"：皮肤 PNG 的完整路径（仅 LOCAL 源）。 */
    public final StringSetting skinFile = add(new StringSetting("Skin file", "")
            .visibleWhen(() -> source.is(Source.LOCAL)));

    /** 来源 "Browse"：打开系统文件选择器填路径（只填路径，不自动应用，与原版一致）。 */
    public final ButtonSetting browse = add(new ButtonSetting("Browse", this::browse)
            .visibleWhen(() -> source.is(Source.LOCAL)));

    /** 来源 "Player name"：在线拉取谁的皮肤（仅 PLAYER 源）。 */
    public final StringSetting playerName = add(new StringSetting("Player name", "")
            .visibleWhen(() -> source.is(Source.PLAYER)));

    /** 来源 "Arm width"：LOCAL 源下手动指定手臂模型（PLAYER 源按 profile 元数据自动判定）。 */
    public final EnumSetting<ArmWidth> armWidth = add(new EnumSetting<>("Arm width", ArmWidth.DEFAULT)
            .visibleWhen(() -> source.is(Source.LOCAL)));

    /** 来源 "Apply skin"。 */
    public final ButtonSetting apply = add(new ButtonSetting("Apply skin", this::applySkin));

    /** 当前已注册的皮肤贴图；{@code null} = 不在覆盖状态（hook 侧只读这个值）。 */
    private static volatile Identifier appliedSkin;

    /** 当前皮肤的手臂模型（hook 侧只读这个值）。 */
    private static volatile PlayerModelType appliedModel = PlayerModelType.WIDE;

    /** 最近一次成功上传的 64x64 皮肤，用于换手臂模型时重新上传。 */
    private volatile BufferedImage appliedImage;

    /** 最近一次上传使用的手臂模型（PLAYER 源会带 profile 里的 slim/default）。 */
    private volatile ArmWidth appliedWidth = ArmWidth.DEFAULT;

    /** 是否已注册贴图，决定 onDisable 要不要 release。 */
    private boolean registered;

    private SkinChanger() {
        super("SkinChanger", Category.RENDER);
        // 来源：切手臂模型时，如果皮肤已经应用，直接按新模型重新推一遍（不必重读文件）。
        armWidth.onChange(value -> {
            BufferedImage image = appliedImage;
            if (isEnabled() && image != null) {
                upload(image, value, "Skin updated");
            }
        });
    }

    @Override
    protected void onEnable() {
        BufferedImage image = appliedImage;
        if (image != null) {
            upload(image, appliedWidth, "Skin reapplied");
        }
    }

    @Override
    protected void onDisable() {
        appliedSkin = null;
        if (registered) {
            registered = false;
            try {
                mc.getTextureManager().release(SKIN_TEXTURE);
            } catch (RuntimeException error) {
                DioxideLite.LOGGER.warn("SkinChanger: failed to release the skin texture", error);
            }
        }
    }

    /**
     * hook 侧只读入口（对应 来源的 {@code SkinSupport_073} 静态状态）：
     * 非 {@code null} 表示本地皮肤已经注册到 {@code TextureManager}。
     */
    public static Identifier appliedSkinTexture() {
        return appliedSkin;
    }

    /** hook 侧只读入口：当前皮肤用手臂模型（来源的 {@code SkinSupport_073.L()} 字符串等价物）。 */
    public static PlayerModelType appliedSkinModel() {
        return appliedModel;
    }

    // --- 应用 ---------------------------------------------------------------------

    /** 按当前 Source 应用皮肤（来源的 Apply 动作）。 */
    public void applySkin() {
        if (source.is(Source.LOCAL)) {
            String path = skinFile.get();
            if (path == null || path.isBlank()) {
                notify(NotificationType.WARNING, "Pick a skin PNG first");
                return;
            }
            LOADER.submit(() -> readFile(path));
            return;
        }
        String name = playerName.get();
        if (name == null || name.isBlank()) {
            notify(NotificationType.WARNING, "Type a player name first");
            return;
        }
        LOADER.submit(() -> fetchByName(name));
    }

    private void readFile(String path) {
        BufferedImage image;
        try {
            image = ImageIO.read(new File(path));
        } catch (IOException error) {
            DioxideLite.LOGGER.warn("SkinChanger: failed to read '{}'", path, error);
            notify(NotificationType.ERROR, "Can't read that file");
            return;
        }
        applyImage(image, armWidth.get(), "Skin applied");
    }

    /** 来源 {@code SkinSupport_072}：先按名字查 UUID，再取 textures 属性里的皮肤地址与手臂模型。 */
    private void fetchByName(String name) {
        try {
            Lookup lookup = resolveSkin(name);
            if (lookup == null) {
                notify(NotificationType.ERROR, "No account named " + name);
                return;
            }
            HttpResponse<byte[]> response = ClientHolder.CLIENT.send(
                    HttpRequest.newBuilder(URI.create(lookup.url()))
                            .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                            .GET()
                            .build(),
                    HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() != 200) {
                notify(NotificationType.ERROR, "Skin download failed: HTTP " + response.statusCode());
                return;
            }
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(response.body()));
            applyImage(image, lookup.width(), "Wearing " + name + "'s skin");
        } catch (IOException | InterruptedException | RuntimeException error) {
            DioxideLite.LOGGER.warn("SkinChanger: lookup for {} failed", name, error);
            notify(NotificationType.ERROR, "Skin lookup failed");
        }
    }

    /** 来源：64x64 或 64x32 才接受，64x32 先做旧版皮肤转换。 */
    private void applyImage(BufferedImage image, ArmWidth width, String message) {
        if (image == null) {
            notify(NotificationType.ERROR, "Not a readable PNG");
            return;
        }
        int imageWidth = image.getWidth();
        int imageHeight = image.getHeight();
        if (imageWidth != 64 || (imageHeight != 64 && imageHeight != 32)) {
            notify(NotificationType.ERROR, "Skin must be 64x64 or 64x32, not " + imageWidth + "x" + imageHeight);
            return;
        }
        upload(imageHeight == 32 ? convertLegacySkin(image) : image, width, message);
    }

    /** 把 64x64 皮肤注册进 {@code TextureManager}（必须渲染线程，DynamicTexture 会申请 GPU 贴图）。 */
    private void upload(BufferedImage sheet, ArmWidth width, String message) {
        PlayerModelType model = width == ArmWidth.SLIM ? PlayerModelType.SLIM : PlayerModelType.WIDE;
        mc.execute(() -> {
            NativeImage pixels;
            try {
                pixels = toNativeImage(sheet);
            } catch (RuntimeException error) {
                DioxideLite.LOGGER.warn("SkinChanger: failed to convert the skin image", error);
                notify(NotificationType.ERROR, "Skin upload failed");
                return;
            }
            try {
                mc.getTextureManager().register(SKIN_TEXTURE,
                        new DynamicTexture(() -> "dioxide-custom-skin", pixels));
            } catch (RuntimeException error) {
                if (!pixels.isClosed()) {
                    pixels.close();
                }
                DioxideLite.LOGGER.warn("SkinChanger: failed to upload the skin texture", error);
                notify(NotificationType.ERROR, "Skin upload failed");
                return;
            }
            appliedImage = sheet;
            appliedWidth = width;
            registered = true;
            appliedSkin = SKIN_TEXTURE;
            appliedModel = model;
            notify(NotificationType.SUCCESS, message);
        });
    }

    // --- 64x32 -> 64x64（来源 用 1.8 的 ImageBufferDownload#parseUserSkin）-------------

    /**
     * 旧版皮肤转换的等价实现：上半部分原样复制，再把旧版唯一的右腿 (0,16) 与右臂 (40,16)
     * 水平镜像到现代布局的左腿 (16,48) / 左臂 (32,48)。
     */
    private static BufferedImage convertLegacySkin(BufferedImage legacy) {
        BufferedImage modern = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = modern.createGraphics();
        try {
            graphics.drawImage(legacy, 0, 0, null);
        } finally {
            graphics.dispose();
        }
        copyMirrored(legacy, 0, 16, modern, 16, 48, 16);
        copyMirrored(legacy, 40, 16, modern, 32, 48, 16);
        return modern;
    }

    private static void copyMirrored(BufferedImage source, int sourceX, int sourceY,
                                     BufferedImage target, int targetX, int targetY, int size) {
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                target.setRGB(targetX + x, targetY + y, source.getRGB(sourceX + (size - 1 - x), sourceY + y));
            }
        }
    }

    /** BufferedImage(ARGB) -> NativeImage(RGBA)；{@code setPixel} 内部会把 ARGB 转成 ABGR 存储。 */
    private static NativeImage toNativeImage(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        NativeImage pixels = new NativeImage(width, height, true);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                pixels.setPixel(x, y, image.getRGB(x, y));
            }
        }
        return pixels;
    }

    // --- Mojang 查询 -----------------------------------------------------------------

    /** 解析结果：皮肤 PNG 地址 + 手臂模型（来源的 {@code SkinSupport_074}）。 */
    private record Lookup(String url, ArmWidth width) {
    }

    /** 名字 -> 皮肤地址/模型；没有这个账号返回 {@code null}。 */
    private static Lookup resolveSkin(String name) throws IOException, InterruptedException {
        HttpResponse<String> profile = ClientHolder.CLIENT.send(
                HttpRequest.newBuilder(URI.create(PROFILE_ENDPOINT + name))
                        .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        // 来源：204/404 = 没有这个账号。
        if (profile.statusCode() == 204 || profile.statusCode() == 404) {
            return null;
        }
        if (profile.statusCode() != 200) {
            throw new IOException("profile lookup failed: HTTP " + profile.statusCode());
        }
        JsonObject profileJson = JsonParser.parseString(profile.body()).getAsJsonObject();
        JsonElement id = profileJson.get("id");
        if (id == null || id.getAsString().length() != 32) {
            throw new IOException("Malformed profile for " + name);
        }
        HttpResponse<String> session = ClientHolder.CLIENT.send(
                HttpRequest.newBuilder(URI.create(SESSION_ENDPOINT + id.getAsString()))
                        .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        if (session.statusCode() != 200) {
            throw new IOException("session lookup failed: HTTP " + session.statusCode());
        }
        JsonObject sessionJson = JsonParser.parseString(session.body()).getAsJsonObject();
        JsonElement properties = sessionJson.get("properties");
        if (properties == null || !properties.isJsonArray()) {
            throw new IOException("No profile properties for " + name);
        }
        for (JsonElement entry : properties.getAsJsonArray()) {
            if (!entry.isJsonObject()) {
                continue;
            }
            JsonObject property = entry.getAsJsonObject();
            JsonElement key = property.get("name");
            JsonElement value = property.get("value");
            if (key == null || value == null || !"textures".equals(key.getAsString())) {
                continue;
            }
            String decoded = new String(Base64.getDecoder().decode(value.getAsString()), StandardCharsets.UTF_8);
            JsonObject textures = JsonParser.parseString(decoded).getAsJsonObject().getAsJsonObject("textures");
            if (textures == null) {
                continue;
            }
            JsonElement skinNode = textures.get("SKIN");
            if (skinNode == null || !skinNode.isJsonObject()) {
                continue;
            }
            JsonElement url = skinNode.getAsJsonObject().get("url");
            if (url == null) {
                continue;
            }
            return new Lookup(url.getAsString(), readArmWidth(skinNode.getAsJsonObject()));
        }
        return null;
    }

    /** 来源的模型判定：textures.SKIN.metadata.model == "slim" 即 Alex 细手臂。 */
    private static ArmWidth readArmWidth(JsonObject skinNode) {
        JsonElement metadata = skinNode.get("metadata");
        if (metadata == null || !metadata.isJsonObject()) {
            return ArmWidth.DEFAULT;
        }
        JsonElement model = metadata.getAsJsonObject().get("model");
        if (model == null || !"slim".equalsIgnoreCase(model.getAsString())) {
            return ArmWidth.DEFAULT;
        }
        return ArmWidth.SLIM;
    }

    // --- 文件选择 -----------------------------------------------------------------

    /** 来源 "Browse"：只把选中的路径塞进文本框（与 来源的 FileDialogUtils 行为一致）。 */
    private void browse() {
        String selected = pickFile();
        if (selected != null) {
            skinFile.set(selected);
        }
    }

    /** 系统原生文件选择器（与 FontModule 一样走 LWJGL 的 tinyfd，避免 macOS 上的 AWT 主线程问题）。 */
    private String pickFile() {
        String startDirectory = currentDirectory();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer patterns = stack.mallocPointer(1);
            patterns.put(stack.UTF8("*.png"));
            patterns.flip();
            String selected = TinyFileDialogs.tinyfd_openFileDialog(
                    "Select a skin PNG",
                    startDirectory,
                    patterns,
                    "PNG images (*.png)",
                    false);
            return selected == null || selected.isBlank() ? null : selected;
        } catch (RuntimeException error) {
            DioxideLite.LOGGER.warn("SkinChanger: could not open the file picker", error);
            notify(NotificationType.ERROR, "Could not open file picker");
            return null;
        }
    }

    /** 文件选择器的起始目录：当前路径的父目录（来源 传的是文本框里已有的路径）。 */
    private String currentDirectory() {
        String path = skinFile.get();
        if (path == null || path.isBlank()) {
            return null;
        }
        try {
            java.nio.file.Path parent = java.nio.file.Path.of(path).toAbsolutePath().getParent();
            return parent == null ? null : parent.toString();
        } catch (RuntimeException error) {
            return null;
        }
    }

    private static void notify(NotificationType type, String message) {
        NotificationManager.INSTANCE.post(type, "SkinChanger", message);
    }
}
