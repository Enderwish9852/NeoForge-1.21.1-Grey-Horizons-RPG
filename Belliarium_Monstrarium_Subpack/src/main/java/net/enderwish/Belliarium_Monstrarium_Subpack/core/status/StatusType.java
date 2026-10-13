package net.enderwish.Belliarium_Monstrarium_Subpack.core.status;

/**
 * StatusType
 *
 * The mod's own "worse poison effects" system. BODY-part entries mirror
 * BodyHealthCapability's disabled/red tiers and are PERMANENT (they hold for
 * as long as the condition does -- cleared only when the body part heals,
 * never by command or /status clear). DEJA_VU is CONDITIONAL: it has a
 * duration, counts as "removable" for /status clear and /status remove, and
 * expires on its own.
 */
public enum StatusType {
    LEFT_HAND_INJURED(false),
    RIGHT_HAND_INJURED(false),
    LEFT_HAND_BROKEN(false),
    RIGHT_HAND_BROKEN(false),
    LEFT_LEG_INJURED(false),
    RIGHT_LEG_INJURED(false),
    LEFT_LEG_BROKEN(false),
    RIGHT_LEG_BROKEN(false),
    DEJA_VU(true);

    private final boolean conditional;

    StatusType(boolean conditional) { this.conditional = conditional; }

    /** True if this status has a duration and can be cleared/removed; false if it's permanent-while-the-condition-holds. */
    public boolean isConditional() { return conditional; }

    public String displayName() {
        String[] words = name().toLowerCase(java.util.Locale.ROOT).split("_");
        StringBuilder sb = new StringBuilder();
        for (String w : words) sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1)).append(' ');
        return sb.toString().trim();
    }
}
