package net.enderwish.Belliarium_Monstrarium_Subpack.client;

import net.enderwish.Atmospheric_Overhaul_Subpack.api.SeasonsAPI;
import net.enderwish.Belliarium_Monstrarium_Subpack.BelliariumMonstrariumSubpack;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.ModAttachments;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.body.BodyHealthCapability;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.status.StatusType;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.survival.SurvivalCapability;
import net.enderwish.Belliarium_Monstrarium_Subpack.event.BodyWetnessTickHandler;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent;

import java.util.List;

/**
 * ClientDebugHandler — the body-side [GH ...] F3 lines, rendered under
 * Atmospheric's block (same CustomizeGuiOverlayEvent.DebugText event;
 * gh_atmospheric constructs before gh_belliarium_monstrarium, so these
 * land right below theirs). Atmospheric's [GH Feels Like] is the
 * ENVIRONMENT's value and stays as-is; what a wet BODY feels lives
 * here: environmental feels-like minus the wet-skin evaporative chill
 * (BodyWetnessTickHandler.wetChillC — the same single source
 * ThirstBodyTempTickHandler builds its body-temp target from), plus
 * body temp, wetness, and every body-part health value.
 */
@EventBusSubscriber(modid = BelliariumMonstrariumSubpack.MODID,
        bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class ClientDebugHandler {

    @SubscribeEvent
    public static void onDebugInfo(CustomizeGuiOverlayEvent.DebugText event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        SurvivalCapability survival = mc.player.getData(ModAttachments.SURVIVAL);
        BodyHealthCapability body = mc.player.getData(ModAttachments.BODY_HEALTH);
        List<String> left = event.getLeft();
        left.add("");

        // ── Body temperature ─────────────────────────────────────────────
        float bodyTemp = survival.getBodyTemp();
        left.add("§6[GH Body Temp]§r "
                + String.format("%.1f°C", bodyTemp)
                + (bodyTemp > StatusType.HYPERTHERMIA_THRESHOLD_C ? " §c[HYPERTHERMIA]§r" : "")
                + (bodyTemp < StatusType.HYPOTHERMIA_THRESHOLD_C ? " §b[HYPOTHERMIA]§r" : ""));

        // ── Wetness + the felt temperature it causes ─────────────────────
        float wetness = survival.getWetness();
        // DRENCHED is stored server-side; derive it here the same way its
        // hysteresis does (applied at 100%, held until 50% dry)
        boolean drenched = wetness > BodyWetnessTickHandler.DRENCHED_CLEAR_WETNESS;
        float chill = BodyWetnessTickHandler.wetChillC(wetness,
                SeasonsAPI.getClientWindSpeed(), drenched);
        float envC = SeasonsAPI.getClientFeelsLikeTempCelsius();
        left.add("§6[GH Body Feels]§r "
                + String.format("%.1f°C", envC - chill)
                + " §7(env " + envC + "°C - chill "
                + String.format("%.1f°C", chill) + ")");
        left.add("§6[GH Wetness]§r "
                + String.format("%.0f%%", wetness)
                + (drenched ? " §b[DRENCHED]§r" : ""));

        // ── Body health ──────────────────────────────────────────────────
        left.add("§6[GH Body Health]§r "
                + "Head " + part(body.getHeadPct(), body.isHeadFatal())
                + " | Torso " + part(body.getTorsoPct(), body.isTorsoFatal()));
        left.add("§6[GH Limbs]§r "
                + "L-Arm " + part(body.getLeftArmPct(), body.isLeftArmDisabled())
                + " | R-Arm " + part(body.getRightArmPct(), body.isRightArmDisabled())
                + " | L-Leg " + part(body.getLeftLegPct(), body.isLeftLegDisabled())
                + " | R-Leg " + part(body.getRightLegPct(), body.isRightLegDisabled())
                + " | L-Foot " + part(body.getLeftFootPct(), body.isLeftFootDisabled())
                + " | R-Foot " + part(body.getRightFootPct(), body.isRightFootDisabled()));
    }

    /** One body part for the F3 line: BROKEN / injured-red / plain %. */
    private static String part(float pct, boolean disabled) {
        if (disabled) return "§4BROKEN§r";
        int whole = Math.round(pct * 100.0f);
        if (pct < 0.4f) return "§c" + whole + "%§r";
        return String.valueOf(whole) + "%";
    }
}
