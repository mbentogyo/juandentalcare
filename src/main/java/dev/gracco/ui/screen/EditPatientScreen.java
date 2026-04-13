package dev.gracco.ui.screen;

import dev.gracco.db.Database;
import dev.gracco.ui.Alert;
import dev.gracco.ui.Theme;
import dev.gracco.ui.element.JRoundedButton;
import dev.gracco.ui.element.JRoundedPanel;
import dev.gracco.util.Validation;

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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;

public class EditPatientScreen extends JFrame {
    private static final String DATE_HINT = "MM/DD/YYYY";
    private static final DateTimeFormatter INPUT_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("MM/dd/uuuu").withResolverStyle(ResolverStyle.STRICT);

    private static EditPatientScreen instance;
    private static int currentPatientId = -1;
    private static Runnable onSaveCallback;

    public static void open(int patientId, Runnable onSave) {
        if (instance == null) {
            instance = new EditPatientScreen(patientId, onSave);
        } else {
            if (currentPatientId != patientId) {
                instance.dispose();
                instance = new EditPatientScreen(patientId, onSave);
            } else {
                onSaveCallback = onSave;
                instance.toFront();
                instance.requestFocus();
            }
        }
    }

    private EditPatientScreen(int patientId, Runnable onSave) {
        currentPatientId = patientId;
        onSaveCallback = onSave;

        Object[] patientData = Database.Patient.getPatientById(patientId);
        if (patientData == null) {
            Alert.error("Patient not found.", null);
            instance = null;
            currentPatientId = -1;
            onSaveCallback = null;
            return;
        }

        setTitle("Edit Patient");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setSize(760, 610);
        setLocationRelativeTo(null);
        setResizable(false);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                instance = null;
                currentPatientId = -1;
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

        JLabel title = new JLabel("Edit Patient");
        title.setFont(Theme.getFont(Theme.FontType.MEDIUM, 24));
        title.setForeground(Theme.BLACK);
        title.setHorizontalAlignment(SwingConstants.CENTER);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JTextField firstNameField = createTextField(fieldSize);
        JTextField lastNameField = createTextField(fieldSize);
        JTextField birthDateField = createTextField(fieldSize);
        JTextField contactNumberField = createTextField(fieldSize);
        JTextField emailField = createTextField(fieldSize);
        JTextField addressField = createTextField(fieldSize);

        installDateMask(birthDateField);
        installPlaceholder(birthDateField, DATE_HINT);

        JComboBox<String> sexBox = new JComboBox<>(new String[]{"Male", "Female"});
        sexBox.setMaximumSize(fieldSize);
        sexBox.setPreferredSize(fieldSize);
        sexBox.setMinimumSize(fieldSize);
        sexBox.setFont(Theme.getFont(Theme.FontType.MEDIUM, 14));
        sexBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        sexBox.setBackground(Theme.WHITE);
        sexBox.setForeground(Theme.BLACK);
        sexBox.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.SECONDARY, 1),
                new EmptyBorder(4, 8, 4, 8)
        ));

        firstNameField.setText((String) patientData[1]);
        lastNameField.setText((String) patientData[2]);

        Date birthDate = (Date) patientData[3];
        if (birthDate != null) {
            LocalDate localDate = birthDate.toLocalDate();
            birthDateField.setForeground(Theme.BLACK);
            birthDateField.setText(localDate.format(INPUT_DATE_FORMATTER));
        }

        sexBox.setSelectedItem(patientData[4] == null ? "" : patientData[4].toString());
        contactNumberField.setText(patientData[5] == null ? "" : patientData[5].toString());
        emailField.setText(patientData[6] == null ? "" : patientData[6].toString());
        addressField.setText(patientData[7] == null ? "" : patientData[7].toString());

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

            String firstName = firstNameField.getText().trim();
            String lastName = lastNameField.getText().trim();
            String birthDateText = birthDateField.getText().trim();
            String sex = sexBox.getSelectedItem() == null ? "" : sexBox.getSelectedItem().toString().trim();
            String contactNumber = contactNumberField.getText().trim();
            String email = emailField.getText().trim();
            String address = addressField.getText().trim();

            if (firstName.isEmpty() || lastName.isEmpty()) {
                Alert.error("First name and last name must not be empty.", this);
                saveButton.setEnabled(true);
                return;
            }

            if (birthDateField.getForeground().equals(Color.GRAY) || birthDateText.isEmpty() || DATE_HINT.equals(birthDateText)) {
                Alert.error("Birthdate must not be empty.", this);
                saveButton.setEnabled(true);
                return;
            }

            if (sex.isEmpty()) {
                Alert.error("Sex must not be empty.", this);
                saveButton.setEnabled(true);
                return;
            }

            LocalDate parsedDate;
            try {
                parsedDate = LocalDate.parse(birthDateText, INPUT_DATE_FORMATTER);
            } catch (Exception ex) {
                Alert.error("Birthdate must be a valid date in MM/DD/YYYY format.", this);
                saveButton.setEnabled(true);
                return;
            }

            if (!email.isBlank() && !Validation.EMAIL_REGEX.matcher(email).matches()) {
                Alert.error("Invalid email! Change the email and try again.", this);
                saveButton.setEnabled(true);
                return;
            }

            if (!contactNumber.isBlank() && !Validation.PHONE_NUMBER_REGEX.matcher(contactNumber).matches()) {
                Alert.error("Phone number must be valid!", this);
                saveButton.setEnabled(true);
                return;
            }

            String result = Database.Patient.updatePatient(
                    patientId,
                    firstName,
                    lastName,
                    Date.valueOf(parsedDate),
                    sex,
                    contactNumber,
                    email,
                    address
            );

            if (result != null) {
                Alert.error(result, this);
                saveButton.setEnabled(true);
                return;
            }

            if (onSaveCallback != null) {
                onSaveCallback.run();
            }

            Alert.success("Patient updated successfully.", this);
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

        formPanel.add(createFieldPanel("First Name", firstNameField));
        formPanel.add(createFieldPanel("Last Name", lastNameField));
        formPanel.add(createFieldPanel("Birthdate", birthDateField));
        formPanel.add(createFieldPanel("Sex", sexBox));
        formPanel.add(createFieldPanel("Contact Number", contactNumberField));
        formPanel.add(createFieldPanel("Email", emailField));
        formPanel.add(createFieldPanel("Address", addressField));
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