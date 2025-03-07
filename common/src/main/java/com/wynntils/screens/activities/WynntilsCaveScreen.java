/*
 * Copyright © Wynntils 2023.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.screens.activities;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Models;
import com.wynntils.core.components.Services;
import com.wynntils.core.text.StyledText;
import com.wynntils.mc.event.MenuEvent;
import com.wynntils.models.activities.caves.CaveInfo;
import com.wynntils.models.activities.event.ActivityTrackerUpdatedEvent;
import com.wynntils.models.activities.event.ActivityUpdatedEvent;
import com.wynntils.models.activities.type.ActivitySortOrder;
import com.wynntils.models.activities.type.ActivityType;
import com.wynntils.screens.activities.widgets.CaveButton;
import com.wynntils.screens.activities.widgets.CaveProgressButton;
import com.wynntils.screens.base.WynntilsListScreen;
import com.wynntils.screens.base.widgets.BackButton;
import com.wynntils.screens.base.widgets.PageSelectorButton;
import com.wynntils.screens.base.widgets.ReloadButton;
import com.wynntils.screens.base.widgets.SortOrderWidget;
import com.wynntils.screens.base.widgets.SortableActivityScreen;
import com.wynntils.screens.wynntilsmenu.WynntilsMenuScreen;
import com.wynntils.services.map.MapTexture;
import com.wynntils.utils.StringUtils;
import com.wynntils.utils.colors.CommonColors;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.mc.type.Location;
import com.wynntils.utils.render.FontRenderer;
import com.wynntils.utils.render.MapRenderer;
import com.wynntils.utils.render.RenderUtils;
import com.wynntils.utils.render.Texture;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.TextShadow;
import com.wynntils.utils.render.type.VerticalAlignment;
import com.wynntils.utils.type.BoundingBox;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class WynntilsCaveScreen extends WynntilsListScreen<CaveInfo, CaveButton>
        implements SortableActivityScreen {
    private ActivitySortOrder activitySortOrder = ActivitySortOrder.LEVEL;
    private CaveInfo trackingRequested = null;

    private WynntilsCaveScreen() {
        super(Component.translatable("screens.wynntils.wynntilsCaveBook.name"));

        // Only register this once
        WynntilsMod.registerEventListener(this);
    }

    @SubscribeEvent
    public void onCaveUpdate(ActivityUpdatedEvent event) {
        if (event.getActivityType() == ActivityType.CAVE && McUtils.mc().screen == this) {
            this.reloadElements();
        }
    }

    @SubscribeEvent
    public void onTrackedActivityUpdate(ActivityTrackerUpdatedEvent event) {
        // Reload so we have the proper order
        if (McUtils.mc().screen == this) {
            this.reloadElements();
        }
    }

    // FIXME: We only need this hack to stop the screen from closing when tracking Caves.
    //        Adding a proper way to add quests with scripted container queries would mean this can get removed.
    @SubscribeEvent(priority = EventPriority.LOW)
    public void onMenuClose(MenuEvent.MenuClosedEvent event) {
        if (McUtils.mc().screen != this) return;

        event.setCanceled(true);
    }

    @Override
    public void onClose() {
        WynntilsMod.unregisterEventListener(this);

        super.onClose();
    }

    public static WynntilsCaveScreen create() {
        return new WynntilsCaveScreen();
    }

    @Override
    protected void doInit() {
        Models.Cave.reloadCaves();

        super.doInit();

        this.addRenderableWidget(new BackButton(
                (int) ((Texture.QUEST_BOOK_BACKGROUND.width() / 2f - 16) / 2f),
                65,
                Texture.BACK_ARROW.width() / 2,
                Texture.BACK_ARROW.height(),
                WynntilsMenuScreen.create()));

        this.addRenderableWidget(new ReloadButton(
                Texture.QUEST_BOOK_BACKGROUND.width() - 21,
                11,
                (int) (Texture.RELOAD_BUTTON.width() / 2 / 1.7f),
                (int) (Texture.RELOAD_BUTTON.height() / 1.7f),
                "cave",
                Models.Cave::reloadCaves));
        this.addRenderableWidget(new PageSelectorButton(
                Texture.QUEST_BOOK_BACKGROUND.width() / 2 + 50 - Texture.FORWARD_ARROW.width() / 2,
                Texture.QUEST_BOOK_BACKGROUND.height() - 25,
                Texture.FORWARD_ARROW.width() / 2,
                Texture.FORWARD_ARROW.height(),
                false,
                this));
        this.addRenderableWidget(new PageSelectorButton(
                Texture.QUEST_BOOK_BACKGROUND.width() - 50,
                Texture.QUEST_BOOK_BACKGROUND.height() - 25,
                Texture.FORWARD_ARROW.width() / 2,
                Texture.FORWARD_ARROW.height(),
                true,
                this));

        this.addRenderableWidget(new SortOrderWidget(
                Texture.QUEST_BOOK_BACKGROUND.width() / 2 + 1,
                11,
                (int) (Texture.SORT_DISTANCE.width() / 1.7f),
                (int) (Texture.SORT_DISTANCE.height() / 2 / 1.7f),
                this));
        this.addRenderableWidget(new CaveProgressButton(
                (int) (Texture.QUEST_BOOK_BACKGROUND.width() / 4f), 10, Texture.CAVE.width(), Texture.CAVE.height()));

        reloadElements();
    }

    @Override
    public void doRender(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        renderBackgroundTexture(poseStack);

        // Make 0, 0 the top left corner of the rendered quest book background
        poseStack.pushPose();
        final float translationX = getTranslationX();
        final float translationY = getTranslationY();
        poseStack.translate(translationX, translationY, 1f);

        renderTitle(poseStack, I18n.get("screens.wynntils.wynntilsCaves.name"));

        renderVersion(poseStack);

        renderWidgets(poseStack, mouseX, mouseY, partialTick);

        if (elements.isEmpty()) {
            renderNoCavesHelper(poseStack);
        }

        renderDescription(poseStack, I18n.get("screens.wynntils.wynntilsCaves.screenDescription"), "");

        renderPageInfo(poseStack, currentPage + 1, maxPage + 1);

        poseStack.popPose();

        renderMap(poseStack);

        renderTooltip(poseStack, mouseX, mouseY);
    }

    private void renderNoCavesHelper(PoseStack poseStack) {
        FontRenderer.getInstance()
                .renderAlignedTextInBox(
                        poseStack,
                        StyledText.fromString(I18n.get("screens.wynntils.wynntilsCaves.tryReload")),
                        Texture.QUEST_BOOK_BACKGROUND.width() / 2f + 15f,
                        Texture.QUEST_BOOK_BACKGROUND.width() - 15f,
                        0,
                        Texture.QUEST_BOOK_BACKGROUND.height(),
                        Texture.QUEST_BOOK_BACKGROUND.width() / 2f - 30f,
                        CommonColors.BLACK,
                        HorizontalAlignment.CENTER,
                        VerticalAlignment.MIDDLE,
                        TextShadow.NONE);
    }

    private void renderMap(PoseStack poseStack) {
        CaveInfo trackedCaveInfo = Models.Activity.getTrackedCaveInfo();
        if (trackedCaveInfo == null) return;
        Optional<Location> nextLocation = trackedCaveInfo.getNextLocation();
        if (nextLocation.isEmpty()) return;

        final float renderX = getTranslationX() + 20;
        final float renderY = getTranslationY() + 100;

        final int mapWidth = Texture.QUEST_BOOK_BACKGROUND.width() / 2 - 30;
        final int mapHeight = 90;
        final float currentZoom = 1;

        final float mapCenterX = nextLocation.get().x;
        final float mapCenterZ = nextLocation.get().z;

        final float centerX = renderX + mapWidth / 2f;
        final float centerZ = renderY + mapHeight / 2f;

        RenderUtils.enableScissor((int) (renderX), (int) (renderY), mapWidth, mapHeight);

        // Background black void color
        RenderUtils.drawRect(poseStack, CommonColors.BLACK, renderX, renderX, 0, mapWidth, mapHeight);

        BoundingBox textureBoundingBox =
                BoundingBox.centered(mapCenterX, mapCenterZ, this.width / currentZoom, this.height / currentZoom);

        List<MapTexture> maps = Services.Map.getMapsForBoundingBox(textureBoundingBox);

        MultiBufferSource.BufferSource bufferSource = MultiBufferSource.immediate(new BufferBuilder(256));

        for (MapTexture map : maps) {
            float textureX = map.getTextureXPosition(mapCenterX);
            float textureZ = map.getTextureZPosition(mapCenterZ);

            MapRenderer.renderMapQuad(
                    map,
                    poseStack,
                    bufferSource,
                    centerX,
                    centerZ,
                    textureX,
                    textureZ,
                    mapWidth,
                    mapHeight,
                    1f / currentZoom);
        }

        bufferSource.endBatch();

        RenderUtils.disableScissor();
    }

    @Override
    protected CaveButton getButtonFromElement(int i) {
        int offset = i % getElementsPerPage();
        return new CaveButton(
                Texture.QUEST_BOOK_BACKGROUND.width() / 2 + 15,
                offset * 13 + 25,
                Texture.QUEST_BOOK_BACKGROUND.width() / 2 - 37,
                9,
                elements.get(i),
                this);
    }

    @Override
    protected void reloadElementsList(String searchTerm) {
        elements.addAll(Models.Cave.getSortedCaves(activitySortOrder).stream()
                .filter(info -> StringUtils.partialMatch(info.getName(), searchTerm))
                .toList());
    }

    @Override
    public ActivitySortOrder getActivitySortOrder() {
        return activitySortOrder;
    }

    @Override
    public void setActivitySortOrder(ActivitySortOrder newSortOrder) {
        if (newSortOrder == null) {
            throw new IllegalStateException("Tried to set null activity sort order");
        }

        this.activitySortOrder = newSortOrder;
        this.setCurrentPage(0);
    }

    public void setTrackingRequested(CaveInfo caveInfo) {
        this.trackingRequested = caveInfo;
    }

    public CaveInfo getTrackingRequested() {
        return trackingRequested;
    }
}
