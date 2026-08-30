package mccfk.DeltaForce.java.client.overlay;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.rzy.dealt_force_skills.item.DfsItemQuality;
import mccfk.DeltaForce.java.DeltaForce;
import mccfk.DeltaForce.java.entity.GroundItemEntity;
import mccfk.DeltaForce.java.item.DeltaForceItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 地面物品信息覆盖层 - 当玩家看向地面物品时显示信息UI
 */
public class GroundItemOverlay {

    public static final IGuiOverlay OVERLAY = GroundItemOverlay::renderOverlay;

    private static float fadeProgress = 0f;
    private static GroundItemEntity currentTarget = null;
    private static long lastDropTime = 0;

    private static void renderOverlay(net.minecraftforge.client.gui.overlay.ForgeGui forgeGui, GuiGraphics guiGraphics,
                                       float partialTick, int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();

        if (mc.options.hideGui || mc.player == null || mc.level == null) {
            fadeProgress = 0f;
            currentTarget = null;
            return;
        }

        HitResult hitResult = mc.hitResult;
        GroundItemEntity targetEntity = null;

        if (hitResult != null && hitResult.getType() == HitResult.Type.ENTITY) {
            Entity entity = ((EntityHitResult) hitResult).getEntity();
            if (entity instanceof GroundItemEntity groundItem) {
                if (!groundItem.getItem().isEmpty()) {
                    targetEntity = groundItem;
                    if (currentTarget != targetEntity) {
                        lastDropTime = System.currentTimeMillis();
                    }
                }
            }
        }

        if (targetEntity != null) {
            fadeProgress = Math.min(1.0f, fadeProgress + 0.1f);
            currentTarget = targetEntity;
        } else {
            fadeProgress = Math.max(0.0f, fadeProgress - 0.1f);
            if (fadeProgress <= 0.01f) {
                fadeProgress = 0f;
                currentTarget = null;
                lastDropTime = 0;
                return;
            }
        }

        if (currentTarget != null && fadeProgress > 0) {
            renderItemInfo(guiGraphics, mc, currentTarget, fadeProgress, screenWidth);
        }
    }

    private static void renderItemInfo(GuiGraphics guiGraphics, Minecraft mc, GroundItemEntity entity, float alpha,
                                        int screenWidth) {
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();

        ItemStack itemStack = entity.getItem();
        DfsItemQuality quality = DfsItemQuality.of(itemStack);

        Component itemName = itemStack.getHoverName();
        Component qualityLine = Component.translatable("tooltip.dealt_force_skills.quality",
                Component.translatable("quality.dealt_force_skills." + quality.key()));

        String description = getDescription(entity);

        int padding = 8;
        int itemDisplaySize = 40;
        int lineHeight = 10;
        int topPadding = 10;
        int bottomPadding = 18;

        List<String> wrappedDescLines = new ArrayList<>();
        if (description != null) {
            for (String line : description.split("\n")) {
                wrappedDescLines.addAll(wrapText(line));
            }
        }

        int maxTextWidth = Math.max(mc.font.width(itemName), mc.font.width(qualityLine));
        for (String line : wrappedDescLines) {
            maxTextWidth = Math.max(maxTextWidth, mc.font.width(line));
        }
        int boxWidth = Math.min(Math.max(150, maxTextWidth + padding * 2 + itemDisplaySize + 8),
                Math.max(150, screenWidth - 40));

        int nameHeight = lineHeight * 2;
        int itemAreaHeight = itemDisplaySize + 4;

        int descHeight = wrappedDescLines.size() * lineHeight;
        int boxHeight = Math.max(60, topPadding + nameHeight + 4 + itemAreaHeight + 4 + descHeight + bottomPadding);

        int boxX = screenWidth - boxWidth - 20;
        int boxY = 60;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        int backgroundColor = (int)(alpha * 240) << 24 | 0x1E1E1E;
        guiGraphics.fill(boxX - 1, boxY - 1, boxX + boxWidth + 1, boxY + boxHeight + 1, 0xFF000000);
        guiGraphics.fill(boxX, boxY, boxX + boxWidth, boxY + boxHeight, backgroundColor);

        int borderColorTop = (int)(alpha * 200) << 24 | 0x5A5A5A;
        int borderColorBottom = (int)(alpha * 200) << 24 | 0x3A3A3A;
        guiGraphics.fill(boxX, boxY, boxX + boxWidth, boxY + 1, borderColorTop);
        guiGraphics.fill(boxX, boxY + boxHeight - 1, boxX + boxWidth, boxY + boxHeight, borderColorBottom);
        guiGraphics.fill(boxX, boxY, boxX + 1, boxY + boxHeight, borderColorTop);
        guiGraphics.fill(boxX + boxWidth - 1, boxY, boxX + boxWidth, boxY + boxHeight, borderColorBottom);

        if (quality == DfsItemQuality.COLORFUL) {
            float rainbowPhase = DfsItemQuality.rainbowPhase();
            int textAlpha = (int)(alpha * 255.0F);
            drawFlowingRainbowString(guiGraphics, mc, itemName.getString(), boxX + padding,
                    boxY + topPadding, textAlpha, rainbowPhase);
            drawFlowingRainbowString(guiGraphics, mc, qualityLine.getString(), boxX + padding,
                    boxY + topPadding + lineHeight, textAlpha, rainbowPhase);
        } else {
            int nameColor = (int)(alpha * 255) << 24 | quality.color();
            guiGraphics.drawString(mc.font, itemName, boxX + padding, boxY + topPadding, nameColor, false);
            guiGraphics.drawString(mc.font, qualityLine, boxX + padding, boxY + topPadding + lineHeight,
                    nameColor, false);
        }

        int currentY = boxY + topPadding + nameHeight + 4;

        int separatorY = currentY;
        int lineColor = (int)(alpha * 100) << 24 | 0x666666;
        guiGraphics.fill(boxX + padding, separatorY, boxX + boxWidth - padding, separatorY + 1, lineColor);

        currentY += 4;

        int itemBoxX = boxX + padding;
        int itemBoxY = currentY;

        int itemBoxColor = (int)(alpha * 150) << 24 | 0x2A2A2A;
        guiGraphics.fill(itemBoxX, itemBoxY, itemBoxX + itemDisplaySize, itemBoxY + itemDisplaySize, itemBoxColor);
        guiGraphics.renderOutline(itemBoxX, itemBoxY, itemDisplaySize, itemDisplaySize, borderColorTop);

        poseStack.pushPose();
        poseStack.translate(itemBoxX + itemDisplaySize / 2f, itemBoxY + itemDisplaySize / 2f, 0);
        poseStack.scale(2.0f, 2.0f, 1.0f);
        ItemStack displayStack = DeltaForceItems.isRawMaterial(itemStack)
            ? new ItemStack(DeltaForceItems.TEMP_DF.get())
            : itemStack;
        guiGraphics.renderFakeItem(displayStack, -8, -8);
        poseStack.popPose();

        if (!wrappedDescLines.isEmpty()) {
            int descStartY = itemBoxY + 2;
            int descStartX = itemBoxX + itemDisplaySize + 8;
            int textColor = (int)(alpha * 200) << 24 | 0xAAAAAA;

            int descY = descStartY;
            for (String line : wrappedDescLines) {
                guiGraphics.drawString(mc.font, line, descStartX, descY, textColor, false);
                descY += lineHeight;
            }
        }

        String pickupHint = "[右键拾取]";
        int hintColor = (int)(alpha * 220) << 24 | 0xFFD700;
        int hintY = boxY + boxHeight - bottomPadding + 2;
        guiGraphics.drawString(mc.font, pickupHint, boxX + padding, hintY, hintColor, false);

        RenderSystem.disableBlend();
        poseStack.popPose();
    }

    private static void drawFlowingRainbowString(GuiGraphics guiGraphics, Minecraft mc, String text,
                                                   int x, int y, int alpha, float phase) {
        int currentX = x;
        int glyphIndex = 0;
        for (int offset = 0; offset < text.length();) {
            int codePoint = text.codePointAt(offset);
            String glyph = new String(Character.toChars(codePoint));
            int color = DfsItemQuality.rainbowColor(phase, glyphIndex, alpha);
            guiGraphics.drawString(mc.font, glyph, currentX, y, color, false);
            currentX += mc.font.width(glyph);
            glyphIndex++;
            offset += Character.charCount(codePoint);
        }
    }

    /**
     * 每25个字符强制换行
     */
    private static List<String> wrapText(String text) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return lines;
        }
        for (int i = 0; i < text.length(); i += 25) {
            int end = Math.min(i + 25, text.length());
            lines.add(text.substring(i, end));
        }
        return lines;
    }

    private static final Set<ResourceLocation> warnedItems = new HashSet<>();

    private static String getDescription(GroundItemEntity entity) {
        ItemStack itemStack = entity.getItem();

        String fileDescription = ItemDescriptionLoader.getDescription(itemStack);
        if (fileDescription != null && !fileDescription.isEmpty()) {
            return fileDescription;
        }

        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(itemStack.getItem());
        if (itemId != null && warnedItems.add(itemId)) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                String locKey = itemStack.getItem().getDescriptionId();
                mc.player.displayClientMessage(
                    Component.literal("§e[DF] §f" + locKey + " §e(" + itemId + ") 未添加描述文件，建议添加，目前自动从物品定义设置，可能失败"),
                    false
                );
            }
        }

        return getFallbackDescription(itemStack);
    }

    /**
     * 从物品自身tooltip获取回退描述
     */
    private static String getFallbackDescription(ItemStack itemStack) {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null || mc.player == null) {
                return null;
            }

            List<Component> tooltip = itemStack.getTooltipLines(
                mc.player,
                TooltipFlag.Default.NORMAL
            );

            if (tooltip.size() <= 1) {
                return null;
            }

            StringBuilder sb = new StringBuilder();
            for (int i = 1; i < tooltip.size(); i++) {
                if (sb.length() > 0) {
                    sb.append("\n");
                }
                sb.append(safeGetString(tooltip.get(i)));
            }
            String result = sb.toString();
            return result.isEmpty() ? null : result;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 安全获取组件文本，处理未翻译的本地化键名
     */
    private static String safeGetString(Component component) {
        String result = component.getString();
        if (result.startsWith("item.") || result.startsWith("tooltip.") ||
            result.startsWith("block.") || result.startsWith("quality.") ||
            result.startsWith("faction.") || result.startsWith("coverage.") ||
            result.startsWith("noise_reduction.") || result.startsWith("value.")) {
            ComponentContents contents = component.getContents();
            if (contents instanceof net.minecraft.network.chat.contents.TranslatableContents translatable) {
                String key = translatable.getKey();
                if (result.equals(key)) {
                    String translated = net.minecraft.client.resources.language.I18n.get(key);
                    if (!translated.equals(key)) {
                        return translated;
                    }
                }
            }
        }
        return result;
    }
}
