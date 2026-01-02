package com.greengrocer.controllers;

import com.greengrocer.app.SessionManager;
import com.greengrocer.dao.UserDAO;
import com.greengrocer.models.User;
import com.greengrocer.utils.PhoneValidator;
import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.controls.MFXTextField;
import javafx.beans.value.ChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controller for profile editing view using MaterialFX components.
 * Allows customers and carriers to update their profile information including
 * detailed address fields and profile image.
 * 
 * @author Emir Kıraç Varol
 * @author Barkev Şarklı
 * @version 2.0.0
 * @since 31.12.2025
 */
public class ProfileEditController implements Initializable {

    /** Username display (not editable) */
    @FXML
    private Label usernameLabel;
    
    /** Tier badge label */
    @FXML
    private Label tierLabel;
    
    /** Profile image container */
    @FXML
    private StackPane profileImageContainer;
    
    /** Profile image view */
    @FXML
    private ImageView profileImageView;
    
    /** Email field (MaterialFX) */
    @FXML
    private MFXTextField emailField;
    
    /** Phone field (MaterialFX) */
    @FXML
    private MFXTextField phoneField;
    
    /** Street address field */
    @FXML
    private MFXTextField streetAddressField;
    
    /** Apartment/Unit field */
    @FXML
    private MFXTextField apartmentField;
    
    /** City field */
    @FXML
    private MFXTextField cityField;
    
    /** Postal code field */
    @FXML
    private MFXTextField postalCodeField;
    
    /** Address preview label */
    @FXML
    private Label addressPreviewLabel;
    
    /** Loyalty points display */
    @FXML
    private Label loyaltyPointsLabel;
    
    /** Completed orders display */
    @FXML
    private Label completedOrdersLabel;
    
    /** Tier value display */
    @FXML
    private Label tierValueLabel;
    
    /** Status message label */
    @FXML
    private Label messageLabel;

    /** User data access object */
    private UserDAO userDAO;
    
    /** Current user */
    private User currentUser;
    
    /** Pending profile image data */
    private byte[] pendingImageData = null;
    private boolean imageRemoved = false;

    /**
     * Initializes the controller.
     * 
     * @param location FXML location
     * @param resources Resource bundle
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        userDAO = new UserDAO();
        currentUser = SessionManager.getInstance().getCurrentUser();
        
        setupIstanbulOnlyAddress();
        loadUserData();
        setupAddressPreviewListeners();
        setupProfileImageClip();
    }
    
    /**
     * Sets up address fields to be Istanbul-only.
     * Sets country to Türkiye.
     */
    private void setupIstanbulOnlyAddress() {
        // Set country to Türkiye and make it read-only
        cityField.setText("Türkiye");
        cityField.setEditable(false);
    }
    
    /**
     * Sets up circular clip for profile image.
     */
    private void setupProfileImageClip() {
        Circle clip = new Circle(48, 48, 48);
        profileImageView.setClip(clip);
    }
    
    /**
     * Sets up listeners to update address preview when fields change.
     */
    private void setupAddressPreviewListeners() {
        ChangeListener<String> previewUpdater = (obs, oldVal, newVal) -> updateAddressPreview();
        
        streetAddressField.textProperty().addListener(previewUpdater);
        apartmentField.textProperty().addListener(previewUpdater);
        cityField.textProperty().addListener(previewUpdater);
        postalCodeField.textProperty().addListener(previewUpdater);
    }
    
    /**
     * Updates the address preview label based on current field values.
     * Shows only the street address.
     */
    private void updateAddressPreview() {
        String street = streetAddressField.getText();
        
        if (street != null && !street.trim().isEmpty()) {
            addressPreviewLabel.setText(street.trim());
            addressPreviewLabel.setStyle("-fx-text-fill: #424242;");
        } else {
            addressPreviewLabel.setText("(Enter street address above)");
            addressPreviewLabel.setStyle("-fx-text-fill: #9E9E9E; -fx-font-style: italic;");
        }
    }

    /**
     * Loads current user data into form fields.
     */
    private void loadUserData() {
        usernameLabel.setText(currentUser.getUsername());
        
        // Set tier badge
        String tier = currentUser.getLoyaltyTier();
        tierLabel.setText("🏆 " + tier + " Member");
        tierLabel.setStyle(getTierStyle(tier));
        
        // Load contact info
        emailField.setText(currentUser.getEmail() != null ? currentUser.getEmail() : "");
        phoneField.setText(currentUser.getPhoneNumber() != null ? currentUser.getPhoneNumber() : "");
        
        // Load detailed address fields
        streetAddressField.setText(currentUser.getStreetAddress() != null ? currentUser.getStreetAddress() : "");
        apartmentField.setText(currentUser.getApartment() != null ? currentUser.getApartment() : "");
        // Country is always Türkiye (set in setupIstanbulOnlyAddress)
        postalCodeField.setText(currentUser.getPostalCode() != null ? currentUser.getPostalCode() : "");
        
        // If detailed fields are empty but legacy address exists, try to parse it
        if (streetAddressField.getText().isEmpty() && currentUser.getAddress() != null && !currentUser.getAddress().isEmpty()) {
            // Put the full address in street field as fallback
            streetAddressField.setText(currentUser.getAddress());
        }
        
        // Load stats
        loyaltyPointsLabel.setText(String.valueOf(currentUser.getLoyaltyPoints()));
        completedOrdersLabel.setText(String.valueOf(currentUser.getCompletedTransactions()));
        tierValueLabel.setText(tier);
        
        // Load profile image
        loadProfileImage();
        
        // Update preview
        updateAddressPreview();
    }
    
    /**
     * Gets the style for tier badge based on tier level.
     */
    private String getTierStyle(String tier) {
        String baseStyle = "-fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 4 12; -fx-background-radius: 12;";
        switch (tier) {
            case "Gold":
                return baseStyle + "-fx-background-color: #FFD700; -fx-text-fill: #5D4037;";
            case "Silver":
                return baseStyle + "-fx-background-color: #C0C0C0; -fx-text-fill: #37474F;";
            case "Bronze":
                return baseStyle + "-fx-background-color: #CD7F32; -fx-text-fill: white;";
            default:
                return baseStyle + "-fx-background-color: #E0E0E0; -fx-text-fill: #616161;";
        }
    }
    
    /**
     * Loads the profile image into the ImageView.
     */
    private void loadProfileImage() {
        Image image = currentUser.getDisplayImage();
        if (image != null) {
            profileImageView.setImage(image);
        } else {
            // Load default placeholder
            loadDefaultProfileImage();
        }
    }
    
    /**
     * Loads the default profile image.
     */
    private void loadDefaultProfileImage() {
        try {
            InputStream is = getClass().getResourceAsStream("/images/default.png");
            if (is != null) {
                profileImageView.setImage(new Image(is));
            }
        } catch (Exception e) {
            System.err.println("Could not load default profile image: " + e.getMessage());
        }
    }
    
    /**
     * Handles uploading a new profile photo.
     */
    @FXML
    private void handleUploadPhoto(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Profile Photo");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif"),
            new FileChooser.ExtensionFilter("All Files", "*.*")
        );
        
        Stage stage = (Stage) profileImageView.getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);
        
        if (file != null) {
            try {
                // Check file size (max 2MB)
                if (file.length() > 2 * 1024 * 1024) {
                    showError("Image file is too large. Maximum size is 2MB.");
                    return;
                }
                
                pendingImageData = Files.readAllBytes(file.toPath());
                imageRemoved = false;
                
                // Preview the image
                Image previewImage = new Image(new ByteArrayInputStream(pendingImageData));
                profileImageView.setImage(previewImage);
                
                showSuccess("Photo selected. Click 'Save Changes' to apply.");
            } catch (Exception e) {
                showError("Could not load image: " + e.getMessage());
            }
        }
    }
    
    /**
     * Handles removing the profile photo.
     */
    @FXML
    private void handleRemovePhoto(ActionEvent event) {
        pendingImageData = null;
        imageRemoved = true;
        loadDefaultProfileImage();
        showSuccess("Photo will be removed. Click 'Save Changes' to apply.");
    }

    /**
     * Handles saving profile changes.
     * 
     * @param event Action event
     */
    @FXML
    private void handleSaveProfile(ActionEvent event) {
        // Validate required fields
        String streetAddress = streetAddressField.getText().trim();
        String district = ""; // District field removed, use empty string
        String city = "Istanbul"; // City is Istanbul for database, but country field displays Türkiye
        
        if (streetAddress.isEmpty()) {
            showError("Street address is required");
            streetAddressField.requestFocus();
            return;
        }
        
        String email = emailField.getText().trim();
        String phone = phoneField.getText().trim();
        String apartment = apartmentField.getText().trim();
        String postalCode = postalCodeField.getText().trim();
        
        // Validate phone number if provided
        if (!phone.isEmpty()) {
            if (!PhoneValidator.isValidTurkishMobile(phone)) {
                showError(PhoneValidator.getValidationErrorMessage(phone));
                phoneField.requestFocus();
                return;
            }
            // Format the phone number
            phone = PhoneValidator.formatTurkishMobile(phone);
        }
        
        try {
            // Update profile with detailed address
            boolean profileSuccess = userDAO.updateProfileDetailed(
                currentUser.getId(),
                streetAddress,
                apartment,
                district,
                city,
                postalCode,
                phone,
                email
            );
            
            // Update profile image if changed
            boolean imageSuccess = true;
            if (pendingImageData != null) {
                imageSuccess = userDAO.updateProfileImage(currentUser.getId(), pendingImageData);
                if (imageSuccess) {
                    currentUser.setProfileImageData(pendingImageData);
                    pendingImageData = null;
                }
            } else if (imageRemoved) {
                imageSuccess = userDAO.updateProfileImage(currentUser.getId(), null);
                if (imageSuccess) {
                    currentUser.setProfileImageData(null);
                    imageRemoved = false;
                }
            }
            
            if (profileSuccess) {
                // Update current user object
                currentUser.setEmail(email);
                currentUser.setPhoneNumber(phone);
                currentUser.setStreetAddress(streetAddress);
                currentUser.setApartment(apartment);
                currentUser.setDistrict(district);
                currentUser.setCity(city);
                currentUser.setPostalCode(postalCode);
                currentUser.setAddress(currentUser.buildFullAddress());
                
                showSuccess("✓ Profile updated successfully!");
            } else {
                showError("Could not update profile");
            }
        } catch (SQLException e) {
            showError("Database error: " + e.getMessage());
        }
    }

    /**
     * Handles canceling and closing window.
     * 
     * @param event Action event
     */
    @FXML
    private void handleCancel(ActionEvent event) {
        ((Stage) usernameLabel.getScene().getWindow()).close();
    }
    
    /**
     * Handles opening the map popup for address selection.
     * 
     * @param event Action event
     */
    @FXML
    private void handleOpenMap(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MapPopupView.fxml"));
            Parent root = loader.load();
            
            Stage mapStage = new Stage();
            mapStage.initModality(Modality.APPLICATION_MODAL);
            mapStage.setTitle("Select Delivery Address");
            
            Scene scene = new Scene(root, 700, 550);
            scene.getStylesheets().add(getClass().getResource("/css/green-money-theme.css").toExternalForm());
            
            mapStage.setScene(scene);
            mapStage.centerOnScreen();
            mapStage.showAndWait();
            
            // Reload user data after map closes (region may have changed)
            currentUser = SessionManager.getInstance().getCurrentUser();
            loadUserData();
        } catch (Exception e) {
            showError("Could not open map: " + e.getMessage());
        }
    }

    /**
     * Shows error message.
     * 
     * @param message Error message
     */
    private void showError(String message) {
        messageLabel.setText(message);
        messageLabel.setStyle("-fx-text-fill: #F44336; -fx-font-weight: bold;");
    }

    /**
     * Shows success message.
     * 
     * @param message Success message
     */
    private void showSuccess(String message) {
        messageLabel.setText(message);
        messageLabel.setStyle("-fx-text-fill: #4CAF50; -fx-font-weight: bold;");
    }
}
