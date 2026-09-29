package net.enderwish.Belliarium_Monstrarium_Subpack.core;

/**
 * CombatLogEntry
 *
 * One row of the future Death Screen Report scoreboard. secondsIntoBattle is
 * relative to that battle's own start. distanceBlocks/-1 and sourceName fall
 * back to a generic label when the DamageSource has no clear attacking entity
 * (fall, starvation, etc.) rather than throwing.
 */
public record CombatLogEntry(
        int secondsIntoBattle,
        String sourceName,
        double distanceBlocks,
        float amount,
        String bodyPart
) {}
