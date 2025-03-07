/*
 * Copyright © Wynntils 2023.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
import com.wynntils.core.text.PartStyle;
import com.wynntils.core.text.StyledText;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TestStyledText {
    @BeforeAll
    public static void setup() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    public void advancedComponentTree_shouldProduceCorrectString() {
        final Component component = Component.empty()
                .append(Component.literal("italicred")
                        .withStyle(ChatFormatting.ITALIC)
                        .withStyle(ChatFormatting.RED)
                        .append(Component.literal("blue")
                                .withStyle(ChatFormatting.BLUE)
                                .withStyle(style -> style.withHoverEvent(
                                        new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("hover")))))
                        .append(Component.literal("nonitalic").withStyle(Style.EMPTY.withItalic(false)))
                        .append(Component.literal("inherited")
                                .append(Component.literal("bold").withStyle(ChatFormatting.BOLD))))
                .append(Component.literal("after")
                        .withStyle(style ->
                                style.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/command"))));

        final String expectedIncludeEvents = "§c§oitalicred§9§o§<1>blue§cnonitalic§oinherited§lbold§r§[1]after";
        final String expectedDefault = "§c§oitalicred§9§oblue§cnonitalic§oinherited§lbold§rafter";
        final String expectedNoFormat = "italicredbluenonitalicinheritedboldafter";

        StyledText styledText = StyledText.fromComponent(component);

        Assertions.assertEquals(
                expectedIncludeEvents,
                styledText.getString(PartStyle.StyleType.INCLUDE_EVENTS),
                "StyledText.getString(INCLUDE_EVENTS) returned an unexpected value.");
        Assertions.assertEquals(
                expectedDefault,
                styledText.getString(PartStyle.StyleType.DEFAULT),
                "StyledText.getString(DEFAULT) returned an unexpected value.");
        Assertions.assertEquals(
                expectedNoFormat,
                styledText.getString(PartStyle.StyleType.NONE),
                "StyledText.getString(NONE) returned an unexpected value.");
    }

    @Test
    public void colorResetsFormatting_shouldProduceCorrectString() {
        // Any color code resets formatting, so the bold should be removed.

        final String testString = "§l§cboldthenred";

        final String expected = "§cboldthenred";

        StyledText styledText = StyledText.fromString(testString);

        Assertions.assertEquals(
                expected,
                styledText.getString(PartStyle.StyleType.DEFAULT),
                "StyledText.getString() returned an unexpected value.");
    }

    @Test
    public void colorCodedStyledComponent_shouldProduceCorrectString() {
        final Component firstTestComponent = Component.literal("§credstring").withStyle(ChatFormatting.BOLD);
        // Any color code resets formatting, so the bold should be removed.
        final String firstExpected = "§credstring";

        final Component secondTestComponent = Component.literal("§lboldstring").withStyle(ChatFormatting.RED);
        // Style is applied first, so the color does not reset the bold.
        final String secondExpected = "§c§lboldstring";

        final Component thirdTestComponent = Component.literal("boldparent")
                .withStyle(ChatFormatting.BOLD)
                .append(Component.literal("coloredchild").withStyle(ChatFormatting.RED));
        // Style is applied first, so the color does not reset the bold.
        final String thirdExpected = "§lboldparent§c§lcoloredchild";

        final Component fourthTestComponent = Component.literal("bold")
                .withStyle(ChatFormatting.BOLD)
                .append(Component.literal("red").withStyle(ChatFormatting.RED))
                .append(Component.literal("unformatted"));
        final String fourthExpected = "§lbold§c§lred§r§lunformatted";

        final Component fifthTestComponent =
                Component.literal("§c§lredthenbold").append(Component.literal("unformattedchild"));
        final String fifthExpected = "§c§lredthenbold§runformattedchild";

        Assertions.assertEquals(
                firstExpected,
                StyledText.fromComponent(firstTestComponent).getString(PartStyle.StyleType.DEFAULT),
                "StyledText.getString() returned an unexpected value.");

        Assertions.assertEquals(
                secondExpected,
                StyledText.fromComponent(secondTestComponent).getString(PartStyle.StyleType.DEFAULT),
                "StyledText.getString() returned an unexpected value.");

        Assertions.assertEquals(
                thirdExpected,
                StyledText.fromComponent(thirdTestComponent).getString(PartStyle.StyleType.DEFAULT),
                "StyledText.getString() returned an unexpected value.");

        Assertions.assertEquals(
                fourthExpected,
                StyledText.fromComponent(fourthTestComponent).getString(PartStyle.StyleType.DEFAULT),
                "StyledText.getString() returned an unexpected value.");

        Assertions.assertEquals(
                fifthExpected,
                StyledText.fromComponent(fifthTestComponent).getString(PartStyle.StyleType.DEFAULT),
                "StyledText.getString() returned an unexpected value.");
    }

    @Test
    public void normalizedStringCreation_shouldProduceCorrectString() {
        final String badText = "ÀÀHello,ÀÀÀWorld!֎";
        final String expected = "Hello, World!";

        StyledText styledText = StyledText.fromString(badText);

        Assertions.assertEquals(
                expected,
                styledText.getNormalized().getString(PartStyle.StyleType.NONE),
                "StyledText.getNormalized().getString() returned an unexpected value.");
    }

    @Test
    public void untrimmedText_shouldProduceTrimmedString() {
        final String badText = "   Hello, World!  ";
        final String expected = "Hello, World!";

        StyledText styledText = StyledText.fromString(badText);

        Assertions.assertEquals(
                expected,
                styledText.trim().getString(PartStyle.StyleType.NONE),
                "StyledText.getString() returned an unexpected value.");
    }

    @Test
    public void emptyText_shouldProduceEmptyString() {
        StyledText styledText = StyledText.fromString("");

        Assertions.assertTrue(styledText.isEmpty(), "StyledText.isEmpty() returned an unexpected value.");
    }

    @Test
    public void blankText_shouldProduceBlankString() {
        StyledText styledText = StyledText.fromString("    ");

        Assertions.assertTrue(styledText.isBlank(), "StyledText.isBlank() returned an unexpected value.");
    }

    @Test
    public void contains_shouldProduceCorrectResult() {
        final String testString = "§c§oitalicred§9§oblue§cnonitalic§oinherited§lbold§rafter";
        final String[] expectedParts = {
            "§c§oitalicred",
            "§9§oblue",
            "§cnonitalic",
            "§oinherited",
            "§lbold",
            "§rafter",
            "licred§9§oblue§cno",
            "erited§lbo"
        };

        StyledText styledText = StyledText.fromString(testString);

        for (String expectedPart : expectedParts) {
            Assertions.assertTrue(
                    styledText.contains(expectedPart),
                    "StyledText.contains() could not find a part matching: " + expectedPart);
        }

        for (String expectedPart : expectedParts) {
            Assertions.assertTrue(
                    styledText.contains(StyledText.fromString(expectedPart)),
                    "StyledText.contains() could not find a part matching: " + expectedPart);
            Assertions.assertTrue(
                    styledText.contains(StyledText.fromString(expectedPart), PartStyle.StyleType.NONE),
                    "StyledText.contains(NONE) could not find a part matching: " + expectedPart);
        }
    }

    @Test
    public void startsWith_shouldProduceCorrectResult() {
        final String testString = "§c§oitalicred§9§oblue§cnonitalic§oinherited§lbold§rafter";
        final String expectedStart = "§c§oitalicred";
        final String expectedMultipartStart = "§c§oitalicred§9§oblue§cnonit";

        StyledText styledText = StyledText.fromString(testString);

        Assertions.assertTrue(
                styledText.startsWith(expectedStart),
                "StyledText.startsWith() did not produce a correct result for: " + expectedStart);
        Assertions.assertTrue(
                styledText.startsWith(StyledText.fromString(expectedStart)),
                "StyledText.startsWith() did not produce a correct result for: " + expectedStart);

        Assertions.assertTrue(
                styledText.startsWith(expectedMultipartStart),
                "StyledText.startsWith() did not produce a correct result for: " + expectedMultipartStart);
        Assertions.assertTrue(
                styledText.startsWith(StyledText.fromString(expectedMultipartStart)),
                "StyledText.startsWith() did not produce a correct result for: " + expectedMultipartStart);
    }

    @Test
    public void endsWith_shouldProduceCorrectResult() {
        final String testString = "§c§oitalicred§9§oblue§cnonitalic§oinherited§lbold§rafter";
        final String expectedEnd = "§rafter";
        final String expectedMultipartEnd = "erited§lbold§rafter";

        StyledText styledText = StyledText.fromString(testString);

        Assertions.assertTrue(
                styledText.endsWith(expectedEnd),
                "StyledText.startsWith() did not produce a correct result for: " + expectedEnd);
        Assertions.assertTrue(
                styledText.endsWith(StyledText.fromString(expectedEnd)),
                "StyledText.startsWith() did not produce a correct result for: " + expectedEnd);

        Assertions.assertTrue(
                styledText.endsWith(expectedMultipartEnd),
                "StyledText.startsWith() did not produce a correct result for: " + expectedMultipartEnd);
        Assertions.assertTrue(
                styledText.endsWith(StyledText.fromString(expectedMultipartEnd)),
                "StyledText.startsWith() did not produce a correct result for: " + expectedMultipartEnd);
    }

    @Test
    public void join_shouldProduceCorrectResult() {
        final StyledText firstStyled = StyledText.fromString("Hello");
        final StyledText secondStyled = StyledText.fromString("World");

        final String expected = "Hello, World";

        StyledText joinedStyled = StyledText.join(", ", List.of(firstStyled, secondStyled));

        Assertions.assertEquals(
                expected,
                joinedStyled.getString(PartStyle.StyleType.NONE),
                "StyledText.join() did not produce a correct result.");
    }

    @Test
    public void concat_shouldProduceCorrectResult() {
        final StyledText firstStyled = StyledText.fromString("Hello, ");
        final StyledText secondStyled = StyledText.fromString("World");

        final String expected = "Hello, World";

        StyledText joinedStyled = StyledText.concat(List.of(firstStyled, secondStyled));

        Assertions.assertEquals(
                expected,
                joinedStyled.getString(PartStyle.StyleType.NONE),
                "StyledText.concat() did not produce a correct result.");
    }

    @Test
    public void append_shouldProduceCorrectResult() {
        final StyledText firstStyled = StyledText.fromString("Hello, ");
        final StyledText secondStyled = StyledText.fromString("World");

        final String expected = "Hello, World";

        StyledText joinedStyled = firstStyled.append(secondStyled);

        Assertions.assertEquals(
                expected,
                joinedStyled.getString(PartStyle.StyleType.NONE),
                "StyledText.append() did not produce a correct result.");
    }

    @Test
    public void prepend_shouldProduceCorrectResult() {
        final StyledText firstStyled = StyledText.fromString("Hello, ");
        final StyledText secondStyled = StyledText.fromString("World");

        final String expected = "Hello, World";

        StyledText joinedStyled = secondStyled.prepend(firstStyled);

        Assertions.assertEquals(
                expected,
                joinedStyled.getString(PartStyle.StyleType.NONE),
                "StyledText.prepend() did not produce a correct result.");
    }

    @Test
    public void styledText_shouldProduceCorrectComponent() {
        final Component component = Component.literal("a").append(Component.literal("b"));

        StyledText styledText = StyledText.fromComponent(component);

        Assertions.assertEquals(
                component, styledText.getComponent(), "StyledText.getComponent() returned an unexpected value.");
    }

    @Test
    public void styledText_shouldProduceCorrectMatcher() {
        final Component component = Component.literal("This is a test string, where there are ")
                .append(Component.literal("multiple").withStyle(ChatFormatting.BOLD))
                .append(Component.literal(" components."));

        final Pattern unformattedPattern = Pattern.compile("This is a test string, where there are (.+) components\\.");
        final Pattern formattedPattern =
                Pattern.compile("This is a test string, where there are §l(.+)§r components\\.");

        final String expectedMatch = "multiple";

        StyledText styledText = StyledText.fromComponent(component);

        Matcher formattedMatcher = styledText.getMatcher(formattedPattern);
        Matcher unformattedMatcher = styledText.getMatcher(unformattedPattern, PartStyle.StyleType.NONE);

        Assertions.assertTrue(formattedMatcher.matches(), "StyledText.matches(DEFAULT) returned an unexpected value.");
        Assertions.assertEquals(
                expectedMatch,
                formattedMatcher.group(1),
                "StyledText.matches(DEFAULT).group() returned an unexpected value.");
        Assertions.assertTrue(unformattedMatcher.matches(), "StyledText.matches(NONE) returned an unexpected value.");
        Assertions.assertEquals(
                expectedMatch,
                unformattedMatcher.group(1),
                "StyledText.matches(NONE).group() returned an unexpected value.");
    }

    @Test
    public void styledText_shouldMatchCorrectly() {
        final Component component = Component.literal("This is a test string, where there are ")
                .append(Component.literal("multiple").withStyle(ChatFormatting.BOLD))
                .append(Component.literal(" components."));

        final Pattern unformattedPattern = Pattern.compile("This is a test string, where there are (.+) components\\.");
        final Pattern formattedPattern =
                Pattern.compile("This is a test string, where there are §l(.+)§r components\\.");

        final String expectedMatch = "multiple";

        StyledText styledText = StyledText.fromComponent(component);

        Assertions.assertTrue(
                styledText.matches(formattedPattern), "StyledText.matches(DEFAULT) returned an unexpected value.");
        Assertions.assertTrue(
                styledText.matches(unformattedPattern, PartStyle.StyleType.NONE),
                "StyledText.matches(NONE) returned an unexpected value.");
    }

    @Test
    public void styledText_shouldSplitCorrectly() {
        final Component component = Component.literal("This is a test string, where there are ")
                .append(Component.literal("multiple").withStyle(ChatFormatting.BOLD))
                .append(Component.literal(" components."));

        StyledText styledText = StyledText.fromComponent(component);

        // Split on the word "multiple" to check style inheritance
        StyledText[] splitTexts = styledText.split("\\.|,|(tip)");

        String[] results = {"This is a test string", " where there are §lmul", "§lle§r components"};

        for (int i = 0; i < splitTexts.length; i++) {
            StyledText result = splitTexts[i];

            Assertions.assertEquals(
                    results[i],
                    result.getString(PartStyle.StyleType.DEFAULT),
                    "StyledText.split() returned an unexpected value.");
        }
    }

    @Test
    public void styledText_shouldNotSplitColor() {
        final Component component =
                Component.literal("This is a formatted test string.").withStyle(ChatFormatting.BOLD);

        StyledText styledText = StyledText.fromComponent(component);

        StyledText[] splitTexts = styledText.split("§");

        final String result = "§lThis is a formatted test string.";

        Assertions.assertEquals(1, splitTexts.length, "StyledText.split() returned an unexpected value.");
        Assertions.assertEquals(
                result,
                splitTexts[0].getString(PartStyle.StyleType.DEFAULT),
                "StyledText.split() returned an unexpected value.");
    }

    @Test
    public void styledText_substringShouldSkipBeginningCorrectly() {
        final Component component = Component.literal("This is a test string").withStyle(ChatFormatting.BOLD);

        StyledText styledText = StyledText.fromComponent(component);

        StyledText substringText = styledText.substring(5);

        final String result = "§lis a test string";

        Assertions.assertEquals(
                result,
                substringText.getString(PartStyle.StyleType.DEFAULT),
                "StyledText.substring() returned an unexpected value.");
    }

    @Test
    public void styledText_multipartSubstringShouldWork() {
        final Component component = Component.literal("a")
                .withStyle(ChatFormatting.BOLD)
                .append(Component.literal("bb"))
                .append(Component.literal("ccc"))
                .append(Component.literal("dddd"));

        StyledText styledText = StyledText.fromComponent(component);

        StyledText substringText = styledText.substring(1, 8);

        final String result = "§lbbcccdd";

        Assertions.assertEquals(
                result,
                substringText.getString(PartStyle.StyleType.DEFAULT),
                "StyledText.substring() returned an unexpected value.");
    }

    @Test
    public void styledText_replaceShouldOnlyReplaceFirstOccurence() {
        final Component component = Component.literal("a")
                .withStyle(ChatFormatting.BOLD)
                .append(Component.literal("bb"))
                .append(Component.literal("ccc"))
                .append(Component.literal("dddd"));

        StyledText styledText = StyledText.fromComponent(component);

        StyledText replacedText = styledText.replaceFirst("b", "x");

        final String result = "§laxbcccdddd";

        Assertions.assertEquals(
                result,
                replacedText.getString(PartStyle.StyleType.DEFAULT),
                "StyledText.replace() returned an unexpected value.");
    }

    @Test
    public void styledText_replaceAllShouldReplaceAllOccurences() {
        final Component component = Component.literal("a")
                .withStyle(ChatFormatting.BOLD)
                .append(Component.literal("bb"))
                .append(Component.literal("ccc"))
                .append(Component.literal("dddd"));

        StyledText styledText = StyledText.fromComponent(component);

        StyledText replacedText = styledText.replaceAll("[b|c]", "x");

        final String result = "§laxxxxxdddd";

        Assertions.assertEquals(
                result,
                replacedText.getString(PartStyle.StyleType.DEFAULT),
                "StyledText.replaceAll() returned an unexpected value.");
    }

    @Test
    public void styledText_withoutFormattingShouldReplaceStripFormatting() {
        final Component component = Component.literal("a")
                .withStyle(ChatFormatting.BOLD)
                .append(Component.literal("bb"))
                .append(Component.literal("ccc").withStyle(ChatFormatting.RED).withStyle(ChatFormatting.ITALIC))
                .append(Component.literal("dddd").withStyle(ChatFormatting.UNDERLINE));

        StyledText styledText = StyledText.fromComponent(component).withoutFormatting();

        final String result = "abbcccdddd";

        Assertions.assertEquals(
                result,
                styledText.getString(PartStyle.StyleType.DEFAULT),
                "StyledText.replaceAll() returned an unexpected value.");

        Assertions.assertEquals(
                result,
                styledText.getString(PartStyle.StyleType.NONE),
                "StyledText.replaceAll() returned an unexpected value.");
    }
}
