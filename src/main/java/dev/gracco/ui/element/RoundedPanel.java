package dev.gracco.ui.element;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

public class RoundedPanel extends JPanel {
    private final Color borderColor;
    private final int arc;
    private final float strokeWidth;

    public RoundedPanel(Color borderColor, int arc, float strokeWidth) {
        this.borderColor = borderColor;
        this.arc = arc;
        this.strokeWidth = strokeWidth;
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int offset = Math.round(strokeWidth);

        g2.setColor(getBackground());
        g2.fillRoundRect(offset / 2, offset / 2, getWidth() - offset, getHeight() - offset, arc, arc);

        g2.setColor(borderColor);
        g2.setStroke(new BasicStroke(strokeWidth));
        g2.drawRoundRect(offset / 2, offset / 2, getWidth() - offset, getHeight() - offset, arc, arc);

        g2.dispose();
        super.paintComponent(g);
    }
}