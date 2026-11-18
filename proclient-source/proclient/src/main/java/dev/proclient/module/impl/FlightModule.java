package dev.proclient.module.impl;

import dev.proclient.module.Category;
import dev.proclient.module.Module;
import dev.proclient.setting.NumSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerAbilities;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.UUID;

/**
 * Flight through the player's own abilities (the same mechanism creative flight uses), so
 * movement stays smooth. The integrated server's copy of the player is told flight is allowed
 * so single-player never reverts it.
 */
public final class FlightModule extends Module {
    private static final float VANILLA_FLY_SPEED = 0.05f;

    private final NumSetting speed = add(new NumSetting("Flight Speed", 2.0, 0.5, 10.0, 0.1));

    private int startFlyingIn = -1;
    private int syncTimer;

    public FlightModule() {
        super("Flight", "Smooth flight with adjustable speed (single-player only).", Category.MOVEMENT, true);
    }

    @Override
    protected void onEnable() {
        startFlyingIn = 6; // give the server copy a few ticks to accept allowFlying first
        syncTimer = 0;
    }

    @Override
    protected void onDisable() {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity p = mc.player;
        startFlyingIn = -1;
        if (p != null) {
            PlayerAbilities ab = p.getAbilities();
            ab.setFlySpeed(VANILLA_FLY_SPEED);
            boolean keep = p.isCreative() || p.isSpectator();
            ab.allowFlying = keep;
            if (!keep && ab.flying) {
                ab.flying = false;
                p.sendAbilitiesUpdate();
            }
        }
        syncServer(mc, false);
    }

    @Override
    public void onTick(MinecraftClient mc) {
        ClientPlayerEntity p = mc.player;
        if (p == null || p.isSpectator()) return;

        PlayerAbilities ab = p.getAbilities();
        ab.allowFlying = true;
        ab.setFlySpeed((float) (speed.get() * VANILLA_FLY_SPEED));

        if (syncTimer-- <= 0) {
            syncTimer = 20;
            syncServer(mc, true);
        }
        if (startFlyingIn > 0 && --startFlyingIn == 0) {
            if (!ab.flying) {
                ab.flying = true;
                p.sendAbilitiesUpdate();
            }
        }
    }

    private static void syncServer(MinecraftClient mc, boolean enable) {
        ClientPlayerEntity p = mc.player;
        IntegratedServer server = mc.getServer();
        if (p == null || server == null) return;
        UUID id = p.getUuid();
        server.execute(() -> {
            ServerPlayerEntity sp = server.getPlayerManager().getPlayer(id);
            if (sp == null) return;
            boolean keep = sp.isCreative() || sp.isSpectator();
            sp.getAbilities().allowFlying = enable || keep;
            if (!enable && !keep) sp.getAbilities().flying = false;
        });
    }
}
