package com.greengrocer.controllers;

import com.greengrocer.app.MainApplication;
import com.greengrocer.app.SessionManager;
import com.greengrocer.app.ThemeManager;
import com.greengrocer.models.Product;
import com.greengrocer.models.User;
import com.greengrocer.services.ProductService;
import com.greengrocer.services.CartService;
import com.greengrocer.services.LoyaltyService;
import com.greengrocer.services.RegionService;
import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.controls.MFXComboBox;
import io.github.palexdev.materialfx.controls.MFXTextField;
import com.greengrocer.models.Region;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.Parent;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controller for the customer main view using MaterialFX components.
 * Handles product browsing, searching, and cart operations.
 * Shows all products immediately upon login (no search required).
 * 
 * @author Barkev Şarklı 
 * @version 2.0 - Green as your money edition
 */
public class CustomerMainController implements Initializable {

    /** Company name label in header */
    @FXML
    private Label companyNameLabel;
    
    /** Logo image view */
    @FXML
    private ImageView logoImageView;
    
    /** Username display label in header */
    @FXML
    private Label usernameLabel;
    
    /** Loyalty tier display label in header */
    @FXML
    private Label loyaltyLabel;
    
    /** Product count label */
    @FXML
    private Label itemCountLabel;
    
    /** Search text field (MaterialFX) */
    @FXML
    private MFXTextField searchField;
    
    /** All products container - shows immediately on login */
    @FXML
    private FlowPane allProductsContainer;
    
    /** Vegetables products container */
    @FXML
    private FlowPane vegetablesContainer;
    
    /** Fruits products container */
    @FXML
    private FlowPane fruitsContainer;
    
    /** Vegetables section VBox */
    @FXML
    private VBox vegetablesSection;
    
    /** Fruits section VBox */
    @FXML
    private VBox fruitsSection;
    
    /** Cart item count label */
    @FXML
    private Label cartCountLabel;
    
    /** Map to store product cards for efficient updates (productId -> VBox card) */
    private java.util.Map<Integer, VBox> productCardMap = new java.util.HashMap<>();
    
    /** Menu button (MaterialFX) */
    @FXML
    private MFXButton menuButton;
    
    /** Chat button */
    @FXML
    private MFXButton chatButton;
    
    /** Region combo box */
    @FXML
    private MFXComboBox<String> regionComboBox;
    
    /** Map button */
    @FXML
    private MFXButton mapButton;
    
    /** Dropdown menu */
    @FXML
    private VBox dropdownMenu;
    
    /** Dropdown overlay background */
    @FXML
    private javafx.scene.layout.Region dropdownOverlay;
    
    /** Dropdown username label */
    @FXML
    private Label dropdownUsernameLabel;
    
    /** Dropdown loyalty label */
    @FXML
    private Label dropdownLoyaltyLabel;
    
    /** Dropdown open state */
    private boolean dropdownOpen = false;

    /** Product service */
    private ProductService productService;
    
    /** Cart service */
    private CartService cartService;
    
    /** Loyalty service */
    private LoyaltyService loyaltyService;
    
    /** Region service */
    private RegionService regionService;
    
    /** Shopping cart window reference */
    private Stage cartStage;
    
    /** Current filter: all, vegetables, fruits */
    private String currentFilter = "all";

    /**
     * Initializes the controller.
     * Immediately loads and displays ALL products so customer can start shopping.
     * 
     * @param location FXML location
     * @param resources Resource bundle
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialize services
        productService = new ProductService();
        cartService = new CartService();
        loyaltyService = new LoyaltyService();
        regionService = RegionService.getInstance();
        
        // Display user info
        User currentUser = SessionManager.getInstance().getCurrentUser();
        
        // Update header user info
        if (usernameLabel != null) {
            usernameLabel.setText(currentUser.getUsername());
        }
        if (loyaltyLabel != null) {
            loyaltyLabel.setText("⭐ " + currentUser.getLoyaltyTier());
        }
        
        // Update dropdown user info
        if (dropdownUsernameLabel != null) {
            dropdownUsernameLabel.setText(currentUser.getUsername());
        }
        if (dropdownLoyaltyLabel != null) {
            dropdownLoyaltyLabel.setText("⭐ " + currentUser.getLoyaltyTier());
        }
        
        // Set current region from user's profile
        if (currentUser.getRegionId() > 0) {
            Region userRegion = regionService.getRegionById(currentUser.getRegionId());
            if (userRegion != null) {
                regionService.setCurrentRegion(userRegion);
            }
        }
        
        // Setup region combo box
        setupRegionComboBox();
        
        // Setup live search listener
        setupLiveSearch();
        
        // Marketing image is set to fixed size in FXML (1200x350) and centered
        
        // IMMEDIATELY load ALL products so customer sees them on login
        loadAllProducts();
        updateCartCount();
    }
    
    /**
     * Sets up the region combo box with available regions.
     */
    private void setupRegionComboBox() {
        if (regionComboBox == null) {
            return;
        }
        
        List<Region> regions = regionService.getIstanbulDistricts();
        ObservableList<String> regionNames = FXCollections.observableArrayList();
        for (Region region : regions) {
            regionNames.add(region.getName());
        }
        regionComboBox.setItems(regionNames);
        
        // Set current region if available
        Region currentRegion = regionService.getCurrentRegion();
        if (currentRegion != null) {
            regionComboBox.setValue(currentRegion.getName());
        }
    }
    
    /**
     * Sets up live search functionality on the search field.
     * Products are filtered as the user types.
     */
    private void setupLiveSearch() {
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            performLiveSearch(newValue);
        });
    }
    
    /**
     * Performs live search based on the current search text.
     * 
     * @param keyword Search keyword
     */
    private void performLiveSearch(String keyword) {
        allProductsContainer.getChildren().clear();
        
        if (keyword == null || keyword.trim().isEmpty()) {
            loadAllProducts();
            return;
        }
        
        keyword = keyword.trim().toLowerCase();
        
        // Search all products
        List<Product> results = new ArrayList<>();
        results.addAll(productService.searchProductsByType(keyword, "vegetable"));
        results.addAll(productService.searchProductsByType(keyword, "fruit"));
        
        itemCountLabel.setText(results.size() + " products found for \"" + keyword + "\"");
        
        for (Product product : results) {
            allProductsContainer.getChildren().add(createProductCard(product));
        }
        
        // Show "no results" message if search is empty
        if (results.isEmpty()) {
            Label noResultsLabel = new Label("No products found for \"" + keyword + "\"");
            noResultsLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #666; -fx-padding: 40px;");
            allProductsContainer.getChildren().add(noResultsLabel);
        }
    }
    
    /**
     * Loads and displays ALL products immediately in the grid.
     * This is the main feature - customer sees products right away.
     */
    private void loadAllProducts() {
        allProductsContainer.getChildren().clear();
        
        // Get ALL available products from database
        List<Product> allProducts = new ArrayList<>();
        allProducts.addAll(productService.getAvailableVegetables());
        allProducts.addAll(productService.getAvailableFruits());
        
        // Update item count label
        itemCountLabel.setText(allProducts.size() + " products available");
        
        // Create product cards and add to grid
        for (Product product : allProducts) {
            allProductsContainer.getChildren().add(createProductCard(product));
        }
    }
    
    /**
     * Loads only vegetables (filtered view)
     */
    private void loadVegetablesOnly() {
        allProductsContainer.getChildren().clear();
        
        List<Product> vegetables = productService.getAvailableVegetables();
        itemCountLabel.setText(vegetables.size() + " vegetables available");
        
        for (Product product : vegetables) {
            allProductsContainer.getChildren().add(createProductCard(product));
        }
    }
    
    /**
     * Loads only fruits (filtered view)
     */
    private void loadFruitsOnly() {
        allProductsContainer.getChildren().clear();
        
        List<Product> fruits = productService.getAvailableFruits();
        itemCountLabel.setText(fruits.size() + " fruits available");
        
        for (Product product : fruits) {
            allProductsContainer.getChildren().add(createProductCard(product));
        }
    }
    
    /**
     * Handles filter vegetables button click.
     * 
     * @param event Action event
     */
    @FXML
    private void handleFilterVegetables(ActionEvent event) {
        currentFilter = "vegetables";
        loadVegetablesOnly();
    }
    
    /**
     * Handles filter fruits button click.
     * 
     * @param event Action event
     */
    @FXML
    private void handleFilterFruits(ActionEvent event) {
        currentFilter = "fruits";
        loadFruitsOnly();
    }
    
    /**
     * Handles show all button click.
     * 
     * @param event Action event
     */
    @FXML
    private void handleShowAll(ActionEvent event) {
        currentFilter = "all";
        loadAllProducts();
    }

    /**
     * Creates a product card UI component with Material Design styling.
     * Uses the "Green as your money" color scheme.
     * ADD TO CART button uses vibrant orange (#E9762B).
     * 
     * @param product Product to display
     * @return VBox containing product card
     */
    private VBox createProductCard(Product product) {
        // Create VBox for card with Material Design styling
        VBox card = new VBox(14);
        card.getStyleClass().add("product-card");
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(20));
        card.setPrefWidth(200);
        
        // Product image container with rounded corners
        StackPane imageContainer = createProductImageContainer(product);
        
        // Product name - Deep green color
        Label nameLabel = new Label(product.getName());
        nameLabel.getStyleClass().add("product-name");
        
        // Product price with threshold indicator and regional pricing - Orange color
        double basePrice = product.getEffectivePrice();
        double regionalPrice = regionService.calculateRegionalPrice(product);
        String priceText = String.format("₺%.2f /kg", regionalPrice);
        Label priceLabel = new Label(priceText);
        priceLabel.getStyleClass().add("product-price");
        
        // Show price adjustment if region has different pricing
        com.greengrocer.models.Region currentRegion = regionService.getCurrentRegion();
        if (currentRegion != null && currentRegion.getDistanceKm() > 0) {
            String adjustText = currentRegion.getPriceDifferenceText();
            Label adjustLabel = new Label("📍 " + adjustText + " (from " + currentRegion.getName() + ")");
            adjustLabel.setStyle("-fx-font-size: 9px; -fx-text-fill: #E9762B; -fx-font-weight: bold;");
            card.getChildren().add(adjustLabel);
        }
        
        // Low stock warning
        if (product.isPriceDoubled()) {
            priceLabel.getStyleClass().add("doubled");
            Label warningLabel = new Label("⚠️ Low Stock - Price Doubled!");
            warningLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #C62828; -fx-font-weight: bold;");
            card.getChildren().add(warningLabel);
        }
        
        // Stock info - Sage green color
        Label stockLabel = new Label(String.format("In Stock: %.1f kg", product.getStock()));
        stockLabel.getStyleClass().add("product-stock");
        
        // In Cart info - Check if product is already in cart
        Label inCartLabel = null;
        try {
            List<com.greengrocer.models.CartItem> cartItems = cartService.getCartItems();
            for (com.greengrocer.models.CartItem item : cartItems) {
                if (item.getProductId() == product.getId()) {
                    inCartLabel = new Label(String.format("In Cart: %.1f kg", item.getQuantity()));
                    inCartLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #E9762B; -fx-font-weight: bold;");
                    break;
                }
            }
        } catch (Exception e) {
            // Ignore errors when checking cart
        }
        
        // Quantity input (MaterialFX)
        MFXTextField quantityField = new MFXTextField();
        quantityField.setFloatingText("Qty (kg)");
        quantityField.setPrefWidth(130);
        quantityField.setText("1.0");
        
        // Add to cart button - VIBRANT ORANGE (#E9762B) for CTA
        MFXButton addButton = new MFXButton("ADD TO CART");
        addButton.getStyleClass().addAll("mfx-button-raised", "add-to-cart-btn");
        addButton.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;");
        addButton.setMaxWidth(Double.MAX_VALUE);
        addButton.setOnAction(e -> handleAddToCart(product, quantityField.getText()));
        
        // Add all components to card
        if (inCartLabel != null) {
            card.getChildren().addAll(imageContainer, nameLabel, priceLabel, stockLabel, inCartLabel, quantityField, addButton);
        } else {
            card.getChildren().addAll(imageContainer, nameLabel, priceLabel, stockLabel, quantityField, addButton);
        }
        
        // Store card reference for efficient updates
        productCardMap.put(product.getId(), card);
        
        return card;
    }
    
    /**
     * Creates a styled image container for a product.
     * Shows the product image if available, otherwise shows a styled placeholder.
     * 
     * @param product Product to create image for
     * @return StackPane containing the product image
     */
    private StackPane createProductImageContainer(Product product) {
        StackPane container = new StackPane();
        container.setMinSize(120, 120);
        container.setMaxSize(120, 120);
        container.setAlignment(Pos.CENTER);
        
        Image productImage = product.getDisplayImage();
        
        if (productImage != null) {
            // Display actual product image with rounded corners
            ImageView imageView = new ImageView(productImage);
            imageView.setFitWidth(110);
            imageView.setFitHeight(110);
            imageView.setPreserveRatio(true);
            imageView.setSmooth(true);
            
            // Apply rounded corners
            Rectangle clip = new Rectangle(110, 110);
            clip.setArcWidth(20);
            clip.setArcHeight(20);
            imageView.setClip(clip);
            
            container.getChildren().add(imageView);
            container.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 14px; " +
                             "-fx-border-color: #E0E0E0; -fx-border-radius: 14px;");
        } else {
            // Styled placeholder with emoji and type
            VBox placeholder = new VBox(4);
            placeholder.setAlignment(Pos.CENTER);
            
            String emoji = product.isVegetable() ? "🥬" : "🍎";
            Label emojiLabel = new Label(emoji);
            emojiLabel.setStyle("-fx-font-size: 48px;");
            
            String typeText = product.isVegetable() ? "VEG" : "FRUIT";
            Label typeLabel = new Label(typeText);
            typeLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #757575;");
            
            placeholder.getChildren().addAll(emojiLabel, typeLabel);
            
            // Background color based on type
            String bgColor = product.isVegetable() ? "#E8F5E9" : "#FFF3E0";
            container.setStyle("-fx-background-color: " + bgColor + "; -fx-background-radius: 14px; " +
                             "-fx-border-color: #E0E0E0; -fx-border-radius: 14px;");
            
            container.getChildren().add(placeholder);
        }
        
        return container;
    }

    /**
     * Handles adding product to cart.
     * 
     * @param product Product to add
     * @param quantityText Quantity input text
     */
    private void handleAddToCart(Product product, String quantityText) {
        try {
            double quantity = cartService.parseQuantity(quantityText);
            
            // Check if quantity exceeds threshold (triggers price doubling)
            if (product.isLargeOrder(quantity)) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("⚠️ Threshold Exceeded");
                alert.setHeaderText("Price Doubling Notice");
                alert.setContentText("You have exceeded the threshold (" + product.getThreshold() + " kg) for " + product.getName() + ".\n\n" +
                        "Because you achieved the threshold, the prices are doubled for this product.");
                alert.showAndWait();
            }
            
            cartService.addToCart(product, quantity);
            updateCartCount();
            
            // Efficiently update just the "In Cart" label for this product
            updateProductCardCartInfo(product.getId());
        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
        }
    }

    /**
     * Handles search/filter functionality.
     * Now delegates to live search logic.
     * 
     * @param event Action event
     */
    @FXML
    private void handleSearch(ActionEvent event) {
        performLiveSearch(searchField.getText());
    }
    
    /**
     * Handles opening shopping cart window.
     * Refreshes product display when cart closes to show updated stock.
     * 
     * @param event Action event
     */
    @FXML
    private void handleOpenCart(ActionEvent event) {
        try {
            if (cartStage == null || !cartStage.isShowing()) {
                cartStage = MainApplication.openNewWindow("/fxml/ShoppingCartView.fxml", "Group03 - GreenGrocer");
                
                // Refresh products when cart closes (to update stock after purchase)
                // Also refresh user info (loyalty points may have changed after order)
                cartStage.setOnHidden(e -> {
                    refreshProducts();
                    updateCartCount();
                    refreshUserInfo();
                });
            } else {
                cartStage.toFront();
            }
            closeDropdown();
        } catch (Exception e) {
            showError("Could not open shopping cart");
        }
    }

    /**
     * Handles viewing order history.
     * 
     * @param event Action event
     */
    @FXML
    private void handleViewOrders(ActionEvent event) {
        try {
            MainApplication.openNewWindow("/fxml/OrderHistoryView.fxml", "Group03 - GreenGrocer");
            closeDropdown();
        } catch (Exception e) {
            showError("Could not open order history");
        }
    }

    /**
     * Handles messaging the owner.
     * 
     * @param event Action event
     */
    @FXML
    private void handleMessageOwner(ActionEvent event) {
        try {
            MainApplication.openNewWindow("/fxml/MessagesView.fxml", "Group03 - GreenGrocer");
            closeDropdown();
        } catch (Exception e) {
            showError("Could not open messaging");
        }
    }

    /**
     * Handles profile editing.
     * Refreshes user info when profile edit window closes.
     * 
     * @param event Action event
     */
    @FXML
    private void handleEditProfile(ActionEvent event) {
        try {
            Stage profileStage = MainApplication.openNewWindow("/fxml/ProfileEditView.fxml", "Group03 - GreenGrocer");
            // Refresh user info when profile edit closes (loyalty points may have changed)
            profileStage.setOnHidden(e -> {
                refreshUserInfo();
            });
            closeDropdown();
        } catch (Exception e) {
            showError("Could not open profile editor");
        }
    }

    /**
     * Handles logout.
     * 
     * @param event Action event
     */
    @FXML
    private void handleLogout(ActionEvent event) {
        SessionManager.getInstance().logout();
        try {
            MainApplication.switchScene("/fxml/LoginView.fxml", "Group03 - GreenGrocer");
        } catch (Exception e) {
            showError("Could not logout");
        }
    }
    
    /**
     * Handles opening AI chat assistant.
     * 
     * @param event Action event
     */
    @FXML
    private void handleOpenChat(ActionEvent event) {
        MainApplication.openChatWindow();
        closeDropdown();
    }
    
    /**
     * Toggles dropdown menu visibility.
     * 
     * @param event Action event
     */
    @FXML
    private void handleToggleDropdown(ActionEvent event) {
        if (dropdownOpen) {
            closeDropdown();
        } else {
            openDropdown();
        }
    }
    
    /**
     * Opens the dropdown menu.
     */
    private void openDropdown() {
        dropdownOpen = true;
        dropdownOverlay.setVisible(true);
        dropdownMenu.setVisible(true);
    }
    
    /**
     * Closes the dropdown menu.
     * 
     * @param event Mouse event
     */
    @FXML
    private void handleCloseDropdown(javafx.scene.input.MouseEvent event) {
        closeDropdown();
    }
    
    /**
     * Closes the dropdown menu.
     */
    private void closeDropdown() {
        dropdownOpen = false;
        dropdownOverlay.setVisible(false);
        dropdownMenu.setVisible(false);
    }
    
    /**
     * Handles dropdown home button click.
     * 
     * @param event Action event
     */
    @FXML
    private void handleDropdownHome(ActionEvent event) {
        handleShowAll(event);
        closeDropdown();
    }
    
    /**
     * Updates cart item count display.
     */
    private void updateCartCount() {
        int count = cartService.getItemCount();
        cartCountLabel.setText(String.valueOf(count));
        cartCountLabel.setVisible(count > 0);
    }
    
    /**
     * Refreshes user info display (username and loyalty tier).
     * Called when returning from profile edit or after loyalty points change.
     */
    private void refreshUserInfo() {
        User currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser == null) return;
        
        // Update header user info
        if (usernameLabel != null) {
            usernameLabel.setText(currentUser.getUsername());
        }
        if (loyaltyLabel != null) {
            loyaltyLabel.setText("⭐ " + currentUser.getLoyaltyTier());
        }
        
        // Update dropdown user info
        if (dropdownUsernameLabel != null) {
            dropdownUsernameLabel.setText(currentUser.getUsername());
        }
        if (dropdownLoyaltyLabel != null) {
            dropdownLoyaltyLabel.setText("⭐ " + currentUser.getLoyaltyTier());
        }
    }

    /**
     * Refreshes product display.
     */
    public void refreshProducts() {
        // Clear card map when doing full refresh
        productCardMap.clear();
        switch (currentFilter) {
            case "vegetables":
                loadVegetablesOnly();
                break;
            case "fruits":
                loadFruitsOnly();
                break;
            default:
                loadAllProducts();
        }
    }
    
    /**
     * Efficiently updates the "In Cart" label for a specific product card.
     * Only updates the label without recreating the entire card.
     * 
     * @param productId Product ID to update
     */
    private void updateProductCardCartInfo(int productId) {
        VBox card = productCardMap.get(productId);
        if (card == null) {
            // Card not found in map, do full refresh as fallback
            refreshProducts();
            return;
        }
        
        // Find existing "In Cart" label
        Label existingInCartLabel = null;
        int inCartLabelIndex = -1;
        int stockLabelIndex = -1;
        
        for (int i = 0; i < card.getChildren().size(); i++) {
            javafx.scene.Node node = card.getChildren().get(i);
            if (node instanceof Label) {
                Label label = (Label) node;
                String text = label.getText();
                if (text != null && text.startsWith("In Stock:")) {
                    stockLabelIndex = i;
                } else if (text != null && text.startsWith("In Cart:")) {
                    existingInCartLabel = label;
                    inCartLabelIndex = i;
                    break;
                }
            }
        }
        
        // Get current cart quantity
        double cartQuantity = 0;
        try {
            List<com.greengrocer.models.CartItem> cartItems = cartService.getCartItems();
            for (com.greengrocer.models.CartItem item : cartItems) {
                if (item.getProductId() == productId) {
                    cartQuantity = item.getQuantity();
                    break;
                }
            }
        } catch (Exception e) {
            // Ignore errors
        }
        
        if (cartQuantity > 0) {
            // Update or add "In Cart" label
            if (existingInCartLabel != null) {
                // Update existing label
                existingInCartLabel.setText(String.format("In Cart: %.1f kg", cartQuantity));
            } else if (stockLabelIndex >= 0) {
                // Add new label after stock label
                Label newInCartLabel = new Label(String.format("In Cart: %.1f kg", cartQuantity));
                newInCartLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #E9762B; -fx-font-weight: bold;");
                card.getChildren().add(stockLabelIndex + 1, newInCartLabel);
            }
        } else {
            // Remove "In Cart" label if product no longer in cart
            if (inCartLabelIndex >= 0) {
                card.getChildren().remove(inCartLabelIndex);
            }
        }
    }

    /**
     * Shows an error alert.
     * 
     * @param message Error message
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
     * 
     * @param message Info message
     */
    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Handles region change from combo box.
     * Updates the current region and refreshes product prices.
     * 
     * @param event Action event
     */
    @FXML
    private void handleRegionChange(ActionEvent event) {
        String selectedRegionName = regionComboBox.getValue();
        if (selectedRegionName == null || selectedRegionName.isEmpty()) {
            return;
        }
        
        List<Region> regions = regionService.getIstanbulDistricts();
        for (Region region : regions) {
            if (region.getName().equals(selectedRegionName)) {
                regionService.setCurrentRegion(region);
                // Refresh products to show updated prices
                refreshProducts();
                break;
            }
        }
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
            ThemeManager.getInstance().applyTheme(scene);
            
            mapStage.setScene(scene);
            mapStage.centerOnScreen();
            mapStage.showAndWait();
            
            // Reload region combo box after map closes (region may have changed)
            setupRegionComboBox();
            // Refresh products to show updated prices
            refreshProducts();
        } catch (Exception e) {
            showError("Could not open map: " + e.getMessage());
        }
    }
    
    /**
     * Toggle between available themes
     * 
     * @param event Action event
     */
    @FXML
    private void handleToggleTheme(ActionEvent event) {
        System.out.println("DEBUG CustomerMainController: handleToggleTheme() called");
        try {
            if (usernameLabel != null && usernameLabel.getScene() != null) {
                ThemeManager.getInstance().toggleTheme(usernameLabel.getScene());
            }
        } catch (Exception e) {
            System.err.println("ERROR CustomerMainController: Error toggling theme: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
