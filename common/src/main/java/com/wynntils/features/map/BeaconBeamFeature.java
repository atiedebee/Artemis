/*
 * Copyright © Wynntils 2022-2023.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.map;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.wynntils.core.components.Models;
import com.wynntils.core.consumers.features.Feature;
import com.wynntils.core.persisted.Persisted;
import com.wynntils.core.persisted.config.Category;
import com.wynntils.core.persisted.config.Config;
import com.wynntils.core.persisted.config.ConfigCategory;
import com.wynntils.mc.event.RenderTileLevelLastEvent;
import com.wynntils.mc.event.TickEvent;
import com.wynntils.models.marker.type.MarkerInfo;
import com.wynntils.utils.MathUtils;
import com.wynntils.utils.colors.CommonColors;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.mc.type.Location;
import com.wynntils.utils.render.CustomBeaconRenderer;
import java.util.List;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.core.Position;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@ConfigCategory(Category.MAP)
public class BeaconBeamFeature extends Feature {
    @Persisted
    public final Config<CustomColor> waypointBeamColor = new Config<>(CommonColors.RED);

    private static final int RAINBOW_CHANGE_RATE = 10;
    private CustomColor currentRainbowColor = CommonColors.RED;

    @SubscribeEvent
    public void onTick(TickEvent event) {
        int r = currentRainbowColor.r;
        int g = currentRainbowColor.g;
        int b = currentRainbowColor.b;

        if (r > 0 && b == 0) {
            r -= RAINBOW_CHANGE_RATE;
            g += RAINBOW_CHANGE_RATE;
        }
        if (g > 0 && r == 0) {
            g -= RAINBOW_CHANGE_RATE;
            b += RAINBOW_CHANGE_RATE;
        }
        if (b > 0 && g == 0) {
            r += RAINBOW_CHANGE_RATE;
            b -= RAINBOW_CHANGE_RATE;
        }

        r = MathUtils.clamp(r, 0, 255);
        g = MathUtils.clamp(g, 0, 255);
        b = MathUtils.clamp(b, 0, 255);
        currentRainbowColor = new CustomColor(r, g, b, 255);
    }

    @SubscribeEvent
    public void onRenderLevelLast(RenderTileLevelLastEvent event) {
        List<MarkerInfo> markers = Models.Marker.getAllMarkers().toList();
        if (markers.isEmpty()) return;

        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource bufferSource =
                MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());

        for (MarkerInfo marker : markers) {
            Position camera = event.getCamera().getPosition();
            Location location = marker.location();

            double dx = location.x - camera.x();
            double dy = location.y - camera.y();
            double dz = location.z - camera.z();

            double distance = MathUtils.magnitude(dx, dz);
            int maxDistance = McUtils.options().renderDistance().get() * 16;

            if (distance > maxDistance) {
                double scale = maxDistance / distance;

                dx *= scale;
                dz *= scale;
            }

            float alpha = 1f;

            if (distance <= 7) {
                alpha = MathUtils.clamp(MathUtils.map((float) distance, 2f, 7f, 0f, 1f), 0f, 1f);
            }

            poseStack.pushPose();
            poseStack.translate(dx, dy, dz);

            CustomColor color =
                    marker.beaconColor() == CustomColor.NONE ? waypointBeamColor.get() : marker.beaconColor();

            float[] colorArray;
            if (color == CommonColors.RAINBOW) {
                colorArray = currentRainbowColor.asFloatArray();
            } else {
                colorArray = color.asFloatArray();
            }

            CustomBeaconRenderer.renderBeaconBeam(
                    poseStack,
                    bufferSource,
                    BeaconRenderer.BEAM_LOCATION,
                    event.getPartialTick(),
                    1f,
                    McUtils.player().level.getGameTime(),
                    0,
                    1024,
                    colorArray,
                    alpha,
                    0.166f,
                    0.33f);

            poseStack.popPose();
        }

        bufferSource.endLastBatch();
    }
}
