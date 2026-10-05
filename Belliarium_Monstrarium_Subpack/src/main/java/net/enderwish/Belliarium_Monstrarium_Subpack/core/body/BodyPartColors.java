package net.enderwish.Belliarium_Monstrarium_Subpack.core.body;

/**
 * BodyPartColors
 *
 * Extracted from BodyHealthHUD's original inline thresholds so DeathReportScreen
 * can reuse the exact same green/yellow/red/grey mapping instead of duplicating
 * the numbers in two places. No behavior change to BodyHealthHUD's output.
 */
public final class BodyPartColors {
    private BodyPartColors() {}

    public static int colorForPct(float pct) {
        if (pct <= 0f) return 0xFF1A1A1A;
        if (pct < 0.4f) return 0xFFFF3333;
        if (pct < 0.75f) return 0xFFFFCC33;
        return 0xFF33CC33;
    }
}
