package dev.hugosmp.auctionhouse.config.screen;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

/**
 * Mini scanner for the legacy {@code &}/{@code §} formatting codes the
 * server's naming rules use. One implementation for the two questions the
 * editor asks about any string: what it costs against a limit, and how it
 * looks when drawn.
 *
 * Costs follow the market rules: a plain code ({@code &d}) is two chars of
 * limit, a hex color ({@code &x&f&f&3&d&a&f}) is fourteen. On signs codes
 * are free and only visible characters count.
 */
final class LegacyText {

    record Segment(Style style, String text) {}

    private LegacyText() {}

    static boolean isPlainCode(char sigil, char code) {
        if (sigil != '&' && sigil != '§') return false;
        char lower = Character.toLowerCase(code);
        return (lower >= '0' && lower <= '9') || (lower >= 'a' && lower <= 'f')
                || lower == 'k' || lower == 'l' || lower == 'm' || lower == 'n'
                || lower == 'o' || lower == 'r';
    }

    /** Length of a hex color run starting at i ("&x&f&f&3&d&a&f"), or -1. */
    static int hexRunLength(String text, int i) {
        if (i + 14 > text.length()) return -1;
        char sigil = text.charAt(i);
        if ((sigil != '&' && sigil != '§') || Character.toLowerCase(text.charAt(i + 1)) != 'x') {
            return -1;
        }
        for (int k = 2; k < 14; k += 2) {
            if (text.charAt(i + k) != sigil
                    && !((sigil == '§') && text.charAt(i + k) == '&')) return -1;
            if (!isHexDigit(text.charAt(i + k + 1))) return -1;
        }
        return 14;
    }

    private static boolean isHexDigit(char c) {
        char lower = Character.toLowerCase(c);
        return (lower >= '0' && lower <= '9') || (lower >= 'a' && lower <= 'f');
    }

    /** Cost of the string against a limit. when codesFree (sign names) only
     *  visible characters count; otherwise every code char counts as typed. */
    static int cost(String text, boolean codesFree) {
        if (text == null) return 0;
        int cost = 0;
        for (int i = 0; i < text.length();) {
            int hex = hexRunLength(text, i);
            if (hex > 0) {
                cost += codesFree ? 0 : hex;
                i += hex;
                continue;
            }
            if (i + 1 < text.length() && isPlainCode(text.charAt(i), text.charAt(i + 1))) {
                cost += codesFree ? 0 : 2;
                i += 2;
                continue;
            }
            cost += 1;
            i += 1;
        }
        return cost;
    }

    /** Visible characters only (codes stripped) — the sign counter. */
    static int visibleLength(String text) {
        return cost(text, true);
    }

    /** Parses into styled segments for preview rendering. */
    static List<Segment> segments(String text) {
        List<Segment> out = new ArrayList<>();
        if (text == null || text.isEmpty()) return out;
        Style style = Style.EMPTY;
        StringBuilder run = new StringBuilder();
        for (int i = 0; i < text.length();) {
            int hex = hexRunLength(text, i);
            if (hex > 0) {
                flush(out, style, run);
                StringBuilder rgb = new StringBuilder();
                for (int k = 3; k < 14; k += 2) rgb.append(text.charAt(i + k));
                int rgbInt = Integer.parseInt(rgb.toString(), 16);
                style = style.withColor(net.minecraft.network.chat.TextColor.fromRgb(rgbInt));
                i += hex;
                continue;
            }
            char c = text.charAt(i);
            if (i + 1 < text.length() && isPlainCode(c, text.charAt(i + 1))) {
                flush(out, style, run);
                char code = Character.toLowerCase(text.charAt(i + 1));
                if (code == 'r') {
                    style = Style.EMPTY;
                } else {
                    ChatFormatting fmt = byLegacyChar(code);
                    // colors are the first 16 enum constants in every version — ordinal
            // is the stable check, isColor() was renamed away in 26.3
            if (fmt != null) style = fmt.ordinal() < 16 ? style.withColor(fmt) : style.applyFormat(fmt);
                }
                i += 2;
                continue;
            }
            run.append(c);
            i += 1;
        }
        flush(out, style, run);
        return out;
    }

    /** Legacy char to ChatFormatting: 0-9/a-f are colors, k r are the
     *  formatters — mapping skips the unused letters (p, q). */
    static ChatFormatting byLegacyChar(char code) {
        ChatFormatting[] values = ChatFormatting.values();
        if (code >= '0' && code <= '9') return values[code - '0'];
        if (code >= 'a' && code <= 'f') return values[code - 'a' + 10];
        return switch (code) {
            case 'k' -> ChatFormatting.OBFUSCATED;
            case 'l' -> ChatFormatting.BOLD;
            case 'm' -> ChatFormatting.STRIKETHROUGH;
            case 'n' -> ChatFormatting.UNDERLINE;
            case 'o' -> ChatFormatting.ITALIC;
            case 'r' -> ChatFormatting.RESET;
            default -> null;
        };
    }

    private static void flush(List<Segment> out, Style style, StringBuilder run) {
        if (run.length() > 0) {
            out.add(new Segment(style, run.toString()));
            run.setLength(0);
        }
    }

    static FormattedCharSequence toSequence(List<Segment> segments) {
        return FormattedCharSequence.composite(
                segments.stream()
                        .map(s -> FormattedCharSequence.forward(s.text(), s.style()))
                        .toList());
    }
}
