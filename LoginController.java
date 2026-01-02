package com.greengrocer.controllers;

import com.greengrocer.app.MainApplication;
import com.greengrocer.app.ThemeManager;
import com.greengrocer.models.User;
import com.greengrocer.services.AuthenticationService;
import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.controls.MFXPasswordField;
import io.github.palexdev.materialfx.controls.MFXTextField;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.util.Duration;
import javafx.animation.PauseTransition;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * controller for the login view
 * 
 * @author Barkev Şarklı
 * @version 1.0
 * @since 24.12.2025
 */
public class LoginController implements Initializable
{
    @FXML
    private MFXTextField usernameField;
    
    @FXML
    private MFXPasswordField passwordField;
    
    @FXML
    private MFXButton loginButton;
    
    @FXML
    private Label messageLabel;
    
    @FXML
    private ImageView mascotImageView;

    private AuthenticationService authService;
    
    private static final String MASCOT_STARE = "/images/stare.png";
    private static final String MASCOT_TYPING = "/images/typing.png";
    private static final String MASCOT_NOT_LOOKING = "/images/not-looking.png";
    private static final String MASCOT_LAUGH = "/images/laugh.png";

    /**
     * initialize the controller
     * @param location FXML location
     * @param resources resource bundle
     * @author Barkev Şarklı
     * @version 1.0
     */
    @Override
    public void initialize(URL location, ResourceBundle resources)
    {
        authService = new AuthenticationService();
        
        if (messageLabel != null)
            messageLabel.setText("");
        
        // Initialize mascot image with default stare image
        if (mascotImageView != null)
        {
            updateMascotImage(MASCOT_STARE);
        }
        
        // Setup mascot reactions to user input
        setupMascotReactions();
        
        // Setup Enter key handlers
        setupEnterKeyHandlers();
    }
    
    /**
     * Setup Enter key handlers for username and password fields
     */
    private void setupEnterKeyHandlers()
    {
        // Username field: On Enter, move focus to password field
        if (usernameField != null)
        {
            usernameField.setOnKeyPressed(event -> {
                if (event.getCode() == KeyCode.ENTER)
                {
                    if (passwordField != null)
                    {
                        passwordField.requestFocus();
                    }
                }
            });
        }
        
        // Password field: On Enter, trigger login
        if (passwordField != null)
        {
            passwordField.setOnKeyPressed(event -> {
                if (event.getCode() == KeyCode.ENTER)
                {
                    handleLogin(new ActionEvent());
                }
            });
        }
    }
    
    /**
     * Setup mascot reactions based on user input
     */
    private void setupMascotReactions()
    {
        // Username field: Show typing.png when user starts typing
        if (usernameField != null)
        {
            usernameField.textProperty().addListener((observable, oldValue, newValue) -> updateMascotBasedOnInput());
        }
        
        // Password field: Show not-looking.png when user types password
        if (passwordField != null)
        {
            passwordField.textProperty().addListener((observable, oldValue, newValue) -> updateMascotBasedOnInput());
            
            // When password field gets focus, show not-looking
            passwordField.focusedProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue && mascotImageView != null)
                {
                    updateMascotImage(MASCOT_NOT_LOOKING);
                }
                else
                {
                    updateMascotBasedOnInput();
                }
            });
        }
    }
    
    /**
     * Update mascot image based on current input state
     */
    private void updateMascotBasedOnInput()
    {
        if (mascotImageView == null)
            return;
        
        // Priority 1: If password field has focus or text, show not-looking
        if (passwordField != null && (passwordField.isFocused() || 
            (passwordField.getText() != null && !passwordField.getText().trim().isEmpty())))
        {
            updateMascotImage(MASCOT_NOT_LOOKING);
            return;
        }
        
        // Priority 2: If username has text, show typing
        if (usernameField != null && usernameField.getText() != null && !usernameField.getText().trim().isEmpty())
        {
            updateMascotImage(MASCOT_TYPING);
            return;
        }
        
        // Priority 3: Default to stare
        updateMascotImage(MASCOT_STARE);
    }

    /**
     * handle login button click
     * @param event action event
     * @author Barkev Şarklı
     * @version 1.0
     */
    @FXML
    private void handleLogin(ActionEvent event)
    {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();
        
        System.out.println("DEBUG: Username field value: '" + username + "' (length=" + username.length() + ")");
        System.out.println("DEBUG: Password field value: '" + password + "' (length=" + password.length() + ")");
        
        if (username.isEmpty())
        {
            showError("Please enter username");
            return;
        }
        
        if (password.isEmpty())
        {
            showError("Please enter password");
            return;
        }
        
        System.out.println("DEBUG: Calling authService.login()...");
        User user = authService.login(username, password);
        System.out.println("DEBUG: Login result: " + (user != null ? "SUCCESS - " + user.getRole() : "FAILED"));
        
        if (user != null)
        {
            // Show laugh image on successful login
            if (mascotImageView != null)
            {
                updateMascotImage(MASCOT_LAUGH);
            }
            // Navigate after a short delay to show the laugh image
            PauseTransition delay = new PauseTransition(Duration.millis(800));
            delay.setOnFinished(e -> navigateToRoleView(user.getRole()));
            delay.play();
        }
        else
        {
            showError("Invalid username or password");
        }
    }

    /**
     * handle register link click
     * @param event action event
     * @author Barkev Şarklı
     * @version 2.0
     */
    @FXML
    private void handleRegisterLink(ActionEvent event)
    {
        System.out.println("DEBUG: handleRegisterLink clicked");
        try
        {
            MainApplication.switchScene("/fxml/RegistrationView.fxml", "Group03 - GreenGrocer");
            System.out.println("DEBUG: Switched to registration view");
        }
        catch (Exception e)
        {
            System.err.println("ERROR: Could not open registration form: " + e.getMessage());
            e.printStackTrace();
            showError("Could not open registration form");
        }
    }

    /**
     * navigate to the appropriate view based on user role
     * @param role user role
     * @author Barkev Şarklı
     * @version 2.0
     */
    private void navigateToRoleView(String role)
    {
        try
        {
            String fxmlPath;
            String title = "Group03 - GreenGrocer";
            
            switch (role.toLowerCase())
            {
                case "customer":
                    fxmlPath = "/fxml/CustomerMainView.fxml";
                    title = "Group03 - GreenGrocer";
                    break;
                case "carrier":
                    fxmlPath = "/fxml/CarrierMainView.fxml";
                    title = "Group03 - GreenGrocer";
                    break;
                case "owner":
                    fxmlPath = "/fxml/OwnerMainView.fxml";
                    title = "Group03 - GreenGrocer";
                    break;
                default:
                    showError("Unknown user role");
                    return;
            }
            
            MainApplication.switchScene(fxmlPath, title);
        }
        catch (Exception e)
        {
            showError("Could not load application view");
            e.printStackTrace();
        }
    }

    private void showError(String message)
    {
        messageLabel.setText(message);
        messageLabel.setStyle("-fx-text-fill: #F44336;");
    }

    private void clearError()
    {
        messageLabel.setText("");
    }
    
    /**
     * Update the mascot image
     * @param imagePath path to the image resource
     */
    private void updateMascotImage(String imagePath)
    {
        try
        {
            Image image = new Image(getClass().getResourceAsStream(imagePath));
            mascotImageView.setImage(image);
        }
        catch (Exception e)
        {
            System.err.println("ERROR: Could not load mascot image: " + imagePath);
            e.printStackTrace();
        }
    }

    /**
     * Toggle between available themes
     * @param event action event
     * @author Barkev Şarklı
     * @version 4.0
     */
    @FXML
    private void handleToggleTheme(ActionEvent event)
    {
        System.out.println("DEBUG LoginController: handleToggleTheme() called");
        try {
            System.out.println("DEBUG LoginController: loginButton = " + loginButton);
            System.out.println("DEBUG LoginController: loginButton.getScene() = " + loginButton.getScene());
            ThemeManager.getInstance().toggleTheme(loginButton.getScene());
        } catch (Exception e) {
            System.err.println("ERROR LoginController: Error toggling theme: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Handle forgot password link click
     * Opens the password reset view
     * @param event action event
     */
    @FXML
    private void handleForgotPassword(ActionEvent event)
    {
        try
        {
            MainApplication.switchScene("/fxml/PasswordResetView.fxml", "Group03 - GreenGrocer");
        }
        catch (Exception e)
        {
            showError("Could not open password reset form");
            e.printStackTrace();
        }
    }
}
