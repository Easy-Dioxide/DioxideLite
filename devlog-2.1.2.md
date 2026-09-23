# DioxideLite v2.1.2 Devlog

> 发布日期：2026-09-23

---

## 修复

### Windows 运行失败（关键修复）
**问题**：v2.1.1 的 jar 中缺少 Skija、ViaVersion、Luaj、WebRTC、Cadence、Kotlin 等运行时依赖，Windows 上启动直接报 `NoClassDefFoundError`。

**原因**：`build.gradle.kts` 中这些依赖用的是 `implementation`（只编译时可用），没有用 `include` 打包进 jar。

**修复**：全部改为 `include`，Fabric Loom 自动打包成 nested jar：
- nested jars 从 67 个 → 94 个
- jar 体积从 85M → 96M
- 新增打包：Skija (Windows + Linux)、ViaVersion 全系列、Luaj、WebRTC、Cadence、Kotlin stdlib、JJWT、MinecraftAuth、ViaLegacy、ViaBedrock 等

---

## 清理

### 去除 setsuna 字样
全量替换所有 setsuna/setsunavia 字样为 dioxidelite/dioxidelitevia：
- 注释中的移植来源说明
- 包名 `com.viaversion.setsunavia` → `com.viaversion.dioxidelitevia`
- `fabric.mod.json` entrypoint / provides / custom 字段
- `mixins.json` package 名
- lang 文件中的翻译键和用户可见文本
- 资源目录 `assets/setsunavia/` → `assets/dioxidelitevia/`

---

## 自定义

### 主菜单背景
- 默认背景图替换为自定义角色背景（cover 模式，居中裁剪不拉伸）
- 支持用户通过 Options → Import 导入自定义背景图
- 支持 Reset 恢复默认

---

## 验证

- 类加载测试：5/5 关键类全部 OK
- 游戏启动：主菜单正常渲染，窗口标题 `DioxideLite 2.1.2`
- ClickGUI：右 Shift 正常打开
- 水牌：左上角 `DIOXIDELITE 2.1.2` 正常显示
