package com.greengrocer.controllers;

import com.greengrocer.app.MainApplication;
import com.greengrocer.models.User;
import com.greengrocer.services.AuthenticationService;
import com.greengrocer.services.EmailNotificationService;
import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.controls.MFXPasswordField;
import io.github.palexdev.materialfx.controls.MFXTextField;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.net.URL;
import java.security.SecureRandom;
import java.util.ResourceBundle;

/**
 * Controller for the registration view with email verification.
 * 
 * Step 1: User fills in registration form
 * Step 2: User enters 6-digit code sent to their email
 * 
 * @author Barkev Şarklı
 * @version 2.0
 * @since 24.12.2025
 */
public class RegistrationController implements Initializable {

    // Step 1: Registration Form
    @FXML private VBox step1Container;
    @FXML private MFXTextField usernameField;
    @FXML private MFXPasswordField passwordField;
    @FXML private MFXPasswordField confirmPasswordField;
    @FXML private MFXTextField emailField;
    @FXML private MFXTextField phoneField;
    @FXML private MFXTextField addressField;
    @FXML private Label requirementsLabel;
    @FXML private Label step1MessageLabel;
    @FXML private MFXButton sendCodeButton;

    // Step 2: Verification Code
    @FXML private VBox step2Container;
    @FXML private Label codeSentToLabel;
    @FXML private MFXTextField code1, code2, code3, code4, code5, code6;
    @FXML private Label step2MessageLabel;
    @FXML private MFXButton verifyButton;

    // Services
    private AuthenticationService authService;
    private EmailNotificationService emailService;
    
    // State
    private String generatedCode;
    private String pendingUsername;
    private String pendingPassword;
    private String pendingEmail;
    private String pendingPhone;
    private String pendingAddress;
    
    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        authService = new AuthenticationService();
        emailService = new EmailNotificationService();
        
        if (requirementsLabel != null) {
            requirementsLabel.setText(authService.getPasswordRequirements());
        }
        
        // Setup auto-focus for code fields
        setupCodeFieldListeners();
        
        // Setup Enter key navigation for registration form fields
        setupEnterKeyNavigation();
    }

    /**
     * Sets up Enter key navigation between registration form fields.
     * When Enter is pressed, focus moves to the next field.
     * On the last field (address), Enter triggers the send code button.
     */
    private void setupEnterKeyNavigation() {
        // Define field order
        if (usernameField != null) {
            usernameField.setOnAction(e -> {
                if (emailField != null) {
                    emailField.requestFocus();
                }
            });
        }
        
        if (emailField != null) {
            emailField.setOnAction(e -> {
                if (passwordField != null) {
                    passwordField.requestFocus();
                }
            });
        }
        
        if (passwordField != null) {
            passwordField.setOnAction(e -> {
                if (confirmPasswordField != null) {
                    confirmPasswordField.requestFocus();
                }
            });
        }
        
        if (confirmPasswordField != null) {
            confirmPasswordField.setOnAction(e -> {
                if (phoneField != null) {
                    phoneField.requestFocus();
                }
            });
        }
        
        if (phoneField != null) {
            phoneField.setOnAction(e -> {
                if (addressField != null) {
                    addressField.requestFocus();
                }
            });
        }
        
        // Last field - trigger send code button
        if (addressField != null) {
            addressField.setOnAction(e -> {
                if (sendCodeButton != null) {
                    sendCodeButton.fire();
                }
            });
        }
    }

    /**
     * Sets up listeners for code input fields to auto-advance.
     */
    private void setupCodeFieldListeners() {
        MFXTextField[] codeFields = {code1, code2, code3, code4, code5, code6};
        
        for (int i = 0; i < codeFields.length; i++) {
            final int index = i;
            MFXTextField field = codeFields[i];
            
            if (field != null) {
                field.textProperty().addListener((obs, oldVal, newVal) -> {
                    // Limit to 1 character
                    if (newVal.length() > 1) {
                        field.setText(newVal.substring(0, 1));
                    }
                    // Auto-advance to next field
                    if (newVal.length() == 1 && index < codeFields.length - 1) {
                        codeFields[index + 1].requestFocus();
                    }
                });
            }
        }
    }

    /**
     * Generates a random 6-digit verification code.
     */
    private String generateVerificationCode() {
        int code = 100000 + RANDOM.nextInt(900000);
        return String.valueOf(code);
    }

    /**
     * Gets the entered code from all 6 fields.
     */
    private String getEnteredCode() {
        return code1.getText() + code2.getText() + code3.getText() +
               code4.getText() + code5.getText() + code6.getText();
    }

    /**
     * Clears all code input fields.
     */
    private void clearCodeFields() {
        code1.clear();
        code2.clear();
        code3.clear();
        code4.clear();
        code5.clear();
        code6.clear();
        code1.requestFocus();
    }

    // ==================== STEP 1: SEND VERIFICATION CODE ====================

    @FXML
    private void handleSendVerificationCode(ActionEvent event) {
        System.out.println("DEBUG: handleSendVerificationCode called");
        
        String username = usernameField != null ? usernameField.getText().trim() : "";
        String password = passwordField != null ? passwordField.getText().trim() : "";
        String confirmPassword = confirmPasswordField != null ? confirmPasswordField.getText().trim() : "";
        String email = emailField != null ? emailField.getText().trim() : "";
        String phone = phoneField != null ? phoneField.getText().trim() : "";
        String address = addressField != null ? addressField.getText().trim() : "";
        
        System.out.println("DEBUG: username='" + username + "', email='" + email + "', address='" + address + "'");
        
        // Validate all fields
        if (username.isEmpty()) {
            showError(step1MessageLabel, "Username is required");
            return;
        }
        
        if (email.isEmpty()) {
            showError(step1MessageLabel, "Email is required for verification");
            return;
        }
        
        String emailError = getEmailValidationError(email);
        if (emailError != null) {
            showError(step1MessageLabel, emailError);
            return;
        }
        
        if (password.isEmpty()) {
            showError(step1MessageLabel, "Password is required");
            return;
        }
        
        if (!password.equals(confirmPassword)) {
            showError(step1MessageLabel, "Passwords do not match");
            return;
        }
        
        if (!authService.isPasswordValid(password)) {
            showError(step1MessageLabel, authService.getPasswordRequirements());
            return;
        }
        
        if (!authService.isUsernameAvailable(username)) {
            showError(step1MessageLabel, "Username already taken");
            return;
        }
        
        if (address.isEmpty()) {
            showError(step1MessageLabel, "Delivery address is required");
            return;
        }
        
        String addressError = getAddressValidationError(address);
        if (addressError != null) {
            showError(step1MessageLabel, addressError);
            return;
        }
        
        // Validate phone number if provided
        if (!phone.isEmpty()) {
            String phoneError = getPhoneValidationError(phone);
            if (phoneError != null) {
                showError(step1MessageLabel, phoneError);
                return;
            }
        }
        
        // Store pending registration data
        pendingUsername = username;
        pendingPassword = password;
        pendingEmail = email;
        // Normalize phone number (remove spaces) before storing
        pendingPhone = phone.isEmpty() ? phone : phone.replaceAll("\\s+", "");
        pendingAddress = address;
        
        // Generate and send verification code
        generatedCode = generateVerificationCode();
        System.out.println("📧 Registration Verification Code for " + email + ": " + generatedCode);
        
        boolean sent = emailService.sendVerificationCode(email, username, generatedCode);
        System.out.println("DEBUG: Email sent result: " + sent);
        
        if (sent) {
            // Move to step 2
            System.out.println("DEBUG: Moving to step 2");
            step1Container.setVisible(false);
            step1Container.setManaged(false);
            step2Container.setVisible(true);
            step2Container.setManaged(true);
            codeSentToLabel.setText("Code sent to " + maskEmail(email));
            code1.requestFocus();
        } else {
            showError(step1MessageLabel, "Failed to send verification code. Please try again.");
        }
    }

    // ==================== STEP 2: VERIFY AND REGISTER ====================

    @FXML
    private void handleVerifyAndRegister(ActionEvent event) {
        String enteredCode = getEnteredCode();
        
        if (enteredCode.length() != 6) {
            showError(step2MessageLabel, "Please enter all 6 digits");
            return;
        }
        
        if (!enteredCode.equals(generatedCode)) {
            showError(step2MessageLabel, "Invalid code. Please try again.");
            clearCodeFields();
            return;
        }
        
        // Code verified - create the account
        try {
            User user = authService.registerCustomer(
                pendingUsername, 
                pendingPassword, 
                pendingAddress, 
                pendingPhone, 
                pendingEmail
            );
            
            if (user != null) {
                // Mark email as verified
                user.setEmailVerified(true);
                
                // Send welcome email
                emailService.sendWelcomeEmail(user);
                
                showSuccess(step2MessageLabel, "✓ Account created successfully! Redirecting to login...");
                
                // Redirect to login after short delay
                javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(
                    javafx.util.Duration.seconds(2)
                );
                pause.setOnFinished(e -> {
                    try {
                        MainApplication.switchScene("/fxml/LoginView.fxml", "Group03 - GreenGrocer");
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                });
                pause.play();
            } else {
                showError(step2MessageLabel, "Registration failed. Please try again.");
            }
        } catch (IllegalArgumentException e) {
            showError(step2MessageLabel, e.getMessage());
        }
    }

    @FXML
    private void handleResendCode(ActionEvent event) {
        // Generate new code and resend
        generatedCode = generateVerificationCode();
        System.out.println("📧 New Verification Code for " + pendingEmail + ": " + generatedCode);
        
        boolean sent = emailService.sendVerificationCode(pendingEmail, pendingUsername, generatedCode);
        
        if (sent) {
            showSuccess(step2MessageLabel, "New code sent!");
            clearCodeFields();
        } else {
            showError(step2MessageLabel, "Failed to resend code");
        }
    }

    @FXML
    private void handleBackToStep1(ActionEvent event) {
        // Go back to step 1
        step2Container.setVisible(false);
        step2Container.setManaged(false);
        step1Container.setVisible(true);
        step1Container.setManaged(true);
        clearCodeFields();
    }

    // ==================== LEGACY METHODS (kept for compatibility) ====================

    /**
     * Legacy register handler - redirects to new verification flow.
     * @deprecated Use handleSendVerificationCode instead
     */
    @Deprecated
    @FXML
    private void handleRegister(ActionEvent event) {
        // Redirect to new flow
        handleSendVerificationCode(event);
    }

    @FXML
    private void handleBackToLogin(ActionEvent event) {
        try {
            MainApplication.switchScene("/fxml/LoginView.fxml", "Green as your money - Login");
        } catch (Exception e) {
            showError(step1MessageLabel, "Could not navigate to login");
        }
    }
    
    @FXML
    private void handleOpenMap(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MapPopupView.fxml"));
            Parent root = loader.load();
            
            MapPopupController mapController = loader.getController();
            
            // Set callback for registration mode - when location is confirmed, update the address field
            mapController.setRegistrationCallback((address, regionId) -> {
                if (addressField != null) {
                    addressField.setText(address);
                }
            });
            
            Stage mapStage = new Stage();
            mapStage.initModality(Modality.APPLICATION_MODAL);
            mapStage.setTitle("Select Delivery Address");
            
            Scene scene = new Scene(root, 700, 550);
            scene.getStylesheets().add(getClass().getResource("/css/green-money-theme.css").toExternalForm());
            
            mapStage.setScene(scene);
            mapStage.centerOnScreen();
            mapStage.showAndWait();
            
        } catch (Exception e) {
            showError(step1MessageLabel, "Could not open map: " + e.getMessage());
        }
    }

    @FXML
    private void validateUsername() {
        String username = usernameField.getText().trim();
        if (!username.isEmpty() && !authService.isUsernameAvailable(username)) {
            usernameField.setStyle("-mfx-border-color: #F44336;");
        } else {
            usernameField.setStyle("");
        }
    }

    @FXML
    private void validatePasswordMatch() {
        String password = passwordField.getText().trim();
        String confirm = confirmPasswordField.getText().trim();
        
        if (!confirm.isEmpty() && !password.equals(confirm)) {
            confirmPasswordField.setStyle("-mfx-border-color: #F44336;");
        } else {
            confirmPasswordField.setStyle("");
        }
    }

    // ==================== HELPERS ====================

    private boolean isValidEmail(String email) {
        if (email == null || email.isEmpty()) {
            return false;
        }
        
        // Check minimum length of 12 characters
        if (email.length() < 12) {
            return false;
        }
        
        // Basic email pattern check
        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            return false;
        }
        
        // Check that between '@' and last '.' there are at least 4 characters
        int atIndex = email.indexOf('@');
        int lastDotIndex = email.lastIndexOf('.');
        
        if (atIndex == -1 || lastDotIndex == -1 || lastDotIndex <= atIndex) {
            return false;
        }
        
        String domainPart = email.substring(atIndex + 1, lastDotIndex);
        if (domainPart.length() < 4) {
            return false;
        }
        
        return true;
    }
    
    private String getEmailValidationError(String email) {
        if (email == null || email.isEmpty()) {
            return "Email is required";
        }
        
        if (email.length() < 12) {
            return "Email must be at least 12 characters long";
        }
        
        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            return "Please enter a valid email address";
        }
        
        int atIndex = email.indexOf('@');
        int lastDotIndex = email.lastIndexOf('.');
        
        if (atIndex == -1 || lastDotIndex == -1 || lastDotIndex <= atIndex) {
            return "Please enter a valid email address";
        }
        
        String domainPart = email.substring(atIndex + 1, lastDotIndex);
        if (domainPart.length() < 4) {
            return "Email domain must have at least 4 characters between '@' and the last '.'";
        }
        
        return null;
    }
    
    private boolean isValidPhone(String phone) {
        if (phone == null || phone.isEmpty()) {
            return false;
        }
        
        // Remove any spaces
        String cleaned = phone.replaceAll("\\s+", "");
        
        // Check format: 11 digits starting with 0
        return cleaned.matches("^0\\d{10}$");
    }
    
    private String getPhoneValidationError(String phone) {
        if (phone == null || phone.isEmpty()) {
            return "Phone number is required";
        }
        
        // Remove any spaces
        String cleaned = phone.replaceAll("\\s+", "");
        
        if (!cleaned.matches("^0\\d{10}$")) {
            return "Phone number must be in format '05334589243' (11 digits starting with 0, no spaces allowed)";
        }
        
        return null;
    }
    
    private boolean isValidAddress(String address) {
        if (address == null || address.isEmpty()) {
            return false;
        }
        
        // Check that it has at least one space (district and city separated by space)
        // Format: "District Istanbul" (no comma, just space)
        if (!address.matches("^\\S+\\s+\\S+.*$")) {
            return false;
        }
        
        // Check that it includes Istanbul or İstanbul (case insensitive)
        // Handle both regular 'I' and Turkish 'İ' characters
        // Turkish 'İ' (capital I with dot) becomes 'i' when lowercased
        // Regular 'I' becomes 'ı' (Turkish lowercase i) when lowercased in Turkish locale
        // So we normalize both 'ı' and 'i' to check for "istanbul"
        String normalizedAddress = address.toLowerCase()
            .replace('ı', 'i'); // Normalize Turkish lowercase 'ı' to 'i'
        
        return normalizedAddress.contains("istanbul");
    }
    
    private String getAddressValidationError(String address) {
        if (address == null || address.isEmpty()) {
            return "Delivery address is required";
        }
        
        // Check that it has at least one space (district and city separated by space)
        if (!address.matches("^\\S+\\s+\\S+.*$")) {
            return "Address must be in format 'District Istanbul' (space required between district and city, no comma)";
        }
        
        // Check that it includes Istanbul or İstanbul (case insensitive)
        // Handle both regular 'I' and Turkish 'İ' characters
        // Turkish 'İ' (capital I with dot) becomes 'i' when lowercased
        // Regular 'I' becomes 'ı' (Turkish lowercase i) when lowercased in Turkish locale
        // So we normalize both 'ı' and 'i' to check for "istanbul"
        String normalizedAddress = address.toLowerCase()
            .replace('ı', 'i'); // Normalize Turkish lowercase 'ı' to 'i'
        
        if (!normalizedAddress.contains("istanbul")) {
            return "Address must include 'Istanbul' or 'İstanbul'";
        }
        
        return null;
    }

    private String maskEmail(String email) {
        int atIndex = email.indexOf('@');
        if (atIndex <= 2) return email;
        return email.substring(0, 2) + "***" + email.substring(atIndex);
    }

    private void showError(Label label, String message) {
        System.out.println("ERROR: " + message);
        if (label != null) {
            label.setText(message);
            label.setStyle("-fx-text-fill: #F44336;");
        } else {
            // Fallback to alert if label is null
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.ERROR
            );
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        }
    }

    private void showSuccess(Label label, String message) {
        System.out.println("SUCCESS: " + message);
        if (label != null) {
            label.setText(message);
            label.setStyle("-fx-text-fill: #4CAF50;");
        } else {
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.INFORMATION
            );
            alert.setTitle("Success");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        }
    }
}
