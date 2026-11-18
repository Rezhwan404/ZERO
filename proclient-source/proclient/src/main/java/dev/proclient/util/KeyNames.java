package dev.proclient.util;

import org.lwjgl.glfw.GLFW;

import java.util.Locale;

public final class KeyNames {
    private KeyNames() {}

    public static String of(int key) {
        if (key < 0) return "NONE";
        if (key >= GLFW.GLFW_KEY_F1 && key <= GLFW.GLFW_KEY_F25) return "F" + (key - GLFW.GLFW_KEY_F1 + 1);
        switch (key) {
            case GLFW.GLFW_KEY_SPACE: return "SPACE";
            case GLFW.GLFW_KEY_ENTER: return "ENTER";
            case GLFW.GLFW_KEY_TAB: return "TAB";
            case GLFW.GLFW_KEY_LEFT_SHIFT: return "L-SHIFT";
            case GLFW.GLFW_KEY_RIGHT_SHIFT: return "R-SHIFT";
            case GLFW.GLFW_KEY_LEFT_CONTROL: return "L-CTRL";
            case GLFW.GLFW_KEY_RIGHT_CONTROL: return "R-CTRL";
            case GLFW.GLFW_KEY_LEFT_ALT: return "L-ALT";
            case GLFW.GLFW_KEY_RIGHT_ALT: return "R-ALT";
            case GLFW.GLFW_KEY_UP: return "UP";
            case GLFW.GLFW_KEY_DOWN: return "DOWN";
            case GLFW.GLFW_KEY_LEFT: return "LEFT";
            case GLFW.GLFW_KEY_RIGHT: return "RIGHT";
            case GLFW.GLFW_KEY_INSERT: return "INSERT";
            case GLFW.GLFW_KEY_DELETE: return "DELETE";
            case GLFW.GLFW_KEY_HOME: return "HOME";
            case GLFW.GLFW_KEY_END: return "END";
            case GLFW.GLFW_KEY_PAGE_UP: return "PG-UP";
            case GLFW.GLFW_KEY_PAGE_DOWN: return "PG-DOWN";
            case GLFW.GLFW_KEY_CAPS_LOCK: return "CAPS";
            case GLFW.GLFW_KEY_BACKSPACE: return "BACKSPACE";
            default:
                String n = GLFW.glfwGetKeyName(key, 0);
                return n != null ? n.toUpperCase(Locale.ROOT) : "KEY " + key;
        }
    }
}
