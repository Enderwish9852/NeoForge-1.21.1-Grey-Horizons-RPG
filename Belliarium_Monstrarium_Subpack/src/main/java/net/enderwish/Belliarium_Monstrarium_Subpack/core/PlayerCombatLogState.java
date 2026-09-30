package net.enderwish.Belliarium_Monstrarium_Subpack.core;

/**
 * PlayerCombatLogState
 *
 * Per-player, in-memory-only -- same lifetime as CombatState, doesn't
 * survive a relog and isn't meant to. Tracks the running numbers behind the
 * Combat Log HUD's persistent exp rows. Reset at the start of every new
 * battle, mirroring CombatState.startCombat().
 */
public class PlayerCombatLogState {

    private boolean wasInCombat = false;

    private int lastCombatTimeMilestoneSecond = 0;
    private float cumulativeCombatTimeXp = 0f;

    private float cumulativeDamageXp = 0f;
    private String lastAttackTypeLabel = "Hit";

    private int comboCount = 0;
    private long lastMeleeHitTick = -1;

    public boolean wasInCombat() { return wasInCombat; }
    public void setWasInCombat(boolean value) { wasInCombat = value; }

    public int getLastCombatTimeMilestoneSecond() { return lastCombatTimeMilestoneSecond; }
    public void setLastCombatTimeMilestoneSecond(int value) { lastCombatTimeMilestoneSecond = value; }
    public float getCumulativeCombatTimeXp() { return cumulativeCombatTimeXp; }
    public void addCombatTimeXp(float amount) { cumulativeCombatTimeXp += amount; }

    public float getCumulativeDamageXp() { return cumulativeDamageXp; }
    public void addDamageXp(float amount) { cumulativeDamageXp += amount; }
    public void resetDamageXp() { cumulativeDamageXp = 0f; }
    public String getLastAttackTypeLabel() { return lastAttackTypeLabel; }
    public void setLastAttackTypeLabel(String label) { lastAttackTypeLabel = label; }

    public int getComboCount() { return comboCount; }
    public long getLastMeleeHitTick() { return lastMeleeHitTick; }

    public void registerMeleeHit(long gameTime, long comboWindowTicks) {
        if (lastMeleeHitTick >= 0 && (gameTime - lastMeleeHitTick) <= comboWindowTicks) {
            comboCount++;
        } else {
            comboCount = 1;
        }
        lastMeleeHitTick = gameTime;
    }

    public void breakCombo() {
        comboCount = 0;
        lastMeleeHitTick = -1;
    }

    public void resetForNewBattle() {
        lastCombatTimeMilestoneSecond = 0;
        cumulativeCombatTimeXp = 0f;
        cumulativeDamageXp = 0f;
        comboCount = 0;
        lastMeleeHitTick = -1;
    }
}
