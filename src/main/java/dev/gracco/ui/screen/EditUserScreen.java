package dev.gracco.ui.screen;

import dev.gracco.db.Admin;
import dev.gracco.db.Enums;
import dev.gracco.ui.Alert;
import dev.gracco.ui.Theme;
import dev.gracco.ui.element.HintTextField;
import dev.gracco.ui.element.JRoundedButton;
import dev.gracco.ui.element.JRoundedPanel;
import dev.gracco.util.Validation;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class EditUserScreen extends JFrame {
    private static EditUserScreen instance;
    private static int currentUserId = -1;

    public static void open(int userId) {
        if (instance == null) {
            instance = new EditUserScreen(userId);
        } else {
            if (currentUserId != userId) {
                instance.dispose();
                instance = new EditUserScreen(userId);
            } else {
                instance.toFront();
                instance.requestFocus();
            }
        }
    }

    private EditUserScreen(int userId) {
        currentUserId = userId;

        Object[] userData = Admin.getUserById(userId);
        if (userData == null) {
            Alert.error("User not found.", null);
            instance = null;
            currentUserId = -1;
            return;
        }

        setTitle("Edit User");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setSize(760, 650);
        setLocationRelativeTo(null);
        setResizable(false);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                instance = null;
                currentUserId = -1;
            }
        });

        JPanel root = new JPanel(new GridBagLayout());
        root.setBackground(Theme.BACKGROUND_GREEN);

        JPanel card = new JRoundedPanel(20, 1, Theme.SECONDARY);
        card.setBorder(new EmptyBorder(30, 30, 30, 30));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Theme.WHITE);
        card.setPreferredSize(new Dimension(650, 560));

        Dimension fieldSize = new Dimension(250, 42);

        JLabel title = new JLabel("Edit User");
        title.setFont(Theme.getFont(Theme.FontType.MEDIUM, 24));
        title.setForeground(Theme.BLACK);
        title.setHorizontalAlignment(SwingConstants.CENTER);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JTextField usernameField = createTextField(fieldSize);
        HintTextField passwordField = createHintTextField(fieldSize, "Unchanged");
        JTextField firstNameField = createTextField(fieldSize);
        JTextField lastNameField = createTextField(fieldSize);
        JTextField emailField = createTextField(fieldSize);
        JTextField contactNumberField = createTextField(fieldSize);

        JComboBox<Enums.Role> roleBox = new JComboBox<>(Enums.Role.values());
        roleBox.setMaximumSize(fieldSize);
        roleBox.setPreferredSize(fieldSize);
        roleBox.setMinimumSize(fieldSize);
        roleBox.setFont(Theme.getFont(Theme.FontType.MEDIUM, 14));
        roleBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        roleBox.setBackground(Theme.WHITE);
        roleBox.setForeground(Theme.BLACK);
        roleBox.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.SECONDARY, 1),
                new EmptyBorder(4, 8, 4, 8)
        ));

        JCheckBox changedPassBox = createCheckBox();
        JCheckBox isActiveBox = createCheckBox();

        usernameField.setText((String) userData[1]);
        changedPassBox.setSelected((boolean) userData[2]);
        roleBox.setSelectedItem(Enums.Role.fromString((String) userData[3]));
        firstNameField.setText((String) userData[4]);
        lastNameField.setText((String) userData[5]);
        emailField.setText((String) userData[6]);
        contactNumberField.setText((String) userData[7]);
        isActiveBox.setSelected((boolean) userData[8]);

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

        saveButton.addActionListener(_ -> {
            saveButton.setEnabled(false);
            String username = usernameField.getText().trim();
            String password = passwordField.getText().trim();
            boolean changedPass = changedPassBox.isSelected();
            Enums.Role role = (Enums.Role) roleBox.getSelectedItem();
            String firstName = firstNameField.getText().trim();
            String lastName = lastNameField.getText().trim();
            String email = emailField.getText().trim();
            String contactNumber = contactNumberField.getText().trim();
            boolean isActive = isActiveBox.isSelected();

            if (username.isEmpty() || firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || contactNumber.isEmpty()) {
                Alert.error("All fields except password must be filled in.", this);
                saveButton.setEnabled(true);
                return;
            }

            if (role == null) {
                Alert.error("Role must not be empty.", this);
                saveButton.setEnabled(true);
                return;
            }

            if (!Validation.USERNAME_REGEX.matcher(username).matches()) {
                Alert.error("Username must be 3 to 20 characters long and can only contain letters, numbers, and underscores.", this);
                saveButton.setEnabled(true);
                return;
            }

            if (!password.isBlank() && password.length() < 8) {
                Alert.error("Password must be at least 8 characters.", this);
                saveButton.setEnabled(true);
                return;
            }

            if (!Validation.EMAIL_REGEX.matcher(email).matches()) {
                Alert.error("Invalid email! Change the email and try again.", this);
                saveButton.setEnabled(true);
                return;
            }

            if (!Validation.PHONE_NUMBER_REGEX.matcher(contactNumber).matches()) {
                Alert.error("Phone number must be valid!", this);
                saveButton.setEnabled(true);
                return;
            }

            String passwordToSave = password;

            String result = Admin.updateUser(
                    userId,
                    username,
                    passwordToSave,
                    changedPass,
                    role,
                    firstName,
                    lastName,
                    email,
                    contactNumber,
                    isActive
            );

            if (result != null) {
                Alert.error(result, this);
                saveButton.setEnabled(true);
                return;
            }

            Alert.success("User updated successfully.", this);
            new javax.swing.Timer(3000, _ -> {
                this.dispose();
            }) {{
                setRepeats(false);
                start();
            }};
        });

        JPanel formPanel = new JPanel(new GridLayout(5, 2, 20, 18));
        formPanel.setBackground(Theme.WHITE);
        formPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        formPanel.setMaximumSize(new Dimension(560, 430));
        formPanel.setPreferredSize(new Dimension(560, 430));

        formPanel.add(createFieldPanel("Username", usernameField));
        formPanel.add(createFieldPanel("Password", passwordField));
        formPanel.add(createFieldPanel("Changed Pass", changedPassBox));
        formPanel.add(createFieldPanel("Role", roleBox));
        formPanel.add(createFieldPanel("First Name", firstNameField));
        formPanel.add(createFieldPanel("Last Name", lastNameField));
        formPanel.add(createFieldPanel("Email", emailField));
        formPanel.add(createFieldPanel("Contact Number", contactNumberField));
        formPanel.add(createFieldPanel("Is Active", isActiveBox));
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

    private JCheckBox createCheckBox() {
        JCheckBox checkBox = new JCheckBox();
        checkBox.setBackground(Theme.WHITE);
        checkBox.setForeground(Theme.BLACK);
        checkBox.setFont(Theme.getFont(Theme.FontType.MEDIUM, 14));
        checkBox.setFocusPainted(false);
        checkBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        return checkBox;
    }

    private HintTextField createHintTextField(Dimension fieldSize, String hint) {
        HintTextField textField = new HintTextField(hint);
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
}