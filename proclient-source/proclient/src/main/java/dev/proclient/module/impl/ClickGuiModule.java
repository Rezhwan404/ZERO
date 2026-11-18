package dev.proclient.module.impl;

import dev.proclient.gui.ClickGuiScreen;
import dev.proclient.module.Category;
import dev.proclient.module.Module;
import dev.proclient.setting.BoolSetting;
import dev.proclient.setting.ColorSetting;
import dev.proclient.setting.NumSetting;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

/** Opens the Click GUI. Its keybind is the GUI key; its settings control the theme. */
public final class ClickGuiModule extends Module {
    public static ClickGuiModule INSTANCE;

    public final ColorSetting accent = add(new ColorSetting("Accent Color", 205));
    public final NumSetting animSpeed = add(new NumSetting("Animation Speed", 1.0, 0.5, 3.0, 0.1));
    public final BoolSetting dim = add(new BoolSetting("Dim Background", true));

    public ClickGuiModule() {
        super("ClickGUI", "Opens this menu. Set the key to open it; change theme here.", Category.MISC, false, true);
        INSTANCE = this;
        setKey(GLFW.GLFW_KEY_RIGHT_SHIFT);
    }

    public static int accentRgb() {
        return INSTANCE == null ? 0x3FA7FF : INSTANCE.accent.rgb();
    }

    @Override
    protected void onEnable() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!(mc.currentScreen instanceof ClickGuiScreen)) mc.setScreen(new ClickGuiScreen());
    }
}
