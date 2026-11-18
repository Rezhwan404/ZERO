package dev.proclient.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class NumSetting extends Setting {
    public final double min;
    public final double max;
    public final double step;
    private double value;

    public NumSetting(String name, double def, double min, double max, double step) {
        super(name);
        this.min = min;
        this.max = max;
        this.step = step;
        this.value = clean(def);
    }

    private double clean(double x) {
        x = Math.max(min, Math.min(max, x));
        if (step > 0) {
            x = Math.round((x - min) / step) * step + min;
            x = Math.max(min, Math.min(max, x));
        }
        return Math.round(x * 10000.0) / 10000.0;
    }

    public double get() {
        return value;
    }

    public float getF() {
        return (float) value;
    }

    public int getI() {
        return (int) Math.round(value);
    }

    public void set(double v) {
        double n = clean(v);
        if (n != value) {
            value = n;
            changed();
        }
    }

    public double fraction() {
        return max == min ? 0 : (value - min) / (max - min);
    }

    public String display() {
        int decimals = step >= 1 ? 0 : (step >= 0.1 ? 1 : 2);
        return String.format("%." + decimals + "f", value);
    }

    @Override
    public JsonElement save() {
        return new JsonPrimitive(value);
    }

    @Override
    public void load(JsonElement e) {
        if (e != null && e.isJsonPrimitive() && e.getAsJsonPrimitive().isNumber()) {
            value = clean(e.getAsDouble());
        }
    }
}
