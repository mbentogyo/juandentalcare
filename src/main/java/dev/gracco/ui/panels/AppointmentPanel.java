package dev.gracco.ui.panels;

import dev.gracco.db.Database;
import dev.gracco.ui.Theme;
import dev.gracco.ui.Theme.FontType;
import dev.gracco.ui.element.DashboardHeaderRenderer;
import dev.gracco.ui.element.JRoundedButton;
import dev.gracco.ui.element.RoundedPanel;
import dev.gracco.ui.screen.AddAppointmentScreen;
import dev.gracco.ui.screen.EditAppointmentScreen;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;

public class AppointmentPanel extends JPanel {
    private static final int PAGE_SIZE = 10;
    private static final String DATE_HINT = "MM/DD/YYYY";
    private static final String TIME_HINT = "HH:MM";

    private static final DateTimeFormatter INPUT_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("MM/dd/uuuu").withResolverStyle(ResolverStyle.STRICT);

    private static final DateTimeFormatter INPUT_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm").withResolverStyle(ResolverStyle.STRICT);

    private static final String[] TABLE_COLUMNS = {
            "Appointment ID",
            "Patient",
            "Dentist",
            "Scheduled Date",
            "Scheduled Time",
            "Status",
            "Reason for Visit",
            "Notes",
            "Created By",
            "Updated By",
            "Created At",
            "Updated At"
    };

    private final DefaultTableModel tableModel;
    private final JTable table;
    private final JScrollPane scrollPane;

    private final JRoundedButton previousButton;
    private final JRoundedButton nextButton;
    private final JRoundedButton refreshButton;
    private final JLabel pageLabel;

    private final JTextField appointmentIdField;
    private final JTextField patientField;
    private final JTextField dentistField;
    private final JTextField scheduledDateField;
    private final JTextField scheduledTimeField;
    private final JComboBox<String> statusBox;
    private final JTextField reasonForVisitField;
    private final JTextField notesField;
    private final JTextField createdByField;
    private final JTextField updatedByField;

    private final JDialog searchPopup;

    private int currentPage = 0;
    private int currentRowCount = 0;

    public AppointmentPanel() {
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
        installTableDoubleClickHandler();

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
        refreshButton = createRefreshButton();
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

        refreshButton.addActionListener(e -> loadPage(currentPage));

        appointmentIdField = createSearchTextField();
        patientField = createSearchTextField();
        dentistField = createSearchTextField();
        scheduledDateField = createSearchTextField();
        scheduledTimeField = createSearchTextField();
        reasonForVisitField = createSearchTextField();
        notesField = createSearchTextField();
        createdByField = createSearchTextField();
        updatedByField = createSearchTextField();

        installDateMask(scheduledDateField);
        installPlaceholder(scheduledDateField, DATE_HINT);

        installTimeMask(scheduledTimeField);
        installPlaceholder(scheduledTimeField, TIME_HINT);

        statusBox = new JComboBox<>(new String[]{
                "",
                "Confirmed",
                "Cancelled",
                "Booked",
                "Rescheduled",
                "No Show"
        });
        statusBox.setFont(Theme.getFont(FontType.REGULAR, 14));
        statusBox.setBackground(Theme.WHITE);
        statusBox.setForeground(Theme.BLACK);
        statusBox.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.SECONDARY, 1),
                new EmptyBorder(6, 8, 6, 8)
        ));

        searchPopup = createSearchPopup();

        JPanel tableWrapper = createTableCard(scrollPane);

        add(createHeader(), BorderLayout.NORTH);
        add(tableWrapper, BorderLayout.CENTER);

        loadPage(0);
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Theme.WHITE);

        JLabel title = new JLabel("Appointments");
        title.setFont(Theme.getFont(FontType.BOLD, 28f));
        title.setForeground(Theme.BLACK);

        JRoundedButton addButton = new JRoundedButton("Add Appointment", 10);
        addButton.setBackground(Theme.PRIMARY);
        addButton.setForeground(Theme.WHITE);
        addButton.setFocusPainted(false);
        addButton.setFont(Theme.getFont(Theme.FontType.SEMI_BOLD, 14));
        addButton.setBorder(new EmptyBorder(10, 18, 10, 18));
        addButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

        addButton.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                addButton.setBackground(Theme.PRIMARY_HOVER);
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                addButton.setBackground(Theme.PRIMARY);
            }
        });

        addButton.addActionListener(_ -> AddAppointmentScreen.open());

        JRoundedButton searchButton = new JRoundedButton("Search", 10, Theme.ACCENT);
        searchButton.setBackground(Theme.WHITE);
        searchButton.setForeground(Theme.BLACK);
        searchButton.setFocusPainted(false);
        searchButton.setFont(Theme.getFont(Theme.FontType.SEMI_BOLD, 14));
        searchButton.setBorder(new EmptyBorder(10, 18, 10, 18));
        searchButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        searchButton.addActionListener(e -> toggleSearchPopup(searchButton));

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

        JPanel buttonWrapper = new JPanel();
        buttonWrapper.setLayout(new BoxLayout(buttonWrapper, BoxLayout.X_AXIS));
        buttonWrapper.setBackground(Theme.WHITE);
        buttonWrapper.add(searchButton);
        buttonWrapper.add(Box.createHorizontalStrut(10));
        buttonWrapper.add(refreshButton);
        buttonWrapper.add(Box.createHorizontalStrut(10));
        buttonWrapper.add(addButton);

        header.add(title, BorderLayout.WEST);
        header.add(buttonWrapper, BorderLayout.EAST);

        return header;
    }

    private JDialog createSearchPopup() {
        Window owner = SwingUtilities.getWindowAncestor(this);

        JDialog dialog = owner == null ? new JDialog() : new JDialog(owner);
        dialog.setUndecorated(true);
        dialog.setModal(false);
        dialog.setAlwaysOnTop(false);
        dialog.setFocusableWindowState(true);
        dialog.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
        dialog.setBackground(new Color(0, 0, 0, 0));

        RoundedPanel wrapper = new RoundedPanel(Theme.ACCENT, 20, 2f);
        wrapper.setLayout(new BorderLayout(0, 12));
        wrapper.setBackground(Theme.WHITE);
        wrapper.setBorder(new EmptyBorder(16, 16, 16, 16));
        wrapper.setPreferredSize(new Dimension(1040, 390));

        JLabel title = new JLabel("Search Filters");
        title.setForeground(Theme.BLACK);
        title.setFont(Theme.getFont(FontType.MEDIUM, 16f));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel fieldsPanel = new JPanel();
        fieldsPanel.setLayout(new BoxLayout(fieldsPanel, BoxLayout.Y_AXIS));
        fieldsPanel.setBackground(Theme.WHITE);

        JPanel firstRow = new JPanel(new GridLayout(1, 4, 14, 0));
        firstRow.setBackground(Theme.WHITE);
        firstRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        firstRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 72));
        firstRow.add(createSearchFieldPanel("Appointment ID", appointmentIdField));
        firstRow.add(createSearchFieldPanel("Patient Name", patientField));
        firstRow.add(createSearchFieldPanel("Dentist Name", dentistField));
        firstRow.add(createSearchFieldPanel("Status", statusBox));

        JPanel secondRow = new JPanel(new GridLayout(1, 4, 14, 0));
        secondRow.setBackground(Theme.WHITE);
        secondRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        secondRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 72));
        secondRow.add(createSearchFieldPanel("Scheduled Date", scheduledDateField));
        secondRow.add(createSearchFieldPanel("Scheduled Time", scheduledTimeField));
        secondRow.add(createSearchFieldPanel("Created By", createdByField));
        secondRow.add(createSearchFieldPanel("Updated By", updatedByField));

        JPanel thirdRow = new JPanel(new GridLayout(1, 2, 14, 0));
        thirdRow.setBackground(Theme.WHITE);
        thirdRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        thirdRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 72));
        thirdRow.add(createSearchFieldPanel("Reason for Visit", reasonForVisitField));
        thirdRow.add(createSearchFieldPanel("Notes", notesField));

        JRoundedButton applyButton = new JRoundedButton("Apply Search", 10);
        applyButton.setBackground(Theme.PRIMARY);
        applyButton.setForeground(Theme.WHITE);
        applyButton.setFocusPainted(false);
        applyButton.setFont(Theme.getFont(FontType.SEMI_BOLD, 14));
        applyButton.setBorder(new EmptyBorder(10, 18, 10, 18));
        applyButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        applyButton.addActionListener(e -> {
            if (!isScheduledDateValid() || !isScheduledTimeValid()) {
                return;
            }
            currentPage = 0;
            loadPage(0);
            searchPopup.setVisible(false);
        });

        JRoundedButton resetButton = new JRoundedButton("Reset Search Settings", 10);
        resetButton.setBackground(Theme.ACCENT);
        resetButton.setForeground(Theme.WHITE);
        resetButton.setFocusPainted(false);
        resetButton.setFont(Theme.getFont(FontType.SEMI_BOLD, 14));
        resetButton.setBorder(new EmptyBorder(10, 18, 10, 18));
        resetButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        resetButton.addActionListener(e -> {
            appointmentIdField.setText("");
            patientField.setText("");
            dentistField.setText("");
            scheduledDateField.setForeground(Color.GRAY);
            scheduledDateField.setText(DATE_HINT);
            scheduledTimeField.setForeground(Color.GRAY);
            scheduledTimeField.setText(TIME_HINT);
            statusBox.setSelectedIndex(0);
            reasonForVisitField.setText("");
            notesField.setText("");
            createdByField.setText("");
            updatedByField.setText("");
            currentPage = 0;
            loadPage(0);
        });

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.setBackground(Theme.WHITE);
        buttonPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        buttonPanel.add(Box.createHorizontalGlue());
        buttonPanel.add(resetButton);
        buttonPanel.add(Box.createHorizontalStrut(10));
        buttonPanel.add(applyButton);

        JPanel fourthRow = new JPanel(new BorderLayout());
        fourthRow.setBackground(Theme.WHITE);
        fourthRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        fourthRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        fourthRow.add(buttonPanel, BorderLayout.EAST);

        fieldsPanel.add(firstRow);
        fieldsPanel.add(Box.createVerticalStrut(14));
        fieldsPanel.add(secondRow);
        fieldsPanel.add(Box.createVerticalStrut(14));
        fieldsPanel.add(thirdRow);
        fieldsPanel.add(Box.createVerticalStrut(14));
        fieldsPanel.add(fourthRow);

        wrapper.add(title, BorderLayout.NORTH);
        wrapper.add(fieldsPanel, BorderLayout.CENTER);

        dialog.setContentPane(wrapper);
        dialog.pack();

        return dialog;
    }

    private void toggleSearchPopup(Component anchor) {
        if (searchPopup.isVisible()) {
            searchPopup.setVisible(false);
            return;
        }

        Window window = SwingUtilities.getWindowAncestor(this);
        if (window == null) {
            return;
        }

        searchPopup.pack();

        Point anchorOnScreen = anchor.getLocationOnScreen();
        int popupWidth = searchPopup.getWidth();
        int x = window.getX() + window.getWidth() - popupWidth - 20;
        int y = anchorOnScreen.y + anchor.getHeight() + 8;

        searchPopup.setLocation(x, y);
        searchPopup.setVisible(true);

        SwingUtilities.invokeLater(() -> {
            scheduledDateField.revalidate();
            scheduledDateField.repaint();
            scheduledTimeField.revalidate();
            scheduledTimeField.repaint();
        });
    }

    private JPanel createSearchFieldPanel(String labelText, Component field) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Theme.WHITE);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel label = new JLabel(labelText);
        label.setFont(Theme.getFont(FontType.MEDIUM, 13));
        label.setForeground(Theme.BLACK);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setHorizontalAlignment(JLabel.LEFT);

        Dimension labelPreferred = label.getPreferredSize();
        label.setPreferredSize(new Dimension(labelPreferred.width, labelPreferred.height));
        label.setMinimumSize(new Dimension(0, labelPreferred.height));
        label.setMaximumSize(new Dimension(Integer.MAX_VALUE, labelPreferred.height));

        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        field.setPreferredSize(new Dimension(250, 42));
        field.setMinimumSize(new Dimension(120, 42));

        if (field instanceof JTextField textField) {
            textField.setAlignmentX(Component.LEFT_ALIGNMENT);
            textField.setHorizontalAlignment(JTextField.LEFT);
        } else if (field instanceof JComboBox<?> comboBox) {
            comboBox.setAlignmentX(Component.LEFT_ALIGNMENT);
            comboBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        } else if (field instanceof JRoundedButton button) {
            button.setAlignmentX(Component.LEFT_ALIGNMENT);
        }

        panel.add(label);
        panel.add(Box.createVerticalStrut(6));
        panel.add(field);

        return panel;
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
        button.setBorder(new EmptyBorder(8, 14, 8, 14));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return button;
    }

    private JRoundedButton createRefreshButton() {
        JRoundedButton button = new JRoundedButton("Refresh", 10, Theme.ACCENT);
        button.setBackground(Theme.ACCENT);
        button.setForeground(Theme.WHITE);
        button.setFocusPainted(false);
        button.setFont(Theme.getFont(FontType.SEMI_BOLD, 14));
        button.setBorder(new EmptyBorder(10, 18, 10, 18));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return button;
    }

    private JTextField createSearchTextField() {
        JTextField textField = new JTextField();
        textField.setFont(Theme.getFont(FontType.REGULAR, 14));
        textField.setBackground(Theme.WHITE);
        textField.setForeground(Theme.BLACK);
        textField.setHorizontalAlignment(JTextField.LEFT);
        textField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.SECONDARY, 1),
                new EmptyBorder(10, 12, 10, 12)
        ));
        return textField;
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
    }

    private void loadPage(int page) {
        currentPage = page;

        Object[][] data = Database.Appointment.getAppointments(
                page,
                appointmentIdField.getText().trim(),
                patientField.getText().trim(),
                dentistField.getText().trim(),
                getScheduledDateForSearch(),
                getScheduledTimeForSearch(),
                getSelectedStatus(),
                reasonForVisitField.getText().trim(),
                notesField.getText().trim(),
                createdByField.getText().trim(),
                updatedByField.getText().trim()
        );

        setTableData(data);
        currentRowCount = data.length;
        updatePaginationState();
        updateRowHeight();
    }

    private String getSelectedStatus() {
        Object value = statusBox.getSelectedItem();
        return value == null ? "" : value.toString().trim();
    }

    private String getScheduledDateForSearch() {
        if (scheduledDateField.getForeground().equals(Color.GRAY) && DATE_HINT.equals(scheduledDateField.getText())) {
            return "";
        }

        String text = scheduledDateField.getText().trim();
        if (text.isEmpty()) {
            return "";
        }

        try {
            LocalDate date = LocalDate.parse(text, INPUT_DATE_FORMATTER);
            return date.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private String getScheduledTimeForSearch() {
        if (scheduledTimeField.getForeground().equals(Color.GRAY) && TIME_HINT.equals(scheduledTimeField.getText())) {
            return "";
        }

        String text = scheduledTimeField.getText().trim();
        if (text.isEmpty()) {
            return "";
        }

        try {
            LocalTime time = LocalTime.parse(text, INPUT_TIME_FORMATTER);
            return time.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private boolean isScheduledDateValid() {
        if (scheduledDateField.getForeground().equals(Color.GRAY) && DATE_HINT.equals(scheduledDateField.getText())) {
            return true;
        }

        String text = scheduledDateField.getText().trim();
        if (text.isEmpty()) {
            return true;
        }

        try {
            LocalDate.parse(text, INPUT_DATE_FORMATTER);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isScheduledTimeValid() {
        if (scheduledTimeField.getForeground().equals(Color.GRAY) && TIME_HINT.equals(scheduledTimeField.getText())) {
            return true;
        }

        String text = scheduledTimeField.getText().trim();
        if (text.isEmpty()) {
            return true;
        }

        try {
            LocalTime.parse(text, INPUT_TIME_FORMATTER);
            return true;
        } catch (Exception e) {
            return false;
        }
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

    private void installPlaceholder(JTextField field, String placeholder) {
        field.setText(placeholder);
        field.setForeground(Color.GRAY);

        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (field.getText().equals(placeholder) && field.getForeground().equals(Color.GRAY)) {
                    field.setText("");
                    field.setForeground(Theme.BLACK);
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (field.getText().trim().isEmpty()) {
                    field.setText(placeholder);
                    field.setForeground(Color.GRAY);
                }
            }
        });
    }

    private void installDateMask(JTextField field) {
        ((AbstractDocument) field.getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
                String currentText = fb.getDocument().getText(0, fb.getDocument().getLength());

                if (DATE_HINT.equals(currentText) && field.getForeground().equals(Color.GRAY)) {
                    currentText = "";
                    offset = 0;
                    length = fb.getDocument().getLength();
                }

                StringBuilder raw = new StringBuilder(currentText.replace("/", ""));
                String replacement = text == null ? "" : text.replaceAll("[^0-9]", "");

                int rawOffset = Math.min(offset - countCharsBefore(currentText, offset, '/'), raw.length());
                int rawLength = Math.min(length, raw.length() - rawOffset);

                raw.replace(rawOffset, rawOffset + rawLength, replacement);

                if (raw.length() > 8) {
                    raw.setLength(8);
                }

                String formatted = formatDateDigits(raw.toString());

                fb.replace(0, fb.getDocument().getLength(), formatted, attrs);

                if (!formatted.isEmpty()) {
                    field.setForeground(Theme.BLACK);
                }
            }

            @Override
            public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
                replace(fb, offset, 0, string, attr);
            }

            @Override
            public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {
                replace(fb, offset, length, "", null);
            }
        });
    }

    private void installTimeMask(JTextField field) {
        ((AbstractDocument) field.getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
                String currentText = fb.getDocument().getText(0, fb.getDocument().getLength());

                if (TIME_HINT.equals(currentText) && field.getForeground().equals(Color.GRAY)) {
                    currentText = "";
                    offset = 0;
                    length = fb.getDocument().getLength();
                }

                StringBuilder raw = new StringBuilder(currentText.replace(":", ""));
                String replacement = text == null ? "" : text.replaceAll("[^0-9]", "");

                int rawOffset = Math.min(offset - countCharsBefore(currentText, offset, ':'), raw.length());
                int rawLength = Math.min(length, raw.length() - rawOffset);

                raw.replace(rawOffset, rawOffset + rawLength, replacement);

                if (raw.length() > 4) {
                    raw.setLength(4);
                }

                String formatted = formatTimeDigits(raw.toString());

                fb.replace(0, fb.getDocument().getLength(), formatted, attrs);

                if (!formatted.isEmpty()) {
                    field.setForeground(Theme.BLACK);
                }
            }

            @Override
            public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
                replace(fb, offset, 0, string, attr);
            }

            @Override
            public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {
                replace(fb, offset, length, "", null);
            }
        });
    }

    private int countCharsBefore(String text, int offset, char target) {
        int count = 0;
        for (int i = 0; i < Math.min(offset, text.length()); i++) {
            if (text.charAt(i) == target) {
                count++;
            }
        }
        return count;
    }

    private String formatDateDigits(String digits) {
        StringBuilder formatted = new StringBuilder();

        for (int i = 0; i < digits.length(); i++) {
            if (i == 2 || i == 4) {
                formatted.append('/');
            }
            formatted.append(digits.charAt(i));
        }

        return formatted.toString();
    }

    private String formatTimeDigits(String digits) {
        StringBuilder formatted = new StringBuilder();

        for (int i = 0; i < digits.length(); i++) {
            if (i == 2) {
                formatted.append(':');
            }
            formatted.append(digits.charAt(i));
        }

        return formatted.toString();
    }

    private void installTableDoubleClickHandler() {
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!SwingUtilities.isLeftMouseButton(e) || e.getClickCount() != 2) {
                    return;
                }

                int viewRow = table.rowAtPoint(e.getPoint());
                if (viewRow < 0) {
                    return;
                }

                int modelRow = table.convertRowIndexToModel(viewRow);
                Object value = tableModel.getValueAt(modelRow, 0);

                if (value == null) {
                    return;
                }

                int appointmentId;
                try {
                    appointmentId = Integer.parseInt(value.toString());
                } catch (NumberFormatException ex) {
                    return;
                }

                EditAppointmentScreen.open(appointmentId, () -> loadPage(currentPage));
            }
        });
    }
}