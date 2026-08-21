package mccfk.DeltaForce.java.client.overlay;

import com.rzy.dealt_force_skills.item.DfsItemQuality;
import mccfk.DeltaForce.java.entity.GroundItemEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import java.util.ArrayList;
import java.util.List;

public final class GroundItemOverlay {
    public static final IGuiOverlay OVERLAY = (forgeGui, graphics, partialTick, width, height) -> {
        Minecraft minecraft = Minecraft.getInstance();
        if (!(minecraft.hitResult instanceof EntityHitResult hit)
                || !(hit.getEntity() instanceof GroundItemEntity groundItem)
                || groundItem.getItem().isEmpty()) {
            return;
        }
        ItemStack stack = groundItem.getItem();
        DfsItemQuality quality = DfsItemQuality.of(stack);
        Component name = stack.getHoverName();
        Component qualityLine = Component.translatable("tooltip.dealt_force_skills.quality",
                Component.translatable("quality.dealt_force_skills." + quality.key()));
        Component hint = Component.translatable("message.deltaforce.ground_item_pickup");
        int textLimit = Math.max(80, Math.min(260, width - 56));
        List<FormattedCharSequence> description = wrapDescription(minecraft.font,
                ItemDescriptionLoader.getDescription(stack), textLimit);
        int contentWidth = Math.max(minecraft.font.width(name), minecraft.font.width(qualityLine));
        contentWidth = Math.max(contentWidth, minecraft.font.width(hint));
        for (FormattedCharSequence line : description) {
            contentWidth = Math.max(contentWidth, minecraft.font.width(line));
        }
        int boxWidth = Math.max(150, Math.min(textLimit + 16, contentWidth + 16));
        int lineHeight = minecraft.font.lineHeight + 2;
        int boxHeight = 16 + lineHeight * (3 + description.size());
        int x = Math.max(4, width - boxWidth - 20);
        int y = Math.max(36, Math.min(60, height - boxHeight - 24));
        graphics.fill(x, y, x + boxWidth, y + boxHeight, 0xD0101415);
        graphics.renderOutline(x, y, boxWidth, boxHeight, 0xFF667070);
        int textY = y + 7;
        graphics.drawString(minecraft.font, name, x + 8, textY, quality.color(), false);
        textY += lineHeight;
        graphics.drawString(minecraft.font, qualityLine, x + 8, textY, quality.color(), false);
        textY += lineHeight + 2;
        for (FormattedCharSequence line : description) {
            graphics.drawString(minecraft.font, line, x + 8, textY, 0xFFB8C2C8, false);
            textY += lineHeight;
        }
        graphics.drawString(minecraft.font, hint, x + 8, y + boxHeight - lineHeight - 3, 0xFFFFD75A, false);
    };

    private static List<FormattedCharSequence> wrapDescription(Font font, String description, int width) {
        List<FormattedCharSequence> lines = new ArrayList<>();
        if (description == null || description.isBlank()) {
            return lines;
        }
        for (String paragraph : description.split("\\n", -1)) {
            lines.addAll(font.split(FormattedText.of(paragraph), width));
        }
        return lines;
    }

    private GroundItemOverlay() {
    }
}
