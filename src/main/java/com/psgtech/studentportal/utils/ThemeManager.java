package com.psgtech.studentportal.utils;

import javafx.scene.Scene;
import java.io.File;
import java.net.MalformedURLException;

public class ThemeManager {
    private static ThemeManager instance;
    private boolean isDarkMode = false;
    private String lightThemePath;
    private String darkThemePath;

    private ThemeManager() {
        // Paths will be loaded relatively
        lightThemePath = getClass().getResource("/styles/style.css").toExternalForm();
        darkThemePath = getClass().getResource("/styles/dark-theme.css").toExternalForm();
    }

    public static ThemeManager getInstance() {
        if (instance == null) {
            instance = new ThemeManager();
        }
        return instance;
    }

    public boolean isDarkMode() {
        return isDarkMode;
    }

    public void setDarkMode(boolean darkMode) {
        isDarkMode = darkMode;
    }

    public void toggleTheme() {
        isDarkMode = !isDarkMode;
    }

    public void applyTheme(Scene scene) {
        if (scene == null)
            return;

        // Always apply base styles
        if (!scene.getStylesheets().contains(lightThemePath)) {
            scene.getStylesheets().add(lightThemePath);
        }

        // Apply dark theme override if enabled
        if (isDarkMode) {
            if (!scene.getStylesheets().contains(darkThemePath)) {
                scene.getStylesheets().add(darkThemePath);
            }
        } else {
            scene.getStylesheets().remove(darkThemePath);
        }
    }
}
