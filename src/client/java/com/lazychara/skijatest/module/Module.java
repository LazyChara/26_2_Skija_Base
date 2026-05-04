package com.lazychara.skijatest.module;
import java.util.ArrayList;
import java.util.List;
public abstract class Module {
    public final String name;
    public final Category category;
    public boolean enabled = false;
    public boolean expanded = false;
    public float expandAnim = 0f;
    public int keybind = -1;
    public boolean isBinding = false;
    public boolean wasKeyPressed = false;
    public float originX, originY;
    public final List<Setting> settings = new ArrayList<>();
    public Module(String name, Category category) {
        this.name = name;
        this.category = category;
    }
    public void toggle() {
        this.enabled = !this.enabled;
        if (this.enabled) {
            onEnable();
            com.lazychara.skijatest.module.NotificationManager.show(this.name + " Enabled");
        } else {
            onDisable();
            com.lazychara.skijatest.module.NotificationManager.show(this.name + " Disabled");
        }
        com.lazychara.skijatest.config.ConfigManager.save();
    }
    protected void onEnable() {}
    protected void onDisable() {}
}
