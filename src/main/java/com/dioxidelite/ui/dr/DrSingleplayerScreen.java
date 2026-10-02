package com.dioxidelite.ui.dr;

import com.dioxidelite.ui.screen.AbstractSkijaScreen;

import org.lwjgl.glfw.GLFW;
import io.github.humbleui.skija.Canvas;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.EditWorldScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.LevelSummary;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class DrSingleplayerScreen extends AbstractSkijaScreen {
    private static final long OPEN_MS = 440L;
    private static final long CLOSE_MS = 440L;
    private final List<WorldEntry> worlds = new ArrayList<>();
    private CompletableFuture<?> loadFuture;
    private long openStartMs;
    private long closeStartMs;
    private boolean closingToMain;
    private int selected = -1;
    private int lastWorldClick = -1;
    private long lastWorldClickMs;
    private boolean loading = true;
    private String loadError = "";
    private long contentReadyMs = 0L;
    // DR 模式的列表版式，只在 DrTheme.active() 时使用，卡片布局不受影响
    private final DrPageRenderer drPage = new DrPageRenderer(new DrPageSource());

    public DrSingleplayerScreen() {
        super(Component.literal("Single player"));
    }

    @Override
    protected void init() {
        openStartMs = System.currentTimeMillis();
        closeStartMs = 0L;
        closingToMain = false;
        drPage.reset();
        loadWorlds();
    }


    private void loadWorlds() {
        if (minecraft == null) return;
        loading = true;
        loadError = "";
        contentReadyMs = 0L;
        drPage.invalidate();
        LevelStorageSource source = minecraft.getLevelSource();
        loadFuture = source.loadLevelSummaries(source.findLevelCandidates()).thenAccept(summaries -> {
            List<WorldEntry> loaded = summaries.stream()
                    .sorted(Comparator.comparingLong(LevelSummary::getLastPlayed).reversed())
                    .map(WorldEntry::new)
                    .toList();
            Minecraft.getInstance().execute(() -> {
                worlds.clear();
                worlds.addAll(loaded);
                selected = worlds.isEmpty() ? -1 : Math.max(0, Math.min(selected, worlds.size() - 1));
                loading = false;
                contentReadyMs = System.currentTimeMillis();
                drPage.invalidate();
            });
        }).exceptionally(t -> {
            Minecraft.getInstance().execute(() -> {
                loading = false;
                contentReadyMs = System.currentTimeMillis();
                loadError = t.getMessage() == null ? t.getClass().getSimpleName() : t.getMessage();
            });
            return null;
        });
    }

    @Override
    protected void drawScreen(Canvas canvas) {
        // 关到主菜单时按 closeProgress 淡出，别直接归零，不然画面是「啪」一下全黑
        float fade = closingToMain ? 1f - ease(closeProgress()) : revealFade();
        int a = Math.round(255f * Math.max(0f, Math.min(1f, fade)));
        drPage.render(canvas, width, height, a);
        if (closingToMain && closeProgress() >= 1f && minecraft != null) {
            minecraft.setScreen(new DrMenuScreen());
        }
    }

    // DR 模式的底栏文案：原版第三行是复制/擦除/换章/退出，这里换成世界列表的操作
    private static final String[] DR_ACTIONS_CN = {"进入游戏", "新建世界", "编辑", "删除", "刷新", "返回"};
    private static final String[] DR_ACTIONS_EN = {"PLAY", "CREATE", "EDIT", "DELETE", "REFRESH", "BACK"};

    private DrPageRenderer drPage() {
        return drPage;
    }


    private float revealFade() {
        long now = System.currentTimeMillis();
        long start = contentReadyMs > 0L ? contentReadyMs : openStartMs;
        long elapsed = now - start;
        if (elapsed <= 0L) return 0f;
        return ease(Math.min(1f, elapsed / 280f));
    }


    @Override
    public boolean keyPressed(KeyEvent event) {
        int keyCode = event.key();
        // DR 版式是纯键盘可玩的，方向键走格、回车确认、Esc 返回
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            playBackSound();
            startClose();
            return true;
        }
        int confirm = drPage.confirmKey(keyCode);
        if (confirm != -2) {
            if (confirm >= 0) {
                // 回车先选中，再回车一次才进世界
                if (selected == confirm) {
                    playClickSound();
                    openSelectedWorld();
                } else {
                    selected = confirm;
                    playMoveSound();
                }
            } else {
                playClickSound();
                drRunAction(drPage.cursorActionIndex());
            }
            return true;
        }
        drPage.keyPressed(keyCode);
        return true;
    }

    private void drRunAction(int action) {
        switch (action) {
            case 0 -> openSelectedWorld();
            case 1 -> createWorld();
            case 2 -> openEditScreen();
            case 3 -> deleteSelectedWorld();
            case 4 -> loadWorlds();
            default -> startClose();
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        drPage.mouseScrolled(verticalAmount);
        return true;
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        drPage.mouseMoved(mouseX, mouseY, width, height);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean consumed) {
        if (closingToMain) return true;
        return drClick(event);
    }

    /** DR 版式的点击：底栏文案按序触发，槽位要再点一次同一个才进世界，跟非 DR 模式的语义对齐 */
    private boolean drClick(MouseButtonEvent event) {
        int action = drPage.actionAt(event.x(), event.y(), width, height);
        if (action >= 0) {
            playClickSound();
            switch (action) {
                case 0 -> openSelectedWorld();
                case 1 -> createWorld();
                case 2 -> openEditScreen();
                case 3 -> deleteSelectedWorld();
                case 4 -> loadWorlds();
                default -> startClose();
            }
            return true;
        }
        int hit = drPage.mouseMoved(event.x(), event.y(), width, height);
        if (hit >= 0) {
            long now = System.currentTimeMillis();
            // 和本页非 DR 版式用同一套双击窗口：第一次只选中，第二次才进
            if (lastWorldClick == hit && now - lastWorldClickMs <= 350L) {
                lastWorldClick = -1;
                lastWorldClickMs = 0L;
                selected = hit;
                playClickSound();
                openSelectedWorld();
                return true;
            }
            selected = hit;
            lastWorldClick = hit;
            lastWorldClickMs = now;
            playMoveSound();
        }
        return true;
    }

    private void createWorld() {
        if (minecraft == null) return;
        CreateWorldScreen.openFresh(minecraft, () -> {
            loadWorlds();
            minecraft.setScreen(this);
        });
    }


    private void openSelectedWorld() {
        if (minecraft == null || selected < 0 || selected >= worlds.size()) return;
        minecraft.createWorldOpenFlows().openWorld(worlds.get(selected).summary().getLevelId(), () -> minecraft.setScreen(this));
    }

    private void openEditScreen() {
        if (minecraft == null || selected < 0 || selected >= worlds.size()) return;
        try {
            LevelStorageSource.LevelStorageAccess access = minecraft.getLevelSource().createAccess(worlds.get(selected).summary().getLevelId());
            minecraft.setScreen(EditWorldScreen.create(minecraft, access, result -> {
                access.safeClose();
                loadWorlds();
                minecraft.setScreen(this);
            }));
        } catch (IOException ignored) {
        }
    }

    private void deleteSelectedWorld() {
        if (minecraft == null || selected < 0 || selected >= worlds.size()) return;
        WorldEntry world = worlds.get(selected);
        minecraft.setScreen(new ConfirmScreen(yes -> {
            if (yes) {
                try (LevelStorageSource.LevelStorageAccess access = minecraft.getLevelSource().createAccess(world.summary().getLevelId())) {
                    access.deleteLevel();
                } catch (IOException ignored) {
                }
                selected = -1;
                loadWorlds();
            }
            minecraft.setScreen(this);
        }, Component.literal("Delete World"), Component.literal(world.name())));
    }


    private void startClose() {
        if (closingToMain) return;
        closingToMain = true;
        closeStartMs = System.currentTimeMillis();
    }

    private void playClickSound() {
        DrSound.play(DrSound.Sfx.SELECT);
    }

    /** DR 模式下移动光标/换选中项的提示音 */
    private void playMoveSound() {
        DrSound.play(DrSound.Sfx.MOVE);
    }

    /** DR 模式的返回提示音 */
    private void playBackSound() {
        DrSound.play(DrSound.Sfx.BACK);
    }

    @Override
    public void onClose() {
        startClose();
    }

    @Override
    public void removed() {
        super.removed();
    }



    private float closeProgress() {
        if (!closingToMain || closeStartMs <= 0L) return 0f;
        return Math.max(0f, Math.min(1f, (System.currentTimeMillis() - closeStartMs) / (float) CLOSE_MS));
    }



    private float ease(float v) {
        float t = 1f - Math.max(0f, Math.min(1f, v));
        return 1f - t * t * t;
    }

    private record WorldEntry(LevelSummary summary) {
        private String name() {
            return summary.getLevelName();
        }

        /**
         * 原版槽位第一行右侧的尾随文本放的是 REFRESH 这类短标记，
         * 这里换成「版本 · 模式」的紧凑写法：getInfo() 原本是一句
         * 「创造模式的世界，版本：1.21.11」，整句塞进第一行会顶出框，
         * 拆成版本号 + 模式名两个短段，宽度才压得住。
         */
        private String info() {
            return summary.getWorldVersionName().getString() + " " + modeShort();
        }

        /** 原版槽位第二行放地点（PLACE[i]），世界列表里最贴近的是游戏模式名 */
        private String subtitle() {
            return summary.getGameMode().getLongDisplayName().getString();
        }

        /** 模式名比版本号长，第二行已经整名显示了，这里只在尾随位留一个短代号省宽度 */
        private String modeShort() {
            return switch (summary.getGameMode()) {
                case CREATIVE -> DrThemeState.isChinese ? "创造" : "Creative";
                case SURVIVAL -> DrThemeState.isChinese ? "生存" : "Survival";
                case ADVENTURE -> DrThemeState.isChinese ? "冒险" : "Adventure";
                case SPECTATOR -> DrThemeState.isChinese ? "旁观" : "Spectator";
            };
        }
    }

    /** 把世界列表翻成 DR 槽位行：第一行世界名（右侧跟最后游玩），第二行存档信息 */
    private final class DrPageSource implements DrPageRenderer.Source {
        private List<DrPage.Action> cachedActions;

        @Override
        public List<DrPage.Row> rows() {
            List<DrPage.Row> list = new ArrayList<>(worlds.size());
            for (WorldEntry world : worlds) {
                list.add(new DrPage.Row(world.name(), world.info(), world.subtitle()));
            }
            return list;
        }

        @Override
        public List<DrPage.Action> actions() {
            String[] labels = DrThemeState.isChinese ? DR_ACTIONS_CN : DR_ACTIONS_EN;
            if (cachedActions == null) {
                cachedActions = DrPage.layoutActions(labels);
            }
            return cachedActions;
        }

        @Override
        public String title() {
            return DrThemeState.isChinese ? "单人游戏" : "Single player";
        }

        @Override
        public int selected() {
            return selected;
        }

        // 列表内容只随加载状态和条数变，选中态另有 selected() 走，不参与行缓存
        @Override
        public int version() {
            return worlds.size() * 31 + (loading ? 7 : 0) + (loadError.isEmpty() ? 0 : 13);
        }
    }


}
