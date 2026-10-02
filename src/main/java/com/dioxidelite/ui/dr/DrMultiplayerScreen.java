package com.dioxidelite.ui.dr;

import com.dioxidelite.ui.screen.AbstractSkijaScreen;

import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;

import io.github.humbleui.skija.Canvas;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.screens.ManageServerScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.client.multiplayer.ServerStatusPinger;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.network.EventLoopGroupHolder;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class DrMultiplayerScreen extends AbstractSkijaScreen {
    private final List<ServerData> servers = new ArrayList<>();
    private final ServerStatusPinger pinger = new ServerStatusPinger();
    private ExecutorService pingExecutor;
    private ServerList serverList;
    private int selected = -1;
    private long openStartMs;
    private long closeStartMs;
    private boolean closingToMain;
    private boolean returningFromManageServer;
    // DR 模式的列表版式，只在 DrTheme.active() 时使用
    private final DrPageRenderer drPage = new DrPageRenderer(new DrPageSource());

    public DrMultiplayerScreen() {
        super(Component.literal("Multiplayer"));
    }

    @Override
    protected void init() {
        openStartMs = System.currentTimeMillis();
        closeStartMs = 0L;
        closingToMain = false;
        serverList = new ServerList(minecraft);
        serverList.load();
        drPage.reset();
        reloadServers();
    }

    private void reloadServers() {
        servers.clear();
        if (serverList != null) {
            for (int i = 0; i < serverList.size(); i++) {
                servers.add(serverList.get(i));
            }
        }
        selected = servers.isEmpty() ? -1 : Math.min(selected < 0 ? 0 : selected, servers.size() - 1);
        drPage.invalidate();
        pingServers();
    }

    private void pingServers() {
        pinger.removeAll();
        if (pingExecutor == null || pingExecutor.isShutdown()) {
            pingExecutor = Executors.newSingleThreadExecutor(runnable -> {
                Thread thread = new Thread(runnable, "ElysianPro-ServerPing");
                thread.setDaemon(true);
                return thread;
            });
        }
        for (ServerData server : servers) {
            server.setState(ServerData.State.PINGING);
            server.motd = Component.empty();
            server.status = Component.empty();
            pingExecutor.execute(() -> {
                try {
                    pinger.pingServer(
                            server,
                            () -> minecraft.execute(() -> {
                                if (serverList != null) serverList.save();
                            }),
                            () -> minecraft.execute(() -> server.setState(
                                    server.protocol == SharedConstants.getCurrentVersion().protocolVersion()
                                            ? ServerData.State.SUCCESSFUL
                                            : ServerData.State.INCOMPATIBLE
                            )),
                            EventLoopGroupHolder.remote(minecraft.options.useNativeTransport())
                    );
                } catch (UnknownHostException exception) {
                    minecraft.execute(() -> server.setState(ServerData.State.UNREACHABLE));
                } catch (Exception exception) {
                    minecraft.execute(() -> server.setState(ServerData.State.UNREACHABLE));
                }
            });
        }
    }

    @Override
    public void tick() {
        super.tick();
        pinger.tick();
    }

    @Override
    protected void drawScreen(Canvas canvas) {
        int a = Math.round(255f * (closingToMain ? 1f - ease(closeProgress()) : ease(openProgress())));
        drPage.render(canvas, width, height, a);
        if (closingToMain && closeProgress() >= 1f && minecraft != null) {
            minecraft.setScreen(new DrMenuScreen());
        }
    }

    // DR 模式的底栏文案：原版第三行是复制/擦除/换章/退出，这里换成服务器列表的操作
    private static final String[] DR_ACTIONS_CN = {"进入", "添加", "编辑", "删除", "返回"};
    private static final String[] DR_ACTIONS_EN = {"JOIN", "ADD", "EDIT", "DELETE", "BACK"};

    /** 把服务器列表翻成 DR 槽位行：第一行服务器名（右侧跟延迟），第二行地址 */
    private final class DrPageSource implements DrPageRenderer.Source {
        private List<DrPage.Action> cachedActions;

        @Override
        public List<DrPage.Row> rows() {
            List<DrPage.Row> list = new ArrayList<>(servers.size());
            for (int i = 0; i < servers.size(); i++) {
                ServerData server = servers.get(i);
                list.add(new DrPage.Row(server.name == null ? "" : server.name,
                        pingText(server), server.ip == null ? "" : server.ip));
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
            return DrThemeState.isChinese ? "多人游戏" : "Multiplayer";
        }

        @Override
        public int selected() {
            return selected;
        }

        // 延迟和人数是异步刷新的，把它们折进版本号，变了才重建行
        @Override
        public int version() {
            int v = servers.size();
            for (ServerData server : servers) {
                v = v * 31 + server.state().ordinal();
                v = v * 31 + (int) server.ping;
                v = v * 31 + (server.players == null ? 0 : server.players.online());
            }
            return v;
        }
    }


    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean consumed) {
        return drClick(event) || super.mouseClicked(event, consumed);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int keyCode = event.key();
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            playBack();
            startClose();
            return true;
        }
        int confirm = drPage.confirmKey(keyCode);
        if (confirm != -2) {
            if (confirm >= 0) {
                // ?????????????
                if (selected == confirm) {
                    playClick();
                    joinSelected();
                } else {
                    selected = confirm;
                    playMove();
                }
            } else {
                playClick();
                switch (drPage.cursorActionIndex()) {
                    case 0 -> joinSelected();
                    case 1 -> addServer();
                    case 2 -> editSelectedServer();
                    case 3 -> deleteSelectedServer();
                    default -> startClose();
                }
            }
            return true;
        }
        drPage.keyPressed(keyCode);
        return true;
    }

    /** DR ???????????????????????????? */
    private boolean drClick(MouseButtonEvent event) {
        int action = drPage.actionAt(event.x(), event.y(), width, height);
        if (action >= 0) {
            playClick();
            switch (action) {
                case 0 -> joinSelected();
                case 1 -> addServer();
                case 2 -> editSelectedServer();
                case 3 -> deleteSelectedServer();
                default -> startClose();
            }
            return true;
        }
        int hit = drPage.mouseMoved(event.x(), event.y(), width, height);
        if (hit >= 0) {
            // ????????????????????
            if (selected == hit) {
                playClick();
                joinSelected();
            } else {
                selected = hit;
                playMove();
            }
        }
        return true;
    }

    private void joinSelected() {
        if (selected < 0 || selected >= servers.size()) return;
        ConnectScreen.startConnecting(this, minecraft, ServerAddress.parseString(servers.get(selected).ip), servers.get(selected), false, null);
    }

    private void addServer() {
        returningFromManageServer = true;
        ServerData data = new ServerData("", "", ServerData.Type.OTHER);
        minecraft.setScreen(new ManageServerScreen(this, Component.literal("Add Server"), ok -> {
            if (ok) {
                serverList.add(data, false);
                serverList.save();
                reloadServers();
            }
            returnFromOverlay();
        }, data));
    }

    private void editSelectedServer() {
        if (selected < 0 || selected >= servers.size()) return;
        returningFromManageServer = true;
        ServerData data = servers.get(selected);
        minecraft.setScreen(new ManageServerScreen(this, Component.literal("Edit Server"), ok -> {
            if (ok) {
                serverList.replace(selected, data);
                serverList.save();
                reloadServers();
            }
            returnFromOverlay();
        }, data));
    }

    private void deleteSelectedServer() {
        if (selected < 0 || selected >= servers.size()) return;
        serverList.remove(servers.get(selected));
        serverList.save();
        reloadServers();
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        drPage.mouseMoved(mouseX, mouseY, width, height);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        drPage.mouseScrolled(verticalAmount);
        return true;
    }


    @Override
    public void onClose() {
        startClose();
    }

    @Override
    public void removed() {
        if (returningFromManageServer) {
            returningFromManageServer = false;
        } else {
            pinger.removeAll();
            if (pingExecutor != null) {
                pingExecutor.shutdownNow();
                pingExecutor = null;
            }
        }
        super.removed();
    }

    private void startClose() {
        if (closingToMain) return;
        closingToMain = true;
        closeStartMs = System.currentTimeMillis();
    }

    private float openProgress() {
        return Math.max(0f, Math.min(1f, (System.currentTimeMillis() - openStartMs) / 440f));
    }

    private float closeProgress() {
        if (!closingToMain || closeStartMs <= 0L) return 0f;
        return Math.max(0f, Math.min(1f, (System.currentTimeMillis() - closeStartMs) / 440f));
    }

    private float ease(float value) {
        float t = 1f - Math.max(0f, Math.min(1f, value));
        return 1f - t * t * t;
    }



    private void returnFromOverlay() {
        minecraft.setScreen(this);
    }

    private void playClick() {
        DrSound.play(DrSound.Sfx.SELECT);
    }

    /** DR 模式下移动光标/换选中项的提示音 */
    private void playMove() {
        DrSound.play(DrSound.Sfx.MOVE);
    }

    /** DR 模式的返回提示音，Esc / 返回项收尾时用 */
    private void playBack() {
        DrSound.play(DrSound.Sfx.BACK);
    }


    private String pingText(ServerData server) {
        return switch (server.state()) {
            case INITIAL, PINGING -> "Pinging...";
            case UNREACHABLE -> "Offline";
            case INCOMPATIBLE, SUCCESSFUL -> server.ping >= 0L ? server.ping + " ms" : "-- ms";
        };
    }




}
