package com.dioxideliteng.krs.hacks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Registry facade for ported KRS modules, keeping the static API KRS code calls
 * ({@code getModuleState}, {@code getModule}, {@code modules}...). Registration into DioxideLiteNG's
 * {@link com.dioxideliteng.module.FeatureManager} happens in {@link com.dioxideliteng.krs.KrsModules}.
 */
public final class ModuleManager {
    public static final List<Module> modules = Collections.synchronizedList(new ArrayList<>());
    public static final List<Module> devModules = Collections.synchronizedList(new ArrayList<>());
    public static final List<Module> allModules = Collections.synchronizedList(new ArrayList<>());

    public static boolean isDebugRendering = false;
    public static int rotTick = 0;
    public static int transactionCounter = 0;
    public static boolean gettingTransactions = false;
    public static boolean plChecked = false;

    private static final Map<Class<?>, Module> BY_CLASS = new HashMap<>();
    private static final Map<String, Module> BY_NAME = new HashMap<>();

    private ModuleManager() {
    }

    public static synchronized void register(Module module) {
        modules.add(module);
        allModules.add(module);
        BY_CLASS.put(module.getClass(), module);
        BY_NAME.put(module.moduleName, module);
    }

    public static synchronized boolean getModuleState(Class<? extends Module> moduleClass) {
        Module module = BY_CLASS.get(moduleClass);
        return module != null && module.tempEnabled;
    }

    public static synchronized <T extends Module> T getModule(Class<T> moduleClass) {
        Module module = BY_CLASS.get(moduleClass);
        return module == null ? null : moduleClass.cast(module);
    }

    /** Looks a module up by its original KRS name (not the possibly suffixed DioxideLiteNG name). */
    public static synchronized Module getModuleByName(String moduleName) {
        return moduleName == null ? null : BY_NAME.get(moduleName);
    }
}
