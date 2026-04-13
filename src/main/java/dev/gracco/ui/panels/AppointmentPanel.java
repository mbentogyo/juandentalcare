package dev.gracco.ui.panels;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

import dev.gracco.db.Database;
import dev.gracco.db.Enums;
import dev.gracco.ui.Theme;
import dev.gracco.ui.Theme.FontType;
import dev.gracco.ui.element.DashboardHeaderRenderer;
import dev.gracco.ui.element.JRoundedButton;
import dev.gracco.ui.element.RoundedPanel;
import dev.gracco.ui.screen.AddAppointmentScreen;
import dev.gracco.ui.screen.EditAppointmentScreen;

public class AppointmentPanel extends JPanel {
    private static final int PAGE_SIZE = 10;
    private static final String[] TABLE_COLUMNS = {
            "ID", "Patient", "Dentist", "Date", "Time", "Status", "Reason", "Notes"
    };

    private final DefaultTableModel tableModel;
    private final JTable table;
    private final JScrollPane scrollPane;
    private final JRoundedButton previousButton;
    private final JRoundedButton nextButton;
    private final JLabel pageLabel;

    private int currentPage = 0;
    private int currentRowCount = 0;

    // hidden columns store patient_id and dentist_user_id at indices 8 and 9
    private Object[][] rawData = new Object[0][0];

    public AppointmentPanel() {
        setLayout(new BorderLayout(20, 20));
        setBackground(Theme.WHITE);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        tableModel = new DefaultTableModel(new Object[0][0], TABLE_COLUMNS) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };

        table = new JTable(tableModel);
        configureTable();

        scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(Theme.SECONDARY, 2));
        scrollPane.getViewport().setBackground(Theme.WHITE);
        scrollPane.getViewport().addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) { updateRowHeight(); }
        });

        previousButton = createPaginationButton("Previous");
        nextButton = createPaginationButton("Next");
        pageLabel = new JLabel();
        pageLabel.setFont(Theme.getFont(FontType.MEDIUM, 14f));
        pageLabel.setForeground(Theme.BLACK);

        previousButton.addActionListener(e -> { if (currentPage > 0) { currentPage--; loadPage(currentPage); } });
        nextButton.addActionListener(e -> { if (currentRowCount == PAGE_SIZE) { currentPage++; loadPage(currentPage); } });

        add(createHeader(), BorderLayout.NORTH);
        add(createTableCard(scrollPane), BorderLayout.CENTER);

        loadPage(0);
        SwingUtilities.invokeLater(this::updateRowHeight);
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Theme.WHITE);

        JLabel title = new JLabel("Appointments");
        title.setFont(Theme.getFont(FontType.BOLD, 28f));
        title.setForeground(Theme.BLACK);

        JPanel buttonWrapper = new JPanel();
        buttonWrapper.setLayout(new BoxLayout(buttonWrapper, BoxLayout.X_AXIS));
        buttonWrapper.setBackground(Theme.WHITE);

        JRoundedButton refreshButton = new JRoundedButton("Refresh", 10);
        refreshButton.setBackground(Theme.ACCENT);
        refreshButton.setForeground(Theme.WHITE);
        refreshButton.setFocusPainted(false);
        refreshButton.setFont(Theme.getFont(FontType.SEMI_BOLD, 14));
        refreshButton.setBorder(new EmptyBorder(10, 18, 10, 18));
        refreshButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        refreshButton.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { refreshButton.setBackground(Theme.ACCENT_HOVER); }
            public void mouseExited(MouseEvent e) { refreshButton.setBackground(Theme.ACCENT); }
        });
        refreshButton.addActionListener(e -> loadPage(currentPage));

        buttonWrapper.add(refreshButton);

        // Only Admin and Clerk can add appointments
        if (Database.User.getRole() != Enums.Role.DENTIST) {
            JRoundedButton addButton = new JRoundedButton("Add Appointment", 10);
            addButton.setBackground(Theme.PRIMARY);
            addButton.setForeground(Theme.WHITE);
            addButton.setFocusPainted(false);
            addButton.setFont(Theme.getFont(FontType.SEMI_BOLD, 14));
            addButton.setBorder(new EmptyBorder(10, 18, 10, 18));
            addButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
            addButton.addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { addButton.setBackground(Theme.PRIMARY_HOVER); }
                public void mouseExited(MouseEvent e) { addButton.setBackground(Theme.PRIMARY); }
            });
            addButton.addActionListener(e -> AddAppointmentScreen.open());
            buttonWrapper.add(Box.createHorizontalStrut(10));
            buttonWrapper.add(addButton);
        }

        header.add(title, BorderLayout.WEST);
        header.add(buttonWrapper, BorderLayout.EAST);
        return header;
    }

    private JPanel createTableCard(JScrollPane scrollPane) {
        RoundedPanel tableCard = new RoundedPanel(Theme.SECONDARY, 20, 2f);
        tableCard.setLayout(new BorderLayout(0, 12));
        tableCard.setBackground(Theme.WHITE);
        tableCard.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JLabel titleLabel = new JLabel("Appointment Records");
        titleLabel.setForeground(Theme.BLACK);
        titleLabel.setFont(Theme.getFont(FontType.MEDIUM, 16f));

        JPanel paginationPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        paginationPanel.setBackground(Theme.WHITE);
        paginationPanel.add(previousButton);
        paginationPanel.add(pageLabel);
        paginationPanel.add(nextButton);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBackground(Theme.WHITE);
        bottomPanel.add(Box.createVerticalStrut(4), BorderLayout.NORTH);
        bottomPanel.add(paginationPanel, BorderLayout.EAST);

        tableCard.add(titleLabel, BorderLayout.NORTH);
        tableCard.add(scrollPane, BorderLayout.CENTER);
        tableCard.add(bottomPanel, BorderLayout.SOUTH);
        return tableCard;
    }

    private JRoundedButton createPaginationButton(String text) {
        JRoundedButton button = new JRoundedButton(text, 10);
        button.setFocusPainted(false);
        button.setFont(Theme.getFont(FontType.MEDIUM, 13f));
        button.setBackground(Theme.WHITE);
        button.setForeground(Theme.BLACK);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.SECONDARY, 2),
                BorderFactory.createEmptyBorder(8, 14, 8, 14)));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return button;
    }

    private void configureTable() {
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

        table.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && table.getSelectedRow() != -1) {
                    int modelRow = table.convertRowIndexToModel(table.getSelectedRow());
                    int appointmentId = (int) tableModel.getValueAt(modelRow, 0);
                    EditAppointmentScreen.open(appointmentId);
                }
            }
        });
    }

    private void loadPage(int page) {
        currentPage = page;
        Integer dentistFilter = Database.User.getRole() == Enums.Role.DENTIST
                ? Database.User.getUserId() : null;
        rawData = Database.Appointment.getAppointments(page, dentistFilter);
        setTableData(rawData);
        currentRowCount = rawData.length;
        updatePaginationState();
        updateRowHeight();
    }

    private void setTableData(Object[][] data) {
        tableModel.setRowCount(0);
        for (Object[] row : data) {
            // columns 0-7 are display columns, 8 and 9 are hidden IDs
            tableModel.addRow(new Object[]{
                    row[0], row[1], row[2], row[3], row[4], row[5], row[6], row[7]
            });
        }
    }

    private void updatePaginationState() {
        pageLabel.setText("Page " + (currentPage + 1));
        previousButton.setEnabled(currentPage > 0);
        nextButton.setEnabled(currentRowCount >= PAGE_SIZE);
    }

    private void updateRowHeight() {
        int viewportHeight = scrollPane.getViewport().getHeight();
        if (viewportHeight > 0) {
            table.setRowHeight(Math.max(1, viewportHeight / PAGE_SIZE));
        }
    }
}
