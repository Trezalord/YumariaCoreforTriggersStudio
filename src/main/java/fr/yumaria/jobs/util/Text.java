package fr.yumaria.jobs.util;

import org.bukkit.ChatColor;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.text.DecimalFormat;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public final class Text {
    private static final DecimalFormat INTEGER_FORMAT = new DecimalFormat("#,##0");
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#,##0.##");
    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");

    private Text() {
    }

    public static String color(String input) {
        if (input == null) {
            return "";
        }
        return ChatColor.translateAlternateColorCodes('&', input);
    }

    public static String stripColor(String input) {
        return ChatColor.stripColor(color(input));
    }

    public static String placeholders(String input, Map<String, String> placeholders) {
        String result = input == null ? "" : input;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue() == null ? "" : entry.getValue());
        }
        return result;
    }

    public static String formatNumber(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.0000001D) {
            return INTEGER_FORMAT.format(value);
        }
        return DECIMAL_FORMAT.format(value);
    }

    public static String formatPercent(double progress, double required) {
        if (required <= 0.0D) {
            return "0";
        }
        double percent = Math.max(0.0D, Math.min(100.0D, (progress / required) * 100.0D));
        return DECIMAL_FORMAT.format(percent);
    }

    public static String formatCommandNumber(double value) {
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }

    public static String normalizeId(String input) {
        return input == null ? "" : input.toLowerCase(Locale.ROOT).trim();
    }

    public static String normalizeLookup(String input) {
        if (input == null) {
            return "";
        }
        String normalized = Normalizer.normalize(stripColor(input), Normalizer.Form.NFD);
        normalized = DIACRITICS.matcher(normalized).replaceAll("");
        return normalized.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "")
                .trim();
    }
}
