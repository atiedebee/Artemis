/*
 * Copyright © Wynntils 2022-2023.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.characterstats.actionbar;

import com.wynntils.handlers.actionbar.ActionBarSegment;
import com.wynntils.handlers.actionbar.type.ActionBarPosition;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CoordinatesSegment implements ActionBarSegment {
    private static final Pattern COORDINATES_PATTERN = Pattern.compile("(§0 *)§7 ?(-?\\d+)§f ?(.+)§7 ?(-?\\d+)( *)");

    private final Runnable onSegmentCleared;
    private boolean hidden;

    public CoordinatesSegment(Runnable onSegmentCleared) {
        this.onSegmentCleared = onSegmentCleared;
    }

    @Override
    public Pattern getPattern() {
        return COORDINATES_PATTERN;
    }

    @Override
    public void update(Matcher matcher) {
        /* Currently we don't care about the actual matches.
        String leftPad = matcher.group(1);
        String xCoord = matcher.group(2);
        String orient = matcher.group(3);
        String yCoord = matcher.group(4);
        String rightPad = matcher.group(5);
         */
    }

    @Override
    public void appeared(Matcher matcher) {
        // Currently we don't care about the actual matches,
        // but we need to signal that other special center segment has been cleared,
        // since coordinate segment is the default/fallback segment
        onSegmentCleared.run();
    }

    @Override
    public ActionBarPosition getPosition() {
        return ActionBarPosition.CENTER;
    }

    @Override
    public boolean isHidden() {
        return hidden;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }
}
