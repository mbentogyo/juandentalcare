package dev.gracco.ui.panels;

import dev.gracco.db.Database;
import dev.gracco.ui.Theme;
import dev.gracco.ui.Theme.FontType;
import dev.gracco.ui.element.DashboardHeaderRenderer;
import dev.gracco.ui.element.JRoundedButton;
import dev.gracco.ui.element.RoundedPanel;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.event.MouseInputAdapter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DashboardPanel extends JPanel {
    private static final String[] TABLE_COLUMNS = {
            "Appointment ID",
            "Patient",
            "Dentist",
            "Scheduled Time",
            "Status",
            "Reason for Visit",
            "Notes"
    };

    private final JLabel todayAppointmentsValue;
    private final JLabel completedTodayValue;
    private final JLabel appointmentsLeftTodayValue;
    private final JLabel tomorrowAppointmentsValue;
    private final JLabel cancelledTodayValue;
    private final JLabel walkInValue;

    private final JLabel nameLabel;

    private final DefaultTableModel tableModel;
    private final JTable table;
    private final TableRowSorter<DefaultTableModel> tableSorter;
    private final Map<Integer, SortOrder> columnSortStates = new HashMap<>();

    private final JRoundedButton refreshButton;

    public DashboardPanel() {
        setLayout(new BorderLayout());
        setBackground(Theme.WHITE);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        todayAppointmentsValue = createValueLabel("0");
        completedTodayValue = createValueLabel("0");
        appointmentsLeftTodayValue = createValueLabel("0");
        tomorrowAppointmentsValue = createValueLabel("0");
        cancelledTodayValue = createValueLabel("0");
        walkInValue = createValueLabel("0");

        nameLabel = new JLabel(getCurrentUserFullName());
        nameLabel.setFont(Theme.getFont(FontType.SEMI_BOLD, 16f));
        nameLabel.setForeground(Theme.BLACK);

        refreshButton = createRefreshButton();
        refreshButton.addActionListener(e -> refreshDashboard());
        refreshButton.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                refreshButton.setBackground(Theme.ACCENT_HOVER);
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                refreshButton.setBackground(Theme.ACCENT);
            }
        });

        tableModel = new DefaultTableModel(new Object[0][0], TABLE_COLUMNS) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return switch (columnIndex) {
                    case 0 -> Integer.class;
                    default -> String.class;
                };
            }
        };

        table = new JTable(tableModel);
        tableSorter = new TableRowSorter<>(tableModel);
        tableSorter.setComparator(0, Comparator.comparingInt(value -> {
            if (value == null) {
                return 0;
            }
            if (value instanceof Number number) {
                return number.intValue();
            }
            return Integer.parseInt(value.toString().trim());
        }));
        table.setRowSorter(tableSorter);

        configureTable();
        installHeaderSorting();

        JScrollPane tableScrollPane = new JScrollPane(table);
        tableScrollPane.setBorder(BorderFactory.createLineBorder(Theme.SECONDARY, 2));
        tableScrollPane.getViewport().setBackground(Theme.WHITE);
        tableScrollPane.setPreferredSize(new Dimension(0, 420));

        JPanel infoPanel = new JPanel(new GridLayout(2, 3, 16, 16));
        infoPanel.setBackground(Theme.WHITE);
        infoPanel.add(createStatCard("Today's appointments", todayAppointmentsValue, new Color(52, 152, 219)));
        infoPanel.add(createStatCard("Completed Today", completedTodayValue, new Color(46, 204, 113)));
        infoPanel.add(createStatCard("Appointments left today", appointmentsLeftTodayValue, new Color(241, 196, 15)));
        infoPanel.add(createStatCard("Tomorrow's appointments", tomorrowAppointmentsValue, new Color(155, 89, 182)));
        infoPanel.add(createStatCard("Cancelled Today", cancelledTodayValue, new Color(231, 76, 60)));
        infoPanel.add(createStatCard("Walk in", walkInValue, new Color(230, 126, 34)));

        JPanel contentPanel = new JPanel(new BorderLayout(0, 16));
        contentPanel.setBackground(Theme.WHITE);
        contentPanel.add(createHeader(), BorderLayout.NORTH);

        JPanel middlePanel = new JPanel(new BorderLayout(0, 16));
        middlePanel.setBackground(Theme.WHITE);
        middlePanel.add(infoPanel, BorderLayout.NORTH);
        middlePanel.add(createTableCard(tableScrollPane), BorderLayout.CENTER);

        contentPanel.add(middlePanel, BorderLayout.CENTER);

        JScrollPane dashboardScrollPane = new JScrollPane(contentPanel);
        dashboardScrollPane.setBorder(null);
        dashboardScrollPane.getViewport().setBackground(Theme.WHITE);
        dashboardScrollPane.getVerticalScrollBar().setUnitIncrement(16);

        add(dashboardScrollPane, BorderLayout.CENTER);

        refreshDashboard();
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Theme.WHITE);

        JPanel textWrapper = new JPanel();
        textWrapper.setLayout(new BoxLayout(textWrapper, BoxLayout.Y_AXIS));
        textWrapper.setBackground(Theme.WHITE);

        JLabel title = new JLabel("Dashboard");
        title.setFont(Theme.getFont(FontType.BOLD, 28f));
        title.setForeground(Theme.BLACK);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel subline = new JPanel();
        subline.setLayout(new BoxLayout(subline, BoxLayout.X_AXIS));
        subline.setBackground(Theme.WHITE);
        subline.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel welcome = new JLabel("Welcome back, ");
        welcome.setFont(Theme.getFont(FontType.MEDIUM, 16f));
        welcome.setForeground(Theme.BLACK);

        JLabel period = new JLabel(".");
        period.setFont(Theme.getFont(FontType.MEDIUM, 16f));
        period.setForeground(Theme.BLACK);

        subline.add(welcome);
        subline.add(nameLabel);
        subline.add(period);

        textWrapper.add(title);
        textWrapper.add(Box.createVerticalStrut(2));
        textWrapper.add(subline);

        header.add(textWrapper, BorderLayout.WEST);
        header.add(refreshButton, BorderLayout.EAST);

        return header;
    }

    private void refreshDashboard() {
        nameLabel.setText(getCurrentUserFullName());

        Database.Dashboard.Stats stats = Database.Dashboard.getStats();
        updateDashboardData(
                stats.getTodayAppointments(),
                stats.getCompletedToday(),
                stats.getAppointmentsLeftToday(),
                stats.getTomorrowAppointments(),
                stats.getCancelledToday(),
                stats.getWalkIn()
        );

        setTableData(Database.Dashboard.getTodayAppointmentsTableData());
    }

    private String getCurrentUserFullName() {
        String firstName = Database.User.getFirstName();
        String lastName = Database.User.getLastName();

        StringBuilder fullName = new StringBuilder();

        if (firstName != null && !firstName.isBlank()) {
            fullName.append(firstName.trim());
        }

        if (lastName != null && !lastName.isBlank()) {
            if (!fullName.isEmpty()) {
                fullName.append(" ");
            }
            fullName.append(lastName.trim());
        }

        return fullName.isEmpty() ? "User" : fullName.toString();
    }

    public void updateDashboardData(
            int todayAppointments,
            int completedToday,
            int appointmentsLeftToday,
            int tomorrowAppointments,
            int cancelledToday,
            int walkIn
    ) {
        todayAppointmentsValue.setText(String.valueOf(todayAppointments));
        completedTodayValue.setText(String.valueOf(completedToday));
        appointmentsLeftTodayValue.setText(String.valueOf(appointmentsLeftToday));
        tomorrowAppointmentsValue.setText(String.valueOf(tomorrowAppointments));
        cancelledTodayValue.setText(String.valueOf(cancelledToday));
        walkInValue.setText(String.valueOf(walkIn));
    }

    public void setTableData(Object[][] data) {
        if (data == null) {
            throw new IllegalArgumentException("Table data cannot be null.");
        }

        for (int row = 0; row < data.length; row++) {
            if (data[row] == null) {
                throw new IllegalArgumentException("Row " + row + " is null.");
            }

            if (data[row].length != TABLE_COLUMNS.length) {
                throw new IllegalArgumentException(
                        "Row " + row + " has " + data[row].length + " values. Expected " + TABLE_COLUMNS.length + "."
                );
            }
        }

        tableModel.setRowCount(0);

        for (Object[] row : data) {
            tableModel.addRow(row);
        }

        tableSorter.setSortKeys(null);
        columnSortStates.clear();

        for (int i = 0; i < table.getColumnCount(); i++) {
            columnSortStates.put(i, SortOrder.ASCENDING);
        }
    }

    public void sortByColumn(int columnIndex, boolean ascending) {
        if (columnIndex < 0 || columnIndex >= table.getColumnCount()) {
            throw new IllegalArgumentException("Invalid column index: " + columnIndex);
        }

        List<RowSorter.SortKey> sortKeys = new ArrayList<>();
        sortKeys.add(new RowSorter.SortKey(columnIndex, ascending ? SortOrder.ASCENDING : SortOrder.DESCENDING));
        tableSorter.setSortKeys(sortKeys);
        tableSorter.sort();
        columnSortStates.put(columnIndex, ascending ? SortOrder.ASCENDING : SortOrder.DESCENDING);
    }

    private void configureTable() {
        table.setRowHeight(36);
        table.setBackground(Theme.WHITE);
        table.setForeground(Theme.BLACK);
        table.setGridColor(Theme.SECONDARY);
        table.setShowGrid(true);
        table.setFillsViewportHeight(true);
        table.setFont(Theme.getFont(FontType.REGULAR, 13f));
        table.setSelectionBackground(Theme.HIGHLIGHT);
        table.setSelectionForeground(Theme.BLACK);
        table.setAutoCreateRowSorter(false);

        JTableHeader header = table.getTableHeader();
        header.setReorderingAllowed(false);
        header.setResizingAllowed(true);
        header.setBackground(Theme.BACKGROUND_GREEN);
        header.setForeground(Theme.BLACK);
        header.setFont(Theme.getFont(FontType.MEDIUM, 14f));
        header.setPreferredSize(new Dimension(header.getPreferredSize().width, 38));
        header.setDefaultRenderer(new DashboardHeaderRenderer(header.getDefaultRenderer()));

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        centerRenderer.setFont(Theme.getFont(FontType.REGULAR, 13f));

        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }
    }

    private void installHeaderSorting() {
        JTableHeader header = table.getTableHeader();

        for (int i = 0; i < table.getColumnCount(); i++) {
            columnSortStates.put(i, SortOrder.ASCENDING);
        }

        header.addMouseListener(new MouseInputAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int viewColumn = header.columnAtPoint(e.getPoint());
                if (viewColumn == -1) {
                    return;
                }

                int modelColumn = table.convertColumnIndexToModel(viewColumn);

                SortOrder current = columnSortStates.getOrDefault(modelColumn, SortOrder.ASCENDING);
                SortOrder next = current == SortOrder.ASCENDING ? SortOrder.DESCENDING : SortOrder.ASCENDING;

                tableSorter.setSortKeys(List.of(new RowSorter.SortKey(modelColumn, next)));
                tableSorter.sort();

                columnSortStates.put(modelColumn, next);
            }
        });
    }

    private JPanel createStatCard(String title, JLabel valueLabel, Color borderColor) {
        RoundedPanel card = new RoundedPanel(borderColor, 18, 2f);
        card.setLayout(new BorderLayout());
        card.setBackground(Theme.WHITE);
        card.setPreferredSize(new Dimension(220, 110));
        card.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(Theme.BLACK);
        titleLabel.setFont(Theme.getFont(FontType.MEDIUM, 15f));

        valueLabel.setForeground(Theme.BLACK);
        valueLabel.setFont(Theme.getFont(FontType.BOLD, 28f));

        card.add(titleLabel, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);

        return card;
    }

    private JPanel createTableCard(JScrollPane scrollPane) {
        RoundedPanel tableCard = new RoundedPanel(Theme.SECONDARY, 20, 2f);
        tableCard.setLayout(new BorderLayout(0, 12));
        tableCard.setBackground(Theme.WHITE);
        tableCard.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JLabel titleLabel = new JLabel("Today's Appointments");
        titleLabel.setForeground(Theme.BLACK);
        titleLabel.setFont(Theme.getFont(FontType.MEDIUM, 16f));

        tableCard.add(titleLabel, BorderLayout.NORTH);
        tableCard.add(scrollPane, BorderLayout.CENTER);

        return tableCard;
    }

    private JLabel createValueLabel(String text) {
        JLabel label = new JLabel(text);
        label.setOpaque(false);
        return label;
    }

    private JRoundedButton createRefreshButton() {
        JRoundedButton button = new JRoundedButton("Refresh", 10, Theme.ACCENT);
        button.setBackground(Theme.ACCENT);
        button.setForeground(Theme.WHITE);
        button.setFocusPainted(false);
        button.setFont(Theme.getFont(FontType.SEMI_BOLD, 14));
        button.setBorder(BorderFactory.createEmptyBorder(10, 18, 10, 18));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return button;
    }
}