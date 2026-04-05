package dev.gracco.ui;

import dev.gracco.Main;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;

public final class Alert {
    private static final String PROPERTY_KEY = "active_alert_overlay";

    public static void error(String message, JFrame frame) {
        show(message, frame,
                new Color(198, 40, 40),
                new Color(255, 235, 238),
                "Error");
    }

    public static void success(String message, JFrame frame) {
        show(message, frame,
                new Color(46, 125, 50),
                new Color(232, 245, 233),
                "Success");
    }

    public static void info(String message, JFrame frame) {
        show(message, frame,
                new Color(25, 118, 210),
                new Color(227, 242, 253),
                "Information");
    }

    public static void fatalError(String message) {
        SwingUtilities.invokeLater(() -> {
            JDialog dialog = new JDialog((Frame) null, "Fatal Error", true);
            dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
            dialog.setUndecorated(true);
            dialog.setAlwaysOnTop(true);

            Color borderColor = new Color(120, 24, 24);
            Color fillColor = new Color(252, 238, 238);

            JPanel root = new JPanel(new GridBagLayout());
            root.setOpaque(false);
            root.setBorder(new EmptyBorder(20, 20, 20, 20));

            JPanel card = new RoundedPanel(24, fillColor, borderColor);
            card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
            card.setOpaque(false);
            card.setBorder(new EmptyBorder(18, 18, 18, 18));
            card.setPreferredSize(new Dimension(460, 240));
            card.setMaximumSize(new Dimension(460, 240));

            JLabel appLabel = new JLabel(Main.getName());
            appLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            appLabel.setHorizontalAlignment(SwingConstants.CENTER);
            appLabel.setFont(Theme.getFont(Theme.FontType.SEMI_BOLD, 20));
            appLabel.setForeground(new Color(110, 110, 110));

            JLabel titleLabel = new JLabel("FATAL ERROR");
            titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
            titleLabel.setFont(Theme.getFont(Theme.FontType.BOLD, 22));
            titleLabel.setForeground(borderColor);

            JTextPane messagePane = createCenteredMessagePane(message);

            JButton closeButton = new JButton("Close Program");
            closeButton.setAlignmentX(Component.CENTER_ALIGNMENT);
            closeButton.setFont(Theme.getFont(Theme.FontType.MEDIUM, 14));
            closeButton.setForeground(Color.WHITE);
            closeButton.setBackground(borderColor);
            closeButton.setFocusPainted(false);
            closeButton.setBorder(BorderFactory.createEmptyBorder(10, 18, 10, 18));
            closeButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            closeButton.addActionListener(e -> {
                dialog.dispose();
                System.exit(1);
            });

            card.add(appLabel);
            card.add(Box.createVerticalStrut(4));
            card.add(titleLabel);
            card.add(Box.createVerticalStrut(10));
            card.add(messagePane);
            card.add(Box.createVerticalGlue());
            card.add(Box.createVerticalStrut(16));
            card.add(closeButton);

            root.add(card);
            dialog.setContentPane(root);
            dialog.pack();
            dialog.setLocationRelativeTo(null);
            dialog.setVisible(true);
        });
    }

    private static void show(String message, JFrame frame, Color borderColor, Color fillColor, String title) {
        if (frame == null) {
            return;
        }

        AlertOverlay existing = (AlertOverlay) frame.getRootPane().getClientProperty(PROPERTY_KEY);
        if (existing != null) {
            existing.close();
        }

        AlertOverlay overlay = new AlertOverlay(message, borderColor, fillColor, title, frame);
        frame.getRootPane().putClientProperty(PROPERTY_KEY, overlay);
        frame.setGlassPane(overlay);
        overlay.setVisible(true);
        overlay.startAutoClose();
    }

    private static JTextPane createCenteredMessagePane(String message) {
        if (message == null) {
            message = "";
        }

        int length = message.length();
        int fontSize = 15;

        if (length > 140) fontSize = 14;
        if (length > 220) fontSize = 13;
        if (length > 320) fontSize = 12;

        JTextPane pane = new JTextPane();
        pane.setEditable(false);
        pane.setFocusable(false);
        pane.setOpaque(false);
        pane.setBorder(null);
        pane.setAlignmentX(Component.CENTER_ALIGNMENT);
        pane.setMaximumSize(new Dimension(380, 90));
        pane.setPreferredSize(new Dimension(380, 90));
        pane.setFont(Theme.getFont(Theme.FontType.REGULAR, fontSize));
        pane.setForeground(new Color(60, 60, 60));
        pane.setText(message);

        StyledDocument doc = pane.getStyledDocument();
        SimpleAttributeSet attrs = new SimpleAttributeSet();
        StyleConstants.setAlignment(attrs, StyleConstants.ALIGN_CENTER);
        doc.setParagraphAttributes(0, doc.getLength(), attrs, false);

        return pane;
    }

    private static final class AlertOverlay extends JComponent {
        private final JFrame frame;
        private final Timer autoCloseTimer;

        private AlertOverlay(String message, Color borderColor, Color fillColor, String title, JFrame frame) {
            this.frame = frame;
            setOpaque(false);
            setLayout(new GridBagLayout());

            addMouseListener(new MouseAdapter() {});
            addMouseMotionListener(new MouseAdapter() {});
            addMouseWheelListener(e -> {});

            JPanel card = new RoundedPanel(24, fillColor, borderColor);
            card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
            card.setOpaque(false);
            card.setBorder(new EmptyBorder(18, 18, 18, 18));
            card.setPreferredSize(new Dimension(420, 180));
            card.setMaximumSize(new Dimension(420, 180));

            JPanel topBar = new JPanel();
            topBar.setOpaque(false);
            topBar.setLayout(new BoxLayout(topBar, BoxLayout.X_AXIS));

            JLabel titleLabel = new JLabel(title);
            titleLabel.setFont(Theme.getFont(Theme.FontType.MEDIUM, 20));
            titleLabel.setForeground(borderColor);

            JButton closeButton = new JButton("×");
            closeButton.setFont(Theme.getFont(Theme.FontType.LIGHT, 24));
            closeButton.setForeground(borderColor);
            closeButton.setBorder(BorderFactory.createEmptyBorder());
            closeButton.setContentAreaFilled(false);
            closeButton.setFocusPainted(false);
            closeButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            closeButton.addActionListener(e -> close());

            topBar.add(titleLabel);
            topBar.add(Box.createHorizontalGlue());
            topBar.add(closeButton);

            JLabel messageLabel = new JLabel();
            messageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            messageLabel.setHorizontalAlignment(SwingConstants.CENTER);
            messageLabel.setFont(Theme.getFont(Theme.FontType.REGULAR, 16));
            messageLabel.setForeground(new Color(60, 60, 60));
            messageLabel.setMaximumSize(new Dimension(360, Integer.MAX_VALUE));

            setMessage(messageLabel, message);

            card.add(topBar);
            card.add(Box.createVerticalStrut(10));
            card.add(messageLabel);

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.anchor = GridBagConstraints.CENTER;
            add(card, gbc);

            autoCloseTimer = new Timer(3000, e -> close());
            autoCloseTimer.setRepeats(false);
        }

        private void startAutoClose() {
            autoCloseTimer.start();
        }

        private void close() {
            autoCloseTimer.stop();
            frame.getRootPane().putClientProperty(PROPERTY_KEY, null);
            setVisible(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setColor(new Color(0, 0, 0, 110));
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.dispose();
        }

        private void setMessage(JLabel label, String message) {
            if (message == null) {
                message = "";
            }

            String escaped = message
                    .replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\n", "<br>");

            int length = escaped.length();
            int fontSize = 16;

            if (length > 120) fontSize = 14;
            if (length > 180) fontSize = 13;
            if (length > 260) fontSize = 12;

            label.setFont(Theme.getFont(Theme.FontType.REGULAR, fontSize));
            label.setText(
                    "<html><div style='text-align:center; width:100%; word-wrap:break-word;'>"
                            + escaped +
                            "</div></html>"
            );
        }
    }

    private static final class RoundedPanel extends JPanel {
        private final int arc;
        private final Color fillColor;
        private final Color borderColor;

        private RoundedPanel(int arc, Color fillColor, Color borderColor) {
            this.arc = arc;
            this.fillColor = fillColor;
            this.borderColor = borderColor;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setColor(fillColor);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);

            g2.setColor(borderColor);
            g2.setStroke(new BasicStroke(3f));
            g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, arc, arc);

            g2.dispose();
            super.paintComponent(g);
        }
    }
}