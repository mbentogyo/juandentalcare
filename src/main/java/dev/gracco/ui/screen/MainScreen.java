package dev.gracco.ui.screen;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import dev.gracco.Main;
import dev.gracco.db.Database;
import dev.gracco.db.Enums;
import dev.gracco.ui.Theme;
import dev.gracco.ui.panels.AdminPanel;
import dev.gracco.ui.panels.AppointmentPanel;
import dev.gracco.ui.panels.DashboardPanel;
import dev.gracco.ui.panels.LogsPanel;
import dev.gracco.ui.panels.PatientPanel;

public class MainScreen extends JFrame {
    private static final int EXPANDED_SIDEBAR_WIDTH = 320;
    private static final int COLLAPSED_SIDEBAR_WIDTH = 100;
    private static final int MIN_WINDOW_WIDTH = 1280;
    private static final int MIN_WINDOW_HEIGHT = 720;
    private static final int SIDEBAR_BUTTON_HEIGHT = 64;

    private final JPanel sidebar;
    private final JPanel contentPanel;
    private final JLabel toggleButton;
    private final JLabel titleLabel;
    private final CardLayout cardLayout;

    private final JButton dashboardButton;
    private final JButton appointmentButton;
    private final JButton patientButton;
    private JButton adminButton;
    private JButton logsButton;

    private boolean sidebarExpanded = true;
    private String selectedPanel = "Dashboard";

    private static final int SIDEBAR_ANIMATION_DURATION = 220;
    private static final int SIDEBAR_ANIMATION_STEP_DELAY = 10;

    private boolean sidebarAnimating = false;

    public MainScreen() {
        setTitle(Main.getName());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(MIN_WINDOW_WIDTH, MIN_WINDOW_HEIGHT));
        setSize(1280, 720);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        getContentPane().setBackground(Theme.WHITE);

        sidebar = new JPanel(new BorderLayout());
        sidebar.setBackground(Theme.BACKGROUND_GREEN);
        sidebar.setPreferredSize(new Dimension(EXPANDED_SIDEBAR_WIDTH, 0));
        sidebar.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        JPanel sidebarTop = new JPanel(new BorderLayout());
        sidebarTop.setBackground(Theme.WHITE);
        sidebarTop.setBorder(BorderFactory.createEmptyBorder(20, 20, 16, 12));

        titleLabel = new JLabel(Main.getName());
        titleLabel.setForeground(Theme.BLACK);
        titleLabel.setHorizontalAlignment(SwingConstants.LEFT);
        titleLabel.setFont(Theme.getFont(Theme.FontType.SEMI_BOLD, 24));

        toggleButton = new JLabel(Theme.getSidebarClose());
        toggleButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        toggleButton.setHorizontalAlignment(SwingConstants.RIGHT);
        toggleButton.setVerticalAlignment(SwingConstants.CENTER);
        toggleButton.setPreferredSize(new Dimension(48, 48));

        toggleButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                toggleSidebar();
            }
        });

        sidebarTop.add(titleLabel, BorderLayout.WEST);
        sidebarTop.add(toggleButton, BorderLayout.EAST);

        JPanel sidebarCenter = new JPanel();
        sidebarCenter.setBackground(Theme.WHITE);
        sidebarCenter.setLayout(new BoxLayout(sidebarCenter, BoxLayout.Y_AXIS));
        sidebarCenter.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        dashboardButton = createSidebarButton("Dashboard");
        appointmentButton = createSidebarButton("Appointments");
        patientButton = createSidebarButton("Patients");

        dashboardButton.addActionListener(_ -> showPanel("Dashboard"));
        appointmentButton.addActionListener(_ -> showPanel("Appointments"));
        patientButton.addActionListener(_ -> showPanel("Patients"));

        sidebarCenter.add(dashboardButton);
        sidebarCenter.add(Box.createVerticalStrut(12));
        sidebarCenter.add(appointmentButton);
        sidebarCenter.add(Box.createVerticalStrut(12));
        sidebarCenter.add(patientButton);

        if (Database.User.getRole() == Enums.Role.ADMIN) {
            adminButton = createSidebarButton("Admin");
            logsButton = createSidebarButton("Logs");

            adminButton.addActionListener(_ -> showPanel("Admin"));
            logsButton.addActionListener(_ -> showPanel("Logs"));

            sidebarCenter.add(Box.createVerticalStrut(12));
            sidebarCenter.add(adminButton);
            sidebarCenter.add(Box.createVerticalStrut(12));
            sidebarCenter.add(logsButton);
        }

        sidebar.add(sidebarTop, BorderLayout.NORTH);
        sidebar.add(sidebarCenter, BorderLayout.CENTER);

        // Logout button at the bottom of sidebar
        JPanel sidebarBottom = new JPanel();
        sidebarBottom.setBackground(Theme.WHITE);
        sidebarBottom.setLayout(new BoxLayout(sidebarBottom, BoxLayout.Y_AXIS));
        sidebarBottom.setBorder(BorderFactory.createEmptyBorder(8, 8, 16, 8));

        JButton logoutButton = createSidebarButton("Logout");
        logoutButton.setBackground(new java.awt.Color(252, 238, 238));
        logoutButton.setForeground(new java.awt.Color(180, 30, 30));
        logoutButton.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 4, 0, 0, new java.awt.Color(220, 80, 80)),
                BorderFactory.createEmptyBorder(0, 18, 0, 18)
        ));
        logoutButton.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                logoutButton.setBackground(new java.awt.Color(248, 215, 215));
            }
            @Override public void mouseExited(MouseEvent e) {
                logoutButton.setBackground(new java.awt.Color(252, 238, 238));
            }
        });
        logoutButton.addActionListener(_ -> {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Are you sure you want to log out?", "Logout",
                    JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (confirm == JOptionPane.YES_OPTION) {
                Database.shutdown();
                Database.initialize();
                dispose();
                SwingUtilities.invokeLater(LoginScreen::new);
            }
        });

        sidebarBottom.add(logoutButton);
        sidebar.add(sidebarBottom, BorderLayout.SOUTH);

        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        contentPanel.setBackground(Theme.BACKGROUND_GREEN);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        contentPanel.add(new DashboardPanel(), "Dashboard");
        contentPanel.add(new AppointmentPanel(), "Appointments");
        contentPanel.add(new PatientPanel(), "Patients");

        if (Database.User.getRole() == Enums.Role.ADMIN) {
            contentPanel.add(new AdminPanel(), "Admin");
            contentPanel.add(new LogsPanel(), "Logs");
        }

        add(sidebar, BorderLayout.WEST);
        add(contentPanel, BorderLayout.CENTER);

        showPanel("Dashboard");

        setVisible(true);
    }

    private JButton createSidebarButton(String text) {
        JButton button = new JButton(text);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, SIDEBAR_BUTTON_HEIGHT));
        button.setPreferredSize(new Dimension(0, SIDEBAR_BUTTON_HEIGHT));
        button.setMinimumSize(new Dimension(0, SIDEBAR_BUTTON_HEIGHT));
        button.setAlignmentX(Component.LEFT_ALIGNMENT);

        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setVerticalTextPosition(SwingConstants.CENTER);
        button.setHorizontalTextPosition(SwingConstants.RIGHT);
        button.setIconTextGap(14);

        button.setFocusPainted(false);
        button.setContentAreaFilled(false);
        button.setOpaque(true);

        button.setForeground(Theme.BLACK);
        button.setBackground(Theme.WHITE);
        button.setFont(Theme.getFont(Theme.FontType.MEDIUM, 16));

        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 4, 0, 0, Theme.SECONDARY),
                BorderFactory.createEmptyBorder(0, 18, 0, 18)
        ));

        button.setBorderPainted(true);

        addHoverColor(button);

        return button;
    }

    private void showPanel(String panelName) {
        selectedPanel = panelName;
        cardLayout.show(contentPanel, panelName);
        updateSidebarSelection();
    }

    private void updateSidebarSelection() {
        styleSidebarButton(dashboardButton, selectedPanel.equals("Dashboard"), "Dashboard",
                Theme.getDashboardColor(), Theme.getDashboardWhite());
        styleSidebarButton(appointmentButton, selectedPanel.equals("Appointments"), "Appointments",
                Theme.getAppointmentColor(), Theme.getAppointmentWhite());
        styleSidebarButton(patientButton, selectedPanel.equals("Patients"), "Patients",
                Theme.getPatientColor(), Theme.getPatientWhite());

        if (Database.User.getRole() == Enums.Role.ADMIN) {
            styleSidebarButton(adminButton, selectedPanel.equals("Admin"), "Admin",
                    Theme.getAdminColor(), Theme.getAdminWhite());
            styleSidebarButton(logsButton, selectedPanel.equals("Logs"), "Logs",
                    Theme.getLogsColor(), Theme.getLogsWhite());
        }
    }

    private void styleSidebarButton(JButton button, boolean selected, String expandedText, Icon normalIcon, Icon whiteIcon) {
        button.setIcon(selected ? whiteIcon : normalIcon);

        int currentWidth = sidebar.getPreferredSize().width;
        boolean showText = currentWidth > 180;

        if (showText) {
            button.setText(expandedText);
            button.setIconTextGap(14);
        } else {
            button.setText("");
            button.setIconTextGap(0);
        }

        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setHorizontalTextPosition(SwingConstants.RIGHT);

        button.putClientProperty("selected", selected);

        if (selected) {
            button.setBackground(Theme.SECONDARY);
            button.setForeground(Theme.WHITE);
        } else {
            button.setBackground(Theme.WHITE);
            button.setForeground(Theme.BLACK);
        }
    }

    private void toggleSidebar() {
        if (sidebarAnimating) return;

        sidebarAnimating = true;
        toggleButton.setEnabled(false);

        int startWidth = sidebar.getPreferredSize().width;
        int targetWidth = sidebarExpanded ? COLLAPSED_SIDEBAR_WIDTH : EXPANDED_SIDEBAR_WIDTH;

        if (!sidebarExpanded) titleLabel.setText(Main.getName());

        int distance = targetWidth - startWidth;
        int steps = Math.max(1, SIDEBAR_ANIMATION_DURATION / SIDEBAR_ANIMATION_STEP_DELAY);

        Timer timer = new Timer(SIDEBAR_ANIMATION_STEP_DELAY, null);
        final int[] currentStep = {0};

        timer.addActionListener(_ -> {
            currentStep[0]++;
            float progress = Math.min((float) currentStep[0] / steps, 1f);

            int newWidth = startWidth + Math.round(distance * progress);

            sidebar.setPreferredSize(new Dimension(newWidth, 0));

            updateSidebarSelection();

            sidebar.revalidate();
            revalidate();
            repaint();

            if (progress >= 1f) {
                timer.stop();

                sidebarExpanded = !sidebarExpanded;

                if (!sidebarExpanded) {
                    titleLabel.setText("");
                    toggleButton.setIcon(Theme.getSidebarOpen());
                } else {
                    toggleButton.setIcon(Theme.getSidebarClose());
                }

                updateSidebarSelection();
                sidebarAnimating = false;
                toggleButton.setEnabled(true);
            }
        });

        timer.setRepeats(true);
        timer.start();
    }

    private void addHoverColor(JButton button) {
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                boolean selected = Boolean.TRUE.equals(button.getClientProperty("selected"));

                if (!selected) {
                    button.setBackground(Theme.HIGHLIGHT);
                    button.setForeground(Theme.BLACK);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                boolean selected = Boolean.TRUE.equals(button.getClientProperty("selected"));

                if (!selected) {
                    button.setBackground(Theme.WHITE);
                    button.setForeground(Theme.BLACK);
                }
            }
        });
    }
}