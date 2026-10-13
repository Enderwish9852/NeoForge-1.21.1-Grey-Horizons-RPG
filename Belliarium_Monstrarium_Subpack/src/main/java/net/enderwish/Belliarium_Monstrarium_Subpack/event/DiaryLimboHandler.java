package net.enderwish.Belliarium_Monstrarium_Subpack.event;

import net.enderwish.Belliarium_Monstrarium_Subpack.BelliariumMonstrariumSubpack;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.ModAttachments;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.body.BodyHealthCapability;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.diary.DiaryLimbo;
import net.enderwish.Belliarium_Monstrarium_Subpack.network.DeathReportPacket;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * DiaryLimboHandler
 *
 * Holds limbo players still every tick. If someone quits while in limbo (head or torso at 0
 * with a rewind available), limbo is re-entered 2 seconds after login, with a generic report.
 */
@EventBusSubscriber(modid = BelliariumMonstrariumSubpack.MODID)
public class DiaryLimboHandler {

    private static final int LOGIN_DELAY_TICKS = 40;
    private static final Map<UUID, Integer> loginCountdown = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        loginCountdown.put(event.getEntity().getUUID(), LOGIN_DELAY_TICKS);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID id = event.getEntity().getUUID();
        loginCountdown.remove(id);
        DiaryLimbo.leave(id);
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        DiaryLimbo.tickFreeze(player);

        Integer left = loginCountdown.get(player.getUUID());
        if (left == null) return;
        if (left > 1) {
            loginCountdown.put(player.getUUID(), left - 1);
            return;
        }
        loginCountdown.remove(player.getUUID());

        BodyHealthCapability cap = player.getData(ModAttachments.BODY_HEALTH);
        if (!(cap.isHeadFatal() || cap.isTorsoFatal())) return;
        if (DiaryLimbo.isInLimbo(player.getUUID()) || !DiaryLimbo.canRewind(player)) return;

        DiaryLimbo.enter(player, new DeathReportPacket(false, player.getName().getString() + " collapsed.",
                cap.getHeadPct(), cap.getTorsoPct(), cap.getLeftArmPct(), cap.getRightArmPct(),
                cap.getLeftLegPct(), cap.getRightLegPct(), cap.getLeftFootPct(), cap.getRightFootPct(),
                List.of(), true));
    }
}
