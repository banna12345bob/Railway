package com.railwayteam.railways.util;

import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.MaterialColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ColorUtils {
    public static MaterialColor materialColorFromDye(@Nullable DyeColor dyeColor, @NotNull MaterialColor defaultColor) {
        if (dyeColor == null)
            return defaultColor;
        return materialColorFromDye(dyeColor);
    }

    public static MaterialColor materialColorFromDye(@NotNull DyeColor dyeColor) {
        return switch (dyeColor) {
            case WHITE -> MaterialColor.TERRACOTTA_WHITE;
            case ORANGE -> MaterialColor.COLOR_ORANGE;
            case MAGENTA -> MaterialColor.COLOR_MAGENTA;
            case LIGHT_BLUE -> MaterialColor.COLOR_LIGHT_BLUE;
            case YELLOW -> MaterialColor.COLOR_YELLOW;
            case LIME -> MaterialColor.COLOR_LIGHT_GREEN;
            case PINK -> MaterialColor.COLOR_PINK;
            case GRAY -> MaterialColor.COLOR_GRAY;
            case LIGHT_GRAY -> MaterialColor.COLOR_LIGHT_GRAY;
            case CYAN -> MaterialColor.COLOR_CYAN;
            case PURPLE -> MaterialColor.COLOR_PURPLE;
            case BLUE -> MaterialColor.COLOR_BLUE;
            case BROWN -> MaterialColor.COLOR_BROWN;
            case GREEN -> MaterialColor.COLOR_GREEN;
            case RED -> MaterialColor.COLOR_RED;
            case BLACK -> MaterialColor.COLOR_BLACK;
        };
    }

    public static String coloredName(String string) {
        return switch (string) {
            case "white" -> "White";
            case "orange" -> "Orange";
            case "magenta" -> "Magenta";
            case "light_blue" -> "Light Blue";
            case "yellow" -> "Yellow";
            case "lime" -> "Lime";
            case "pink" -> "Pink";
            case "gray" -> "Gray";
            case "light_gray" -> "Light Gray";
            case "cyan" -> "Cyan";
            case "purple" -> "Purple";
            case "blue" -> "Blue";
            case "brown" -> "Brown";
            case "green" -> "Green";
            case "red" -> "Red";
            case "black" -> "Black";
            default -> "Unknown Color";
        };
    }

    public static Item getDyeColorDyeItem(DyeColor color) {
        return switch (color) {
            case WHITE -> Items.WHITE_DYE;
            case ORANGE -> Items.ORANGE_DYE;
            case MAGENTA -> Items.MAGENTA_DYE;
            case LIGHT_BLUE -> Items.LIGHT_BLUE_DYE;
            case YELLOW -> Items.YELLOW_DYE;
            case LIME -> Items.LIME_DYE;
            case PINK -> Items.PINK_DYE;
            case GRAY -> Items.GRAY_DYE;
            case LIGHT_GRAY -> Items.LIGHT_GRAY_DYE;
            case CYAN -> Items.CYAN_DYE;
            case PURPLE -> Items.PURPLE_DYE;
            case BLUE -> Items.BLUE_DYE;
            case BROWN -> Items.BROWN_DYE;
            case GREEN -> Items.GREEN_DYE;
            case RED -> Items.RED_DYE;
            case BLACK -> Items.BLACK_DYE;
        };
    }
}
