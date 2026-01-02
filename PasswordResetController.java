package com.greengrocer.controllers;

import com.greengrocer.app.MainApplication;
import com.greengrocer.dao.UserDAO;
import com.greengrocer.models.User;
import com.greengrocer.services.AuthenticationService;
import com.greengrocer.services.EmailNotificationService;
import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.controls.MFXPasswordField;
import io.github.palexdev.materialfx.controls.MFXTextField;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.security.SecureRandom;
import java.util.ResourceBundle;

/**
 * Controller for the password reset view.
 * Implements 3-step password reset:
 * 1. Enter email
 * 2. Verify 6-digit code
 * 3. Set new password
 * 
 * @author Green Grocer Team
 * @version 1.0
 */
public class PasswordResetController implements Initializable {

    // Step 1 elements
    @FXML private VBox step1Container;
    @FXML private MFXTextField emailField;
    @FXML private MFXButton sendCodeButton;
    @FXML private Label step1MessageLabel;

    // Step 2 elements
    @FXML private VBox step2Container;
    @FXML private Label codeSentLabel;
    @FXML private MFXTextField code1, code2, code3, code4, code5, code6;
    @FXML private MFXButton verifyCodeButton;
    @FXML private Label step2MessageLabel;

    // Step 3 elements
    @FXML private VBox step3Container;
    @FXML private MFXPasswordField newPasswordField;
    @FXML private MFXPasswordField confirmPasswordField;
    @FXML private MFXButton resetPasswordButton;
    @FXML private Label step3MessageLabel;

    // Services
    private UserDAO userDAO;
    private AuthenticationService authService;
    private EmailNotificationService emailService;

    // State
    private String currentEmail;
    private String generatedCode;
    private User userToReset;
    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        userDAO = new UserDAO();
        authService = new AuthenticationService();
        emailService = new EmailNotificationService();
        
        // Setup auto-focus for code fields
        setupCodeFieldListeners();
    }

    /**
     * Sets up listeners for code input fields to auto-advance.
     */
    private void setupCodeFieldListeners() {
        MFXTextField[] codeFields = {code1, code2, code3, code4, code5, code6};
        
        for (int i = 0; i < codeFields.length; i++) {
            final int index = i;
            MFXTextField field = codeFields[i];
            
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

    /**
     * Generates a random 6-digit code.
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

    // ==================== STEP 1: SEND CODE ====================

    @FXML
    private void handleSendCode(ActionEvent event) {
        String email = emailField.getText().trim();
        
        if (email.isEmpty()) {
            showError(step1MessageLabel, "Please enter your email address");
            return;
        }
        
        if (!isValidEmail(email)) {
            showError(step1MessageLabel, "Please enter a valid email address");
            return;
        }
        
        // Check if user exists with this email
        userToReset = userDAO.findByEmail(email);
        if (userToReset == null) {
            showError(step1MessageLabel, "No account found with this email");
            return;
        }
        
        // Generate and send code
        currentEmail = email;
        generatedCode = generateVerificationCode();
        
        System.out.println("🔐 Password Reset Code for " + email + ": " + generatedCode);
        
        boolean sent = emailService.sendPasswordResetCode(userToReset, generatedCode);
        
        if (sent) {
            // Move to step 2
            step1Container.setVisible(false);
            step1Container.setManaged(false);
            step2Container.setVisible(true);
            step2Container.setManaged(true);
            codeSentLabel.setText("Code sent to " + maskEmail(email));
            code1.requestFocus();
        } else {
            showError(step1MessageLabel, "Failed to send verification code. Please try again.");
        }
    }

    // ==================== STEP 2: VERIFY CODE ====================

    @FXML
    private void handleVerifyCode(ActionEvent event) {
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
        
        // Code verified - move to step 3
        step2Container.setVisible(false);
        step2Container.setManaged(false);
        step3Container.setVisible(true);
        step3Container.setManaged(true);
        newPasswordField.requestFocus();
    }

    @FXML
    private void handleResendCode(ActionEvent event) {
        // Generate new code and resend
        generatedCode = generateVerificationCode();
        System.out.println("🔐 New Password Reset Code for " + currentEmail + ": " + generatedCode);
        
        boolean sent = emailService.sendPasswordResetCode(userToReset, generatedCode);
        
        if (sent) {
            showSuccess(step2MessageLabel, "New code sent!");
            clearCodeFields();
        } else {
            showError(step2MessageLabel, "Failed to resend code");
        }
    }

    // ==================== STEP 3: RESET PASSWORD ====================

    @FXML
    private void handleResetPassword(ActionEvent event) {
        String newPassword = newPasswordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        
        if (newPassword.isEmpty()) {
            showError(step3MessageLabel, "Please enter a new password");
            return;
        }
        
        if (!newPassword.equals(confirmPassword)) {
            showError(step3MessageLabel, "Passwords do not match");
            return;
        }
        
        if (!authService.isPasswordValid(newPassword)) {
            showError(step3MessageLabel, authService.getPasswordRequirements());
            return;
        }
        
        // Update password in database
        boolean updated = userDAO.updatePassword(userToReset.getId(), newPassword);
        
        if (updated) {
            showSuccess(step3MessageLabel, "Password reset successful! Redirecting to login...");
            
            // Redirect to login after short delay
            javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(2));
            pause.setOnFinished(e -> {
                try {
                    MainApplication.switchScene("/fxml/LoginView.fxml", "Group03 - GreenGrocer");
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });
            pause.play();
        } else {
            showError(step3MessageLabel, "Failed to reset password. Please try again.");
        }
    }

    // ==================== NAVIGATION ====================

    @FXML
    private void handleBackToLogin(ActionEvent event) {
        try {
            MainApplication.switchScene("/fxml/LoginView.fxml", "Green as your money - Login");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ==================== HELPERS ====================

    private boolean isValidEmail(String email) {
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    private String maskEmail(String email) {
        int atIndex = email.indexOf('@');
        if (atIndex <= 2) return email;
        return email.substring(0, 2) + "***" + email.substring(atIndex);
    }

    private void showError(Label label, String message) {
        label.setText(message);
        label.setStyle("-fx-text-fill: #F44336;");
    }

    private void showSuccess(Label label, String message) {
        label.setText(message);
        label.setStyle("-fx-text-fill: #4CAF50;");
    }
}

