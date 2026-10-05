package net.enderwish.Belliarium_Monstrarium_Subpack.core.skills;

/**
 * KeyComboCodec
 *
 * Packs a GLFW key code plus Ctrl/Shift/Alt flags into one int, so every
 * existing Integer-keybind field (LearnedSkillsCapability, SkillBindKeyPacket,
 * SkillBindResultPacket) needs zero structural changes -- they just now
 * carry a packed value.
 */
public final class KeyComboCodec {
    private KeyComboCodec() {}

    private static final int CTRL_BIT  = 1 << 24;
    private static final int SHIFT_BIT = 1 << 25;
    private static final int ALT_BIT   = 1 << 26;

    public static int pack(int keyCode, boolean ctrl, boolean shift, boolean alt) {
        int packed = keyCode;
        if (ctrl) packed |= CTRL_BIT;
        if (shift) packed |= SHIFT_BIT;
        if (alt) packed |= ALT_BIT;
        return packed;
    }

    public static int keyCode(int packed) { return packed & 0xFFFFFF; }
    public static boolean hasCtrl(int packed) { return (packed & CTRL_BIT) != 0; }
    public static boolean hasShift(int packed) { return (packed & SHIFT_BIT) != 0; }
    public static boolean hasAlt(int packed) { return (packed & ALT_BIT) != 0; }

    /** Server-safe display -- no client-only key-NAME lookup available server-side, so this shows the raw code. */
    public static String describe(int packed) {
        StringBuilder sb = new StringBuilder();
        if (hasCtrl(packed)) sb.append("Ctrl+");
        if (hasShift(packed)) sb.append("Shift+");
        if (hasAlt(packed)) sb.append("Alt+");
        sb.append("Key(").append(keyCode(packed)).append(")");
        return sb.toString();
    }
}
