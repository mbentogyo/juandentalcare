package dev.gracco.ui.panels;

import dev.gracco.db.Database;
import dev.gracco.ui.Theme;
import dev.gracco.ui.Theme.FontType;
import dev.gracco.ui.element.DashboardHeaderRenderer;
import dev.gracco.ui.element.JRoundedButton;
import dev.gracco.ui.element.RoundedPanel;
import dev.gracco.ui.screen.AddUserScreen;
import dev.gracco.ui.screen.EditUserScreen;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class AdminPanel extends JPanel {
    private static final int PAGE_SIZE = 10;

    private static final String[] TABLE_COLUMNS = {
            "User ID",
            "Username",
            "Changed Pass",
            "Role ID",
            "First Name",
            "Last Name",
            "Email",
            "Contact Number",
            "Is Active",
            "Created At",
            "Updated At"
    };

    private final DefaultTableModel tableModel;
    private final JTable table;
    private final JScrollPane scrollPane;

    private final JRoundedButton previousButton;
    private final JRoundedButton nextButton;
    private final JLabel pageLabel;

    private int currentPage = 0;
    private int currentRowCount = 0;

    public AdminPanel() {
        setLayout(new BorderLayout(20, 20));
        setBackground(Theme.WHITE);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        tableModel = new DefaultTableModel(new Object[0][0], TABLE_COLUMNS) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(tableModel);
        configureTable();

        scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(Theme.SECONDARY, 2));
        scrollPane.getViewport().setBackground(Theme.WHITE);

        scrollPane.getViewport().addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                updateRowHeight();
            }
        });

        previousButton = createPaginationButton("Previous");
        nextButton = createPaginationButton("Next");
        pageLabel = new JLabel();
        pageLabel.setFont(Theme.getFont(FontType.MEDIUM, 14f));
        pageLabel.setForeground(Theme.BLACK);

        previousButton.addActionListener(e -> {
            if (currentPage > 0) {
                currentPage--;
                loadPage(currentPage);
            }
        });

        nextButton.addActionListener(e -> {
            if (currentRowCount == PAGE_SIZE) {
                currentPage++;
                loadPage(currentPage);
            }
        });

        JPanel tableWrapper = createTableCard(scrollPane);

        add(createHeader(), BorderLayout.NORTH);
        add(tableWrapper, BorderLayout.CENTER);

        loadPage(0);

        SwingUtilities.invokeLater(this::updateRowHeight);
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Theme.WHITE);

        JPanel textWrapper = new JPanel();
        textWrapper.setLayout(new BoxLayout(textWrapper, BoxLayout.Y_AXIS));
        textWrapper.setBackground(Theme.WHITE);

        JLabel title = new JLabel("Admin");
        title.setFont(Theme.getFont(FontType.BOLD, 28f));
        title.setForeground(Theme.BLACK);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        textWrapper.add(title);
        header.add(textWrapper, BorderLayout.WEST);

        JRoundedButton addButton = new JRoundedButton("Add User", 10);
        addButton.setBackground(Theme.PRIMARY);
        addButton.setForeground(Theme.WHITE);
        addButton.setFocusPainted(false);
        addButton.setFont(Theme.getFont(Theme.FontType.SEMI_BOLD, 14));
        addButton.setBorder(new EmptyBorder(10, 18, 10, 18));
        addButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

        addButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                addButton.setBackground(Theme.PRIMARY_HOVER);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                addButton.setBackground(Theme.PRIMARY);
            }
        });

        addButton.addActionListener(_ -> AddUserScreen.open());

        JRoundedButton refreshButton = new JRoundedButton("Refresh", 10);
        refreshButton.setBackground(Theme.ACCENT);
        refreshButton.setForeground(Theme.WHITE);
        refreshButton.setFocusPainted(false);
        refreshButton.setFont(Theme.getFont(Theme.FontType.SEMI_BOLD, 14));
        refreshButton.setBorder(new EmptyBorder(10, 18, 10, 18));
        refreshButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

        refreshButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                refreshButton.setBackground(Theme.ACCENT_HOVER);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                refreshButton.setBackground(Theme.ACCENT);
            }
        });

        refreshButton.addActionListener(_ -> loadPage(currentPage));

        JPanel buttonWrapper = new JPanel();
        buttonWrapper.setLayout(new BoxLayout(buttonWrapper, BoxLayout.X_AXIS));
        buttonWrapper.setBackground(Theme.WHITE);
        buttonWrapper.add(refreshButton);
        buttonWrapper.add(Box.createHorizontalStrut(10));
        buttonWrapper.add(addButton);

        header.add(buttonWrapper, BorderLayout.EAST);

        return header;
    }

    private JPanel createTableCard(JScrollPane scrollPane) {
        RoundedPanel tableCard = new RoundedPanel(Theme.SECONDARY, 20, 2f);
        tableCard.setLayout(new BorderLayout(0, 12));
        tableCard.setBackground(Theme.WHITE);
        tableCard.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JLabel titleLabel = new JLabel("User Accounts");
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
                BorderFactory.createEmptyBorder(8, 14, 8, 14)
        ));
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
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && table.getSelectedRow() != -1) {
                    int selectedRow = table.getSelectedRow();
                    int modelRow = table.convertRowIndexToModel(selectedRow);
                    int userId = (int) tableModel.getValueAt(modelRow, 0);
                    EditUserScreen.open(userId);
                }
            }
        });
    }

    private void loadPage(int page) {
        currentPage = page;
        Object[][] data = Database.Admin.getAdmins(page);
        setTableData(data);
        currentRowCount = data.length;
        updatePaginationState();
        updateRowHeight();
    }

    private void setTableData(Object[][] data) {
        if (data == null) {
            data = new Object[0][0];
        }

        tableModel.setRowCount(0);

        for (Object[] row : data) {
            tableModel.addRow(row);
        }
    }

    private void updatePaginationState() {
        pageLabel.setText("Page " + (currentPage + 1));
        previousButton.setEnabled(currentPage > 0);
        nextButton.setEnabled(currentRowCount >= PAGE_SIZE);
    }

    private void updateRowHeight() {
        JViewport viewport = scrollPane.getViewport();
        int viewportHeight = viewport.getHeight();

        if (viewportHeight > 0) {
            int rowHeight = Math.max(1, viewportHeight / PAGE_SIZE);
            table.setRowHeight(rowHeight);
        }
    }
}