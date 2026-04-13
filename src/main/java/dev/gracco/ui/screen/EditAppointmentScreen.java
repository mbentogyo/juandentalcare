package dev.gracco.ui.screen;

import dev.gracco.db.Database;
import dev.gracco.db.Enums;
import dev.gracco.ui.Alert;
import dev.gracco.ui.Theme;
import dev.gracco.ui.element.JRoundedButton;
import dev.gracco.ui.element.JRoundedPanel;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.Date;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class EditAppointmentScreen extends JFrame {
    private static final String DATE_HINT = "MM/DD/YYYY";
    private static final String TIME_HINT = "HH:MM AM/PM";

    private static final DateTimeFormatter INPUT_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("MM/dd/uuuu").withResolverStyle(ResolverStyle.STRICT);

    private static final DateTimeFormatter INPUT_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("hh:mm a").withResolverStyle(ResolverStyle.STRICT);

    private static EditAppointmentScreen instance;
    private static int currentAppointmentId = -1;
    private static Runnable onSaveCallback;

    public static void open(int appointmentId, Runnable onSave) {
        if (instance == null) {
            instance = new EditAppointmentScreen(appointmentId, onSave);
        } else {
            if (currentAppointmentId != appointmentId) {
                instance.dispose();
                instance = new EditAppointmentScreen(appointmentId, onSave);
            } else {
                onSaveCallback = onSave;
                instance.toFront();
                instance.requestFocus();
            }
        }
    }

    private EditAppointmentScreen(int appointmentId, Runnable onSave) {
        currentAppointmentId = appointmentId;
        onSaveCallback = onSave;

        Object[] appointmentData = Database.Appointment.getAppointmentById(appointmentId);
        if (appointmentData == null) {
            Alert.error("Appointment not found.", null);
            instance = null;
            currentAppointmentId = -1;
            onSaveCallback = null;
            return;
        }

        setTitle("Edit Appointment");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setSize(760, 610);
        setLocationRelativeTo(null);
        setResizable(false);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                instance = null;
                currentAppointmentId = -1;
                onSaveCallback = null;
            }
        });

        JPanel root = new JPanel(new GridBagLayout());
        root.setBackground(Theme.BACKGROUND_GREEN);

        JPanel card = new JRoundedPanel(20, 1, Theme.SECONDARY);
        card.setBorder(new EmptyBorder(30, 30, 30, 30));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Theme.WHITE);
        card.setPreferredSize(new Dimension(650, 480));

        Dimension fieldSize = new Dimension(250, 42);

        JLabel title = new JLabel("Edit Appointment");
        title.setFont(Theme.getFont(Theme.FontType.MEDIUM, 24));
        title.setForeground(Theme.BLACK);
        title.setHorizontalAlignment(SwingConstants.CENTER);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JTextField patientNameField = createTextField(fieldSize);
        patientNameField.setEditable(false);
        patientNameField.setFocusable(false);
        patientNameField.setBackground(Theme.HIGHLIGHT);

        JComboBox<String> dentistBox = new JComboBox<>();
        dentistBox.setMaximumSize(fieldSize);
        dentistBox.setPreferredSize(fieldSize);
        dentistBox.setMinimumSize(fieldSize);
        dentistBox.setFont(Theme.getFont(Theme.FontType.MEDIUM, 14));
        dentistBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        dentistBox.setBackground(Theme.WHITE);
        dentistBox.setForeground(Theme.BLACK);
        dentistBox.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.SECONDARY, 1),
                new EmptyBorder(4, 8, 4, 8)
        ));

        JTextField scheduledDateField = createTextField(fieldSize);
        JTextField scheduledTimeField = createTextField(fieldSize);
        JTextField reasonField = createTextField(fieldSize);
        JTextField notesField = createTextField(fieldSize);

        installDateMask(scheduledDateField);
        installPlaceholder(scheduledDateField, DATE_HINT);
        installPlaceholder(scheduledTimeField, TIME_HINT);

        JComboBox<Enums.Status> statusBox = new JComboBox<>(Enums.Status.values());
        statusBox.setMaximumSize(fieldSize);
        statusBox.setPreferredSize(fieldSize);
        statusBox.setMinimumSize(fieldSize);
        statusBox.setFont(Theme.getFont(Theme.FontType.MEDIUM, 14));
        statusBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        statusBox.setBackground(Theme.WHITE);
        statusBox.setForeground(Theme.BLACK);
        statusBox.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.SECONDARY, 1),
                new EmptyBorder(4, 8, 4, 8)
        ));

        int currentDentistId = ((Number) appointmentData[3]).intValue();
        String currentDentistName = appointmentData[4] == null ? "Unknown Dentist" : appointmentData[4].toString();

        Map<String, Integer> dentistMap = new LinkedHashMap<>();

        List<Object[]> dentists = Database.Appointment.getDentists();
        for (Object[] dentist : dentists) {
            int dentistId = ((Number) dentist[0]).intValue();
            String label = dentist[1].toString() + " (#" + dentistId + ")";
            dentistMap.put(label, dentistId);
            dentistBox.addItem(label);
        }

        String selectedDentistLabel = null;
        for (Map.Entry<String, Integer> entry : dentistMap.entrySet()) {
            if (entry.getValue() == currentDentistId) {
                selectedDentistLabel = entry.getKey();
                break;
            }
        }

        if (selectedDentistLabel == null) {
            selectedDentistLabel = currentDentistName + " (#" + currentDentistId + ")";
            dentistMap.put(selectedDentistLabel, currentDentistId);
            dentistBox.addItem(selectedDentistLabel);
        }

        patientNameField.setText(appointmentData[2] == null ? "" : appointmentData[2].toString());
        dentistBox.setSelectedItem(selectedDentistLabel);

        Date scheduledDate = (Date) appointmentData[5];
        if (scheduledDate != null) {
            scheduledDateField.setForeground(Theme.BLACK);
            scheduledDateField.setText(scheduledDate.toLocalDate().format(INPUT_DATE_FORMATTER));
        }

        Time scheduledTime = (Time) appointmentData[6];
        if (scheduledTime != null) {
            scheduledTimeField.setForeground(Theme.BLACK);
            scheduledTimeField.setText(scheduledTime.toLocalTime().format(INPUT_TIME_FORMATTER));
        }

        if (appointmentData[7] != null) {
            String currentStatus = appointmentData[7].toString().trim();

            for (Enums.Status statusValue : Enums.Status.values()) {
                if (statusValue.toString().equalsIgnoreCase(currentStatus)) {
                    statusBox.setSelectedItem(statusValue);
                    break;
                }
            }
        }

        reasonField.setText(appointmentData[8] == null ? "" : appointmentData[8].toString());
        notesField.setText(appointmentData[9] == null ? "" : appointmentData[9].toString());

        JRoundedButton saveButton = new JRoundedButton("Save Changes", 10);
        saveButton.setMaximumSize(fieldSize);
        saveButton.setPreferredSize(fieldSize);
        saveButton.setMinimumSize(fieldSize);
        saveButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        saveButton.setBackground(Theme.PRIMARY);
        saveButton.setForeground(Theme.WHITE);
        saveButton.setFocusPainted(false);
        saveButton.setFont(Theme.getFont(Theme.FontType.SEMI_BOLD, 15));
        saveButton.setBorder(new EmptyBorder(12, 20, 12, 20));
        saveButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

        saveButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                saveButton.setBackground(Theme.PRIMARY_HOVER);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                saveButton.setBackground(Theme.PRIMARY);
            }
        });

        saveButton.addActionListener(e -> {
            saveButton.setEnabled(false);

            Object selectedDentist = dentistBox.getSelectedItem();
            String scheduledDateText = scheduledDateField.getText().trim();
            String scheduledTimeText = scheduledTimeField.getText().trim();
            Enums.Status selectedStatus = (Enums.Status) statusBox.getSelectedItem();
            String status = selectedStatus == null ? "" : selectedStatus.toString();
            String reason = reasonField.getText().trim();
            String notes = notesField.getText().trim();

            if (selectedDentist == null || selectedDentist.toString().isBlank()) {
                Alert.error("Dentist must not be empty.", this);
                saveButton.setEnabled(true);
                return;
            }

            if (scheduledDateField.getForeground().equals(Color.GRAY) || scheduledDateText.isEmpty() || DATE_HINT.equals(scheduledDateText)) {
                Alert.error("Scheduled date must not be empty.", this);
                saveButton.setEnabled(true);
                return;
            }

            if (scheduledTimeField.getForeground().equals(Color.GRAY) || scheduledTimeText.isEmpty() || TIME_HINT.equals(scheduledTimeText)) {
                Alert.error("Scheduled time must not be empty.", this);
                saveButton.setEnabled(true);
                return;
            }

            if (status.isEmpty()) {
                Alert.error("Status must not be empty.", this);
                saveButton.setEnabled(true);
                return;
            }

            if (reason.isEmpty()) {
                Alert.error("Reason for visit must not be empty.", this);
                saveButton.setEnabled(true);
                return;
            }

            LocalDate parsedDate;
            try {
                parsedDate = LocalDate.parse(scheduledDateText, INPUT_DATE_FORMATTER);
            } catch (Exception ex) {
                Alert.error("Scheduled date must be a valid date in MM/DD/YYYY format.", this);
                saveButton.setEnabled(true);
                return;
            }

            LocalTime parsedTime;
            try {
                parsedTime = LocalTime.parse(scheduledTimeText.toUpperCase(), INPUT_TIME_FORMATTER);
            } catch (Exception ex) {
                Alert.error("Scheduled time must be in HH:MM AM/PM format.", this);
                saveButton.setEnabled(true);
                return;
            }

            Integer dentistUserId = dentistMap.get(selectedDentist.toString());
            if (dentistUserId == null) {
                Alert.error("Invalid dentist selected.", this);
                saveButton.setEnabled(true);
                return;
            }

            String result = Database.Appointment.updateAppointment(
                    appointmentId,
                    dentistUserId,
                    Date.valueOf(parsedDate),
                    Time.valueOf(parsedTime),
                    status,
                    reason,
                    notes
            );

            if (result != null) {
                Alert.error(result, this);
                saveButton.setEnabled(true);
                return;
            }

            if (onSaveCallback != null) {
                onSaveCallback.run();
            }

            Alert.success("Appointment updated successfully.", this);
            new javax.swing.Timer(3000, event -> dispose()) {{
                setRepeats(false);
                start();
            }};
        });

        JPanel formPanel = new JPanel(new GridLayout(4, 2, 20, 20));
        formPanel.setBackground(Theme.WHITE);
        formPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        formPanel.setMaximumSize(new Dimension(560, 390));
        formPanel.setPreferredSize(new Dimension(560, 390));

        formPanel.add(createFieldPanel("Patient", patientNameField));
        formPanel.add(createFieldPanel("Dentist", dentistBox));
        formPanel.add(createFieldPanel("Scheduled Date", scheduledDateField));
        formPanel.add(createFieldPanel("Scheduled Time", scheduledTimeField));
        formPanel.add(createFieldPanel("Status", statusBox));
        formPanel.add(createFieldPanel("Reason for Visit", reasonField));
        formPanel.add(createFieldPanel("Notes", notesField));
        formPanel.add(createFieldPanel("", saveButton));

        card.add(Box.createVerticalGlue());
        card.add(title);
        card.add(Box.createVerticalStrut(24));
        card.add(formPanel);
        card.add(Box.createVerticalGlue());

        root.add(card);
        setContentPane(root);

        setVisible(true);
    }

    private JPanel createFieldPanel(String text, Component field) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Theme.WHITE);
        panel.setBorder(new EmptyBorder(0, 0, 6, 0));

        if (!text.isBlank()) {
            JLabel label = new JLabel(text);
            label.setFont(Theme.getFont(Theme.FontType.MEDIUM, 14));
            label.setForeground(Theme.BLACK);
            label.setAlignmentX(Component.LEFT_ALIGNMENT);
            panel.add(label);
            panel.add(Box.createVerticalStrut(6));
        } else {
            panel.add(Box.createVerticalStrut(26));
        }

        field.setMaximumSize(new Dimension(250, 42));
        field.setPreferredSize(new Dimension(250, 42));
        field.setMinimumSize(new Dimension(250, 42));

        if (field instanceof JComponent jComponent) {
            jComponent.setAlignmentX(Component.LEFT_ALIGNMENT);
        }

        panel.add(field);

        return panel;
    }

    private JTextField createTextField(Dimension fieldSize) {
        JTextField textField = new JTextField();
        textField.setMaximumSize(fieldSize);
        textField.setPreferredSize(fieldSize);
        textField.setMinimumSize(fieldSize);
        textField.setFont(Theme.getFont(Theme.FontType.MEDIUM, 14));
        textField.setAlignmentX(Component.LEFT_ALIGNMENT);
        textField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.SECONDARY, 1),
                new EmptyBorder(10, 12, 10, 12)
        ));
        return textField;
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

                int rawOffset = Math.min(offset - countSlashesBefore(currentText, offset), raw.length());
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

    private int countSlashesBefore(String text, int offset) {
        int count = 0;
        for (int i = 0; i < Math.min(offset, text.length()); i++) {
            if (text.charAt(i) == '/') {
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
}