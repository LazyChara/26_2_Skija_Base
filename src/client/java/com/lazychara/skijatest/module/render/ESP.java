package com.lazychara.skijatest.module.render;
import com.lazychara.skijatest.module.BooleanSetting;
import com.lazychara.skijatest.module.Category;
import com.lazychara.skijatest.module.Module;
public class ESP extends Module {
    public final BooleanSetting render3D = new BooleanSetting("Render 3D", true);
    public final BooleanSetting render2D = new BooleanSetting("Render 2D", true);
    public final BooleanSetting players = new BooleanSetting("Players", true);
    public final BooleanSetting mobs = new BooleanSetting("Mobs", true);
    public final BooleanSetting animals = new BooleanSetting("Animals", false);
    public final BooleanSetting items = new BooleanSetting("Items", false);
    public final BooleanSetting names = new BooleanSetting("Names", true);
    public final BooleanSetting health = new BooleanSetting("Health", true);
    public final BooleanSetting distance = new BooleanSetting("Distance", true);
    public ESP() {
        super("ESP", Category.RENDER);
        this.settings.add(render3D);
        this.settings.add(render2D);
        this.settings.add(players);
        this.settings.add(mobs);
        this.settings.add(animals);
        this.settings.add(items);
        this.settings.add(names);
        this.settings.add(health);
        this.settings.add(distance);
    }
}
