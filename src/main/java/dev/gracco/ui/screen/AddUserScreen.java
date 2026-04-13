package dev.gracco.ui.screen;

import dev.gracco.db.Database;
import dev.gracco.db.Enums;
import dev.gracco.ui.Alert;
import dev.gracco.ui.Theme;
import dev.gracco.ui.element.JRoundedButton;
import dev.gracco.ui.element.JRoundedPanel;
import dev.gracco.util.Validation;

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
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class AddUserScreen extends JFrame {
    private static AddUserScreen instance;

    public static void open() {
        if (instance == null) {
            instance = new AddUserScreen();
        } else {
            instance.toFront();
            instance.requestFocus();
        }
    }

    private AddUserScreen() {
        setTitle("Add User");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setSize(760, 580);
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
        card.setBorder(new EmptyBorder(30, 30, 30, 30));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Theme.WHITE);
        card.setPreferredSize(new Dimension(650, 500));

        Dimension fieldSize = new Dimension(250, 42);

        JLabel title = new JLabel("Add User");
        title.setFont(Theme.getFont(Theme.FontType.MEDIUM, 24));
        title.setForeground(Theme.BLACK);
        title.setHorizontalAlignment(SwingConstants.CENTER);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JTextField usernameField = createTextField(fieldSize);
        JTextField passwordField = createTextField(fieldSize);

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

        JTextField firstNameField = createTextField(fieldSize);
        JTextField lastNameField = createTextField(fieldSize);
        JTextField emailField = createTextField(fieldSize);
        JTextField contactNumberField = createTextField(fieldSize);

        JRoundedButton enterButton = new JRoundedButton("Add User", 10);
        enterButton.setMaximumSize(fieldSize);
        enterButton.setPreferredSize(fieldSize);
        enterButton.setMinimumSize(fieldSize);
        enterButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        enterButton.setBackground(Theme.PRIMARY);
        enterButton.setForeground(Theme.WHITE);
        enterButton.setFocusPainted(false);
        enterButton.setFont(Theme.getFont(Theme.FontType.SEMI_BOLD, 15));
        enterButton.setBorder(new EmptyBorder(12, 20, 12, 20));
        enterButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

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

        enterButton.addActionListener(_ -> {
            enterButton.setEnabled(false);
            String username = usernameField.getText().trim();
            String password = passwordField.getText().trim();
            Enums.Role role = (Enums.Role) roleBox.getSelectedItem();
            String firstName = firstNameField.getText().trim();
            String lastName = lastNameField.getText().trim();
            String email = emailField.getText().trim();
            String contactNumber = contactNumberField.getText().trim();

            //Validation
            if (username.isEmpty() || password.isEmpty() || firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || contactNumber.isEmpty()) {
                Alert.error("All fields must be filled in.", this);
                enterButton.setEnabled(true);
                return;
            }

            if (role == null) {
                Alert.error("Role must not be empty! I don't know how you did this and I am impressed.", this);
                enterButton.setEnabled(true);
                return;
            }


            if(!Validation.USERNAME_REGEX.matcher(username).matches()) {
                Alert.error("Username must be 3 to 20 characters long and can only contain letters, numbers, and underscores.", this);
                enterButton.setEnabled(true);
                return;
            }

            if(password.length() < 8) {
                Alert.error("Password must be at least 8 characters.", this);
                enterButton.setEnabled(true);
                return;
            }

            if(!Validation.EMAIL_REGEX.matcher(email).matches()) {
                Alert.error("Invalid email! Change the email and try again.", this);
                enterButton.setEnabled(true);
                return;
            }

            if(!Validation.PHONE_NUMBER_REGEX.matcher(contactNumber).matches()) {
                Alert.error("Phone number must be valid!", this);
                enterButton.setEnabled(true);
                return;
            }

            String result = Database.Admin.addUser(username, password, role, firstName, lastName, email, contactNumber);

            if (result != null) {
                Alert.error(result, this);
                enterButton.setEnabled(true);
                return;
            }

            Alert.success("User added successfully.", this);
            new javax.swing.Timer(3000, _ -> {
                this.dispose();
            }) {{
                setRepeats(false);
                start();
            }};
        });

        JPanel formPanel = new JPanel(new GridLayout(4, 2, 20, 18));
        formPanel.setBackground(Theme.WHITE);
        formPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        formPanel.setMaximumSize(new Dimension(560, 340));
        formPanel.setPreferredSize(new Dimension(560, 340));

        formPanel.add(createFieldPanel("Username", usernameField));
        formPanel.add(createFieldPanel("Password (Please write this down!)", passwordField));
        formPanel.add(createFieldPanel("Role", roleBox));
        formPanel.add(createFieldPanel("First Name", firstNameField));
        formPanel.add(createFieldPanel("Last Name", lastNameField));
        formPanel.add(createFieldPanel("Email", emailField));
        formPanel.add(createFieldPanel("Contact Number", contactNumberField));
        formPanel.add(createFieldPanel("", enterButton));

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

        if (field instanceof JTextField textField) {
            textField.setAlignmentX(Component.LEFT_ALIGNMENT);
        } else if (field instanceof JComboBox<?> comboBox) {
            comboBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        } else if (field instanceof JRoundedButton button) {
            button.setAlignmentX(Component.LEFT_ALIGNMENT);
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
}