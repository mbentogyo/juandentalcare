package dev.gracco;

import dev.gracco.db.Database;
import dev.gracco.ui.Theme;
import dev.gracco.ui.screen.LoginScreen;
import lombok.Getter;

import javax.swing.SwingUtilities;

public class Main {
    @Getter private static final String name = "Juan Dental Care";

    public static void main(String[] args) {
        System.out.println("Theme initialized: " + Theme.initialize());
        System.out.println("Database initialized: " + Database.initialize());
        SwingUtilities.invokeLater(LoginScreen::new);
    }
}