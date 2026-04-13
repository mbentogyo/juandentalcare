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
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

import dev.gracco.db.Database;
import dev.gracco.db.Enums;
import dev.gracco.ui.Alert;
import dev.gracco.ui.Theme;
import dev.gracco.ui.element.JRoundedButton;
import dev.gracco.ui.element.JRoundedPanel;

public class AddAppointmentScreen extends JFrame {
    private static AddAppointmentScreen instance;
    private static final String DATE_HINT = "MM/DD/YYYY";
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

    private AddAppointmentScreen() {
        setTitle("Add Appointment");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setSize(760, 640);
        setLocationRelativeTo(null);
        setResizable(false);

        addWindowListener(new WindowAdapter() {
            @Override public void windowClosed(WindowEvent e) { instance = null; }
        });

        JPanel root = new JPanel(new GridBagLayout());
        root.setBackground(Theme.BACKGROUND_GREEN);

        JPanel card = new JRoundedPanel(20, 1, Theme.SECONDARY);
        card.setBorder(new EmptyBorder(30, 30, 30, 30));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Theme.WHITE);
        card.setPreferredSize(new Dimension(680, 560));

        Dimension fieldSize = new Dimension(270, 42);

        JLabel title = new JLabel("Add Appointment");
        title.setFont(Theme.getFont(Theme.FontType.MEDIUM, 24));
        title.setForeground(Theme.BLACK);
        title.setHorizontalAlignment(SwingConstants.CENTER);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Patient combo
        Object[][] patients = Database.Appointment.getPatientList();
        JComboBox<String> patientBox = new JComboBox<>();
        int[] patientIds = new int[patients.length];
        for (int i = 0; i < patients.length; i++) {
            patientBox.addItem((String) patients[i][1]);
            patientIds[i] = (int) patients[i][0];
        }
        styleComboBox(patientBox, fieldSize);

        // Dentist combo
        Object[][] dentists = Database.Appointment.getDentists();
        JComboBox<String> dentistBox = new JComboBox<>();
        int[] dentistIds = new int[dentists.length];
        for (int i = 0; i < dentists.length; i++) {
            dentistBox.addItem((String) dentists[i][1]);
            dentistIds[i] = (int) dentists[i][0];
        }
        styleComboBox(dentistBox, fieldSize);

        JTextField dateField = createTextField(fieldSize);
        installDateMask(dateField);
        installPlaceholder(dateField, DATE_HINT);

        JTextField timeField = createTextField(fieldSize);
        installPlaceholder(timeField, "HH:MM (e.g. 09:30)");

        JComboBox<String> statusBox = new JComboBox<>();
        for (Enums.Status s : Enums.Status.values()) statusBox.addItem(s.toString());
        styleComboBox(statusBox, fieldSize);

        JTextField reasonField = createTextField(fieldSize);

        JTextArea notesArea = new JTextArea(3, 20);
        notesArea.setFont(Theme.getFont(Theme.FontType.REGULAR, 14));
        notesArea.setLineWrap(true);
        notesArea.setWrapStyleWord(true);
        notesArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.SECONDARY, 1),
                new EmptyBorder(8, 10, 8, 10)
        ));
        JScrollPane notesScroll = new JScrollPane(notesArea);
        notesScroll.setBorder(BorderFactory.createLineBorder(Theme.SECONDARY, 1));
        notesScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        notesScroll.setPreferredSize(new Dimension(270, 80));

        JRoundedButton addButton = createPrimaryButton("Add Appointment", fieldSize);

        addButton.addActionListener(_ -> {
            addButton.setEnabled(false);
            if (patients.length == 0) { Alert.error("No patients found.", this); addButton.setEnabled(true); return; }
            if (dentists.length == 0) { Alert.error("No dentists found.", this); addButton.setEnabled(true); return; }

            String dateText = getActualText(dateField).trim();
            String timeText = getActualText(timeField).trim();
            String reason = reasonField.getText().trim();

            if (dateText.isEmpty() || timeText.isEmpty() || reason.isEmpty()) {
                Alert.error("Date, time, and reason are required.", this);
                addButton.setEnabled(true);
                return;
            }

            LocalDate parsedDate;
            try { parsedDate = LocalDate.parse(dateText, DATE_FORMATTER); }
            catch (Exception ex) { Alert.error("Date must follow MM/DD/YYYY.", this); addButton.setEnabled(true); return; }

            if (!timeText.matches("^([01]?\\d|2[0-3]):[0-5]\\d$")) {
                Alert.error("Time must be in HH:MM format (e.g. 09:30).", this);
                addButton.setEnabled(true);
                return;
            }

            int pId = patientIds[patientBox.getSelectedIndex()];
            int dId = dentistIds[dentistBox.getSelectedIndex()];
            String status = (String) statusBox.getSelectedItem();
            String notes = notesArea.getText().trim();

            String result = Database.Appointment.addAppointment(pId, dId, Date.valueOf(parsedDate),
                    timeText, status, reason, notes);

            if (result != null) { Alert.error(result, this); addButton.setEnabled(true); return; }

            Alert.success("Appointment added successfully.", this);
            new javax.swing.Timer(3000, _ -> dispose()) {{ setRepeats(false); start(); }};
        });

        JPanel formPanel = new JPanel(new GridLayout(4, 2, 20, 14));
        formPanel.setBackground(Theme.WHITE);
        formPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        formPanel.setMaximumSize(new Dimension(600, 280));
        formPanel.setPreferredSize(new Dimension(600, 280));

        formPanel.add(createFieldPanel("Patient", patientBox));
        formPanel.add(createFieldPanel("Dentist", dentistBox));
        formPanel.add(createFieldPanel("Date (MM/DD/YYYY)", dateField));
        formPanel.add(createFieldPanel("Time (HH:MM)", timeField));
        formPanel.add(createFieldPanel("Status", statusBox));
        formPanel.add(createFieldPanel("Reason for Visit", reasonField));

        JPanel notesRow = new JPanel(new GridLayout(1, 2, 20, 0));
        notesRow.setBackground(Theme.WHITE);
        notesRow.setAlignmentX(Component.CENTER_ALIGNMENT);
        notesRow.setMaximumSize(new Dimension(600, 100));
        notesRow.setPreferredSize(new Dimension(600, 100));
        notesRow.add(createFieldPanel("Notes (optional)", notesScroll));
        notesRow.add(createFieldPanel("", addButton));

        card.add(title);
        card.add(Box.createVerticalStrut(20));
        card.add(formPanel);
        card.add(Box.createVerticalStrut(14));
        card.add(notesRow);

        root.add(card);
        setContentPane(root);
        setVisible(true);
    }

    private JPanel createFieldPanel(String text, Component field) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Theme.WHITE);
        panel.setBorder(new EmptyBorder(0, 0, 4, 0));

        if (!text.isBlank()) {
            JLabel label = new JLabel(text);
            label.setFont(Theme.getFont(Theme.FontType.MEDIUM, 13));
            label.setForeground(Theme.BLACK);
            label.setAlignmentX(Component.LEFT_ALIGNMENT);
            panel.add(label);
            panel.add(Box.createVerticalStrut(4));
        } else {
            panel.add(Box.createVerticalStrut(22));
        }

        field.setMaximumSize(new Dimension(270, field instanceof JScrollPane ? 80 : 42));
        field.setPreferredSize(new Dimension(270, field instanceof JScrollPane ? 80 : 42));
        if (field instanceof JComponent jc) jc.setAlignmentX(Component.LEFT_ALIGNMENT);
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

    private void styleComboBox(JComboBox<?> box, Dimension size) {
        box.setMaximumSize(size); box.setPreferredSize(size); box.setMinimumSize(size);
        box.setFont(Theme.getFont(Theme.FontType.MEDIUM, 14));
        box.setAlignmentX(Component.LEFT_ALIGNMENT);
        box.setBackground(Theme.WHITE); box.setForeground(Theme.BLACK);
        box.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.SECONDARY, 1), new EmptyBorder(4, 8, 4, 8)));
    }

    private JRoundedButton createPrimaryButton(String text, Dimension size) {
        JRoundedButton btn = new JRoundedButton(text, 10);
        btn.setMaximumSize(size); btn.setPreferredSize(size); btn.setMinimumSize(size);
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setBackground(Theme.PRIMARY); btn.setForeground(Theme.WHITE);
        btn.setFocusPainted(false);
        btn.setFont(Theme.getFont(Theme.FontType.SEMI_BOLD, 15));
        btn.setBorder(new EmptyBorder(12, 20, 12, 20));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setBackground(Theme.PRIMARY_HOVER); }
            public void mouseExited(MouseEvent e) { btn.setBackground(Theme.PRIMARY); }
        });
        return btn;
    }

    private void installPlaceholder(JTextField field, String placeholder) {
        field.setText(placeholder);
        field.setForeground(Color.GRAY);
        field.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                if (field.getText().equals(placeholder) && field.getForeground().equals(Color.GRAY)) {
                    field.setText(""); field.setForeground(Theme.BLACK);
                }
            }
            public void focusLost(FocusEvent e) {
                if (field.getText().trim().isEmpty()) {
                    field.setText(placeholder); field.setForeground(Color.GRAY);
                }
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
