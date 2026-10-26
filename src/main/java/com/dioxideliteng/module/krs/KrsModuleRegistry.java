package com.dioxideliteng.module.krs;

import com.dioxideliteng.krs.hacks.Module;
import com.dioxideliteng.module.Feature;
import com.dioxideliteng.module.FeatureManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * KRS 模块的注册中心。
 *
 * <p>把 KRS 的每一个功能模块包装成 {@link KrsModuleAdapter}（本质是 NG 的 {@link Feature}），
 * 再交给 NG 的 {@code FeatureManager}，使其出现在 ClickGui 的对应分类里。</p>
 *
 * <p><b>命名规则</b>：KRS 模块与 NG 已有模块同名（类名或显示名，忽略大小写）时，
 * 在名称后加 "2"，例如 {@code KillAura} → {@code KillAura2}。
 * 类名与显示名同步改名，避免界面出现两个同名条目。</p>
 */
public final class KrsModuleRegistry {

    private static final List<KrsModuleAdapter> ADAPTERS = new ArrayList<>();

    /**
     * {@code ModuleManager.devModules} 中需要移植到界面的模块。
     * 这些模块只是被 KRS 放在 dev 分组，实际是正常功能。
     */
    private static final Set<String> DEV_WHITELIST = Set.of(
            "FukumaiPlayerTracker",
            "PluginsDetector"
    );

    private static boolean registered = false;

    private KrsModuleRegistry() {
    }

    /**
     * 建立桥接对象。必须在 {@code KrsClient.init()} 之后、
     * {@code FeatureManager.registerModules()} 结束之后调用：需要先知道 NG 已有哪些名字。
     */
    public static synchronized List<KrsModuleAdapter> createAdapters() {
        if (registered) {
            return ADAPTERS;
        }
        registered = true;

        Set<String> takenNames = new HashSet<>();
        for (Feature feature : FeatureManager.getModules()) {
            takenNames.add(normalize(feature.getName()));
            takenNames.add(normalize(feature.getClass().getSimpleName()));
        }

        for (Module module : collectModules()) {
            // KRS 的显示名带空格（"Kill Aura"），NG 界面统一是紧凑写法（"KillAura"），
            // 这里抹掉空格保持界面风格一致；重名判定本来就忽略空格，因此不影响结果。
            String displayName = toDisplayName(module.moduleName);
            String unique = uniqueName(displayName, takenNames);
            takenNames.add(normalize(unique));
            takenNames.add(normalize(module.getClass().getSimpleName()));
            ADAPTERS.add(new KrsModuleAdapter(module, unique));
        }
        return ADAPTERS;
    }

    /** 去掉 KRS 显示名里的空格，与 NG 的命名风格对齐。 */
    private static String toDisplayName(String krsName) {
        return krsName == null ? "" : krsName.replaceAll("\\s+", "");
    }

    /**
     * 参与移植的 KRS 模块集合。
     *
     * <p>主力列表是 {@code ModuleManager.modules}；此外 {@code ModuleManager.devModules} 里
     * 有几个并非调试用途、而是用户明确要求保留的功能（如 {@code FukumaiPlayerTracker}），
     * 这些按白名单单独并入，其余的调试工具（Sex / Debugga / MovementUtilTest 等）不暴露给界面。</p>
     */
    private static List<Module> collectModules() {
        List<Module> result = new ArrayList<>(com.dioxideliteng.krs.hacks.ModuleManager.modules);
        Set<Class<?>> present = new HashSet<>();
        for (Module module : result) {
            present.add(module.getClass());
        }
        for (Module module : com.dioxideliteng.krs.hacks.ModuleManager.devModules) {
            if (DEV_WHITELIST.contains(module.getClass().getSimpleName()) && present.add(module.getClass())) {
                result.add(module);
            }
        }
        return result;
    }

    /**
     * 名称归一化：忽略大小写与空格/连字符等分隔符。
     *
     * <p>KRS 的显示名习惯带空格（{@code "Kill Aura"}），NG 的是紧凑写法
     * （{@code "KillAura"}），不归一化就会漏判重名。</p>
     */
    private static String normalize(String name) {
        return name == null ? "" : name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    /** 重名时追加 2、3……直到不冲突。 */
    private static String uniqueName(String base, Set<String> taken) {
        if (!taken.contains(normalize(base))) {
            return base;
        }
        int suffix = 2;
        while (taken.contains(normalize(base + suffix))) {
            suffix++;
        }
        return base + suffix;
    }

    public static List<KrsModuleAdapter> getAdapters() {
        return ADAPTERS;
    }
}
