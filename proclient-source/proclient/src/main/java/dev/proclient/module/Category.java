package dev.proclient.module;

public enum Category {
    COMBAT("Combat"),
    RENDER("Render"),
    MOVEMENT("Movement"),
    HUD("HUD"),
    MISC("Miscellaneous");

    public final String label;

    Category(String label) {
        this.label = label;
    }
}
