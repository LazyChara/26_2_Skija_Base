package com.lazychara.skijatest.module;
public class BooleanSetting extends Setting {
    public boolean value;
    public BooleanSetting(String name, boolean defaultValue) {
        super(name);
        this.value = defaultValue;
    }
    public void toggle() {
        this.value = !this.value;
    }
}
