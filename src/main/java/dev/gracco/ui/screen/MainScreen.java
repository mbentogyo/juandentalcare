package dev.gracco.ui.screen;

import dev.gracco.Main;
import dev.gracco.ui.Theme;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class MainScreen extends JFrame {
    private static final int EXPANDED_SIDEBAR_WIDTH = 260;
    private static final int COLLAPSED_SIDEBAR_WIDTH = 70;
    private static final int MIN_WINDOW_WIDTH = 1280;
    private static final int MIN_WINDOW_HEIGHT = 720;

    private final JPanel sidebar;
    private final JPanel contentPanel;
    private final JLabel toggleButton;
    private final JLabel titleLabel;
    private final CardLayout cardLayout;

    private final JButton oneButton;
    private final JButton twoButton;
    private final JButton threeButton;

    private boolean sidebarExpanded = true;
    private String selectedPanel = "One";

    public MainScreen() {
        setTitle("MainScreen");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(MIN_WINDOW_WIDTH, MIN_WINDOW_HEIGHT));
        setSize(1280, 720);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        getContentPane().setBackground(Theme.WHITE);

        sidebar = new JPanel(new BorderLayout());
        sidebar.setBackground(Theme.BACKGROUND_GREEN);
        sidebar.setPreferredSize(new Dimension(EXPANDED_SIDEBAR_WIDTH, 0));
        sidebar.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel sidebarTop = new JPanel(new BorderLayout());
        sidebarTop.setBackground(Theme.BACKGROUND_GREEN);
        sidebarTop.setBorder(BorderFactory.createEmptyBorder(12, 12, 16, 12));

        titleLabel = new JLabel(Main.getName());
        titleLabel.setForeground(Theme.BLACK);
        titleLabel.setHorizontalAlignment(SwingConstants.LEFT);

        toggleButton = new JLabel("≡");
        toggleButton.setForeground(Theme.BLACK);
        toggleButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        toggleButton.setHorizontalAlignment(SwingConstants.CENTER);
        toggleButton.setPreferredSize(new Dimension(44, 36));

        toggleButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                toggleSidebar();
            }
        });

        sidebarTop.add(titleLabel, BorderLayout.WEST);
        sidebarTop.add(toggleButton, BorderLayout.EAST);

        JPanel sidebarCenter = new JPanel();
        sidebarCenter.setBackground(Theme.BACKGROUND_GREEN);
        sidebarCenter.setLayout(new BoxLayout(sidebarCenter, BoxLayout.Y_AXIS));
        sidebarCenter.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));

        oneButton = createSidebarButton("One");
        twoButton = createSidebarButton("Two");
        threeButton = createSidebarButton("Three");

        oneButton.addActionListener(e -> showPanel("One"));
        twoButton.addActionListener(e -> showPanel("Two"));
        threeButton.addActionListener(e -> showPanel("Three"));

        sidebarCenter.add(oneButton);
        sidebarCenter.add(Box.createVerticalStrut(10));
        sidebarCenter.add(twoButton);
        sidebarCenter.add(Box.createVerticalStrut(10));
        sidebarCenter.add(threeButton);

        sidebar.add(sidebarTop, BorderLayout.NORTH);
        sidebar.add(sidebarCenter, BorderLayout.CENTER);

        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        contentPanel.setBackground(Theme.WHITE);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        contentPanel.add(createContentPanel("One"), "One");
        contentPanel.add(createContentPanel("Two"), "Two");
        contentPanel.add(createContentPanel("Three"), "Three");

        add(sidebar, BorderLayout.WEST);
        add(contentPanel, BorderLayout.CENTER);

        showPanel("One");

        setVisible(true);
    }

    private JButton createSidebarButton(String text) {
        JButton button = new JButton(text);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        button.setPreferredSize(new Dimension(0, 48));
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(true);
        button.setForeground(Theme.BLACK);
        button.setBackground(Theme.WHITE);
        addHoverColor(button, Theme.HIGHLIGHT);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 4, 0, 0, Theme.SECONDARY),
                BorderFactory.createEmptyBorder(0, 14, 0, 14)
        ));
        button.setBorderPainted(true);
        return button;
    }

    private JPanel createContentPanel(String text) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(Theme.WHITE);

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Theme.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24)
        );

        JLabel label = new JLabel(text);
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setForeground(Theme.BLACK);

        panel.add(label, BorderLayout.CENTER);
        wrapper.add(panel, BorderLayout.CENTER);

        return wrapper;
    }

    private void showPanel(String panelName) {
        selectedPanel = panelName;
        cardLayout.show(contentPanel, panelName);
        updateSidebarSelection();
    }

    private void updateSidebarSelection() {
        styleSidebarButton(oneButton, selectedPanel.equals("One"), "One", "1");
        styleSidebarButton(twoButton, selectedPanel.equals("Two"), "Two", "2");
        styleSidebarButton(threeButton, selectedPanel.equals("Three"), "Three", "3");
    }

    private void styleSidebarButton(JButton button, boolean selected, String expandedText, String collapsedText) {
        button.setText(sidebarExpanded ? expandedText : collapsedText);
        button.setHorizontalAlignment(sidebarExpanded ? SwingConstants.LEFT : SwingConstants.CENTER);

        if (selected) {
            button.setBackground(Theme.SECONDARY);
            button.setForeground(Theme.WHITE);
        } else {
            button.setBackground(Theme.WHITE);
            button.setForeground(Theme.BLACK);
        }
    }

    private void toggleSidebar() {
        sidebarExpanded = !sidebarExpanded;
        sidebar.setPreferredSize(new Dimension(sidebarExpanded ? EXPANDED_SIDEBAR_WIDTH : COLLAPSED_SIDEBAR_WIDTH, 0));
        titleLabel.setText(sidebarExpanded ? Main.getName() : "");
        updateSidebarSelection();
        revalidate();
        repaint();
    }

    private void addHoverColor(JButton button, Color highlight) {
        button.addMouseListener(new MouseAdapter() {
            boolean notHovered = true;
            Color background = null;
            Color foreground = null;

            @Override
            public void mouseEntered(MouseEvent e) {
                if (notHovered) {
                    foreground = button.getForeground();
                    background = button.getBackground();
                    notHovered = false;
                }
                button.setBackground(highlight);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (!notHovered) {
                    button.setBackground(background);
                    background = null;

                    button.setForeground(foreground);
                    foreground = null;
                    notHovered = true;
                }
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                notHovered = true;
            }
        });
    }
}
