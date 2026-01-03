package com.greengrocer.utils;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextInputDialog;
import java.util.Optional;

/**
 * utility class for displaying alerts and dialogs.
 * 
 * @author Samira Çeçen
 * @since 24-12-2025
 */
public class AlertHelper {

    /**
     * shows an information alert.
     * 
     * @param title alert title
     * @param header header text
     * @param content content message
     */
    public static void showInfo(String title, String header, String content) {
        // Create information alert
        // Set title, header, content
        // Show and wait
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    /**
     * shows an information alert with default title.
     * 
     * @param content content message
     */
    public static void showInfo(String content) {
        showInfo("Information", null, content);
    }

    /**
     * shows an error alert.
     * 
     * @param title alert title
     * @param header header text
     * @param content error message
     */
    public static void showError(String title, String header, String content) {
        // Create error alert
        // Set title, header, content
        // Show and wait
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    /**
     * shows an error alert with default title.
     * 
     * @param content error message
     */
    public static void showError(String content) {
        showError("Error", null, content);
    }

    /**
     * shows a warning alert.
     * 
     * @param title alert title
     * @param header header text
     * @param content warning message
     */
    public static void showWarning(String title, String header, String content) {
        // Create warning alert
        // Set title, header, content
        // Show and wait
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    /**
     * shows a warning alert with default title.
     * 
     * @param content warning message
     */
    public static void showWarning(String content) {
        showWarning("Warning", null, content);
    }

    /**
     * shows a confirmation dialog.
     * 
     * @param title dialog title
     * @param header header text
     * @param content confirmation message
     * @return true if user clicks ok
     */
    public static boolean showConfirmation(String title, String header, String content) {
        // Create confirmation alert
        // Set title, header, content
        // Show and wait
        // Return true if OK clicked
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    /**
     * shows a confirmation dialog with default title.
     * 
     * @param content confirmation message
     * @return true if user clicks ok
     */
    public static boolean showConfirmation(String content) {
        return showConfirmation("Confirm", null, content);
    }

    /**
     * shows a text input dialog.
     * 
     * @param title dialog title
     * @param header header text
     * @param content prompt message
     * @param defaultValue default input value
     * @return user input or null if cancelled
     */
    public static String showTextInput(String title, String header, String content, String defaultValue) {
        // Create text input dialog
        // Set title, header, content
        // Set default value
        // Show and wait
        // Return input or null
        TextInputDialog dialog = new TextInputDialog(defaultValue);
        dialog.setTitle(title);
        dialog.setHeaderText(header);
        dialog.setContentText(content);
        
        Optional<String> result = dialog.showAndWait();
        return result.orElse(null);
    }

    /**
     * shows a text input dialog with default title.
     * 
     * @param prompt prompt message
     * @return user input or null if cancelled
     */
    public static String showTextInput(String prompt) {
        return showTextInput("Input", null, prompt, "");
    }
}

