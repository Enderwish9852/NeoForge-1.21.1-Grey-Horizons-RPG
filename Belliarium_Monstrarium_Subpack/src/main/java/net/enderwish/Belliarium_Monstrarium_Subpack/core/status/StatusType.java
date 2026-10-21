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
    DEJA_VU(true),
    /** PERMANENT: granted on first join, never expires, never removable.
     * Slowly drains Sanity (see SanityTickHandler). */
    HIVE_MIND_INFLUENCE(false),
    /** CONDITIONAL/durational: new-player grace — grants +2 sanity/minute
     * for the first 7 in-game days (config), so beginners can set up
     * before the Hive Mind drain starts biting. Granted once, on first
     * world join; never re-granted after it expires. */
    ADVENTURERS_HEART(true),
    /** DERIVED, like the body-part statuses: active only while the body
     *  temp is past its clinical threshold (see
     *  StatusManager.activeEnvironmentStatuses). Never stored, so
     *  /status remove correctly refuses it -- cool down or warm up
     *  instead. */
    HYPERTHERMIA(false),
    HYPOTHERMIA(false),
    /** CONDITIONAL: body wetness maxed out — soaked through. Applied the
     *  moment wetness reaches 100 (indefinite duration, stored like DEJA_VU)
     *  and removed only when the body is half-dried (wetness <= 50), per
     *  the hysteresis rule. While active the body cools very fast (see
     *  ThirstBodyTempTickHandler). */
    DRENCHED(true);

    /** Real clinical thresholds in degrees C: shared by
     *  ThirstBodyTempTickHandler's effects and the /status derivation so
     *  the two can never drift apart. */
    public static final float HYPERTHERMIA_THRESHOLD_C = 40.0f;
    public static final float HYPOTHERMIA_THRESHOLD_C = 35.0f;

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
