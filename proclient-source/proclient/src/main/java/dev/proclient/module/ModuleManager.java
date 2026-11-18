package dev.proclient.module;

import dev.proclient.module.impl.ClickGuiModule;
import dev.proclient.module.impl.EntityEspModule;
import dev.proclient.module.impl.FlightModule;
import dev.proclient.module.impl.HudModule;
import dev.proclient.module.impl.ReachModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ModuleManager {
    public final List<Module> modules = new ArrayList<>();
    private final Map<Module, Boolean> keyDown = new HashMap<>();

    public ModuleManager() {
        modules.add(new ReachModule());
        modules.add(new EntityEspModule());
        modules.add(new FlightModule());
        modules.add(new HudModule());
        modules.add(new ClickGuiModule());
    }

    public List<Module> inCategory(Category c) {
        List<Module> out = new ArrayList<>();
        for (Module m : modules) if (m.category == c) out.add(m);
        return out;
    }

    public void tick(MinecraftClient mc) {
        enforceSingleplayer(mc);
        for (Module m : modules) {
            if (m.isEnabled()) m.onTick(mc);
        }
        pollKeys(mc);
    }

    /** Reach and Flight must never stay active on a multiplayer server. */
    public void enforceSingleplayer(MinecraftClient mc) {
        if (mc.player == null || mc.world == null || mc.isInSingleplayer()) return;
        for (Module m : modules) {
            if (m.singleplayerOnly && m.isEnabled()) m.forceDisable("multiplayer detected");
        }
    }

    private void pollKeys(MinecraftClient mc) {
        long handle = mc.getWindow().getHandle();
        boolean canFire = mc.currentScreen == null && mc.getOverlay() == null;
        for (Module m : modules) {
            int key = m.key();
            if (key < 0) {
                keyDown.put(m, false);
                continue;
            }
            boolean down = GLFW.glfwGetKey(handle, key) == GLFW.GLFW_PRESS;
            boolean was = keyDown.getOrDefault(m, false);
            keyDown.put(m, down);
            if (down && !was && canFire) m.toggle();
        }
    }

    public void renderHud(DrawContext ctx, float tickProgress) {
        for (Module m : modules) {
            if (m.isEnabled()) m.renderHud(ctx, tickProgress);
        }
    }
}
