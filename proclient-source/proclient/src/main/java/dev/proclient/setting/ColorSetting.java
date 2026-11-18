package dev.proclient.setting;

/** A colour chosen by hue (0-360). Saturation/brightness are fixed for a consistent look. */
public final class ColorSetting extends NumSetting {
    public ColorSetting(String name, double hue) {
        super(name, hue, 0, 360, 1);
    }

    public int rgb() {
        return hueToRgb(get());
    }

    public static int hueToRgb(double hue) {
        double s = 0.70, v = 1.0;
        double h = (((hue % 360) + 360) % 360) / 60.0;
        double c = v * s;
        double x = c * (1 - Math.abs(h % 2 - 1));
        double m = v - c;
        double r, g, b;
        switch ((int) h) {
            case 0 -> { r = c; g = x; b = 0; }
            case 1 -> { r = x; g = c; b = 0; }
            case 2 -> { r = 0; g = c; b = x; }
            case 3 -> { r = 0; g = x; b = c; }
            case 4 -> { r = x; g = 0; b = c; }
            default -> { r = c; g = 0; b = x; }
        }
        int ri = (int) Math.round((r + m) * 255);
        int gi = (int) Math.round((g + m) * 255);
        int bi = (int) Math.round((b + m) * 255);
        return (ri << 16) | (gi << 8) | bi;
    }
}
