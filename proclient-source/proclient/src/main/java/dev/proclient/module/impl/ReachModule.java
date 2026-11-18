package dev.proclient.module.impl;

import dev.proclient.module.Category;
import dev.proclient.module.Module;
import dev.proclient.setting.NumSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.UUID;

/**
 * Extends interaction range through vanilla's own interaction-range attributes (no packet or
 * server-check tampering). The modifier is applied to the client player and to the integrated
 * server's copy of the player, so single-player validation agrees with the client.
 */
public final class ReachModule extends Module {
    private static final Identifier MOD_ID = Identifier.of("proclient", "reach");
    private static final double VANILLA_ENTITY_REACH = 3.0;

    private final NumSetting distance = add(new NumSetting("Reach Distance", 4.5, 3.0, 8.0, 0.1));

    private ClientPlayerEntity appliedTo;
    private double appliedDelta = Double.NaN;

    public ReachModule() {
        super("Reach", "Extends entity and block reach (single-player only).", Category.COMBAT, true);
    }

    @Override
    protected void onDisable() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            apply(mc, mc.player, 0.0, true);
        }
        appliedTo = null;
        appliedDelta = Double.NaN;
    }

    @Override
    public void onTick(MinecraftClient mc) {
        ClientPlayerEntity p = mc.player;
        if (p == null) return;
        double delta = distance.get() - VANILLA_ENTITY_REACH;
        if (p != appliedTo || delta != appliedDelta) {
            apply(mc, p, delta, false);
            appliedTo = p;
            appliedDelta = delta;
        }
    }

    private static void apply(MinecraftClient mc, ClientPlayerEntity p, double delta, boolean remove) {
        applyTo(p, delta, remove);
        IntegratedServer server = mc.getServer();
        if (server == null) return;
        UUID id = p.getUuid();
        server.execute(() -> {
            ServerPlayerEntity sp = server.getPlayerManager().getPlayer(id);
            if (sp != null) applyTo(sp, delta, remove);
        });
    }

    private static void applyTo(LivingEntity e, double delta, boolean remove) {
        set(e.getAttributeInstance(EntityAttributes.ENTITY_INTERACTION_RANGE), delta, remove);
        set(e.getAttributeInstance(EntityAttributes.BLOCK_INTERACTION_RANGE), delta, remove);
    }

    private static void set(EntityAttributeInstance inst, double delta, boolean remove) {
        if (inst == null) return;
        inst.removeModifier(MOD_ID);
        if (!remove && delta != 0.0) {
            inst.addTemporaryModifier(new EntityAttributeModifier(MOD_ID, delta, EntityAttributeModifier.Operation.ADD_VALUE));
        }
    }
}
