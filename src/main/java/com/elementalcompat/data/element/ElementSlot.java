package com.elementalcompat.data.element;

public enum ElementSlot {
    MAINHAND,
    OFFHAND,
    HEAD,
    CHEST,
    LEGS,
    FEET,
    ANY;

    public static ElementSlot fromString(String name) {
        if (name == null) return ANY;

        for (ElementSlot slot : values()) {
            if (slot.name().equalsIgnoreCase(name)) {
                return slot;
            }
        }
        return ANY;
    }
}
