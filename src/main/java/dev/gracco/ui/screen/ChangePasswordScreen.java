package dev.gracco.ui.screen;

import dev.gracco.db.Database;
import dev.gracco.ui.Alert;
import dev.gracco.ui.Theme;
import dev.gracco.ui.element.JRoundedButton;
import dev.gracco.ui.element.JRoundedPanel;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class ChangePasswordScreen extends JFrame {
    public ChangePasswordScreen() {
        setTitle("Change Password");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(470, 440);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel root = new JPanel(new GridBagLayout());
        root.setBackground(Theme.BACKGROUND_GREEN);

        JPanel card = new JRoundedPanel(20, 1, Theme.SECONDARY);
        card.setBorder(new EmptyBorder(30, 30, 30, 30));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Theme.WHITE);
        card.setPreferredSize(new Dimension(370, 340));

        Dimension fieldSize = new Dimension(240, 42);

        JLabel title = new JLabel("Change Your Password");
        title.setFont(Theme.getFont(Theme.FontType.MEDIUM, 24));
        title.setForeground(Theme.BLACK);
        title.setHorizontalAlignment(SwingConstants.CENTER);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel message = new JLabel("""
                <html><div style='text-align: center; width: 240px;'>
                Your password is still the default password and must be replaced before continuing.
                </div></html>
                """);
        message.setFont(Theme.getFont(Theme.FontType.REGULAR, 14));
        message.setForeground(Theme.BLACK);
        message.setHorizontalAlignment(SwingConstants.CENTER);
        message.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel passwordLabel = new JLabel("New Password");
        passwordLabel.setFont(Theme.getFont(Theme.FontType.MEDIUM, 14));
        passwordLabel.setForeground(Theme.BLACK);
        passwordLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPasswordField passwordField = new JPasswordField();
        passwordField.setMaximumSize(fieldSize);
        passwordField.setPreferredSize(fieldSize);
        passwordField.setFont(Theme.getFont(Theme.FontType.MEDIUM, 14));
        passwordField.setAlignmentX(Component.CENTER_ALIGNMENT);
        passwordField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.SECONDARY, 1),
                new EmptyBorder(10, 12, 10, 12)
        ));

        JRoundedButton enterButton = new JRoundedButton("Enter", 10);
        enterButton.setMaximumSize(fieldSize);
        enterButton.setPreferredSize(fieldSize);
        enterButton.setAlignmentX(Component.CENTER_ALIGNMENT);
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
            String newPassword = new String(passwordField.getPassword());
            enterButton.setEnabled(false);

            if (!newPassword.isBlank() && newPassword.length() > 7 && Database.User.changePassword(newPassword)) {
                Alert.success("Successfully changed your password. Redirecting...", this);

                new javax.swing.Timer(3000, _ -> {
                    new MainScreen();
                    this.dispose();
                }) {{
                    setRepeats(false);
                    start();
                }};
            } else {
                Alert.error("Invalid password, try again!", this);
                enterButton.setEnabled(true);
            }
        });

        card.add(title);
        card.add(Box.createVerticalStrut(20));
        card.add(message);
        card.add(Box.createVerticalStrut(20));
        card.add(passwordLabel);
        card.add(Box.createVerticalStrut(4));
        card.add(passwordField);
        card.add(Box.createVerticalStrut(18));
        card.add(enterButton);

        root.add(card);
        setContentPane(root);

        setVisible(true);
    }
}