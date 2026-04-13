package dev.gracco.ui.screen;

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

import dev.gracco.db.Database;
import dev.gracco.ui.Alert;
import dev.gracco.ui.Theme;
import dev.gracco.ui.element.JRoundedButton;
import dev.gracco.ui.element.JRoundedPanel;
import dev.gracco.util.Validation;

public class EditPatientScreen extends JFrame {
    private static EditPatientScreen instance;
    private static int currentPatientId = -1;
    private static final String DATE_HINT = "MM/DD/YYYY";
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("MM/dd/uuuu").withResolverStyle(ResolverStyle.STRICT);

    public static void open(int patientId) {
        if (instance == null) {
            instance = new EditPatientScreen(patientId);
        } else if (currentPatientId != patientId) {
            instance.dispose();
            instance = new EditPatientScreen(patientId);
        } else {
            instance.toFront(); instance.requestFocus();
        }
    }

    private EditPatientScreen(int patientId) {
        currentPatientId = patientId;
        Object[] data = Database.Patient.getPatientById(patientId);
        if (data == null) { Alert.error("Patient not found.", null); instance = null; currentPatientId = -1; return; }

        setTitle("Edit Patient");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setSize(760, 580);
        setLocationRelativeTo(null);
        setResizable(false);

        addWindowListener(new WindowAdapter() {
            public void windowClosed(WindowEvent e) { instance = null; currentPatientId = -1; }
        });

        JPanel root = new JPanel(new GridBagLayout());
        root.setBackground(Theme.BACKGROUND_GREEN);

        JPanel card = new JRoundedPanel(20, 1, Theme.SECONDARY);
        card.setBorder(new EmptyBorder(30, 30, 30, 30));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Theme.WHITE);
        card.setPreferredSize(new Dimension(650, 500));

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

        firstNameField.setText((String) data[1]);
        lastNameField.setText((String) data[2]);
        if (data[3] != null) {
            String formatted = ((Date) data[3]).toLocalDate()
                    .format(DateTimeFormatter.ofPattern("MM/dd/yyyy"));
            birthDateField.setText(formatted);
            birthDateField.setForeground(Theme.BLACK);
        } else {
            installPlaceholder(birthDateField, DATE_HINT);
        }
        contactNumberField.setText(data[5] != null ? (String) data[5] : "");
        emailField.setText(data[6] != null ? (String) data[6] : "");
        addressField.setText(data[7] != null ? (String) data[7] : "");

        JComboBox<String> sexBox = new JComboBox<>(new String[]{"Male", "Female"});
        sexBox.setMaximumSize(fieldSize); sexBox.setPreferredSize(fieldSize); sexBox.setMinimumSize(fieldSize);
        sexBox.setFont(Theme.getFont(Theme.FontType.MEDIUM, 14));
        sexBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        sexBox.setBackground(Theme.WHITE); sexBox.setForeground(Theme.BLACK);
        sexBox.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.SECONDARY, 1), new EmptyBorder(4, 8, 4, 8)));
        if (data[4] != null) sexBox.setSelectedItem(data[4]);

        JRoundedButton saveButton = new JRoundedButton("Save Changes", 10);
        saveButton.setMaximumSize(fieldSize); saveButton.setPreferredSize(fieldSize); saveButton.setMinimumSize(fieldSize);
        saveButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        saveButton.setBackground(Theme.PRIMARY); saveButton.setForeground(Theme.WHITE);
        saveButton.setFocusPainted(false);
        saveButton.setFont(Theme.getFont(Theme.FontType.SEMI_BOLD, 15));
        saveButton.setBorder(new EmptyBorder(12, 20, 12, 20));
        saveButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        saveButton.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { saveButton.setBackground(Theme.PRIMARY_HOVER); }
            public void mouseExited(MouseEvent e) { saveButton.setBackground(Theme.PRIMARY); }
        });

        saveButton.addActionListener(_ -> {
            saveButton.setEnabled(false);
            String firstName = firstNameField.getText().trim();
            String lastName = lastNameField.getText().trim();
            String birthDateText = getActualText(birthDateField).trim();
            String sex = (String) sexBox.getSelectedItem();
            String contactNumber = contactNumberField.getText().trim();
            String email = emailField.getText().trim();
            String address = addressField.getText().trim();

            if (firstName.isEmpty() || lastName.isEmpty() || birthDateText.isEmpty()
                    || sex == null || contactNumber.isEmpty() || email.isEmpty() || address.isEmpty()) {
                Alert.error("All fields must be filled in.", this); saveButton.setEnabled(true); return;
            }

            LocalDate parsedDate;
            try { parsedDate = LocalDate.parse(birthDateText, DATE_FORMATTER); }
            catch (Exception ex) { Alert.error("Birth date must follow MM/DD/YYYY.", this); saveButton.setEnabled(true); return; }

            if (!Validation.EMAIL_REGEX.matcher(email).matches()) {
                Alert.error("Invalid email.", this); saveButton.setEnabled(true); return;
            }
            if (!Validation.PHONE_NUMBER_REGEX.matcher(contactNumber).matches()) {
                Alert.error("Phone number must be valid.", this); saveButton.setEnabled(true); return;
            }

            String result = Database.Patient.updatePatient(patientId, firstName, lastName,
                    Date.valueOf(parsedDate), sex, contactNumber, email, address);

            if (result != null) { Alert.error(result, this); saveButton.setEnabled(true); return; }

            Alert.success("Patient updated successfully.", this);
            new javax.swing.Timer(3000, _ -> dispose()) {{ setRepeats(false); start(); }};
        });

        JPanel formPanel = new JPanel(new GridLayout(4, 2, 20, 18));
        formPanel.setBackground(Theme.WHITE);
        formPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        formPanel.setMaximumSize(new Dimension(560, 340));
        formPanel.setPreferredSize(new Dimension(560, 340));

        formPanel.add(createFieldPanel("First Name", firstNameField));
        formPanel.add(createFieldPanel("Last Name", lastNameField));
        formPanel.add(createFieldPanel("Birth Date (MM/DD/YYYY)", birthDateField));
        formPanel.add(createFieldPanel("Sex", sexBox));
        formPanel.add(createFieldPanel("Contact Number", contactNumberField));
        formPanel.add(createFieldPanel("Email", emailField));
        formPanel.add(createFieldPanel("Address", addressField));
        formPanel.add(createFieldPanel("", saveButton));

        card.add(title);
        card.add(Box.createVerticalStrut(24));
        card.add(formPanel);

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
        if (field instanceof JTextField tf) tf.setAlignmentX(Component.LEFT_ALIGNMENT);
        else if (field instanceof JComboBox<?> cb) cb.setAlignmentX(Component.LEFT_ALIGNMENT);
        else if (field instanceof JRoundedButton btn) btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(field);
        return panel;
    }

    private JTextField createTextField(Dimension size) {
        JTextField f = new JTextField();
        f.setMaximumSize(size); f.setPreferredSize(size); f.setMinimumSize(size);
        f.setFont(Theme.getFont(Theme.FontType.MEDIUM, 14));
        f.setAlignmentX(Component.LEFT_ALIGNMENT);
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.SECONDARY, 1), new EmptyBorder(10, 12, 10, 12)));
        return f;
    }

    private void installPlaceholder(JTextField field, String placeholder) {
        field.setText(placeholder); field.setForeground(Color.GRAY);
        field.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                if (field.getText().equals(placeholder) && field.getForeground().equals(Color.GRAY)) {
                    field.setText(""); field.setForeground(Theme.BLACK);
                }
            }
            public void focusLost(FocusEvent e) {
                if (field.getText().trim().isEmpty()) { field.setText(placeholder); field.setForeground(Color.GRAY); }
            }
        });
    }

    private String getActualText(JTextField field) {
        if (field.getForeground().equals(Color.GRAY)) return "";
        return field.getText();
    }

    private void installDateMask(JTextField field) {
        ((AbstractDocument) field.getDocument()).setDocumentFilter(new DocumentFilter() {
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
                String current = fb.getDocument().getText(0, fb.getDocument().getLength());
                if (DATE_HINT.equals(current) && field.getForeground().equals(Color.GRAY)) {
                    current = ""; offset = 0; length = fb.getDocument().getLength();
                }
                StringBuilder raw = new StringBuilder(current.replace("/", ""));
                String rep = text == null ? "" : text.replaceAll("[^0-9]", "");
                int ro = Math.min(offset - countSlashes(current, offset), raw.length());
                int rl = Math.min(length, raw.length() - ro);
                raw.replace(ro, ro + rl, rep);
                if (raw.length() > 8) raw.setLength(8);
                String fmt = formatDigits(raw.toString());
                fb.replace(0, fb.getDocument().getLength(), fmt, attrs);
                if (!fmt.isEmpty()) field.setForeground(Theme.BLACK);
            }
            public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException { replace(fb, offset, 0, string, attr); }
            public void remove(FilterBypass fb, int offset, int length) throws BadLocationException { replace(fb, offset, length, "", null); }
        });
    }

    private int countSlashes(String text, int offset) {
        int c = 0;
        for (int i = 0; i < Math.min(offset, text.length()); i++) if (text.charAt(i) == '/') c++;
        return c;
    }

    private String formatDigits(String d) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < d.length(); i++) { if (i == 2 || i == 4) sb.append('/'); sb.append(d.charAt(i)); }
        return sb.toString();
    }
}
