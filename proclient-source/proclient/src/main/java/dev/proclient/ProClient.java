package dev.proclient;

import dev.proclient.config.ConfigManager;
import dev.proclient.module.ModuleManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.util.Identifier;

public final class ProClient implements ClientModInitializer {
    public static final String ID = "proclient";
    private static ModuleManager modules;

    public static ModuleManager modules() {
        return modules;
    }

    @Override
    public void onInitializeClient() {
        modules = new ModuleManager();
        ConfigManager.init(modules);

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            modules.tick(mc);
            ConfigManager.tick();
        });
        ClientPlayConnectionEvents.JOIN.register((handler, sender, mc) -> modules.enforceSingleplayer(mc));
        ClientLifecycleEvents.CLIENT_STOPPING.register(mc -> ConfigManager.save());

        HudElementRegistry.attachElementBefore(
                VanillaHudElements.CHAT,
                Identifier.of(ID, "overlay"),
                (ctx, tickCounter) -> modules.renderHud(ctx, tickCounter.getTickProgress(false)));
    }
}
