package dev.proclient.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public final class ModeSetting extends Setting {
    public final String[] options;
    private int index;

    public ModeSetting(String name, String... options) {
        super(name);
        this.options = options;
    }

    public String get() {
        return options[index];
    }

    public int index() {
        return index;
    }

    public void cycle(int dir) {
        index = ((index + dir) % options.length + options.length) % options.length;
        changed();
    }

    @Override
    public JsonElement save() {
        return new JsonPrimitive(get());
    }

    @Override
    public void load(JsonElement e) {
        if (e != null && e.isJsonPrimitive() && e.getAsJsonPrimitive().isString()) {
            String s = e.getAsString();
            for (int i = 0; i < options.length; i++) {
                if (options[i].equals(s)) {
                    index = i;
                    return;
                }
            }
        }
    }
}
