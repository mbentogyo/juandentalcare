package dev.gracco.ui.screen;

import dev.gracco.db.Database;
import dev.gracco.ui.Alert;
import dev.gracco.ui.Theme;
import dev.gracco.ui.element.JRoundedButton;
import dev.gracco.ui.element.JRoundedPanel;
import lombok.Getter;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
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
import java.util.List;

public class AddAppointmentScreen extends JFrame {
    private static AddAppointmentScreen instance;
    private static final String DATE_HINT = "MM/DD/YYYY";
    private static final String TIME_HINT = "hh:mm";

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("MM/dd/uuuu").withResolverStyle(ResolverStyle.STRICT);

    public static void open() {
        if (instance == null) {
            instance = new AddAppointmentScreen();
        } else {
            instance.toFront();
            instance.requestFocus();
        }
    }

    private static class DentistItem {
        @Getter private final int userId;
        private final String name;

        public DentistItem(int userId, String name) {
            this.userId = userId;
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private AddAppointmentScreen() {
        setTitle("Add Appointment");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setSize(900, 650);
        setLocationRelativeTo(null);
        setResizable(false);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                instance = null;
            }
        });

        JPanel root = new JPanel(new GridBagLayout());
        root.setBackground(Theme.BACKGROUND_GREEN);

        JPanel card = new JRoundedPanel(20, 1, Theme.SECONDARY);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Theme.WHITE);
        card.setBorder(new EmptyBorder(28, 28, 28, 28));
        card.setPreferredSize(new Dimension(780, 580));
        card.setMinimumSize(new Dimension(780, 580));
        card.setMaximumSize(new Dimension(780, 580));

        Dimension fieldSize = new Dimension(300, 44);

        JLabel title = new JLabel("Add Appointment");
        title.setFont(Theme.getFont(Theme.FontType.MEDIUM, 24));
        title.setForeground(Theme.BLACK);
        title.setHorizontalAlignment(SwingConstants.CENTER);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JTextField patientIdField = createTextField(fieldSize);
        JTextField scheduledDateField = createTextField(fieldSize);
        JTextField scheduledTimeField = createTextField(new Dimension(210, 44));
        JTextField reasonForVisitField = createTextField(fieldSize);

        final String[] patientIdValue = {""};
        final int[] selectedPatientId = {0};
        final boolean[] showingPatientName = {false};

        patientIdField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (showingPatientName[0]) {
                    patientIdField.setText(patientIdValue[0]);
                    patientIdField.setForeground(Theme.BLACK);
                    showingPatientName[0] = false;
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                String raw = patientIdField.getText().trim();

                if (raw.isEmpty()) {
                    patientIdValue[0] = "";
                    selectedPatientId[0] = 0;
                    showingPatientName[0] = false;
                    patientIdField.setText("");
                    patientIdField.setForeground(Theme.BLACK);
                    return;
                }

                if (!raw.matches("\\d+")) {
                    patientIdValue[0] = "";
                    selectedPatientId[0] = 0;
                    patientIdField.setText("Unknown");
                    patientIdField.setForeground(Color.GRAY);
                    showingPatientName[0] = true;
                    return;
                }

                patientIdValue[0] = raw;

                int patientId = Integer.parseInt(raw);
                String patientName = Database.Appointment.getPatientFullNameById(patientId);

                if ("Unknown".equals(patientName)) {
                    selectedPatientId[0] = 0;
                    patientIdField.setForeground(Color.GRAY);
                } else {
                    selectedPatientId[0] = patientId;
                    patientIdField.setForeground(Theme.BLACK);
                }

                patientIdField.setText(patientName);
                showingPatientName[0] = true;
            }
        });

        patientIdField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                if (showingPatientName[0]) {
                    e.consume();
                    return;
                }

                char c = e.getKeyChar();
                if (!Character.isDigit(c) && c != KeyEvent.VK_BACK_SPACE && c != KeyEvent.VK_DELETE) {
                    e.consume();
                }
            }
        });

        JTextField notesField = new JTextField();
        notesField.setFont(Theme.getFont(Theme.FontType.MEDIUM, 13));
        notesField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.SECONDARY, 1),
                new EmptyBorder(8, 12, 8, 12)
        ));

        installDateMask(scheduledDateField);
        installPlaceholder(scheduledDateField, DATE_HINT);

        installTimeMask(scheduledTimeField);
        installPlaceholder(scheduledTimeField, TIME_HINT);

        JComboBox<String> amPmBox = new JComboBox<>(new String[]{"AM", "PM"});
        amPmBox.setMaximumSize(new Dimension(80, 44));
        amPmBox.setPreferredSize(new Dimension(80, 44));
        amPmBox.setMinimumSize(new Dimension(80, 44));
        amPmBox.setFont(Theme.getFont(Theme.FontType.MEDIUM, 14));
        amPmBox.setBackground(Theme.WHITE);
        amPmBox.setForeground(Theme.BLACK);
        amPmBox.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.SECONDARY, 1),
                new EmptyBorder(4, 8, 4, 8)
        ));

        JComboBox<String> statusBox = new JComboBox<>(new String[]{
                "Confirmed",
                "Cancelled",
                "Booked",
                "Rescheduled",
                "No Show"
        });
        statusBox.setMaximumSize(new Dimension(300, 44));
        statusBox.setPreferredSize(new Dimension(300, 44));
        statusBox.setMinimumSize(new Dimension(300, 44));
        statusBox.setFont(Theme.getFont(Theme.FontType.MEDIUM, 14));
        statusBox.setBackground(Theme.WHITE);
        statusBox.setForeground(Theme.BLACK);
        statusBox.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.SECONDARY, 1),
                new EmptyBorder(4, 8, 4, 8)
        ));

        JComboBox<DentistItem> dentistBox = new JComboBox<>();
        dentistBox.setMaximumSize(new Dimension(300, 44));
        dentistBox.setPreferredSize(new Dimension(300, 44));
        dentistBox.setMinimumSize(new Dimension(300, 44));
        dentistBox.setFont(Theme.getFont(Theme.FontType.MEDIUM, 14));
        dentistBox.setBackground(Theme.WHITE);
        dentistBox.setForeground(Theme.BLACK);
        dentistBox.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.SECONDARY, 1),
                new EmptyBorder(4, 8, 4, 8)
        ));

        List<Object[]> dentists = Database.Appointment.getDentists();
        for (Object[] dentist : dentists) {
            dentistBox.addItem(new DentistItem((Integer) dentist[0], (String) dentist[1]));
        }

        JRoundedButton enterButton = new JRoundedButton("Add Appointment", 10);
        enterButton.setMaximumSize(new Dimension(300, 44));
        enterButton.setPreferredSize(new Dimension(300, 44));
        enterButton.setMinimumSize(new Dimension(300, 44));
        enterButton.setBackground(Theme.PRIMARY);
        enterButton.setForeground(Theme.WHITE);
        enterButton.setFocusPainted(false);
        enterButton.setFont(Theme.getFont(Theme.FontType.SEMI_BOLD, 15));
        enterButton.setBorder(new EmptyBorder(12, 20, 12, 20));
        enterButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        enterButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        enterButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                enterButton.setBackground(Theme.PRIMARY_HOVER);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                enterButton.setBackground(Theme.PRIMARY);
            }
        });

        enterButton.addActionListener(e -> {
            enterButton.setEnabled(false);

            String scheduledDateText = getActualText(scheduledDateField, DATE_HINT).trim();
            String scheduledTimeText = getActualText(scheduledTimeField, TIME_HINT).trim();
            String amPm = (String) amPmBox.getSelectedItem();
            String status = (String) statusBox.getSelectedItem();
            String reasonForVisit = reasonForVisitField.getText().trim();
            String notes = notesField.getText().trim();
            DentistItem selectedDentist = (DentistItem) dentistBox.getSelectedItem();

            if (selectedPatientId[0] <= 0 || scheduledDateText.isEmpty()
                    || scheduledTimeText.isEmpty() || amPm == null || status == null
                    || reasonForVisit.isEmpty() || selectedDentist == null) {
                Alert.error("All fields except notes must be filled in.", this);
                enterButton.setEnabled(true);
                return;
            }

            int patientId = selectedPatientId[0];
            int dentistUserId = selectedDentist.getUserId();

            LocalDate parsedDate;
            try {
                parsedDate = LocalDate.parse(scheduledDateText, DATE_FORMATTER);
            } catch (Exception ex) {
                Alert.error("Scheduled date must follow MM/DD/YYYY and must be a valid date.", this);
                enterButton.setEnabled(true);
                return;
            }

            LocalTime parsedTime;
            try {
                String[] parts = scheduledTimeText.split(":");

                if (parts.length != 2) {
                    throw new IllegalArgumentException();
                }

                int hour12 = Integer.parseInt(parts[0]);
                int minute = Integer.parseInt(parts[1]);

                if (hour12 < 1 || hour12 > 12 || minute < 0 || minute > 59) {
                    throw new IllegalArgumentException();
                }

                int hour24 = hour12 % 12;
                if ("PM".equals(amPm)) {
                    hour24 += 12;
                }

                parsedTime = LocalTime.of(hour24, minute);
            } catch (Exception ex) {
                Alert.error("Scheduled time must follow hh:mm with AM or PM and must be a valid time.", this);
                enterButton.setEnabled(true);
                return;
            }

            String result = Database.Appointment.addAppointment(
                    patientId,
                    dentistUserId,
                    Date.valueOf(parsedDate),
                    Time.valueOf(parsedTime),
                    status,
                    reasonForVisit,
                    notes.isBlank() ? null : notes
            );

            if (result != null) {
                Alert.error(result, this);
                enterButton.setEnabled(true);
                return;
            }

            Alert.success("Appointment added successfully.", this);
            new javax.swing.Timer(3000, timerEvent -> dispose()) {{
                setRepeats(false);
                start();
            }};
        });

        JPanel timePanel = new JPanel();
        timePanel.setLayout(new BoxLayout(timePanel, BoxLayout.X_AXIS));
        timePanel.setBackground(Theme.WHITE);
        timePanel.setOpaque(true);
        timePanel.setPreferredSize(new Dimension(300, 44));
        timePanel.setMinimumSize(new Dimension(300, 44));
        timePanel.setMaximumSize(new Dimension(300, 44));
        timePanel.add(scheduledTimeField);
        timePanel.add(Box.createHorizontalStrut(10));
        timePanel.add(amPmBox);

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(Theme.WHITE);
        formPanel.setOpaque(false);
        formPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        formPanel.setMaximumSize(new Dimension(660, 280));
        formPanel.setPreferredSize(new Dimension(660, 280));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.NONE;

        gbc.insets = new Insets(0, 10, 20, 10);
        gbc.gridx = 0;
        gbc.gridy = 0;
        formPanel.add(createFieldPanel("Patient ID", patientIdField), gbc);

        gbc.gridx = 1;
        formPanel.add(createFieldPanel("Dentist", dentistBox), gbc);

        gbc.insets = new Insets(0, 10, 16, 10);
        gbc.gridx = 0;
        gbc.gridy = 1;
        formPanel.add(createFieldPanel("Scheduled Date (MM/DD/YYYY)", scheduledDateField), gbc);

        gbc.gridx = 1;
        formPanel.add(createFieldPanel("Scheduled Time (hh:mm)", timePanel), gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        formPanel.add(createFieldPanel("Status", statusBox), gbc);

        gbc.gridx = 1;
        formPanel.add(createFieldPanel("Reason for Visit", reasonForVisitField), gbc);

        JPanel notesPanel = createNotesPanel("Notes", notesField);
        notesPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel buttonPanel = new JPanel(new GridBagLayout());
        buttonPanel.setBackground(Theme.WHITE);
        buttonPanel.setOpaque(false);
        buttonPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        buttonPanel.setMaximumSize(new Dimension(660, 44));
        buttonPanel.setPreferredSize(new Dimension(660, 44));
        buttonPanel.add(enterButton);

        card.add(title);
        card.add(Box.createVerticalStrut(22));
        card.add(formPanel);
        card.add(Box.createVerticalStrut(8));
        card.add(notesPanel);
        card.add(Box.createVerticalStrut(10));
        card.add(Box.createVerticalGlue());
        card.add(buttonPanel);

        root.add(card);
        setContentPane(root);

        setVisible(true);
    }

    private JPanel createFieldPanel(String text, Component field) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Theme.WHITE);
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(0, 0, 0, 0));
        panel.setPreferredSize(new Dimension(300, 72));
        panel.setMinimumSize(new Dimension(300, 72));
        panel.setMaximumSize(new Dimension(300, 72));

        if (!text.isBlank()) {
            JLabel label = new JLabel(text);
            label.setFont(Theme.getFont(Theme.FontType.MEDIUM, 14));
            label.setForeground(Theme.BLACK);
            label.setAlignmentX(Component.LEFT_ALIGNMENT);
            panel.add(label);
            panel.add(Box.createVerticalStrut(6));
        } else {
            panel.add(Box.createVerticalStrut(25));
        }

        field.setPreferredSize(new Dimension(300, 44));
        field.setMinimumSize(new Dimension(300, 44));
        field.setMaximumSize(new Dimension(300, 44));

        if (field instanceof JTextField textField) {
            textField.setAlignmentX(Component.LEFT_ALIGNMENT);
        } else if (field instanceof JComboBox<?> comboBox) {
            comboBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        } else if (field instanceof JRoundedButton button) {
            button.setAlignmentX(Component.CENTER_ALIGNMENT);
        } else if (field instanceof JPanel panelField) {
            panelField.setAlignmentX(Component.LEFT_ALIGNMENT);
        }

        panel.add(field);
        return panel;
    }

    private JPanel createNotesPanel(String text, JTextField textField) {
        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setBackground(Theme.WHITE);
        wrapper.setOpaque(false);
        wrapper.setMaximumSize(new Dimension(660, 90));
        wrapper.setPreferredSize(new Dimension(660, 90));
        wrapper.setMinimumSize(new Dimension(660, 90));

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Theme.WHITE);
        panel.setOpaque(false);
        panel.setPreferredSize(new Dimension(620, 82));
        panel.setMinimumSize(new Dimension(620, 82));
        panel.setMaximumSize(new Dimension(620, 82));

        JLabel label = new JLabel(text);
        label.setFont(Theme.getFont(Theme.FontType.MEDIUM, 14));
        label.setForeground(Theme.BLACK);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);

        textField.setAlignmentX(Component.LEFT_ALIGNMENT);
        textField.setPreferredSize(new Dimension(620, 44));
        textField.setMinimumSize(new Dimension(620, 44));
        textField.setMaximumSize(new Dimension(620, 44));

        panel.add(label);
        panel.add(Box.createVerticalStrut(6));
        panel.add(textField);

        wrapper.add(panel);
        return wrapper;
    }

    private JTextField createTextField(Dimension fieldSize) {
        JTextField textField = new JTextField();
        textField.setMaximumSize(new Dimension(fieldSize.width, 44));
        textField.setPreferredSize(new Dimension(fieldSize.width, 44));
        textField.setMinimumSize(new Dimension(fieldSize.width, 44));
        textField.setFont(Theme.getFont(Theme.FontType.MEDIUM, 14));
        textField.setAlignmentX(Component.LEFT_ALIGNMENT);
        textField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.SECONDARY, 1),
                new EmptyBorder(8, 12, 8, 12)
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

    private String getActualText(JTextField field, String placeholder) {
        if (field.getForeground().equals(Color.GRAY) && placeholder.equals(field.getText())) {
            return "";
        }
        return field.getText();
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

                int rawOffset = Math.min(offset - countCharBefore(currentText, offset, '/'), raw.length());
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

                int rawOffset = Math.min(offset - countCharBefore(currentText, offset, ':'), raw.length());
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

    private int countCharBefore(String text, int offset, char target) {
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
}