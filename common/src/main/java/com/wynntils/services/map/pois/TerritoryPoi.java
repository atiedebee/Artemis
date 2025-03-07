/*
 * Copyright © Wynntils 2022-2023.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.services.map.pois;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.core.components.Models;
import com.wynntils.core.text.StyledText;
import com.wynntils.models.territories.TerritoryInfo;
import com.wynntils.models.territories.profile.TerritoryProfile;
import com.wynntils.screens.maps.GuildMapScreen;
import com.wynntils.services.map.type.DisplayPriority;
import com.wynntils.utils.colors.CommonColors;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.mc.type.PoiLocation;
import com.wynntils.utils.render.Texture;
import com.wynntils.utils.render.buffered.BufferedFontRenderer;
import com.wynntils.utils.render.buffered.BufferedRenderUtils;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.TextShadow;
import com.wynntils.utils.render.type.VerticalAlignment;
import java.util.function.Supplier;
import net.minecraft.client.renderer.MultiBufferSource;

public class TerritoryPoi implements Poi {
    private final Supplier<TerritoryProfile> territoryProfileSupplier;
    private final PoiLocation territoryCenter;
    private final int width;
    private final int height;

    private final TerritoryInfo territoryInfo;
    private final boolean fakeTerritoryInfo;

    private TerritoryProfile territoryProfileCache;

    public TerritoryPoi(TerritoryProfile territoryProfile) {
        this(() -> territoryProfile, null);
    }

    public TerritoryPoi(Supplier<TerritoryProfile> territoryProfileSupplier, TerritoryInfo territoryInfo) {
        this(territoryProfileSupplier, territoryInfo, false);
    }

    // Note: This constructor is used to create a TerritoryPoi based on both the API and advancement data
    public TerritoryPoi(TerritoryProfile territoryProfile, TerritoryInfo territoryInfo) {
        this(() -> territoryProfile, territoryInfo, true);
    }

    private TerritoryPoi(
            Supplier<TerritoryProfile> territoryProfileSupplier,
            TerritoryInfo territoryInfo,
            boolean fakeTerritoryInfo) {
        this.territoryProfileSupplier = territoryProfileSupplier;

        TerritoryProfile territoryProfile = getTerritoryProfile();
        this.width = territoryProfile.getEndX() - territoryProfile.getStartX();
        this.height = territoryProfile.getEndZ() - territoryProfile.getStartZ();
        this.territoryCenter = new PoiLocation(
                territoryProfile.getStartX() + width / 2, null, territoryProfile.getStartZ() + height / 2);

        this.territoryInfo = territoryInfo;
        this.fakeTerritoryInfo = fakeTerritoryInfo;
    }

    @Override
    public void renderAt(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            float renderX,
            float renderY,
            boolean hovered,
            float scale,
            float mapZoom) {
        poseStack.pushPose();
        poseStack.translate(0, 0, 100);

        final float renderWidth = width * mapZoom;
        final float renderHeight = height * mapZoom;
        final float actualRenderX = renderX - renderWidth / 2f;
        final float actualRenderZ = renderY - renderHeight / 2f;

        TerritoryProfile territoryProfile = getTerritoryProfile();

        CustomColor color;
        if (isTerritoryInfoUsable()
                && McUtils.mc().screen instanceof GuildMapScreen guildMapScreen
                && guildMapScreen.isResourceMode()) {
            color = territoryInfo.getResourceColor();
        } else if (!isTerritoryInfoUsable() || territoryInfo.getGuildName().equals(territoryProfile.getGuild())) {
            // We know the guild name with it's color
            color = territoryProfile.getGuildColor();
        } else {
            // We don't know the holding guild's color
            // FIXME: Will be fixed when Athena API is added
            color = CommonColors.WHITE;
        }

        BufferedRenderUtils.drawRect(
                poseStack,
                bufferSource,
                color.withAlpha(65),
                actualRenderX,
                actualRenderZ,
                0,
                renderWidth,
                renderHeight);
        BufferedRenderUtils.drawRectBorders(
                poseStack,
                bufferSource,
                color,
                actualRenderX,
                actualRenderZ,
                actualRenderX + renderWidth,
                actualRenderZ + renderHeight,
                0,
                1.5f);

        if (isTerritoryInfoUsable() && territoryInfo.isHeadquarters()) {
            BufferedRenderUtils.drawTexturedRect(
                    poseStack,
                    bufferSource,
                    Texture.GUILD_HEADQUARTERS_ICON,
                    actualRenderX + renderWidth / 2f - Texture.GUILD_HEADQUARTERS_ICON.width() / 2f,
                    actualRenderZ + renderHeight / 2f - Texture.GUILD_HEADQUARTERS_ICON.height() / 2f);
        } else {
            String guildPrefix =
                    isTerritoryInfoUsable() ? territoryInfo.getGuildPrefix() : territoryProfile.getGuildPrefix();
            BufferedFontRenderer.getInstance()
                    .renderAlignedTextInBox(
                            poseStack,
                            bufferSource,
                            StyledText.fromString(guildPrefix),
                            actualRenderX,
                            actualRenderX + renderWidth,
                            actualRenderZ,
                            actualRenderZ + renderHeight,
                            0,
                            color,
                            HorizontalAlignment.CENTER,
                            VerticalAlignment.MIDDLE,
                            TextShadow.OUTLINE);
        }

        String guildName = isTerritoryInfoUsable() ? territoryInfo.getGuildName() : territoryProfile.getFriendlyName();
        Models.GuildAttackTimer.getAttackTimerForTerritory(guildName).ifPresent(attackTimer -> {
            final String timeLeft = attackTimer.timerString();

            BufferedFontRenderer.getInstance()
                    .renderAlignedTextInBox(
                            poseStack,
                            bufferSource,
                            StyledText.fromString(timeLeft),
                            actualRenderX,
                            actualRenderX + renderWidth,
                            actualRenderZ,
                            actualRenderZ + renderHeight,
                            0,
                            CommonColors.WHITE,
                            HorizontalAlignment.CENTER,
                            VerticalAlignment.BOTTOM,
                            TextShadow.OUTLINE);
        });

        if (hovered) {
            BufferedFontRenderer.getInstance()
                    .renderAlignedTextInBox(
                            poseStack,
                            bufferSource,
                            StyledText.fromString(guildName),
                            actualRenderX,
                            actualRenderX + renderWidth,
                            actualRenderZ,
                            actualRenderZ + renderHeight,
                            0,
                            CommonColors.WHITE,
                            HorizontalAlignment.CENTER,
                            VerticalAlignment.TOP,
                            TextShadow.OUTLINE);
        }

        poseStack.popPose();
    }

    private boolean isTerritoryInfoUsable() {
        return !fakeTerritoryInfo && territoryInfo != null;
    }

    @Override
    public PoiLocation getLocation() {
        return territoryCenter;
    }

    @Override
    public boolean hasStaticLocation() {
        return true;
    }

    @Override
    public int getWidth(float mapZoom, float scale) {
        return (int) (width * mapZoom);
    }

    @Override
    public int getHeight(float mapZoom, float scale) {
        return (int) (height * mapZoom);
    }

    @Override
    public String getName() {
        return getTerritoryProfile().getName();
    }

    @Override
    public DisplayPriority getDisplayPriority() {
        return DisplayPriority.HIGHEST;
    }

    public TerritoryInfo getTerritoryInfo() {
        return territoryInfo;
    }

    public boolean isFakeTerritoryInfo() {
        return fakeTerritoryInfo;
    }

    public TerritoryProfile getTerritoryProfile() {
        return tryGetUpdatedTerritoryProfile();
    }

    private TerritoryProfile tryGetUpdatedTerritoryProfile() {
        TerritoryProfile territoryProfile = territoryProfileSupplier.get();
        return territoryProfile != null ? territoryProfileCache = territoryProfile : territoryProfileCache;
    }
}
