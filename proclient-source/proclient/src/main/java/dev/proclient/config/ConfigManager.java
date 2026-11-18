package dev.proclient.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.proclient.module.Module;
import dev.proclient.module.ModuleManager;
import dev.proclient.setting.Setting;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Stores every module's state, keybind and settings in config/proclient.json.
 * Writes go to a temp file first and are moved into place, the previous good file is kept
 * as .bak, and a corrupt file is renamed aside instead of being overwritten or crashing the game.
 */
public final class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final long SAVE_DELAY_MS = 1500;

    private static ModuleManager manager;
    private static Path file;
    private static Path backup;
    private static Path temp;
    private static boolean dirty;
    private static long lastChange;

    private ConfigManager() {}

    public static void init(ModuleManager mm) {
        manager = mm;
        Path dir = FabricLoader.getInstance().getConfigDir();
        file = dir.resolve("proclient.json");
        backup = dir.resolve("proclient.json.bak");
        temp = dir.resolve("proclient.json.tmp");
        load();
        if (!Files.exists(file)) save(); // write defaults on first run
    }

    public static void markDirty() {
        dirty = true;
        lastChange = System.currentTimeMillis();
    }

    /** Called every client tick: debounced auto-save. */
    public static void tick() {
        if (dirty && System.currentTimeMillis() - lastChange >= SAVE_DELAY_MS) save();
    }

    private static void load() {
        if (tryLoad(file)) return;
        if (Files.exists(file)) quarantine();
        tryLoad(backup); // falls back to defaults if this fails as well
    }

    private static boolean tryLoad(Path path) {
        if (!Files.isRegularFile(path)) return false;
        try {
            String text = Files.readString(path, StandardCharsets.UTF_8);
            JsonElement root = JsonParser.parseString(text);
            if (!root.isJsonObject()) throw new IllegalStateException("root is not an object");
            apply(root.getAsJsonObject());
            return true;
        } catch (IOException | RuntimeException e) {
            System.err.println("[ProClient] Could not read " + path.getFileName() + ": " + e.getMessage());
            return false;
        }
    }

    private static void quarantine() {
        try {
            Path bad = file.resolveSibling("proclient.json.corrupt-" + System.currentTimeMillis());
            Files.move(file, bad, StandardCopyOption.REPLACE_EXISTING);
            System.err.println("[ProClient] Corrupt config moved to " + bad.getFileName());
        } catch (IOException e) {
            System.err.println("[ProClient] Could not quarantine corrupt config: " + e.getMessage());
        }
    }

    private static void apply(JsonObject root) {
        JsonElement modsEl = root.get("modules");
        if (modsEl == null || !modsEl.isJsonObject()) return;
        JsonObject mods = modsEl.getAsJsonObject();
        for (Module m : manager.modules) {
            JsonElement me = mods.get(m.name);
            if (me == null || !me.isJsonObject()) continue;
            JsonObject mo = me.getAsJsonObject();
            try {
                JsonElement en = mo.get("enabled");
                if (en != null && en.isJsonPrimitive() && en.getAsJsonPrimitive().isBoolean()) m.restoreEnabled(en.getAsBoolean());
                JsonElement key = mo.get("key");
                if (key != null && key.isJsonPrimitive() && key.getAsJsonPrimitive().isNumber()) m.setKey(key.getAsInt());
                JsonElement se = mo.get("settings");
                if (se != null && se.isJsonObject()) {
                    JsonObject so = se.getAsJsonObject();
                    for (Setting s : m.settings) {
                        JsonElement v = so.get(s.name);
                        if (v == null) continue;
                        try {
                            s.load(v);
                        } catch (RuntimeException ignored) {
                            // bad value for one setting: keep its default
                        }
                    }
                }
            } catch (RuntimeException ignored) {
                // bad entry for one module: keep its defaults
            }
        }
        dirty = false;
    }

    public static synchronized void save() {
        if (manager == null) return;
        JsonObject root = new JsonObject();
        root.addProperty("version", 1);
        JsonObject mods = new JsonObject();
        for (Module m : manager.modules) {
            JsonObject mo = new JsonObject();
            if (!m.isMomentary()) mo.addProperty("enabled", m.isEnabled());
            mo.addProperty("key", m.key());
            JsonObject so = new JsonObject();
            for (Setting s : m.settings) so.add(s.name, s.save());
            mo.add("settings", so);
            mods.add(m.name, mo);
        }
        root.add("modules", mods);
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(temp, GSON.toJson(root), StandardCharsets.UTF_8);
            if (Files.exists(file)) Files.copy(file, backup, StandardCopyOption.REPLACE_EXISTING);
            try {
                Files.move(temp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
            }
            dirty = false;
        } catch (IOException e) {
            System.err.println("[ProClient] Could not save config: " + e.getMessage());
        }
    }
}
