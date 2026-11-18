package dev.proclient.setting;

import com.google.gson.JsonElement;
import dev.proclient.config.ConfigManager;

public abstract class Setting {
    public final String name;

    protected Setting(String name) {
        this.name = name;
    }

    public abstract JsonElement save();

    public abstract void load(JsonElement element);

    protected void changed() {
        ConfigManager.markDirty();
    }
}
