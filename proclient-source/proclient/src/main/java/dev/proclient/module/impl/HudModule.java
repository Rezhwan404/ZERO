package dev.proclient.module.impl;

import dev.proclient.module.Category;
import dev.proclient.module.Module;
import dev.proclient.setting.BoolSetting;
import dev.proclient.setting.ModeSetting;
import dev.proclient.setting.NumSetting;
import dev.proclient.util.Gfx;
import dev.proclient.ProClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class HudModule extends Module {
    private final BoolSetting fps = add(new BoolSetting("FPS Counter", true));
    private final BoolSetting coords = add(new BoolSetting("Coordinates", true));
    private final BoolSetting xyz = add(new BoolSetting("XYZ (precise)", false));
    private final BoolSetting biome = add(new BoolSetting("Biome", true));
    private final BoolSetting entities = add(new BoolSetting("Entity Counter", true));
    private final BoolSetting moduleList = add(new BoolSetting("Module List", true));
    private final BoolSetting background = add(new BoolSetting("Background", true));
    private final ModeSetting corner = add(new ModeSetting("Position", "Top Left", "Top Right", "Bottom Left", "Bottom Right"));
    private final NumSetting offX = add(new NumSetting("Offset X", 4, 0, 400, 1));
    private final NumSetting offY = add(new NumSetting("Offset Y", 4, 0, 400, 1));
    private final NumSetting scale = add(new NumSetting("HUD Scale", 1.0, 0.5, 2.0, 0.1));

    private int entityCount;
    private int tickCounter;

    public HudModule() {
        super("HUD", "FPS, coordinates, biome, entity count and active modules.", Category.HUD, false);
        restoreEnabled(true); // HUD is on by default
    }

    @Override
    public void onTick(MinecraftClient mc) {
        if (!entities.get() || mc.world == null) return;
        if (tickCounter++ % 10 != 0) return;
        int n = 0;
        for (Entity ignored : mc.world.getEntities()) n++;
        entityCount = n;
    }

    @Override
    public void renderHud(DrawContext ctx, float tickProgress) {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.world == null) return;

        List<String[]> info = new ArrayList<>(); // {label, value}
        if (fps.get()) info.add(new String[]{"FPS", String.valueOf(mc.getCurrentFps())});
        if (coords.get()) {
            String dir = p.getHorizontalFacing().asString();
            dir = dir.substring(0, 1).toUpperCase(Locale.ROOT) + dir.substring(1);
            info.add(new String[]{"Pos", p.getBlockX() + ", " + p.getBlockY() + ", " + p.getBlockZ() + "  " + dir});
        }
        if (xyz.get()) {
            info.add(new String[]{"X", String.format("%.2f", p.getX())});
            info.add(new String[]{"Y", String.format("%.2f", p.getY())});
            info.add(new String[]{"Z", String.format("%.2f", p.getZ())});
        }
        if (biome.get()) info.add(new String[]{"Biome", biomeName(mc, p)});
        if (entities.get()) info.add(new String[]{"Entities", String.valueOf(entityCount)});

        List<String> active = new ArrayList<>();
        if (moduleList.get()) {
            for (Module m : ProClient.modules().modules) {
                if (m.isEnabled() && !m.isMomentary()) active.add(m.name);
            }
        }
        if (info.isEmpty() && active.isEmpty()) return;

        TextRenderer tr = mc.textRenderer;
        int accent = ClickGuiModule.accentRgb();
        active.sort((a, b) -> tr.getWidth(b) - tr.getWidth(a));

        int lineH = tr.fontHeight + 2;
        int w = 0;
        for (String[] l : info) w = Math.max(w, tr.getWidth(l[0] + ": " + l[1]));
        for (String s : active) w = Math.max(w, tr.getWidth(s));
        int gap = (!info.isEmpty() && !active.isEmpty()) ? 4 : 0;
        int h = info.size() * lineH + gap + active.size() * lineH;

        float s = scale.getF();
        int sw = (int) (mc.getWindow().getScaledWidth() / s);
        int sh = (int) (mc.getWindow().getScaledHeight() / s);
        boolean right = corner.get().contains("Right");
        boolean bottom = corner.get().contains("Bottom");
        int pad = 4;
        int x = right ? sw - w - pad * 2 - offX.getI() : offX.getI();
        int y = bottom ? sh - h - pad * 2 - offY.getI() : offY.getI();

        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().scale(s, s);
        if (background.get()) {
            Gfx.rounded(ctx, x, y, w + pad * 2, h + pad * 2, 4, 0x99000000);
            int barX = right ? x + w + pad * 2 - 2 : x;
            ctx.fill(barX, y + 3, barX + 2, y + h + pad * 2 - 3, Gfx.opaque(accent));
        }
        int tx = x + pad;
        int ty = y + pad + 1;
        for (String[] l : info) {
            String label = l[0] + ": ";
            int lx = right ? tx + w - tr.getWidth(label + l[1]) : tx;
            Gfx.text(ctx, tr, label, lx, ty, accent, true);
            Gfx.text(ctx, tr, l[1], lx + tr.getWidth(label), ty, 0xFFFFFF, true);
            ty += lineH;
        }
        ty += gap;
        for (String name : active) {
            int lx = right ? tx + w - tr.getWidth(name) : tx;
            Gfx.text(ctx, tr, name, lx, ty, accent, true);
            ty += lineH;
        }
        ctx.getMatrices().popMatrix();
    }

    private static String biomeName(MinecraftClient mc, ClientPlayerEntity p) {
        return mc.world.getBiome(p.getBlockPos()).getKey()
                .map(key -> {
                    Identifier id = key.getValue();
                    return Text.translatable("biome." + id.getNamespace() + "." + id.getPath()).getString();
                })
                .orElse("Unknown");
    }
}
