package dev.gracco.ui.panels;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
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

import dev.gracco.db.Database;
import dev.gracco.ui.Theme;
import dev.gracco.ui.Theme.FontType;
import dev.gracco.ui.element.DashboardHeaderRenderer;
import dev.gracco.ui.element.RoundedPanel;

public class DashboardPanel extends JPanel {
    private static final String[] TABLE_COLUMNS = {"ID", "Patient", "Dentist", "Time", "Status", "Reason"};

    private final JLabel todayAppointmentsValue;
    private final JLabel completedTodayValue;
    private final JLabel appointmentsLeftTodayValue;
    private final JLabel tomorrowAppointmentsValue;
    private final JLabel cancelledTodayValue;
    private final JLabel walkInValue;

    private final DefaultTableModel tableModel;
    private final JTable table;
    private final TableRowSorter<DefaultTableModel> tableSorter;
    private final Map<Integer, SortOrder> columnSortStates = new HashMap<>();

    public DashboardPanel() {
        setLayout(new BorderLayout(20, 20));
        setBackground(Theme.WHITE);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel infoPanel = new JPanel(new java.awt.GridLayout(2, 3, 16, 16));
        infoPanel.setBackground(Theme.WHITE);

        todayAppointmentsValue = createValueLabel("0");
        completedTodayValue = createValueLabel("0");
        appointmentsLeftTodayValue = createValueLabel("0");
        tomorrowAppointmentsValue = createValueLabel("0");
        cancelledTodayValue = createValueLabel("0");
        walkInValue = createValueLabel("0");

        infoPanel.add(createStatCard("Today's appointments", todayAppointmentsValue, new Color(52, 152, 219)));
        infoPanel.add(createStatCard("Completed Today", completedTodayValue, new Color(46, 204, 113)));
        infoPanel.add(createStatCard("Appointments left today", appointmentsLeftTodayValue, new Color(241, 196, 15)));
        infoPanel.add(createStatCard("Tomorrow's appointments", tomorrowAppointmentsValue, new Color(155, 89, 182)));
        infoPanel.add(createStatCard("Cancelled Today", cancelledTodayValue, new Color(231, 76, 60)));
        infoPanel.add(createStatCard("Walk in", walkInValue, new Color(230, 126, 34)));

        Object[][] data = new Object[0][0];

        tableModel = new DefaultTableModel(data, TABLE_COLUMNS) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(tableModel);
        tableSorter = new TableRowSorter<>(tableModel);
        table.setRowSorter(tableSorter);

        configureTable();
        installHeaderSorting();

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(Theme.SECONDARY, 2));
        scrollPane.getViewport().setBackground(Theme.WHITE);

        JPanel tableWrapper = createTableCard(scrollPane);

        JPanel topWrapper = new JPanel(new BorderLayout(0, 16));
        topWrapper.setBackground(Theme.WHITE);
        topWrapper.add(createHeader(), BorderLayout.NORTH);
        topWrapper.add(infoPanel, BorderLayout.CENTER);

        add(topWrapper, BorderLayout.NORTH);
        add(tableWrapper, BorderLayout.CENTER);

        loadDashboardData();
    }

    private void loadDashboardData() {
        int[] stats = Database.Appointment.getDashboardStats();
        updateDashboardData(stats[0], stats[1], stats[2], stats[3], stats[4], stats[5]);
        Object[][] todayRows = Database.Appointment.getTodayAppointments();
        tableModel.setRowCount(0);
        for (Object[] row : todayRows) tableModel.addRow(row);
    }

    private JPanel createHeader() {
        JPanel header = new JPanel();
        header.setLayout(new BorderLayout());
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

        JLabel name = new JLabel(Database.User.getFirstName());
        name.setFont(Theme.getFont(FontType.SEMI_BOLD, 16f));
        name.setForeground(Theme.BLACK);

        JLabel period = new JLabel(".");
        period.setFont(Theme.getFont(FontType.MEDIUM, 16f));
        period.setForeground(Theme.BLACK);

        subline.add(welcome);
        subline.add(name);
        subline.add(period);

        textWrapper.add(title);
        textWrapper.add(subline);

        header.add(textWrapper, BorderLayout.WEST);

        return header;
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
                if (viewColumn == -1) return;

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
}