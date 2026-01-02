package com.greengrocer.controllers;

import com.greengrocer.app.SessionManager;
import com.greengrocer.models.Region;
import com.greengrocer.models.User;
import com.greengrocer.services.RegionService;
import com.greengrocer.services.UserService;
import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.controls.MFXTextField;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.BiConsumer;

/**
 * Controller for the interactive map popup.
 * Displays Istanbul districts with distance-based pricing from Fatih.
 * Users can select their district to see price adjustments and delivery fees.
 * 
 * @author Barkev Şarklı 
 * @version 3.0 - Distance-based pricing from Fatih
 */
public class MapPopupController implements Initializable {
    
    @FXML
    private FlowPane europeanDistrictsPane;
    
    @FXML
    private FlowPane asianDistrictsPane;
    
    @FXML
    private MFXTextField addressField;
    
    @FXML
    private Label selectedDistrictLabel;
    
    @FXML
    private Label distanceLabel;
    
    @FXML
    private Label priceAdjustmentLabel;
    
    @FXML
    private Label deliveryFeeLabel;
    
    @FXML
    private VBox selectionInfoPanel;
    
    /** Currently selected region */
    private Region selectedRegion;
    
    /** Region service for loading districts */
    private RegionService regionService;
    
    /** User service for updating address */
    private UserService userService;
    
    /** Callback for registration mode - called with (address, regionId) when location is confirmed */
    private BiConsumer<String, Integer> registrationCallback;
    
    // European side districts (west of Bosphorus)
    private static final String[] EUROPEAN_DISTRICTS = {
        "Fatih", "Beyoglu", "Eminonu", "Eyupsultan", "Besiktas", "Sisli", 
        "Bakirkoy", "Zeytinburnu", "Bayrampasa", "Gungoren", "Bahcelievler",
        "Avcilar", "Kucukcekmece", "Buyukcekmece", "Esenyurt", "Beylikduzu"
    };
    
    // Asian side districts (east of Bosphorus)
    private static final String[] ASIAN_DISTRICTS = {
        "Kadikoy", "Uskudar", "Atasehir", "Maltepe", "Kartal", "Pendik",
        "Tuzla", "Umraniye", "Sancaktepe", "Sultanbeyli", "Beykoz", "Cekmekoy"
    };

    /**
     * Initializes the map popup controller.
     * Loads all Istanbul districts from the database.
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        regionService = RegionService.getInstance();
        userService = new UserService();
        
        // Load districts into the UI
        loadDistricts();
        
        // Load current user's region if available
        loadCurrentUserRegion();
    }
    
    /**
     * Loads districts from the database and creates buttons for each.
     */
    private void loadDistricts() {
        List<Region> istanbulRegions = regionService.getRegionsByCity("Istanbul");
        
        // Create buttons for European side
        for (String districtName : EUROPEAN_DISTRICTS) {
            Region region = findRegionByName(istanbulRegions, districtName);
            if (region != null) {
                europeanDistrictsPane.getChildren().add(createDistrictButton(region));
            } else {
                // Create a placeholder with estimated distance
                europeanDistrictsPane.getChildren().add(createPlaceholderButton(districtName, true));
            }
        }
        
        // Create buttons for Asian side
        for (String districtName : ASIAN_DISTRICTS) {
            Region region = findRegionByName(istanbulRegions, districtName);
            if (region != null) {
                asianDistrictsPane.getChildren().add(createDistrictButton(region));
            } else {
                // Create a placeholder with estimated distance
                asianDistrictsPane.getChildren().add(createPlaceholderButton(districtName, false));
            }
        }
    }
    
    /**
     * Finds a region by name from a list.
     */
    private Region findRegionByName(List<Region> regions, String name) {
        return regions.stream()
                .filter(r -> r.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * Creates a district selection button with pricing info.
     */
    private VBox createDistrictButton(Region region) {
        VBox card = new VBox(4);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(10, 14, 10, 14));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 8; " +
                     "-fx-border-color: #41644A; -fx-border-radius: 8; -fx-cursor: hand;");
        card.setPrefWidth(135);
        
        // District name
        Label nameLabel = new Label(region.getName());
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #0D4715;");
        
        // Distance info
        String distText = region.getDistanceKm() == 0 ? "Store" : String.format("%.0f km", region.getDistanceKm());
        Label distLabel = new Label(distText);
        distLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #41644A;");
        
        // Price adjustment
        String priceText = region.getDistanceKm() == 0 ? "Base Price" : region.getPriceDifferenceText();
        Label priceLabel = new Label(priceText);
        priceLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #E9762B; -fx-font-weight: bold;");
        
        card.getChildren().addAll(nameLabel, distLabel, priceLabel);
        
        // Click handler
        card.setOnMouseClicked(e -> selectRegion(region));
        
        // Hover effect
        card.setOnMouseEntered(e -> card.setStyle(
            "-fx-background-color: #41644A; -fx-background-radius: 8; " +
            "-fx-border-color: #0D4715; -fx-border-radius: 8; -fx-cursor: hand;"));
        card.setOnMouseExited(e -> card.setStyle(
            "-fx-background-color: white; -fx-background-radius: 8; " +
            "-fx-border-color: #41644A; -fx-border-radius: 8; -fx-cursor: hand;"));
        
        // Highlight if it's the store location (Fatih)
        if (region.getDistanceKm() == 0) {
            card.setStyle("-fx-background-color: #E9762B; -fx-background-radius: 8; " +
                         "-fx-border-color: #0D4715; -fx-border-radius: 8; -fx-cursor: hand;");
            nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: white;");
            distLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: white;");
            priceLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: white; -fx-font-weight: bold;");
        }
        
        return card;
    }
    
    /**
     * Creates a placeholder button for districts not in the database.
     */
    private VBox createPlaceholderButton(String name, boolean isEuropean) {
        // Estimate distance based on typical Istanbul geography
        double estimatedDistance = isEuropean ? 15.0 : 18.0;
        
        Region tempRegion = new Region(name, "Istanbul", estimatedDistance);
        return createDistrictButton(tempRegion);
    }
    
    /**
     * Handles selecting a region/district.
     */
    private void selectRegion(Region region) {
        this.selectedRegion = region;
        
        // Update the info panel
        selectedDistrictLabel.setText(region.getName() + ", Istanbul");
        
        if (region.getDistanceKm() == 0) {
            distanceLabel.setText("Here!");
            distanceLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #E9762B;");
        } else {
            distanceLabel.setText(String.format("%.0f km", region.getDistanceKm()));
            distanceLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #41644A;");
        }
        
        priceAdjustmentLabel.setText(region.getPriceDifferenceText());
        deliveryFeeLabel.setText(String.format("₺%.2f", region.getDeliveryFee()));
        
        // Pre-fill address with district name
        if (addressField.getText().isEmpty() || !addressField.getText().contains(region.getName())) {
            addressField.setText(region.getName() + ", Istanbul");
        }
    }
    
    /**
     * Loads the current user's region if available.
     */
    private void loadCurrentUserRegion() {
        User currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser != null) {
            // Load user's current address
            if (currentUser.getAddress() != null && !currentUser.getAddress().isEmpty()) {
                addressField.setText(currentUser.getAddress());
            }
            
            // Load user's current region
            if (currentUser.getRegionId() > 0) {
                Region userRegion = regionService.getRegionById(currentUser.getRegionId());
                if (userRegion != null) {
                    selectRegion(userRegion);
                }
            }
        }
    }
    
    /**
     * Sets a callback for registration mode.
     * When location is confirmed during registration, this callback will be called with the address and region ID.
     */
    public void setRegistrationCallback(BiConsumer<String, Integer> callback) {
        this.registrationCallback = callback;
    }
    
    /**
     * Handles confirming the selected address.
     * Saves the region and address to the user's profile, or calls registration callback if in registration mode.
     */
    @FXML
    private void handleConfirmAddress(ActionEvent event) {
        if (selectedRegion == null) {
            showError("Please select a district from the map first");
            return;
        }
        
        String address = addressField.getText().trim();
        if (address.isEmpty()) {
            showError("Please enter your specific address");
            return;
        }
        
        // If in registration mode, call the callback and close
        if (registrationCallback != null) {
            int regionId = selectedRegion.getId() > 0 ? selectedRegion.getId() : 0;
            registrationCallback.accept(address, regionId);
            handleClose(event);
            return;
        }
        
        // Otherwise, update existing user's region and address
        User currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser != null) {
            try {
                // Update region
                if (selectedRegion.getId() > 0) {
                    userService.updateUserRegion(currentUser.getId(), selectedRegion.getId());
                    currentUser.setRegionId(selectedRegion.getId());
                    regionService.setCurrentRegion(selectedRegion);
                }
                
                // Update address
                currentUser.setAddress(address);
                userService.updateUserAddress(currentUser.getId(), address);
                
                // Show success with pricing info
                showInfo("Location Updated!\n\n" + selectedRegion.getPricingSummary());
                handleClose(event);
                
            } catch (Exception e) {
                showError("Failed to save location: " + e.getMessage());
            }
        } else {
            showError("No user session found. Please log in first.");
        }
    }
    
    /**
     * Handles closing the popup.
     */
    @FXML
    private void handleClose(ActionEvent event) {
        Stage stage = (Stage) addressField.getScene().getWindow();
        stage.close();
    }
    
    /**
     * Shows an error alert.
     */
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    /**
     * Shows an info alert.
     */
    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
