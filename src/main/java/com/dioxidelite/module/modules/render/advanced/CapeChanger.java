package com.dioxidelite.module.modules.render.advanced;

import com.dioxidelite.DioxideLite;
import com.dioxidelite.event.Listen;
import com.dioxidelite.event.events.TickEvent;
import com.dioxidelite.module.Category;
import com.dioxidelite.module.Module;
import com.dioxidelite.notification.NotificationManager;
import com.dioxidelite.notification.NotificationType;
import com.dioxidelite.setting.settings.ButtonSetting;
import com.dioxidelite.setting.settings.ColorSetting;
import com.dioxidelite.setting.settings.DoubleSetting;
import com.dioxidelite.setting.settings.EnumSetting;
import com.dioxidelite.setting.settings.StringSetting;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 本地披风替换（移植自 来源客户端 {@code features/render/CapeChanger}：
 * "Overrides your own cape texture locally"）。
 *
 * <p>DIOXIDE 源：生成 来源风格的默认披风（来源的 {@code RenderSupport_135}：512x256 画布 + 垂直渐变 +
 * 面板 + 8px 描边框 + logo），本项目把 {@code custom:textures/gui/logo.png} 换成
 * {@code dioxide-lite:textures/hud/dioxide_logo.png}，其余坐标/颜色/线宽 1:1。</p>
 *
 * <p>FILE 源：读磁盘 PNG，按 来源的 {@code RenderSupport_137} 合成为披风贴图
 * （64x32 的 u 倍画布、(12,1) 的深色底、(1,1) 起的 10x16 披风区、Fit/Fill 缩放 + Zoom + Offset +
 * Background 合成），再用 {@code TextureManager#register} 注册成 {@code dioxide-lite:custom/capes/local}。</p>
 *
 * <p>贴图上传必须走渲染线程（{@code DynamicTexture} 会申请 GPU 贴图），IO/合成在 daemon 线程池里做，
 * 与 来源的 {@code custom-cape-loader} 一致；异步结果带序号，过期结果直接丢弃（来源的 {@code h} 计数器）。</p>
 */
// PORT-NOTE: 需要 AbstractClientPlayer#getSkin()（或 PlayerInfo#getSkin）的 mixin hook 才能真正把本地披风挂到玩家身上；
// 本端口只实现了 1:1 设置面、默认/PNG 披风合成、贴图注册（dioxide-lite:custom/capes/local）与 appliedCapeTexture() 只读入口，
// 未实现的还有 来源的拖拽编辑 GUI（UiSupport_562）。
public final class CapeChanger extends Module {

    public static final CapeChanger INSTANCE = new CapeChanger();

    /** 来源 {@code FeatureMode_292}：Source —— 内置 来源 logo 披风，或磁盘上的 PNG。 */
    public enum Source {
        DIOXIDE,
        FILE
    }

    /** 来源 {@code RenderMode_134}：Fit —— 完整显示（留背景）/ 裁满披风 / 直接把 PNG 当成品披风贴图。 */
    public enum Fit {
        FIT,
        FILL,
        TEXTURE
    }

    // --- 来源 RenderSupport_137 的披风贴图布局（均以 u = 源图宽度/64 为单位）-------------

    private static final int SHEET_WIDTH = 64;
    private static final int SHEET_HEIGHT = 32;
    /** 披风绘制区起点：来源的 setClip(1*u, 1*u, ...) 与 drawImage(1*u + rect, ...)。 */
    private static final int CAPE_DRAW_X = 1;
    private static final int CAPE_DRAW_Y = 1;
    /** 披风区尺寸：来源的 n2 = 10*u、n3 = 16*u。 */
    private static final int CAPE_WIDTH = 10;
    private static final int CAPE_HEIGHT = 16;
    /** 披风区内侧的深色底：来源的 fillRect(12*u, 1*u, 10*u, 16*u)。 */
    private static final int CAPE_BACKDROP_X = 12;
    private static final int CAPE_BACKDROP_Y = 1;
    /** 背景覆盖范围：来源的 fillRect(0, 0, 22*u, 17*u)。 */
    private static final int BACKGROUND_WIDTH = 22;
    private static final int BACKGROUND_HEIGHT = 17;
    /** 来源 常量 k = 0.7f：披风区深色底的压暗系数。 */
    private static final float BACKDROP_DARKEN = 0.7F;

    // --- 来源 RenderSupport_135 的默认披风画布 ----------------------------------------

    private static final int DEFAULT_CANVAS_WIDTH = 512;
    private static final int DEFAULT_CANVAS_HEIGHT = 256;
    private static final Color DEFAULT_GRADIENT_TOP = new Color(0xFF23242C, true);
    private static final Color DEFAULT_GRADIENT_BOTTOM = new Color(0xFF0C0C11, true);
    private static final Color DEFAULT_PANEL = new Color(0xFF0A0A0E, true);
    private static final Color DEFAULT_FRAME = new Color(0x22FFFFFF, true);

    /** 本地披风贴图 id（来源: {@code custom:capes/local}）。 */
    private static final Identifier CAPE_TEXTURE = Identifier.fromNamespaceAndPath(DioxideLite.MOD_ID, "custom/capes/local");

    /** 来源 读的是 {@code custom:textures/gui/logo.png}；本项目同位置资源是 dioxide_logo.png。 */
    private static final Identifier LOGO_TEXTURE =
            Identifier.fromNamespaceAndPath(DioxideLite.MOD_ID, "textures/hud/dioxide_logo.png");

    /** 来源的 {@code custom-cape-loader}：单线程 daemon，只做读盘 + 合成。 */
    private static final ExecutorService LOADER = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "custom-cape-loader");
        thread.setDaemon(true);
        return thread;
    });

    public final EnumSetting<Source> source = add(new EnumSetting<>("Source", Source.DIOXIDE));

    /** 来源 "Cape file"：披风 PNG 的完整路径（仅 FILE 源）。 */
    public final StringSetting capeFile = add(new StringSetting("Cape file", "")
            .visibleWhen(() -> source.is(Source.FILE)));

    /** 来源 "Browse"：打开系统文件选择器填路径（只填路径，不自动应用，与原版一致）。 */
    public final ButtonSetting browse = add(new ButtonSetting("Browse", this::browse)
            .visibleWhen(() -> source.is(Source.FILE)));

    /** 来源 "Apply cape"：读入并应用当前路径的 PNG。 */
    public final ButtonSetting apply = add(new ButtonSetting("Apply cape", this::applyCape)
            .visibleWhen(() -> source.is(Source.FILE)));

    /** 来源 "Fit"：Fit 完整显示、Fill 裁满披风、Texture 直接把 PNG 当成品披风贴图。 */
    public final EnumSetting<Fit> fit = add(new EnumSetting<>("Fit", Fit.FIT)
            .visibleWhen(() -> source.is(Source.FILE)));

    /** 来源 "Background"，默认 {@code -15329507} = {@code 0xFF16171D}。 */
    public final ColorSetting background = add(new ColorSetting("Background", new Color(0xFF16171D, true))
            .visibleWhen(() -> source.is(Source.FILE) && !fit.is(Fit.TEXTURE)));

    /** 来源 "Zoom"（%），10..400 / 步进 5。 */
    public final DoubleSetting zoom = add(new DoubleSetting("Zoom", 100.0, 10.0, 400.0, 5.0)
            .visibleWhen(() -> source.is(Source.FILE) && !fit.is(Fit.TEXTURE)));

    /** 来源 "Offset X"（%），-100..100 / 步进 1。 */
    public final DoubleSetting offsetX = add(new DoubleSetting("Offset X", 0.0, -100.0, 100.0, 1.0)
            .visibleWhen(() -> source.is(Source.FILE) && !fit.is(Fit.TEXTURE)));

    /** 来源 "Offset Y"（%），-100..100 / 步进 1。 */
    public final DoubleSetting offsetY = add(new DoubleSetting("Offset Y", 0.0, -100.0, 100.0, 1.0)
            .visibleWhen(() -> source.is(Source.FILE) && !fit.is(Fit.TEXTURE)));

    /** 当前已注册的披风贴图；{@code null} = 不在覆盖状态（hook 侧只读这个值）。 */
    private static volatile Identifier appliedCape;

    /** 已加载的源图（来源 字段 {@code a}）：Fit/Zoom/Offset/Background 改动时用它重新合成。 */
    private volatile BufferedImage sourceImage;

    /** 上次成功加载的路径（来源 字段 {@code e}）：同一路径重复 Apply 时只重新合成，不重新读盘。 */
    private volatile String lastPath = "";

    /** 来源的 {@code h} 计数器：异步结果过期即丢弃。 */
    private final AtomicInteger generation = new AtomicInteger();

    /** 是否已注册贴图，决定 onDisable 要不要 release。 */
    private boolean registered;

    /** 配置加载阶段就启用时（GPU device 还没建好）延后到主菜单/进游戏后再应用。 */
    private boolean pendingApply;

    private CapeChanger() {
        super("CapeChanger", Category.RENDER);
        // 来源 在 Source 变化时重新应用（Source 切到 DIOXIDE 会自动生成默认披风）。
        source.onChange(value -> {
            if (isEnabled()) {
                applyCape();
            }
        });
        // 来源 在 Fit/Background/Zoom/Offset 变化时用已加载的源图重新合成（l()）。
        fit.onChange(value -> recompose());
        background.onChange(value -> recompose());
        zoom.onChange(value -> recompose());
        offsetX.onChange(value -> recompose());
        offsetY.onChange(value -> recompose());
    }

    @Override
    protected void onEnable() {
        // 配置加载阶段就会 setEnabled(true)，那时 GPU device 还没建好，
        // DynamicTexture 会拿不到贴图；这时先记下来，等主菜单/进游戏后的第一个 tick 再应用。
        if (mc.level == null && mc.screen == null) {
            pendingApply = true;
            return;
        }
        applyCurrentSource();
    }

    /** 延迟应用：等渲染器就绪后的第一个客户端 tick。 */
    @Listen
    private void onTick(TickEvent.Pre event) {
        if (!pendingApply || (mc.level == null && mc.screen == null)) {
            return;
        }
        pendingApply = false;
        applyCurrentSource();
    }

    /** 启用时按当前 Source 应用（FILE 有已加载的图就重新合成，否则走 Apply 流程）。 */
    private void applyCurrentSource() {
        if (source.is(Source.FILE)) {
            if (sourceImage != null) {
                recompose();
                return;
            }
            String path = capeFile.get();
            if (path == null || path.isBlank()) {
                return;
            }
        }
        applyCape();
    }

    @Override
    protected void onDisable() {
        appliedCape = null;
        if (registered) {
            registered = false;
            try {
                mc.getTextureManager().release(CAPE_TEXTURE);
            } catch (RuntimeException error) {
                DioxideLite.LOGGER.warn("CapeChanger: failed to release the cape texture", error);
            }
        }
    }

    /**
     * hook 侧只读入口（对应 来源的 {@code RenderSupport_136} 静态贴图 id）：
     * 非 {@code null} 表示本地披风已经注册到 {@code TextureManager}，可直接拿来做
     * {@code AbstractClientPlayer#getSkin()} 的披风覆盖。
     */
    public static Identifier appliedCapeTexture() {
        return appliedCape;
    }

    // --- 应用 ---------------------------------------------------------------------

    /** 按当前 Source 应用披风（来源的 Apply 动作 + Source 变化回调）。 */
    public synchronized void applyCape() {
        if (source.is(Source.DIOXIDE)) {
            int ticket = generation.incrementAndGet();
            LOADER.submit(() -> {
                try {
                    BufferedImage cape = drawDefaultCape();
                    upload(cape, "cape applied", ticket);
                } catch (IOException | RuntimeException error) {
                    DioxideLite.LOGGER.warn("CapeChanger: failed to draw the default cape", error);
                    notify(NotificationType.ERROR, "Can't draw the default cape");
                }
            });
            return;
        }
        String path = capeFile.get();
        if (path == null || path.isBlank()) {
            notify(NotificationType.WARNING, "Pick a cape PNG first");
            return;
        }
        if (sourceImage != null && path.equals(lastPath)) {
            // 同一张图重复 Apply：只按当前 Fit/Zoom/Offset 重新合成（来源 l()）。
            recompose();
            return;
        }
        int ticket = generation.incrementAndGet();
        LOADER.submit(() -> {
            BufferedImage loaded;
            try {
                loaded = ImageIO.read(new File(path));
            } catch (IOException error) {
                DioxideLite.LOGGER.warn("CapeChanger: failed to read '{}'", path, error);
                notify(NotificationType.ERROR, "Can't read that file");
                return;
            }
            if (loaded == null) {
                notify(NotificationType.ERROR, "Not a readable PNG");
                return;
            }
            if (ticket != generation.get()) {
                return;
            }
            sourceImage = loaded;
            lastPath = path;
            composeAndUpload(loaded, "Cape applied");
        });
    }

    /** 来源 {@code l()}：用已加载的源图按当前 Fit/Zoom/Offset/Background 重新合成并上传。 */
    public void recompose() {
        BufferedImage image = sourceImage;
        if (image == null || !isEnabled() || !source.is(Source.FILE)) {
            return;
        }
        composeAndUpload(image, "Cape updated");
    }

    /** 合成（Fit/Fill + Zoom + Offset + Background）后交给渲染线程注册。 */
    private void composeAndUpload(BufferedImage image, String message) {
        Fit mode = fit.get();
        int backgroundArgb = background.argb();
        double zoomValue = zoom.get() / 100.0;
        double offsetXValue = offsetX.get() / 100.0;
        double offsetYValue = offsetY.get() / 100.0;
        int ticket = generation.incrementAndGet();
        LOADER.submit(() -> {
            BufferedImage sheet;
            try {
                sheet = compose(image, mode, backgroundArgb, zoomValue, offsetXValue, offsetYValue);
            } catch (RuntimeException error) {
                DioxideLite.LOGGER.warn("CapeChanger: failed to compose the cape", error);
                notify(NotificationType.ERROR, "Cape compose failed");
                return;
            }
            upload(sheet, message, ticket);
        });
    }

    /** 把合成好的贴图注册进 {@code TextureManager}（必须渲染线程，DynamicTexture 会申请 GPU 贴图）。 */
    private void upload(BufferedImage sheet, String message, int ticket) {
        mc.execute(() -> {
            if (ticket != generation.get()) {
                return;
            }
            NativeImage pixels;
            try {
                pixels = toNativeImage(sheet);
            } catch (RuntimeException error) {
                DioxideLite.LOGGER.warn("CapeChanger: failed to convert the cape image", error);
                notify(NotificationType.ERROR, "Cape upload failed");
                return;
            }
            try {
                mc.getTextureManager().register(CAPE_TEXTURE,
                        new DynamicTexture(() -> "dioxide-custom-cape", pixels));
            } catch (RuntimeException error) {
                if (!pixels.isClosed()) {
                    pixels.close();
                }
                DioxideLite.LOGGER.warn("CapeChanger: failed to upload the cape texture", error);
                notify(NotificationType.ERROR, "Cape upload failed");
                return;
            }
            registered = true;
            appliedCape = CAPE_TEXTURE;
            notify(NotificationType.SUCCESS, message);
        });
    }

    // --- 默认（来源 logo）披风：RenderSupport_135 的 1:1 移植 --------------------------

    /** 生成 来源风格的默认披风画布（512x256：渐变底 + 面板 + 描边框 + logo）。 */
    private BufferedImage drawDefaultCape() throws IOException {
        BufferedImage canvas = new BufferedImage(DEFAULT_CANVAS_WIDTH, DEFAULT_CANVAS_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = canvas.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            // 来源: new GradientPaint(0, 0, -14474196, 0, 136, -15987695) + fillRect(0, 0, 176, 136)
            graphics.setPaint(new GradientPaint(0.0F, 0.0F, DEFAULT_GRADIENT_TOP, 0.0F, 136.0F, DEFAULT_GRADIENT_BOTTOM));
            graphics.fillRect(0, 0, 176, 136);
            // 来源: setPaint(-16119282) + fillRect(96, 8, 80, 128)
            graphics.setPaint(DEFAULT_PANEL);
            graphics.fillRect(96, 8, 80, 128);
            // 来源: setPaint(0x22FFFFFF) + stroke 8.0f + drawRect(20, 20, 56, 104)
            graphics.setPaint(DEFAULT_FRAME);
            graphics.setStroke(new BasicStroke(8.0F));
            graphics.drawRect(20, 20, 56, 104);
            BufferedImage logo = readLogo();
            if (logo != null) {
                int logoSize = 56;
                int logoHeight = Math.round((float) logoSize * logo.getHeight() / logo.getWidth());
                graphics.drawImage(logo, 8 + (80 - logoSize) / 2, 8 + (128 - logoHeight) / 2,
                        logoSize, logoHeight, null);
            }
        } finally {
            graphics.dispose();
        }
        return canvas;
    }

    /** 读默认披风用的 logo；资源缺失时只画底色（来源 会直接报错，这里做了降级）。 */
    private BufferedImage readLogo() throws IOException {
        Optional<Resource> resource = mc.getResourceManager().getResource(LOGO_TEXTURE);
        if (resource.isEmpty()) {
            DioxideLite.LOGGER.warn("CapeChanger: {} is missing, drawing the default cape without a logo", LOGO_TEXTURE);
            return null;
        }
        try (InputStream stream = resource.get().open()) {
            return ImageIO.read(stream);
        }
    }

    // --- PNG -> 披风贴图：RenderSupport_137 的 1:1 移植 -------------------------------

    /**
     * 把任意 PNG 合成为披风贴图（来源 {@code RenderSupport_137.L(BufferedImage, RenderMode, int, double, double, double)}）：
     * 输出为 {@code (64 x 32) * u}，u = 源图宽度 / 64（至少 1）；
     * 先铺 22x17 的背景色、再在 (12,1) 铺 10x16 的压暗底，最后把缩放后的图裁进 (1,1) 起的 10x16 披风区。
     */
    private static BufferedImage compose(BufferedImage source, Fit mode, int backgroundArgb,
                                         double zoom, double offsetX, double offsetY) {
        if (mode == Fit.TEXTURE) {
            // 来源: CapeUtils.parseCape(image) —— 这张 PNG 本身就是成品披风贴图，原样使用。
            return source;
        }
        int unit = Math.max(1, source.getWidth() / SHEET_WIDTH);
        int capeWidth = CAPE_WIDTH * unit;
        int capeHeight = CAPE_HEIGHT * unit;
        BufferedImage sheet = new BufferedImage(SHEET_WIDTH * unit, SHEET_HEIGHT * unit, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = sheet.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            Color base = new Color(backgroundArgb | 0xFF000000, true);
            graphics.setPaint(base);
            graphics.fillRect(0, 0, BACKGROUND_WIDTH * unit, BACKGROUND_HEIGHT * unit);
            graphics.setPaint(darken(base));
            graphics.fillRect(CAPE_BACKDROP_X * unit, CAPE_BACKDROP_Y * unit, capeWidth, capeHeight);
            double[] rect = fitRect(source, mode, capeWidth, capeHeight, zoom, offsetX, offsetY);
            int drawX = CAPE_DRAW_X * unit;
            int drawY = CAPE_DRAW_Y * unit;
            graphics.setClip(drawX, drawY, capeWidth, capeHeight);
            graphics.drawImage(source,
                    drawX + (int) Math.round(rect[0]),
                    drawY + (int) Math.round(rect[1]),
                    Math.max(1, (int) Math.round(rect[2])),
                    Math.max(1, (int) Math.round(rect[3])),
                    null);
        } finally {
            graphics.dispose();
        }
        return sheet;
    }

    /**
     * Fit/Fill 缩放 + Zoom + Offset，返回 {@code [x, y, width, height]}（来源 {@code RenderSupport_137} 的拟合函数）：
     * Fit 取 min 比例（整图可见、四周露背景），Fill 取 max 比例（裁满披风区），再乘 Zoom 并居中叠加百分比偏移。
     */
    private static double[] fitRect(BufferedImage source, Fit mode, double targetWidth, double targetHeight,
                                    double zoom, double offsetX, double offsetY) {
        double scale = mode == Fit.FILL
                ? Math.max(targetWidth / source.getWidth(), targetHeight / source.getHeight())
                : Math.min(targetWidth / source.getWidth(), targetHeight / source.getHeight());
        double width = source.getWidth() * scale * zoom;
        double height = source.getHeight() * scale * zoom;
        double x = (targetWidth - width) / 2.0 + offsetX * targetWidth;
        double y = (targetHeight - height) / 2.0 + offsetY * targetHeight;
        return new double[]{x, y, width, height};
    }

    /** 来源 常量 {@code k = 0.7f}：背景色的压暗版本，用作披风区底。 */
    private static Color darken(Color color) {
        return new Color(
                Math.round(color.getRed() * BACKDROP_DARKEN),
                Math.round(color.getGreen() * BACKDROP_DARKEN),
                Math.round(color.getBlue() * BACKDROP_DARKEN),
                color.getAlpha());
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

    // --- 文件选择 -----------------------------------------------------------------

    /** 来源 "Browse"：只把选中的路径塞进文本框（与 来源的 FileDialogUtils 行为一致）。 */
    private void browse() {
        String selected = pickFile();
        if (selected != null) {
            capeFile.set(selected);
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
                    "Select a cape PNG",
                    startDirectory,
                    patterns,
                    "PNG images (*.png)",
                    false);
            return selected == null || selected.isBlank() ? null : selected;
        } catch (RuntimeException error) {
            DioxideLite.LOGGER.warn("CapeChanger: could not open the file picker", error);
            notify(NotificationType.ERROR, "Could not open file picker");
            return null;
        }
    }

    /** 文件选择器的起始目录：当前路径的父目录（来源 传的是文本框里已有的路径）。 */
    private String currentDirectory() {
        String path = capeFile.get();
        if (path == null || path.isBlank()) {
            return null;
        }
        try {
            Path parent = Path.of(path).toAbsolutePath().getParent();
            return parent == null ? null : parent.toString();
        } catch (RuntimeException error) {
            return null;
        }
    }

    private static void notify(NotificationType type, String message) {
        NotificationManager.INSTANCE.post(type, "CapeChanger", message);
    }
}
