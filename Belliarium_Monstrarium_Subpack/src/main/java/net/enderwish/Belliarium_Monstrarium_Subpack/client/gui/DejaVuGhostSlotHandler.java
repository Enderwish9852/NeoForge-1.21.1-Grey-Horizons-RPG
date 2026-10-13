package net.enderwish.Belliarium_Monstrarium_Subpack.client.gui;

import net.enderwish.Belliarium_Monstrarium_Subpack.BelliariumMonstrariumSubpack;
import net.enderwish.Belliarium_Monstrarium_Subpack.client.DejaVuClientState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ContainerScreenEvent;

import java.util.List;

/**
 * DejaVuGhostSlotHandler
 *
 * While a remembered container's screen is open (DejaVuClientState.isGhostActive()),
 * draws each looted item as a pulsing, translucent "ghost" directly over the
 * real slot it used to occupy -- the real slot underneath is already empty.
 * Fires on ContainerScreenEvent.Render.Foreground, which NeoForge posts from
 * inside AbstractContainerScreen.render() after the pose stack has already
 * been translated by (leftPos, topPos), so Slot.x/Slot.y line up directly.
 */
@EventBusSubscriber(modid = BelliariumMonstrariumSubpack.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class DejaVuGhostSlotHandler {
    private DejaVuGhostSlotHandler() {}

    @SubscribeEvent
    public static void onRenderForeground(ContainerScreenEvent.Render.Foreground event) {
        if (!DejaVuClientState.isGhostActive()) return;

        AbstractContainerScreen<?> screen = event.getContainerScreen();
        GuiGraphics graphics = event.getGuiGraphics();
        Minecraft mc = Minecraft.getInstance();

        List<Integer> slotIndices = DejaVuClientState.getGhostSlotIndices();
        List<String> itemIds = DejaVuClientState.getGhostItemIds();
        List<Integer> counts = DejaVuClientState.getGhostCounts();

        float pulse = 0.55f + 0.35f * Mth.sin((System.currentTimeMillis() % 1000) / 1000f * (float) Math.PI * 2f);

        for (int i = 0; i < slotIndices.size(); i++) {
            int slotIndex = slotIndices.get(i);
            if (slotIndex < 0 || slotIndex >= screen.getMenu().slots.size()) continue;
            Slot slot = screen.getMenu().slots.get(slotIndex);

            ResourceLocation id = ResourceLocation.tryParse(itemIds.get(i));
            Item item = id == null ? Items.AIR : BuiltInRegistries.ITEM.get(id);
            if (item == Items.AIR) continue;

            ItemStack ghost = new ItemStack(item, Math.max(1, counts.get(i)));

            graphics.pose().pushPose();
            graphics.setColor(1f, 1f, 1f, pulse);
            graphics.renderItem(ghost, slot.x, slot.y);
            graphics.setColor(1f, 1f, 1f, 1f);
            graphics.renderItemDecorations(mc.font, ghost, slot.x, slot.y);
            graphics.pose().popPose();
        }
    }
}
