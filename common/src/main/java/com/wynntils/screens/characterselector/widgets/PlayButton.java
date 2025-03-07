/*
 * Copyright © Wynntils 2022-2023.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.screens.characterselector.widgets;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.core.components.Models;
import com.wynntils.screens.base.widgets.WynntilsButton;
import com.wynntils.screens.characterselector.CharacterSelectorScreen;
import com.wynntils.utils.mc.TooltipUtils;
import com.wynntils.utils.render.FontRenderer;
import com.wynntils.utils.render.RenderUtils;
import com.wynntils.utils.render.Texture;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;

public class PlayButton extends WynntilsButton {
    private static final List<Component> TOOLTIP = List.of(
            Component.translatable("screens.wynntils.characterSelection.playButton.play")
                    .withStyle(ChatFormatting.GREEN),
            Component.translatable("screens.wynntils.characterSelection.playButton.description")
                    .withStyle(ChatFormatting.GRAY));
    private final CharacterSelectorScreen characterSelectorScreen;

    public PlayButton(int x, int y, int width, int height, CharacterSelectorScreen characterSelectorScreen) {
        super(x, y, width, height, Component.literal("Play Button"));
        this.characterSelectorScreen = characterSelectorScreen;
    }

    @Override
    public void onPress() {
        if (characterSelectorScreen.getSelected() != null) {
            int slot = characterSelectorScreen.getSelected().getClassInfo().slot();
            Models.CharacterSelection.playWithCharacter(slot);
        }
    }

    @Override
    public void renderWidget(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        RenderUtils.drawTexturedRect(
                poseStack,
                Texture.PLAY_BUTTON.resource(),
                this.getX(),
                this.getY(),
                0,
                this.width,
                this.height,
                0,
                characterSelectorScreen.getSelected() == null ? Texture.PLAY_BUTTON.height() / 2 : 0,
                Texture.PLAY_BUTTON.width(),
                Texture.PLAY_BUTTON.height() / 2,
                Texture.PLAY_BUTTON.width(),
                Texture.PLAY_BUTTON.height());

        if (isHovered) {
            poseStack.pushPose();
            List<ClientTooltipComponent> clientTooltipComponents =
                    TooltipUtils.componentToClientTooltipComponent(TOOLTIP);
            poseStack.translate(
                    mouseX
                            - TooltipUtils.getToolTipWidth(
                                    clientTooltipComponents,
                                    FontRenderer.getInstance().getFont()),
                    mouseY - TooltipUtils.getToolTipHeight(clientTooltipComponents),
                    100);
            RenderUtils.drawTooltip(
                    poseStack, TOOLTIP, FontRenderer.getInstance().getFont(), true);
            poseStack.popPose();
        }
    }
}
