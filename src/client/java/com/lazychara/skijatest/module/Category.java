package com.lazychara.skijatest.module;

public enum Category {
    COMBAT("Combat"),
    MOVEMENT("Movement"),
    RENDER("Render"),
    PLAYER("Player"),
    MISC("Misc");

    public final String name;

    Category(String name) {
        this.name = name;
    }
}
