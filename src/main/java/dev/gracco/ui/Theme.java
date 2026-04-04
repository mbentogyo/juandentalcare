package dev.gracco.ui;

import lombok.Getter;

import java.awt.Color;
import java.awt.Font;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.Map;

public class Theme {
    public enum FontType {
        BLACK("Poppins-Black.ttf"),
        BLACK_ITALIC("Poppins-BlackItalic.ttf"),
        BOLD("Poppins-Bold.ttf"),
        BOLD_ITALIC("Poppins-BoldItalic.ttf"),
        EXTRA_BOLD("Poppins-ExtraBold.ttf"),
        EXTRA_BOLD_ITALIC("Poppins-ExtraBoldItalic.ttf"),
        EXTRA_LIGHT("Poppins-ExtraLight.ttf"),
        EXTRA_LIGHT_ITALIC("Poppins-ExtraLightItalic.ttf"),
        ITALIC("Poppins-Italic.ttf"),
        LIGHT("Poppins-Light.ttf"),
        LIGHT_ITALIC("Poppins-LightItalic.ttf"),
        MEDIUM("Poppins-Medium.ttf"),
        MEDIUM_ITALIC("Poppins-MediumItalic.ttf"),
        REGULAR("Poppins-Regular.ttf"),
        SEMI_BOLD("Poppins-SemiBold.ttf"),
        SEMI_BOLD_ITALIC("Poppins-SemiBoldItalic.ttf"),
        THIN("Poppins-Thin.ttf"),
        THIN_ITALIC("Poppins-ThinItalic.ttf");

        private final String fileName;

        FontType(String fileName) {
            this.fileName = fileName;
        }

        public String getFileName() {
            return fileName;
        }
    }

    //Colors
    public static final Color PRIMARY = new Color(51, 150, 55);
    public static final Color PRIMARY_HOVER = new Color(45, 125, 48);
    public static final Color SECONDARY = new Color(109, 176, 104);
    public static final Color ACCENT = new Color(55, 170, 210);
    public static final Color ACCENT_HOVER = new Color(37, 150, 190);
    public static final Color HIGHLIGHT = new Color(183, 216, 178);
    public static final Color BACKGROUND_GREEN = new Color(231, 242, 229);
    public static final Color WHITE = new Color(255, 255, 255);
    public static final Color BLACK = new Color(0, 0, 0);

    //Fonts
    private static final Map<FontType, Font> FONT_CACHE = new EnumMap<>(FontType.class);
    private static boolean initialized = false;

    public static Font getFont(FontType type, float size) {
        Font base = FONT_CACHE.get(type);
        if (base == null) {
            return new Font("SansSerif", Font.PLAIN, (int) size);
        }
        return base.deriveFont(size);
    }

    //Initialization
    public static boolean initialize() {
        if (initialized) return true;

        for (FontType type : FontType.values()) {
            try {
                InputStream is = Theme.class.getResourceAsStream("/fonts/" + type.getFileName());
                Font font = Font.createFont(Font.TRUETYPE_FONT, is);
                FONT_CACHE.put(type, font);
            } catch (Exception e) {
                return false;
            }
        }

        initialized = true;
        return true;
    }
}
