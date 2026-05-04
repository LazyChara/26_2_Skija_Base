package com.lazychara.skijatest.module;
import com.lazychara.skijatest.client.SkijaTestClient;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
public class ModuleManager {
    public static final List<Module> modules = new ArrayList<>();
    public static void init() {
        try {
            net.fabricmc.loader.api.ModContainer mod = net.fabricmc.loader.api.FabricLoader.getInstance()
                    .getModContainer("skija-test").orElse(null);
            if (mod == null) {
                SkijaTestClient.LOGGER.error("[SkijaTest] 他妈的没有模块");
                return;
            }
            for (java.nio.file.Path root : mod.getRootPaths()) {
                java.nio.file.Path moduleDir = root.resolve("com/lazychara/skijatest/module");
                if (java.nio.file.Files.exists(moduleDir)) {
                    java.nio.file.Files.walk(moduleDir)
                            .filter(p -> p.toString().endsWith(".class"))
                            .forEach(p -> {
                                String className = root.relativize(p).toString()
                                        .replace("/", ".")
                                        .replace("\\", ".")
                                        .replace(".class", "");
                                try {
                                    Class<?> clazz = Class.forName(className);
                                    if (Module.class.isAssignableFrom(clazz) && clazz != Module.class) {
                                        modules.add((Module) clazz.getDeclaredConstructor().newInstance());
                                        SkijaTestClient.LOGGER.info("[SkijaTest] loaded module: {}",
                                                clazz.getSimpleName());
                                    }
                                } catch (Exception e) {
                                }
                            });
                }
            }
        } catch (Exception e) {
            SkijaTestClient.LOGGER.error("[SkijaTest] Failed to load modules", e);
        }
    }
    public static List<Module> getModulesByCategory(Category category) {
        return modules.stream().filter(m -> m.category == category).collect(Collectors.toList());
    }
}
