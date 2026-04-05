package dev.gracco.ui;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;

public final class Alert {
    private static final String PROPERTY_KEY = "active_alert_overlay";

    public static void error(String message, JFrame frame) {
        show(message, frame, new Color(255, 0, 0), new Color(255, 235, 238), "Error");
    }

    public static void success(String message, JFrame frame) {
        show(message, frame, new Color(0, 255, 0), new Color(232, 245, 233), "Success");
    }

    public static void info(String message, JFrame frame) {
        show(message, frame, new Color(0, 0, 255), new Color(227, 242, 253), "Information");
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
            titleLabel.setFont(new Font("SansSerif", Font.BOLD, 20));
            titleLabel.setForeground(borderColor);

            JButton closeButton = new JButton("×");
            closeButton.setFont(new Font("SansSerif", Font.BOLD, 20));
            closeButton.setForeground(borderColor);
            closeButton.setBorder(BorderFactory.createEmptyBorder());
            closeButton.setContentAreaFilled(false);
            closeButton.setFocusPainted(false);
            closeButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            closeButton.addActionListener(e -> close());

            topBar.add(titleLabel);
            topBar.add(Box.createHorizontalGlue());
            topBar.add(closeButton);

            JLabel messageLabel = new JLabel(toHtml(message), SwingConstants.CENTER);
            messageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            messageLabel.setFont(new Font("SansSerif", Font.PLAIN, 15));
            messageLabel.setForeground(new Color(30, 30, 30));

            card.add(topBar);
            card.add(Box.createVerticalStrut(18));
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

        private String toHtml(String message) {
            if (message == null) {
                message = "";
            }
            String escaped = message
                    .replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\n", "<br>");
            return "<html><div style='text-align:center; width:340px;'>" + escaped + "</div></html>";
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