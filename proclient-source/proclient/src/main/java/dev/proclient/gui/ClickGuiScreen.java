package dev.proclient.gui;

import dev.proclient.ProClient;
import dev.proclient.module.Category;
import dev.proclient.module.Module;
import dev.proclient.module.impl.ClickGuiModule;
import dev.proclient.config.ConfigManager;
import dev.proclient.setting.BoolSetting;
import dev.proclient.setting.ColorSetting;
import dev.proclient.setting.ModeSetting;
import dev.proclient.setting.NumSetting;
import dev.proclient.setting.Setting;
import dev.proclient.util.Gfx;
import dev.proclient.util.KeyNames;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Immediate-mode Click GUI: every frame we lay everything out and record clickable rectangles
 * ("hits"); mouse events are resolved against the rectangles of the last frame.
 */
public final class ClickGuiScreen extends Screen {
    private static final int W = 500, H = 320, SIDE = 112, CARD_HEAD = 30;
    private static final int BG = 0x0C0C0E, PANEL = 0x131316, CARD = 0x17171B, CARD_HOVER = 0x1D1D22;
    private static final int TRACK = 0x2A2A30, TEXT = 0xE6E6EA, MUTED = 0x7C7C86;

    private interface Action { void run(int button, double mx); }
    private interface Drag { void to(double mx); }
    private record Hit(int x, int y, int w, int h, boolean scrolled, Action click, Drag drag) {}

    private final List<Hit> hits = new ArrayList<>();
    private final Map<Module, Float> enableAnim = new HashMap<>();
    private final Map<Module, Float> expandAnim = new HashMap<>();
    private final Set<Module> expanded = new HashSet<>();

    private Category category = Category.COMBAT;
    private String query = "";
    private Module listening;
    private Drag drag;

    private float open, fade, catY = -1, scroll, scrollTarget, maxScroll;
    private long lastNanos = System.nanoTime();
    private float frameDt = 0.016f;
    private final long openedAt = System.currentTimeMillis();
    private int wx, wy, ww, wh, viewTop, viewBottom;

    public ClickGuiScreen() {
        super(Text.literal("Pro Client"));
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void removed() {
        ConfigManager.save();
    }

    // ---------------------------------------------------------------- rendering

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float deltaTicks) {
        long now = System.nanoTime();
        float dt = Math.min(0.05f, (now - lastNanos) / 1_000_000_000f);
        lastNanos = now;
        frameDt = dt;
        float speed = (float) ClickGuiModule.INSTANCE.animSpeed.get();
        open = approach(open, 1f, dt, 12f * speed);
        fade = open;
        hits.clear();

        int accent = ClickGuiModule.accentRgb();
        ww = Math.min(W, width - 16);
        wh = Math.min(H, height - 16);
        wx = (width - ww) / 2;
        wy = (height - wh) / 2 + Math.round((1f - open) * 14f);

        if (ClickGuiModule.INSTANCE.dim.get()) ctx.fill(0, 0, width, height, c(0x73000000));
        Gfx.rounded(ctx, wx - 1, wy - 1, ww + 2, wh + 2, 9, c(0xFF1E1E23));
        Gfx.rounded(ctx, wx, wy, ww, wh, 8, c(Gfx.opaque(BG)));

        drawSidebar(ctx, mouseX, mouseY, accent);
        drawContent(ctx, mouseX, mouseY, accent);
    }

    private void drawSidebar(DrawContext ctx, int mx, int my, int accent) {
        int sx = wx + 8, sy = wy + 8, sw = SIDE - 8, sh = wh - 16;
        Gfx.rounded(ctx, sx, sy, sw, sh, 6, c(Gfx.opaque(PANEL)));
        text(ctx, "PRO", sx + 12, sy + 12, accent);
        text(ctx, "CLIENT", sx + 12 + textRenderer.getWidth("PRO "), sy + 12, 0xFFFFFF);

        Category[] cats = Category.values();
        float targetY = sy + 38 + category.ordinal() * 26;
        if (catY < 0) catY = targetY;
        catY = approach(catY, targetY, frameDt, 18f);
        if (query.isEmpty()) Gfx.rounded(ctx, sx + 6, Math.round(catY), sw - 12, 22, 5, c(Gfx.argb(accent, 0x38)));

        for (int i = 0; i < cats.length; i++) {
            Category cat = cats[i];
            int y = sy + 38 + i * 26;
            boolean sel = cat == category && query.isEmpty();
            boolean hover = inside(mx, my, sx + 6, y, sw - 12, 22);
            int color = sel ? accent : (hover ? 0xCFCFD6 : MUTED);
            text(ctx, cat.label, sx + 16, y + 7, color);
            hits.add(new Hit(sx + 6, y, sw - 12, 22, false, (b, m) -> {
                category = cat;
                query = "";
                scrollTarget = 0;
            }, null));
        }
        text(ctx, "v1.0  MC 1.21.11", sx + 12, sy + sh - 14, 0x4A4A52);
    }

    private void drawContent(DrawContext ctx, int mx, int my, int accent) {
        int cx = wx + SIDE + 8, cw = ww - SIDE - 16;

        // search bar
        Gfx.rounded(ctx, cx, wy + 8, cw, 22, 6, c(Gfx.opaque(PANEL)));
        boolean caret = (System.currentTimeMillis() / 500) % 2 == 0;
        if (query.isEmpty()) text(ctx, "Search modules...", cx + 10, wy + 15, 0x55555E);
        else text(ctx, query + (caret ? "_" : ""), cx + 10, wy + 15, TEXT);

        viewTop = wy + 38;
        viewBottom = wy + wh - 22;
        text(ctx, "Click: toggle   Right-click: settings   ESC: close", cx + 2, wy + wh - 15, 0x4A4A52);

        List<Module> list = visibleModules();
        scrollTarget = clamp(scrollTarget, 0, maxScroll);
        scroll = approach(scroll, scrollTarget, frameDt, 20f);

        ctx.enableScissor(cx, viewTop, cx + cw, viewBottom);
        int y = viewTop - Math.round(scroll);
        for (Module m : list) {
            float ea = approach(enableAnim.getOrDefault(m, m.isEnabled() ? 1f : 0f), m.isEnabled() ? 1f : 0f, frameDt, 16f);
            float xa = approach(expandAnim.getOrDefault(m, 0f), expanded.contains(m) ? 1f : 0f, frameDt, 16f);
            enableAnim.put(m, ea);
            expandAnim.put(m, xa);

            int fullSettingsH = settingsHeight(m);
            int ch = CARD_HEAD + Math.round(fullSettingsH * xa);
            drawCard(ctx, m, cx, y, cw, ch, ea, xa, mx, my, accent);
            y += ch + 6;
        }
        ctx.disableScissor();

        if (list.isEmpty()) text(ctx, "No modules match.", cx + 4, viewTop + 4, MUTED);
        int contentH = y + Math.round(scroll) - viewTop;
        maxScroll = Math.max(0, contentH - (viewBottom - viewTop));
    }

    private void drawCard(DrawContext ctx, Module m, int cx, int y, int cw, int ch, float ea, float xa,
                          int mx, int my, int accent) {
        boolean hover = inside(mx, my, cx, y, cw, CARD_HEAD) && my >= viewTop && my <= viewBottom;
        Gfx.rounded(ctx, cx, y, cw, ch, 6, c(Gfx.opaque(hover ? CARD_HOVER : CARD)));
        if (ea > 0.01f) Gfx.rounded(ctx, cx, y + 7, 3, CARD_HEAD - 14, 1, c(Gfx.argb(accent, Math.round(255 * ea))));

        text(ctx, m.name, cx + 12, y + 6, Gfx.lerpRgb(0xD0D0D6, accent, ea));
        String desc = trim(m.description, cw - 90);
        text(ctx, desc, cx + 12, y + 17, 0x62626C);

        // toggle switch (momentary modules show a bind hint instead)
        int swx = cx + cw - 52, swy = y + 9;
        if (m.isMomentary()) {
            text(ctx, "[" + KeyNames.of(m.key()) + "]", swx - 2, swy + 2, MUTED);
        } else {
            Gfx.rounded(ctx, swx, swy, 26, 12, 6, c(Gfx.opaque(Gfx.lerpRgb(TRACK, accent, ea))));
            Gfx.rounded(ctx, swx + 1 + Math.round(14 * ea), swy + 1, 10, 10, 5, c(0xFFFFFFFF));
        }
        text(ctx, expanded.contains(m) ? "-" : "+", cx + cw - 14, y + 10, MUTED);

        final int arrowX = cx + cw - 24;
        hits.add(new Hit(cx, y, cw, CARD_HEAD, true, (b, mouseX) -> {
            if (b == 1 || mouseX >= arrowX) {
                if (!expanded.remove(m)) expanded.add(m);
            } else {
                m.toggle();
            }
        }, null));

        if (xa <= 0.02f) return;
        ctx.enableScissor(cx, Math.max(y, viewTop), cx + cw, Math.min(y + ch, viewBottom));
        int rx = cx + 12, rw = cw - 24, ry = y + CARD_HEAD + 4;
        boolean interactive = xa > 0.98f;
        for (Setting s : m.settings) {
            ry += drawSetting(ctx, s, rx, ry, rw, mx, my, accent, interactive);
        }
        drawBindRow(ctx, m, rx, ry, rw, mx, my, accent, interactive);
        ctx.disableScissor();
    }

    private int drawSetting(DrawContext ctx, Setting s, int rx, int ry, int rw, int mx, int my, int accent, boolean live) {
        if (s instanceof BoolSetting b) {
            text(ctx, b.name, rx, ry + 6, 0xBDBDC6);
            int sx = rx + rw - 24;
            float t = b.get() ? 1f : 0f;
            Gfx.rounded(ctx, sx, ry + 4, 24, 12, 6, c(Gfx.opaque(b.get() ? accent : TRACK)));
            Gfx.rounded(ctx, sx + 1 + Math.round(12 * t), ry + 5, 10, 10, 5, c(0xFFFFFFFF));
            if (live) hits.add(new Hit(rx, ry, rw, 20, true, (btn, m) -> b.toggle(), null));
            return 22;
        }
        if (s instanceof ColorSetting cs) {
            text(ctx, cs.name, rx, ry + 3, 0xBDBDC6);
            Gfx.rounded(ctx, rx + rw - 16, ry + 2, 16, 9, 3, c(Gfx.opaque(cs.rgb())));
            int ty = ry + 16;
            int segs = Math.max(1, rw / 3);
            for (int i = 0; i < segs; i++) {
                int x0 = rx + i * rw / segs, x1 = rx + (i + 1) * rw / segs;
                ctx.fill(x0, ty, x1, ty + 5, c(Gfx.opaque(ColorSetting.hueToRgb(360.0 * i / segs))));
            }
            int kx = rx + (int) Math.round(cs.fraction() * rw);
            Gfx.rounded(ctx, kx - 2, ty - 2, 4, 9, 2, c(0xFFFFFFFF));
            if (live) addSlider(cs, rx, ty - 4, rw, 12);
            return 30;
        }
        if (s instanceof NumSetting n) {
            text(ctx, n.name, rx, ry + 3, 0xBDBDC6);
            String v = n.display();
            text(ctx, v, rx + rw - textRenderer.getWidth(v), ry + 3, accent);
            int ty = ry + 16;
            Gfx.rounded(ctx, rx, ty, rw, 4, 2, c(Gfx.opaque(TRACK)));
            int fw = (int) Math.round(n.fraction() * rw);
            if (fw > 0) Gfx.rounded(ctx, rx, ty, fw, 4, 2, c(Gfx.opaque(accent)));
            Gfx.rounded(ctx, rx + fw - 3, ty - 2, 6, 8, 3, c(0xFFFFFFFF));
            if (live) addSlider(n, rx, ty - 4, rw, 12);
            return 30;
        }
        if (s instanceof ModeSetting md) {
            text(ctx, md.name, rx, ry + 6, 0xBDBDC6);
            String v = md.get();
            int bw = textRenderer.getWidth(v) + 14;
            Gfx.rounded(ctx, rx + rw - bw, ry + 2, bw, 16, 5, c(Gfx.opaque(TRACK)));
            text(ctx, v, rx + rw - bw + 7, ry + 6, accent);
            if (live) hits.add(new Hit(rx + rw - bw, ry + 2, bw, 16, true, (btn, m) -> md.cycle(btn == 1 ? -1 : 1), null));
            return 22;
        }
        return 0;
    }

    private void addSlider(NumSetting n, int rx, int y, int rw, int h) {
        Drag d = mouseX -> n.set(n.min + clamp((float) ((mouseX - rx) / rw), 0f, 1f) * (n.max - n.min));
        hits.add(new Hit(rx, y, rw, h, true, (b, m) -> d.to(m), d));
    }

    private void drawBindRow(DrawContext ctx, Module m, int rx, int ry, int rw, int mx, int my, int accent, boolean live) {
        text(ctx, "Keybind", rx, ry + 6, 0xBDBDC6);
        boolean waiting = listening == m;
        String v = waiting ? "Press a key..." : KeyNames.of(m.key());
        int bw = textRenderer.getWidth(v) + 14;
        Gfx.rounded(ctx, rx + rw - bw, ry + 2, bw, 16, 5, c(Gfx.opaque(waiting ? Gfx.lerpRgb(TRACK, accent, 0.45f) : TRACK)));
        text(ctx, v, rx + rw - bw + 7, ry + 6, waiting ? 0xFFFFFF : accent);
        if (live) {
            hits.add(new Hit(rx + rw - bw, ry + 2, bw, 16, true, (btn, mouse) -> {
                if (btn == 1) m.setKey(-1);
                else listening = m;
            }, null));
        }
    }

    private int settingsHeight(Module m) {
        int h = 4 + 22; // padding + bind row
        for (Setting s : m.settings) {
            h += (s instanceof NumSetting) ? 30 : 22; // ColorSetting extends NumSetting
        }
        return h + 4;
    }

    private List<Module> visibleModules() {
        List<Module> out = new ArrayList<>();
        String q = query.trim().toLowerCase(Locale.ROOT);
        for (Module m : ProClient.modules().modules) {
            if (q.isEmpty() ? m.category == category
                    : (m.name.toLowerCase(Locale.ROOT).contains(q) || m.description.toLowerCase(Locale.ROOT).contains(q))) {
                out.add(m);
            }
        }
        return out;
    }

    // ---------------------------------------------------------------- input

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (listening != null) {
            listening = null; // any click cancels key capture
            return true;
        }
        double mx = click.x(), my = click.y();
        int button = click.button();
        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit h = hits.get(i);
            if (!inside(mx, my, h.x, h.y, h.w, h.h)) continue;
            if (h.scrolled && (my < viewTop || my > viewBottom)) continue;
            h.click.run(button, mx);
            drag = h.drag;
            return true;
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        if (drag != null) {
            drag.to(click.x());
            return true;
        }
        return super.mouseDragged(click, offsetX, offsetY);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (drag != null) {
            drag = null;
            ConfigManager.markDirty();
            return true;
        }
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        scrollTarget = clamp((float) (scrollTarget - vertical * 28), 0, maxScroll);
        return true;
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        int key = input.key();
        if (listening != null) {
            if (key == GLFW.GLFW_KEY_ESCAPE) {
                // cancel
            } else if (key == GLFW.GLFW_KEY_BACKSPACE || key == GLFW.GLFW_KEY_DELETE) {
                listening.setKey(-1);
            } else {
                listening.setKey(key);
            }
            listening = null;
            return true;
        }
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }
        if (key == ClickGuiModule.INSTANCE.key() && System.currentTimeMillis() - openedAt > 400) {
            close(); // the GUI key also closes the GUI (ignored while the opening key is still held)
            return true;
        }
        if (key == GLFW.GLFW_KEY_BACKSPACE && !query.isEmpty()) {
            query = query.substring(0, query.length() - 1);
            scrollTarget = 0;
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharInput input) {
        int cp = input.codepoint();
        if (listening == null && cp >= 32 && cp != 127 && query.length() < 24) {
            query += new String(Character.toChars(cp));
            scrollTarget = 0;
            return true;
        }
        return super.charTyped(input);
    }

    // ---------------------------------------------------------------- helpers

    private int c(int argb) {
        int a = Math.round(((argb >>> 24) & 0xFF) * fade);
        return (a << 24) | (argb & 0xFFFFFF);
    }

    private void text(DrawContext ctx, String s, int x, int y, int rgb) {
        ctx.drawText(textRenderer, s, x, y, c(Gfx.opaque(rgb)), false);
    }

    private String trim(String s, int maxWidth) {
        if (textRenderer.getWidth(s) <= maxWidth) return s;
        while (s.length() > 1 && textRenderer.getWidth(s + "...") > maxWidth) s = s.substring(0, s.length() - 1);
        return s + "...";
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private static float clamp(float v, float lo, float hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    /** Frame-rate independent exponential approach. */
    private static float approach(float cur, float target, float dt, float rate) {
        float k = 1f - (float) Math.exp(-rate * dt);
        float v = cur + (target - cur) * k;
        return Math.abs(target - v) < 0.002f ? target : v;
    }
}
