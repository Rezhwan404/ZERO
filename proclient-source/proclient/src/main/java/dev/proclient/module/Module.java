package dev.proclient.module;

import dev.proclient.config.ConfigManager;
import dev.proclient.setting.Setting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public abstract class Module {
    public final String name;
    public final String description;
    public final Category category;
    public final boolean singleplayerOnly;
    /** A momentary module runs onEnable() when toggled and never stays enabled (e.g. ClickGUI). */
    protected final boolean momentary;
    public final List<Setting> settings = new ArrayList<>();

    private boolean enabled;
    private int key = -1;

    protected Module(String name, String description, Category category, boolean singleplayerOnly, boolean momentary) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.singleplayerOnly = singleplayerOnly;
        this.momentary = momentary;
    }

    protected Module(String name, String description, Category category, boolean singleplayerOnly) {
        this(name, description, category, singleplayerOnly, false);
    }

    protected <T extends Setting> T add(T setting) {
        settings.add(setting);
        return setting;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isMomentary() {
        return momentary;
    }

    public int key() {
        return key;
    }

    public void setKey(int k) {
        if (k != key) {
            key = k;
            ConfigManager.markDirty();
        }
    }

    public void toggle() {
        setEnabled(momentary || !enabled);
    }

    public void setEnabled(boolean value) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (momentary) {
            if (value) onEnable();
            return;
        }
        if (value == enabled) return;
        if (value && singleplayerOnly && mc.player != null && !mc.isInSingleplayer()) {
            mc.player.sendMessage(Text.literal("[ProClient] " + name + " only works in single-player."), true);
            return;
        }
        enabled = value;
        if (value) onEnable(); else onDisable();
        ConfigManager.markDirty();
    }

    /** Used by the config loader: restores the flag without running onEnable. */
    public void restoreEnabled(boolean value) {
        if (!momentary) enabled = value;
    }

    public void forceDisable(String reason) {
        if (!enabled) return;
        enabled = false;
        onDisable();
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            mc.player.sendMessage(Text.literal("[ProClient] " + name + " disabled: " + reason), true);
        }
        ConfigManager.markDirty();
    }

    protected void onEnable() {}

    protected void onDisable() {}

    public void onTick(MinecraftClient mc) {}

    public void renderHud(DrawContext ctx, float tickProgress) {}
}
