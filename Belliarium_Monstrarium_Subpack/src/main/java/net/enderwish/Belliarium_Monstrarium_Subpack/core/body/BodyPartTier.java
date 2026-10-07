package net.enderwish.Belliarium_Monstrarium_Subpack.core.body;

/**
 * BodyPartTier
 *
 * Severity classification, same 0.4/0.75 boundaries BodyPartColors
 * already uses for rendering -- kept as a SEPARATE small duplication
 * rather than having BodyPartColors (a pure rendering class) depend on
 * this gameplay enum, or vice versa.
 */
public enum BodyPartTier {
    GREEN, YELLOW, RED, GREY;

    public static BodyPartTier from(float pct) {
        if (pct <= 0f) return GREY;
        if (pct < 0.4f) return RED;
        if (pct < 0.75f) return YELLOW;
        return GREEN;
    }
}
