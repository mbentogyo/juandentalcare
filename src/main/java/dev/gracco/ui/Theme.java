package dev.gracco.ui;

import lombok.Getter;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.image.ImageTranscoder;

import javax.swing.ImageIcon;
import java.awt.Color;
import java.awt.Font;
import java.awt.image.BufferedImage;
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

    @Getter private static ImageIcon adminWhite;
    @Getter private static ImageIcon adminColor;
    @Getter private static ImageIcon appointmentWhite;
    @Getter private static ImageIcon appointmentColor;
    @Getter private static ImageIcon dashboardWhite;
    @Getter private static ImageIcon dashboardColor;
    @Getter private static ImageIcon logsWhite;
    @Getter private static ImageIcon logsColor;
    @Getter private static ImageIcon patientWhite;
    @Getter private static ImageIcon patientColor;
    @Getter private static ImageIcon sidebarOpen;
    @Getter private static ImageIcon sidebarClose;

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

    // Images
    public static ImageIcon loadSvgImage(String resourcePath, int width, int height) {
        try (InputStream inputStream =
                     Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath)) {

            if (inputStream == null) {
                throw new RuntimeException("Resource not found: " + resourcePath);
            }

            class SvgTranscoder extends ImageTranscoder {
                private BufferedImage image;

                @Override
                public BufferedImage createImage(int w, int h) {
                    return new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                }

                @Override
                public void writeImage(BufferedImage img, org.apache.batik.transcoder.TranscoderOutput output) {
                    this.image = img;
                }

                public BufferedImage getImage() {
                    return image;
                }
            }

            SvgTranscoder transcoder = new SvgTranscoder();
            transcoder.addTranscodingHint(ImageTranscoder.KEY_WIDTH, (float) width);
            transcoder.addTranscodingHint(ImageTranscoder.KEY_HEIGHT, (float) height);
            transcoder.transcode(new TranscoderInput(inputStream), null);

            return new ImageIcon(transcoder.getImage());

        } catch (Exception e) {
            Alert.fatalError(e.getMessage());
            return null;
        }
    }

    //Initialization
    public static boolean initialize() {
        if (initialized) return true;

        //Fonts
        for (FontType type : FontType.values()) {
            try {
                InputStream is = Theme.class.getResourceAsStream("/fonts/" + type.getFileName());
                Font font = Font.createFont(Font.TRUETYPE_FONT, is);
                FONT_CACHE.put(type, font);
            } catch (Exception e) {
                Alert.fatalError(e.getMessage());
                return false;
            }
        }

        //Images
        adminWhite = loadSvgImage("svg/admin_white.svg", 30, 30);
        adminColor = loadSvgImage("svg/admin_color.svg", 30, 30);
        appointmentWhite = loadSvgImage("svg/appointment_white.svg", 30, 30);
        appointmentColor = loadSvgImage("svg/appointment_color.svg", 30, 30);
        dashboardWhite = loadSvgImage("svg/dashboard_white.svg", 30, 30);
        dashboardColor = loadSvgImage("svg/dashboard_color.svg", 30, 30);
        logsWhite =  loadSvgImage("svg/logs_white.svg", 30, 30);
        logsColor = loadSvgImage("svg/logs_color.svg", 30, 30);
        patientWhite = loadSvgImage("svg/patient_white.svg", 30, 30);
        patientColor = loadSvgImage("svg/patient_color.svg", 30, 30);
        sidebarOpen = loadSvgImage("svg/sidebar_open.svg", 30, 30);
        sidebarClose = loadSvgImage("svg/sidebar_close.svg", 30, 30);

        initialized = true;
        return true;
    }
}
