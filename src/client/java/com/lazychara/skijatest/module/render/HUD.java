package com.lazychara.skijatest.module.render;

import com.lazychara.skijatest.module.BooleanSetting;
import com.lazychara.skijatest.module.Category;
import com.lazychara.skijatest.module.Module;

public class HUD extends Module {
    public final BooleanSetting xyz = new BooleanSetting("XYZ", false);
    public final BooleanSetting potions = new BooleanSetting("Potions", false);

    public int mainX = -1;
    public int mainY = 6;
    public int xyzX = -1;
    public int xyzY = -1;
    public int potionsX = -1;
    public int potionsY = 36;

    public HUD() {
        super("HUD", Category.RENDER);
        this.enabled = true;
        this.settings.add(xyz);
        this.settings.add(potions);
    }
}
