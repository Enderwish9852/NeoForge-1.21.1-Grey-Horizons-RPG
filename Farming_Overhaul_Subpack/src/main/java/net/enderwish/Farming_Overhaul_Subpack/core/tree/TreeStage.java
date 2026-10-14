package net.enderwish.Farming_Overhaul_Subpack.core.tree;

/**
 * TreeStage — biological growth stages of a planted tree.
 *
 * A player-planted tree grows one stage per season, taking a full
 * year (4 seasons) to reach MATURE. Each stage scales every part of
 * the generated tree biologically:
 *
 *   SAPLING     — a slender stem with a small leaf tuft; no branches
 *   YOUNG       — first real limbs, thin trunk, small crown
 *   SEMI_MATURE — near-full spread, still thinning upward
 *   MATURE      — the full generated tree (fruit + root flare)
 *
 * sizeScale multiplies every length/height/count in TreeGenerator;
 * maxTrunk caps the trunk thickness so young trees are visibly
 * thin regardless of species.
 */
public enum TreeStage {
    SAPLING    ("sapling",     0.30f, 2),
    YOUNG      ("young",       0.55f, 3),
    SEMI_MATURE("semi_mature", 0.80f, 5),
    MATURE     ("mature",      1.00f, 6);

    public final String displayName;
    /** Multiplier applied to heights, lengths, counts and canopy radius. */
    public final float sizeScale;
    /** Upper bound for the trunk thickness level at this stage. */
    public final int maxTrunk;

    TreeStage(String displayName, float sizeScale, int maxTrunk) {
        this.displayName = displayName;
        this.sizeScale = sizeScale;
        this.maxTrunk = maxTrunk;
    }

    public int id() { return ordinal(); }

    public TreeStage next() {
        return TreeStage.values()[Math.min(values().length - 1, ordinal() + 1)];
    }

    public TreeStage previous() {
        return TreeStage.values()[Math.max(0, ordinal() - 1)];
    }

    public static TreeStage byId(int id) {
        TreeStage[] all = values();
        return all[Math.floorMod(id, all.length)];
    }

    /** Lenient name lookup ("semi", "semi_mature", "SAPLING" all work). */
    public static TreeStage byName(String input) {
        if (input == null) return null;
        String s = input.toLowerCase().trim();
        for (TreeStage st : values()) {
            if (st.displayName.equals(s)) return st;
        }
        if (s.equals("semi") || s.equals("halfgrown")) return SEMI_MATURE;
        if (s.equals("adult") || s.equals("full")) return MATURE;
        return null;
    }
}
