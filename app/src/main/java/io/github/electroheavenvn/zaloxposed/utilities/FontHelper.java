package io.github.electroheavenvn.zaloxposed.utilities;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.Map;

public final class FontHelper 
{
    private FontHelper() {}

    public static final String STYLE_NONE = "none";
    public static final String STYLE_BOLD = "bold";
    public static final String STYLE_ITALIC = "italic";
    public static final String STYLE_BOLD_ITALIC = "bold_italic";
    public static final String STYLE_SCRIPT = "script";
    public static final String STYLE_GOTHIC = "gothic";
    public static final String STYLE_MONO = "mono";
    public static final String STYLE_BUBBLE = "bubble";
    public static final String STYLE_STRIKE = "strike";
    public static final String STYLE_UNDERLINE = "underline";
    public static final String STYLE_FLIP = "flip";
    public static final String STYLE_REGIONAL = "regional";
    public static final String STYLE_UPPER = "upper";
    public static final String STYLE_LOWER = "lower";

    // Color theme badges
    public static final String THEME_NONE = "none";
    public static final String THEME_RED = "red_block";
    public static final String THEME_BLUE = "blue_block";
    public static final String THEME_GREEN = "green_block";
    public static final String THEME_YELLOW = "yellow_block";
    public static final String THEME_PURPLE = "purple_block";
    public static final String THEME_ORANGE = "orange_block";
    public static final String THEME_PINK = "pink_block";
    public static final String THEME_RAINBOW = "rainbow";
    public static final String THEME_FIRE = "fire";
    public static final String THEME_STAR = "star";
    public static final String THEME_DIAMOND = "diamond";
    public static final String THEME_VIP = "vip";

    private static final Map<Character, String> FLIP_MAP = new HashMap<>();

    static 
    {
        String normal = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789?!.,;";
        String[] flipped = {
            "ɐ", "q", "ɔ", "p", "ǝ", "ɟ", "ƃ", "ɥ", "ı", "ɾ", "ʞ", "l", "ɯ", "u", "o", "d", "b", "ɹ", "s", "ʇ", "n", "ʌ", "ʍ", "x", "ʎ", "z",
            "∀", "𐐒", "Ɔ", "p", "Ǝ", "Ⅎ", "⅁", "H", "I", "ſ", "ʞ", "˥", "W", "N", "O", "Ԁ", "Q", "ɹ", "S", "┴", "∩", "Λ", "M", "X", "⅄", "Z",
            "0", "Ɩ", "ᄅ", "Ɛ", "ㄣ", "ϛ", "9", "ㄥ", "8", "6", "¿", "¡", "˙", "‘", "؛"
        };
        for (int i = 0; i < normal.length() && i < flipped.length; i++) 
        {
            FLIP_MAP.put(normal.charAt(i), flipped[i]);
        }
    }

    public static String processText(String text, String defaultStyle) 
    {
        if (text == null || text.isEmpty()) 
        {
            return text;
        }

        String trimmed = text.trim();
        String lower = trimmed.toLowerCase();

        // Check command prefixes
        if (lower.startsWith("/b ") || lower.startsWith("/bold ")) 
        {
            int idx = trimmed.indexOf(' ');
            return transform(trimmed.substring(idx + 1).trim(), STYLE_BOLD);
        }
        if (lower.startsWith("/i ") || lower.startsWith("/italic ")) 
        {
            int idx = trimmed.indexOf(' ');
            return transform(trimmed.substring(idx + 1).trim(), STYLE_ITALIC);
        }
        if (lower.startsWith("/bi ")) 
        {
            int idx = trimmed.indexOf(' ');
            return transform(trimmed.substring(idx + 1).trim(), STYLE_BOLD_ITALIC);
        }
        if (lower.startsWith("/c ") || lower.startsWith("/script ") || lower.startsWith("/tay ")) 
        {
            int idx = trimmed.indexOf(' ');
            return transform(trimmed.substring(idx + 1).trim(), STYLE_SCRIPT);
        }
        if (lower.startsWith("/g ") || lower.startsWith("/gothic ")) 
        {
            int idx = trimmed.indexOf(' ');
            return transform(trimmed.substring(idx + 1).trim(), STYLE_GOTHIC);
        }
        if (lower.startsWith("/m ") || lower.startsWith("/mono ")) 
        {
            int idx = trimmed.indexOf(' ');
            return transform(trimmed.substring(idx + 1).trim(), STYLE_MONO);
        }
        if (lower.startsWith("/o ") || lower.startsWith("/bubble ") || lower.startsWith("/tron ")) 
        {
            int idx = trimmed.indexOf(' ');
            return transform(trimmed.substring(idx + 1).trim(), STYLE_BUBBLE);
        }
        if (lower.startsWith("/s ") || lower.startsWith("/strike ") || lower.startsWith("/gach ")) 
        {
            int idx = trimmed.indexOf(' ');
            return transform(trimmed.substring(idx + 1).trim(), STYLE_STRIKE);
        }
        if (lower.startsWith("/u ") || lower.startsWith("/under ") || lower.startsWith("/chan ")) 
        {
            int idx = trimmed.indexOf(' ');
            return transform(trimmed.substring(idx + 1).trim(), STYLE_UNDERLINE);
        }
        if (lower.startsWith("/flip ") || lower.startsWith("/dao ")) 
        {
            int idx = trimmed.indexOf(' ');
            return transform(trimmed.substring(idx + 1).trim(), STYLE_FLIP);
        }
        if (lower.startsWith("/reg ") || lower.startsWith("/bluebox ")) 
        {
            int idx = trimmed.indexOf(' ');
            return transform(trimmed.substring(idx + 1).trim(), STYLE_REGIONAL);
        }

        // Color theme prefixes
        if (lower.startsWith("/red ")) return applyTheme(trimmed.substring(5).trim(), THEME_RED);
        if (lower.startsWith("/blue ")) return applyTheme(trimmed.substring(6).trim(), THEME_BLUE);
        if (lower.startsWith("/green ")) return applyTheme(trimmed.substring(7).trim(), THEME_GREEN);
        if (lower.startsWith("/yellow ")) return applyTheme(trimmed.substring(8).trim(), THEME_YELLOW);
        if (lower.startsWith("/purple ")) return applyTheme(trimmed.substring(8).trim(), THEME_PURPLE);
        if (lower.startsWith("/orange ")) return applyTheme(trimmed.substring(8).trim(), THEME_ORANGE);
        if (lower.startsWith("/pink ")) return applyTheme(trimmed.substring(6).trim(), THEME_PINK);
        if (lower.startsWith("/rainbow ")) return applyTheme(trimmed.substring(9).trim(), THEME_RAINBOW);
        if (lower.startsWith("/fire ")) return applyTheme(trimmed.substring(6).trim(), THEME_FIRE);
        if (lower.startsWith("/vip ")) return applyTheme(trimmed.substring(5).trim(), THEME_VIP);

        // If no prefix, check if a default font style is enabled in Settings
        if (defaultStyle != null && !defaultStyle.isEmpty() && !STYLE_NONE.equalsIgnoreCase(defaultStyle)) 
        {
            if (!text.startsWith("/")) 
            {
                return transform(text, defaultStyle);
            }
        }

        return text;
    }

    public static String applyTheme(String text, String theme) 
    {
        if (text == null || text.isEmpty()) return text;
        String cleaned = stripThemes(text);

        switch (theme) 
        {
            case THEME_RED: return "🟥 " + cleaned + " 🟥";
            case THEME_BLUE: return "🟦 " + cleaned + " 🟦";
            case THEME_GREEN: return "🟩 " + cleaned + " 🟩";
            case THEME_YELLOW: return "🟨 " + cleaned + " 🟨";
            case THEME_PURPLE: return "🟪 " + cleaned + " 🟪";
            case THEME_ORANGE: return "🟧 " + cleaned + " 🟧";
            case THEME_PINK: return "🌸 " + cleaned + " 🌸";
            case THEME_RAINBOW: return "🌈 " + cleaned + " 🌈";
            case THEME_FIRE: return "🔥 " + cleaned + " 🔥";
            case THEME_STAR: return "⭐ " + cleaned + " ⭐";
            case THEME_DIAMOND: return "💎 " + cleaned + " 💎";
            case THEME_VIP: return "🚨 [VIP] " + cleaned + " 🚨";
            default: return text;
        }
    }

    public static String stripThemes(String text) 
    {
        if (text == null) return "";
        return text.replaceAll("^[🟥🟦🟩🟨🟪🟧🌸🌈🔥⭐💎🚨\\[VIP\\]\\s]+", "")
                   .replaceAll("[🟥🟦🟩🟨🟪🟧🌸🌈🔥⭐💎🚨\\s]+$", "")
                   .trim();
    }

    public static String transform(String text, String style) 
    {
        if (text == null || text.isEmpty() || STYLE_NONE.equalsIgnoreCase(style)) 
        {
            return text;
        }

        if (STYLE_UPPER.equalsIgnoreCase(style)) 
        {
            return text.toUpperCase();
        }

        if (STYLE_LOWER.equalsIgnoreCase(style)) 
        {
            return text.toLowerCase();
        }

        if (STYLE_FLIP.equalsIgnoreCase(style)) 
        {
            StringBuilder sb = new StringBuilder();
            for (int i = text.length() - 1; i >= 0; i--) 
            {
                char ch = text.charAt(i);
                String flip = FLIP_MAP.get(ch);
                sb.append(flip != null ? flip : String.valueOf(ch));
            }
            return sb.toString();
        }

        if (STYLE_REGIONAL.equalsIgnoreCase(style)) 
        {
            StringBuilder sb = new StringBuilder();
            String upper = text.toUpperCase();
            for (int i = 0; i < upper.length(); i++) 
            {
                char ch = upper.charAt(i);
                if (ch >= 'A' && ch <= 'Z') 
                {
                    sb.append(codePointToString(0x1F1E6 + (ch - 'A'))).append(" ");
                }
                else 
                {
                    sb.append(ch);
                }
            }
            return sb.toString().trim();
        }

        StringBuilder sb = new StringBuilder();
        int length = text.length();

        for (int i = 0; i < length; i++) 
        {
            char ch = text.charAt(i);

            // Handle newline, whitespace, tabs
            if (ch == '\n' || ch == '\r' || ch == '\t') 
            {
                sb.append(ch);
                continue;
            }

            // Normalize character to NFD to separate base glyph and diacritical marks
            String s = String.valueOf(ch);
            String nfd = Normalizer.normalize(s, Normalizer.Form.NFD);

            if (nfd.isEmpty()) 
            {
                sb.append(ch);
                continue;
            }

            char base = nfd.charAt(0);
            String accents = nfd.length() > 1 ? nfd.substring(1) : "";

            String mapped = mapChar(base, style);
            sb.append(mapped);
            sb.append(accents);
        }

        return sb.toString();
    }

    private static String mapChar(char base, String style) 
    {
        if (base == 'đ') 
        {
            return mapChar('d', style);
        }
        if (base == 'Đ') 
        {
            return mapChar('D', style);
        }

        switch (style) 
        {
            case STYLE_BOLD:
                if (base >= 'A' && base <= 'Z') 
                {
                    return codePointToString(0x1D400 + (base - 'A'));
                }
                if (base >= 'a' && base <= 'z') 
                {
                    return codePointToString(0x1D41A + (base - 'a'));
                }
                if (base >= '0' && base <= '9') 
                {
                    return codePointToString(0x1D7CE + (base - '0'));
                }
                break;

            case STYLE_ITALIC:
                if (base >= 'A' && base <= 'Z') 
                {
                    return codePointToString(0x1D434 + (base - 'A'));
                }
                if (base >= 'a' && base <= 'z') 
                {
                    if (base == 'h') 
                    {
                        return "\u210E"; // Planck constant
                    }
                    return codePointToString(0x1D44E + (base - 'a'));
                }
                break;

            case STYLE_BOLD_ITALIC:
                if (base >= 'A' && base <= 'Z') 
                {
                    return codePointToString(0x1D468 + (base - 'A'));
                }
                if (base >= 'a' && base <= 'z') 
                {
                    return codePointToString(0x1D482 + (base - 'a'));
                }
                break;

            case STYLE_SCRIPT:
                if (base >= 'A' && base <= 'Z') 
                {
                    return codePointToString(0x1D4D0 + (base - 'A'));
                }
                if (base >= 'a' && base <= 'z') 
                {
                    return codePointToString(0x1D4EA + (base - 'a'));
                }
                break;

            case STYLE_GOTHIC:
                if (base >= 'A' && base <= 'Z') 
                {
                    return codePointToString(0x1D56C + (base - 'A'));
                }
                if (base >= 'a' && base <= 'z') 
                {
                    return codePointToString(0x1D586 + (base - 'a'));
                }
                break;

            case STYLE_MONO:
                if (base >= 'A' && base <= 'Z') 
                {
                    return codePointToString(0x1D670 + (base - 'A'));
                }
                if (base >= 'a' && base <= 'z') 
                {
                    return codePointToString(0x1D68A + (base - 'a'));
                }
                if (base >= '0' && base <= '9') 
                {
                    return codePointToString(0x1D7F6 + (base - '0'));
                }
                break;

            case STYLE_BUBBLE:
                if (base >= 'A' && base <= 'Z') 
                {
                    return codePointToString(0x24B6 + (base - 'A'));
                }
                if (base >= 'a' && base <= 'z') 
                {
                    return codePointToString(0x24D0 + (base - 'a'));
                }
                if (base >= '1' && base <= '9') 
                {
                    return codePointToString(0x2460 + (base - '1'));
                }
                if (base == '0') 
                {
                    return "\u24EA";
                }
                break;

            case STYLE_STRIKE:
                return String.valueOf(base) + "\u0336";

            case STYLE_UNDERLINE:
                return String.valueOf(base) + "\u0332";
        }

        return String.valueOf(base);
    }

    private static String codePointToString(int codePoint) 
    {
        return new String(Character.toChars(codePoint));
    }
}
