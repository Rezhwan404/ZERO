package dev.proclient.module.impl;

import dev.proclient.module.Category;
import dev.proclient.module.Module;
import dev.proclient.setting.BoolSetting;
import dev.proclient.setting.ColorSetting;
import dev.proclient.setting.NumSetting;
import dev.proclient.util.Gfx;
import dev.proclient.util.Projector;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Highlights entities with vanilla's glow outline (visible through walls, drawn by the game's own
 * renderer, so it is cheap) and draws name / distance / health tags above them.
 * Category membership is rebuilt once per tick, not per frame.
 */
public final class EntityEspModule extends Module {
    public enum Cat {
        PLAYER("Players", 0),
        MOB("Mobs", 28),
        ANIMAL("Animals", 140),
        ITEM("Items", 200);

        final String label;
        final int defaultHue;

        Cat(String label, int defaultHue) {
            this.label = label;
            this.defaultHue = defaultHue;
        }
    }

    private record Target(Entity entity, int rgb, double distSq) {}

    private static volatile Int2IntOpenHashMap glowMap = new Int2IntOpenHashMap();

    private final NumSetting range = add(new NumSetting("ESP Range", 64, 8, 256, 1));
    private final BoolSetting outlines = add(new BoolSetting("Outlines", true));
    private final BoolSetting names = add(new BoolSetting("Names", true));
    private final BoolSetting distance = add(new BoolSetting("Distance", true));
    private final BoolSetting health = add(new BoolSetting("Health", true));
    private final BoolSetting[] show = new BoolSetting[Cat.values().length];
    private final ColorSetting[] color = new ColorSetting[Cat.values().length];

    private final List<Target> targets = new ArrayList<>();
    private final float[] screen = new float[2];

    public EntityEspModule() {
        super("Entity ESP", "Outlines and labels nearby entities by category.", Category.RENDER, false);
        for (Cat c : Cat.values()) {
            show[c.ordinal()] = add(new BoolSetting("Show " + c.label, true));
            color[c.ordinal()] = add(new ColorSetting(c.label + " Color", c.defaultHue));
        }
    }

    // ---- hooks used by EntityMixin (render thread only) ----

    public static boolean hasGlowTargets() {
        return !glowMap.isEmpty();
    }

    /** ARGB outline colour for this entity id, or 0 if it is not an ESP target. */
    public static int glowColor(int entityId) {
        return glowMap.get(entityId);
    }

    // ---- module ----

    @Override
    protected void onDisable() {
        glowMap = new Int2IntOpenHashMap();
        targets.clear();
    }

    @Override
    public void onTick(MinecraftClient mc) {
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.world == null) {
            glowMap = new Int2IntOpenHashMap();
            targets.clear();
            return;
        }
        double r = range.get();
        double r2 = r * r;
        Int2IntOpenHashMap map = new Int2IntOpenHashMap();
        List<Target> list = new ArrayList<>();

        for (Entity e : mc.world.getEntities()) {
            if (e == p || e.isRemoved()) continue;
            Cat cat = categorize(e);
            if (cat == null || !show[cat.ordinal()].get()) continue;
            double d2 = p.squaredDistanceTo(e);
            if (d2 > r2) continue;
            int rgb = color[cat.ordinal()].rgb();
            if (outlines.get()) map.put(e.getId(), Gfx.opaque(rgb));
            list.add(new Target(e, rgb, d2));
        }
        list.sort(Comparator.comparingDouble(Target::distSq));
        targets.clear();
        targets.addAll(list);
        glowMap = map;
    }

    private static Cat categorize(Entity e) {
        if (e instanceof PlayerEntity) return Cat.PLAYER;
        if (e instanceof ItemEntity) return Cat.ITEM;
        if (e instanceof ArmorStandEntity) return null;
        if (e instanceof Monster) return Cat.MOB;
        if (e instanceof LivingEntity) return Cat.ANIMAL;
        return null;
    }

    @Override
    public void renderHud(DrawContext ctx, float tickProgress) {
        if (!(names.get() || distance.get() || health.get())) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.world == null || !mc.options.getPerspective().isFirstPerson()) return;

        TextRenderer tr = mc.textRenderer;
        int drawn = 0;
        for (Target t : targets) {
            if (drawn >= 64) break;
            Entity e = t.entity();
            if (e.isRemoved()) continue;
            Vec3d top = e.getLerpedPos(tickProgress).add(0, e.getHeight() + 0.35, 0);
            if (!Projector.project(mc, p, top, tickProgress, screen)) continue;

            String text = label(e, p);
            if (text.isEmpty()) continue;
            int w = tr.getWidth(text);
            int x = Math.round(screen[0]) - w / 2;
            int y = Math.round(screen[1]) - tr.fontHeight;
            Gfx.rounded(ctx, x - 3, y - 2, w + 6, tr.fontHeight + 4, 3, 0x99000000);
            Gfx.text(ctx, tr, text, x, y, t.rgb(), false);
            drawn++;
        }
    }

    private String label(Entity e, ClientPlayerEntity p) {
        StringBuilder sb = new StringBuilder();
        if (names.get()) {
            if (e instanceof ItemEntity item) {
                sb.append(item.getStack().getName().getString());
                if (item.getStack().getCount() > 1) sb.append(" x").append(item.getStack().getCount());
            } else {
                sb.append(e.getName().getString());
            }
        }
        if (distance.get()) {
            if (sb.length() > 0) sb.append("  ");
            sb.append(String.format("%.1fm", p.distanceTo(e)));
        }
        if (health.get() && e instanceof LivingEntity le) {
            if (sb.length() > 0) sb.append("  ");
            sb.append(String.format("%.1f/%.1f HP", le.getHealth(), le.getMaxHealth()));
        }
        return sb.toString();
    }
}
