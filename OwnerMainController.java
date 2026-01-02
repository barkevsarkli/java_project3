package com.greengrocer.controllers;

import com.greengrocer.app.MainApplication;
import com.greengrocer.app.SessionManager;
import com.greengrocer.app.ThemeManager;
import com.greengrocer.dao.UserDAO;
import com.greengrocer.models.*;
import com.greengrocer.services.*;
import io.github.palexdev.materialfx.controls.*;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controller for the owner main view using MaterialFX components.
 * Handles product management, carrier management, orders, messages, and reports.
 * 
 * @author Samira Çeçen
 * @version 1.0.0
 */
public class OwnerMainController implements Initializable {

    /** Tab pane for different sections */
    @FXML
    private TabPane mainTabPane;
    
    // Products Tab
    @FXML
    private VBox productsListContainer;
    @FXML
    private MFXTextField productNameField;
    @FXML
    private MFXComboBox<String> productTypeCombo;
    @FXML
    private MFXTextField productPriceField;
    @FXML
    private MFXTextField productStockField;
    @FXML
    private MFXTextField productThresholdField;
    @FXML
    private ImageView productImagePreview;
    @FXML
    private Label imageStatusLabel;
    
    /** Currently selected image data for product */
    private byte[] selectedImageData = null;
    /** Currently selected product */
    private Product selectedProduct = null;
    
    // Carriers Tab
    @FXML
    private VBox carriersListContainer;
    @FXML
    private VBox carrierRatingsListContainer;
    @FXML
    private MFXTextField carrierUsernameField;
    @FXML
    private MFXTextField carrierEmailField;
    @FXML
    private MFXPasswordField carrierPasswordField;
    /** Currently selected carrier */
    private User selectedCarrier = null;
    
    // Orders Tab
    @FXML
    private VBox ordersListContainer;
    /** Currently selected order */
    private Order selectedOrder = null;
    
    // Messages Tab
    @FXML
    private VBox messagesListContainer;
    @FXML
    private Label messageCountLabel;
    @FXML
    private Label unreadMessagesCount;
    @FXML
    private Label urgentMessagesCount;
    @FXML
    private Label todayMessagesCount;
    
    // Activity Feed Tab
    @FXML
    private VBox activityFeedContainer;
    
    // Reports Tab
    @FXML
    private VBox reportsContainer;
    @FXML
    private Label reportDateLabel;
    @FXML
    private Label totalRevenueLabel;
    @FXML
    private Label totalOrdersLabel;
    @FXML
    private Label totalCustomersLabel;
    @FXML
    private Label avgOrderValueLabel;
    @FXML
    private javafx.scene.chart.PieChart salesPieChart;
    @FXML
    private javafx.scene.chart.BarChart<String, Number> revenueBarChart;
    @FXML
    private javafx.scene.chart.CategoryAxis revenueXAxis;
    @FXML
    private javafx.scene.chart.NumberAxis revenueYAxis;
    @FXML
    private VBox topProductsContainer;
    @FXML
    private VBox carrierPerformanceContainer;
    @FXML
    private VBox lowStockContainer;
    
    // Customers Tab
    @FXML
    private MFXTextField customerSearchField;
    @FXML
    private VBox customersListContainer;
    @FXML
    private VBox customerDetailsPanel;
    @FXML
    private VBox noCustomerSelected;
    @FXML
    private VBox customerDetailsContent;
    @FXML
    private ImageView customerProfileImage;
    @FXML
    private Label customerNameLabel;
    @FXML
    private Label customerEmailLabel;
    @FXML
    private Label customerPhoneLabel;
    @FXML
    private Label customerAddressLabel;
    @FXML
    private Label customerOrdersLabel;
    @FXML
    private Label customerTotalSpentLabel;
    @FXML
    private Label customerPointsLabel;
    @FXML
    private Label customerCreatedAtLabel;
    @FXML
    private Label customerVerifiedLabel;
    @FXML
    private Label customerRegionLabel;
    @FXML
    private VBox customerOrdersContainer;
    
    /** Currently selected customer for details view */
    private User selectedCustomer;
    
    /** Auto-refresh timeline */
    private Timeline autoRefreshTimeline;
    
    /** Date formatter for activity feed */
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    /** Services */
    private ProductService productService;
    private CarrierService carrierService;
    private OrderService orderService;
    private MessageService messageService;
    private CouponService couponService;
    private ReportService reportService;
    private UserDAO userDAO;

    /**
     * Initializes the controller.
     * 
     * @param location FXML location
     * @param resources Resource bundle
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialize services
        productService = new ProductService();
        carrierService = new CarrierService();
        orderService = new OrderService();
        messageService = new MessageService();
        couponService = new CouponService();
        reportService = new ReportService();
        userDAO = new UserDAO();
        
        // Setup tabs with null checks
        Platform.runLater(() -> {
            try {
                setupProductsTab();
                setupCarriersTab();
                setupOrdersTab();
                setupCustomersTab();
                setupMessagesTab();
                setupReportsTab();
                setupActivityFeed();
                
                // Start auto-refresh every 10 seconds
                startAutoRefresh();
            } catch (Exception e) {
                System.err.println("Error initializing Owner view: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
    
    /**
     * Starts auto-refresh timeline for real-time updates.
     */
    private void startAutoRefresh() {
        autoRefreshTimeline = new Timeline(new KeyFrame(Duration.seconds(10), event -> {
            try {
                loadActivityFeed();
            } catch (Exception e) {
                System.err.println("Auto-refresh error: " + e.getMessage());
            }
        }));
        autoRefreshTimeline.setCycleCount(Timeline.INDEFINITE);
        autoRefreshTimeline.play();
    }
    
    /**
     * Sets up the activity feed tab.
     */
    private void setupActivityFeed() {
        loadActivityFeed();
    }
    
    /**
     * Loads and displays recent activity in the feed (orders + messages).
     */
    private void loadActivityFeed() {
        if (activityFeedContainer == null) return;
        
        activityFeedContainer.getChildren().clear();
        
        // Get recent orders
        List<Order> allOrders = orderService.getAllOrders();
        
        // Get recent messages
        List<Message> allMessages = messageService.getAllMessagesForOwner();
        
        boolean hasOrders = allOrders != null && !allOrders.isEmpty();
        boolean hasMessages = allMessages != null && !allMessages.isEmpty();
        
        if (!hasOrders && !hasMessages) {
            Label emptyLabel = new Label("No recent activity");
            emptyLabel.setStyle("-fx-text-fill: #757575; -fx-padding: 20;");
            activityFeedContainer.getChildren().add(emptyLabel);
            return;
        }
        
        // Create a combined list of activities sorted by time
        java.util.List<ActivityItem> activities = new java.util.ArrayList<>();
        
        // Add orders as activities
        if (allOrders != null) {
            for (Order order : allOrders) {
                LocalDateTime time = order.getOrderTime() != null ? order.getOrderTime() : LocalDateTime.now();
                activities.add(new ActivityItem("order", time, order, null));
            }
        }
        
        // Add messages as activities
        if (allMessages != null) {
            for (Message message : allMessages) {
                LocalDateTime time = message.getSentAt() != null ? message.getSentAt() : LocalDateTime.now();
                activities.add(new ActivityItem("message", time, null, message));
            }
        }
        
        // Sort by time (newest first)
        activities.sort((a, b) -> b.timestamp.compareTo(a.timestamp));
        
        // Display top 50 activities
        for (int i = 0; i < Math.min(activities.size(), 50); i++) {
            ActivityItem item = activities.get(i);
            HBox card;
            if ("order".equals(item.type)) {
                card = createActivityCard(item.order);
            } else {
                card = createMessageActivityCard(item.message);
            }
            activityFeedContainer.getChildren().add(card);
        }
    }
    
    /**
     * Helper class for sorting mixed activities.
     */
    private static class ActivityItem {
        String type;
        LocalDateTime timestamp;
        Order order;
        Message message;
        
        ActivityItem(String type, LocalDateTime timestamp, Order order, Message message) {
            this.type = type;
            this.timestamp = timestamp;
            this.order = order;
            this.message = message;
        }
    }
    
    /**
     * Creates an activity card for a message.
     */
    private HBox createMessageActivityCard(Message message) {
        HBox card = new HBox(12);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(10, 14, 10, 14));
        
        String bgColor = message.isRead() ? "#F5F5F5" : "#E8F5E9";
        String icon = "💬";
        
        card.setStyle("-fx-background-color: " + bgColor + "; -fx-background-radius: 10;");
        
        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 20px;");
        
        VBox content = new VBox(4);
        HBox.setHgrow(content, Priority.ALWAYS);
        
        String senderName = message.getSenderName() != null ? message.getSenderName() : "User #" + message.getSenderId();
        Label titleLabel = new Label("Message from " + senderName);
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
        
        String preview = message.getContent();
        if (preview != null && preview.length() > 50) {
            preview = preview.substring(0, 47) + "...";
        }
        Label detailLabel = new Label(preview != null ? preview : "(no content)");
        detailLabel.setStyle("-fx-text-fill: #666666; -fx-font-size: 12px;");
        
        content.getChildren().addAll(titleLabel, detailLabel);
        
        // Timestamp
        String timeStr = message.getSentAt() != null 
            ? message.getSentAt().format(DATE_TIME_FORMATTER)
            : "Just now";
        Label timeLabel = new Label(timeStr);
        timeLabel.setStyle("-fx-text-fill: #9E9E9E; -fx-font-size: 11px;");
        
        // Unread indicator
        if (!message.isRead()) {
            Label unreadBadge = new Label("NEW");
            unreadBadge.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-size: 10px; " +
                    "-fx-padding: 2 6; -fx-background-radius: 8;");
            card.getChildren().addAll(iconLabel, content, unreadBadge, timeLabel);
        } else {
            card.getChildren().addAll(iconLabel, content, timeLabel);
        }
        
        // Click to view message
        card.setOnMouseClicked(e -> handleOpenFullInbox(null));
        card.setStyle(card.getStyle() + "; -fx-cursor: hand;");
        
        return card;
    }
    
    /**
     * Creates an activity card for an order.
     */
    private HBox createActivityCard(Order order) {
        HBox card = new HBox(12);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(10, 14, 10, 14));
        
        String bgColor;
        String icon;
        String message;
        
        switch (order.getStatus().toLowerCase()) {
            case "pending":
                bgColor = "#FFF3E0";
                icon = "Cart";
                message = "New order placed";
                break;
            case "confirmed":
                bgColor = "#E3F2FD";
                icon = "✅";
                message = "Order confirmed";
                break;
            case "assigned":
                bgColor = "#F3E5F5";
                icon = "🚚";
                message = "Assigned to carrier";
                break;
            case "delivered":
                bgColor = "#E8F5E9";
                icon = "Orders";
                message = "Order delivered";
                break;
            case "cancelled":
                bgColor = "#FFEBEE";
                icon = "❌";
                message = "Order cancelled";
                break;
            default:
                bgColor = "#FAFAFA";
                icon = "📋";
                message = "Order updated";
        }
        
        card.setStyle("-fx-background-color: " + bgColor + "; -fx-background-radius: 8;");
        
        // Icon
        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 20px;");
        
        // Info
        VBox infoBox = new VBox(2);
        HBox.setHgrow(infoBox, Priority.ALWAYS);
        
        Label messageLabel = new Label(message + " #" + order.getId());
        messageLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;");
        
        String customerInfo = order.getCustomerName() != null 
            ? order.getCustomerName() 
            : "Customer #" + order.getCustomerId();
        Label detailsLabel = new Label(customerInfo + " • " + String.format("₺%.2f", order.getTotalCost()));
        detailsLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #616161;");
        
        infoBox.getChildren().addAll(messageLabel, detailsLabel);
        
        // Time
        String timeStr = order.getOrderTime() != null 
            ? order.getOrderTime().format(DATE_TIME_FORMATTER) 
            : "N/A";
        Label timeLabel = new Label(timeStr);
        timeLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #9E9E9E;");
        
        card.getChildren().addAll(iconLabel, infoBox, timeLabel);
        
        return card;
    }
    
    /**
     * Handles refreshing the activity feed.
     */
    @FXML
    private void handleRefreshActivity(ActionEvent event) {
        loadActivityFeed();
        loadCarrierRatings();
    }
    
    /**
     * Handles refreshing all data including reports/statistics.
     */
    @FXML
    private void handleRefreshAll(ActionEvent event) {
        try {
            loadProducts();
            loadCarriers();
            loadOrders();
            loadMessages();
            loadActivityFeed();
            setupReportsTab(); // Refresh statistics and charts
        } catch (Exception e) {
            showError("Error refreshing data: " + e.getMessage());
        }
    }

    // ==================== PRODUCTS TAB ====================

    /**
     * Sets up products tab with MaterialFX components.
     */
    private void setupProductsTab() {
        // Configure MaterialFX product type combo
        // Set up table columns
        // Load products
        
        productTypeCombo.setItems(FXCollections.observableArrayList("vegetable", "fruit"));
        loadProducts();
    }

    /**
     * Loads all products into styled list.
     */
    private void loadProducts() {
        if (productsListContainer == null) return;
        
        productsListContainer.getChildren().clear();
        selectedProduct = null;
        
        try {
            List<Product> products = productService.getAllProducts();
            if (products == null || products.isEmpty()) {
                Label emptyLabel = new Label("No products found");
                emptyLabel.setStyle("-fx-text-fill: #757575; -fx-padding: 20;");
                productsListContainer.getChildren().add(emptyLabel);
                return;
            }
            
            for (Product product : products) {
                HBox card = createProductCard(product);
                productsListContainer.getChildren().add(card);
            }
        } catch (Exception e) {
            System.err.println("Error loading products: " + e.getMessage());
            Label errorLabel = new Label("Error loading products");
            errorLabel.setStyle("-fx-text-fill: #C62828; -fx-padding: 20;");
            productsListContainer.getChildren().add(errorLabel);
        }
    }
    
    /**
     * Creates a styled card for a product.
     */
    private HBox createProductCard(Product product) {
        HBox card = new HBox(12);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(12, 16, 12, 16));
        card.setStyle("-fx-background-color: #FAFAFA; -fx-background-radius: 8; -fx-border-color: #E0E0E0; -fx-border-radius: 8; -fx-cursor: hand;");
        
        // Product image or icon
        javafx.scene.Node imageNode = createProductImageNode(product);
        
        // Product info
        VBox infoBox = new VBox(2);
        HBox.setHgrow(infoBox, Priority.ALWAYS);
        
        Label nameLabel = new Label(product.getName());
        nameLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        
        HBox typeRow = new HBox(6);
        typeRow.setAlignment(Pos.CENTER_LEFT);
        
        Label typeLabel = new Label(product.getType().toUpperCase());
        typeLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #757575;");
        
        // Show if image is present
        if (product.getImageData() != null && product.getImageData().length > 0) {
            Label imgBadge = new Label("📷");
            imgBadge.setStyle("-fx-font-size: 10px;");
            typeRow.getChildren().addAll(typeLabel, imgBadge);
        } else {
            typeRow.getChildren().add(typeLabel);
        }
        
        infoBox.getChildren().addAll(nameLabel, typeRow);
        
        // Price and stock
        VBox statsBox = new VBox(2);
        statsBox.setAlignment(Pos.CENTER_RIGHT);
        
        Label priceLabel = new Label(String.format("₺%.2f/kg", product.getPrice()));
        priceLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #E9762B;");
        
        Label stockLabel = new Label(String.format("Stock: %.1f kg", product.getStock()));
        stockLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: " + (product.getStock() <= product.getThreshold() ? "#C62828" : "#757575") + ";");
        
        statsBox.getChildren().addAll(priceLabel, stockLabel);
        
        card.getChildren().addAll(imageNode, infoBox, statsBox);
        
        // Selection handling
        card.setOnMouseClicked(e -> selectProduct(product, card));
        card.setOnMouseEntered(e -> {
            if (selectedProduct != product) {
                card.setStyle("-fx-background-color: #F5F5F5; -fx-background-radius: 8; -fx-border-color: #BDBDBD; -fx-border-radius: 8; -fx-cursor: hand;");
            }
        });
        card.setOnMouseExited(e -> {
            if (selectedProduct != product) {
                card.setStyle("-fx-background-color: #FAFAFA; -fx-background-radius: 8; -fx-border-color: #E0E0E0; -fx-border-radius: 8; -fx-cursor: hand;");
            }
        });
        
        return card;
    }
    
    /**
     * Creates an image node for a product - shows actual image if available, otherwise emoji.
     */
    private javafx.scene.Node createProductImageNode(Product product) {
        javafx.scene.layout.StackPane container = new javafx.scene.layout.StackPane();
        container.setMinSize(48, 48);
        container.setMaxSize(48, 48);
        container.setAlignment(Pos.CENTER);
        
        Image productImage = product.getDisplayImage();
        
        if (productImage != null) {
            // Show actual product image
            ImageView imageView = new ImageView(productImage);
            imageView.setFitWidth(44);
            imageView.setFitHeight(44);
            imageView.setPreserveRatio(true);
            imageView.setSmooth(true);
            
            // Rounded corners
            javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(44, 44);
            clip.setArcWidth(10);
            clip.setArcHeight(10);
            imageView.setClip(clip);
            
            container.getChildren().add(imageView);
            container.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 8; -fx-border-color: #E0E0E0; -fx-border-radius: 8;");
        } else {
            // Show emoji placeholder
            String icon = product.isVegetable() ? "🥬" : "🍎";
            Label iconLabel = new Label(icon);
            iconLabel.setStyle("-fx-font-size: 24px;");
            container.getChildren().add(iconLabel);
            
            String bgColor = product.isVegetable() ? "#E8F5E9" : "#FFF3E0";
            container.setStyle("-fx-background-color: " + bgColor + "; -fx-background-radius: 8;");
        }
        
        return container;
    }
    
    private HBox selectedProductCard = null;
    
    private void selectProduct(Product product, HBox card) {
        if (selectedProductCard != null) {
            selectedProductCard.setStyle("-fx-background-color: #FAFAFA; -fx-background-radius: 8; -fx-border-color: #E0E0E0; -fx-border-radius: 8; -fx-cursor: hand;");
        }
        selectedProduct = product;
        selectedProductCard = card;
        card.setStyle("-fx-background-color: #E8F5E9; -fx-background-radius: 8; -fx-border-color: #4CAF50; -fx-border-width: 2; -fx-border-radius: 8; -fx-cursor: hand;");
        
        // Populate form fields
        productNameField.setText(product.getName());
        productTypeCombo.selectItem(product.getType());
        productPriceField.setText(String.valueOf(product.getPrice()));
        productStockField.setText(String.valueOf(product.getStock()));
        productThresholdField.setText(String.valueOf(product.getThreshold()));
    }

    /**
     * Handles selecting image for product.
     * 
     * @param event Action event
     */
    @FXML
    private void handleSelectImage(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Product Image");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );
        
        File selectedFile = fileChooser.showOpenDialog(mainTabPane.getScene().getWindow());
        
        if (selectedFile != null) {
            try {
                // Read file into byte array
                FileInputStream fis = new FileInputStream(selectedFile);
                selectedImageData = fis.readAllBytes();
                fis.close();
                
                // Show preview
                Image preview = new Image(selectedFile.toURI().toString(), 100, 100, true, true);
                if (productImagePreview != null) {
                    productImagePreview.setImage(preview);
                }
                if (imageStatusLabel != null) {
                    imageStatusLabel.setText("✓ Image selected: " + selectedFile.getName());
                    imageStatusLabel.setStyle("-fx-text-fill: #4CAF50;");
                }
                
            } catch (Exception e) {
                showError("Could not load image: " + e.getMessage());
                selectedImageData = null;
            }
        }
    }
    
    /**
     * Clears the selected product image.
     * 
     * @param event Action event
     */
    @FXML
    private void handleClearImage(ActionEvent event) {
        selectedImageData = null;
        if (productImagePreview != null) {
            productImagePreview.setImage(null);
        }
        if (imageStatusLabel != null) {
            imageStatusLabel.setText("No image selected");
            imageStatusLabel.setStyle("-fx-text-fill: #757575;");
        }
    }
    
    /**
     * Handles adding new product.
     * 
     * @param event Action event
     */
    @FXML
    private void handleAddProduct(ActionEvent event) {
        // Validate all MaterialFX fields
        // Create product
        // Refresh table
        
        try {
            String name = productNameField.getText().trim();
            String type = productTypeCombo.getSelectedItem();
            double price = Double.parseDouble(productPriceField.getText().trim());
            double stock = Double.parseDouble(productStockField.getText().trim());
            double threshold = Double.parseDouble(productThresholdField.getText().trim());
            
            // Validate stock is at least 1kg
            if (stock < 1) {
                showError("Stock must be at least 1 kg");
                return;
            }
            
            // Validate threshold is positive
            if (threshold <= 0) {
                showError("Threshold must be greater than zero");
                return;
            }
            
            // Validate threshold is not greater than stock
            if (threshold > stock) {
                showError("Threshold cannot be greater than stock quantity");
                return;
            }
            
            // Use selected image data (may be null)
            Product product = productService.addProduct(name, type, price, stock, threshold, selectedImageData);
            if (product != null) {
                loadProducts();
                clearProductFields();
            }
        } catch (NumberFormatException e) {
            showError("Invalid number format");
        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
        }
    }

    /**
     * Handles updating selected product.
     * 
     * @param event Action event
     */
    @FXML
    private void handleUpdateProduct(ActionEvent event) {
        if (selectedProduct == null) {
            showError("Please select a product to update");
            return;
        }
        
        try {
            String name = productNameField.getText().trim();
            double price = Double.parseDouble(productPriceField.getText().trim());
            double stock = Double.parseDouble(productStockField.getText().trim());
            double threshold = Double.parseDouble(productThresholdField.getText().trim());
            
            // Validate stock is at least 1kg
            if (stock < 1) {
                showError("Stock must be at least 1 kg");
                return;
            }
            
            if (threshold <= 0) {
                showError("Threshold must be greater than zero");
                return;
            }
            
            // Validate threshold is not greater than stock
            if (threshold > stock) {
                showError("Threshold cannot be greater than stock quantity");
                return;
            }
            
            // Update stock first
            productService.updateStock(selectedProduct.getId(), stock);
            
            // Then update other fields
            boolean success = productService.updateProduct(selectedProduct.getId(), name, price, threshold);
            if (success) {
                loadProducts();
            }
        } catch (NumberFormatException e) {
            showError("Invalid number format");
        }
    }

    /**
     * Handles removing selected product.
     * 
     * @param event Action event
     */
    @FXML
    private void handleRemoveProduct(ActionEvent event) {
        if (selectedProduct == null) {
            showError("Please select a product to remove");
            return;
        }
        
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, 
                "Delete product: " + selectedProduct.getName() + "?");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            if (productService.deleteProduct(selectedProduct.getId())) {
                loadProducts();
            }
        }
    }
    
    /**
     * Handles uploading image for selected product.
     * Saves image as BLOB directly to the database.
     * 
     * @param event Action event
     */
    @FXML
    private void handleUploadProductImage(ActionEvent event) {
        if (selectedProduct == null) {
            showError("Please select a product first");
            return;
        }
        
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Image for " + selectedProduct.getName());
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif"),
            new FileChooser.ExtensionFilter("PNG", "*.png"),
            new FileChooser.ExtensionFilter("JPEG", "*.jpg", "*.jpeg")
        );
        
        File selectedFile = fileChooser.showOpenDialog(mainTabPane.getScene().getWindow());
        
        if (selectedFile != null) {
            try {
                // Validate file size (max 5MB)
                if (selectedFile.length() > 5 * 1024 * 1024) {
                    showError("Image file is too large. Maximum size is 5MB.");
                    return;
                }
                
                // Read file into byte array
                FileInputStream fis = new FileInputStream(selectedFile);
                byte[] imageData = fis.readAllBytes();
                fis.close();
                
                System.out.println("DEBUG: Uploading image for product " + selectedProduct.getName() + 
                                   ", size: " + imageData.length + " bytes");
                
                // Update product with image (saves as BLOB to database)
                if (productService.updateProductImage(selectedProduct.getId(), imageData)) {
                    // Refresh product list to show new image
                    loadProducts();
                    loadActivityFeed();
                } else {
                    showError("Could not save image to database.\nPlease try again.");
                }
                
            } catch (Exception e) {
                showError("Could not load image: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * Clears product input fields.
     */
    private void clearProductFields() {
        productNameField.clear();
        productTypeCombo.clearSelection();
        productPriceField.clear();
        productStockField.clear();
        productThresholdField.clear();
        
        // Clear image
        selectedImageData = null;
        if (productImagePreview != null) {
            productImagePreview.setImage(null);
        }
        if (imageStatusLabel != null) {
            imageStatusLabel.setText("No image selected");
            imageStatusLabel.setStyle("-fx-text-fill: #757575;");
        }
    }

    // ==================== CARRIERS TAB ====================

    /**
     * Sets up carriers tab with MaterialFX components.
     */
    private void setupCarriersTab() {
        // Set up MaterialFX table columns
        // Load carriers
        loadCarriers();
    }

    /**
     * Loads all carriers into styled list.
     */
    private void loadCarriers() {
        if (carriersListContainer == null) return;
        
        carriersListContainer.getChildren().clear();
        selectedCarrier = null;
        
        try {
            List<User> carriers = carrierService.getAllCarriers();
            if (carriers == null || carriers.isEmpty()) {
                Label emptyLabel = new Label("No carriers employed");
                emptyLabel.setStyle("-fx-text-fill: #757575; -fx-padding: 20;");
                carriersListContainer.getChildren().add(emptyLabel);
            } else {
                for (User carrier : carriers) {
                    HBox card = createCarrierCard(carrier);
                    carriersListContainer.getChildren().add(card);
                }
            }
            
            // Load carrier ratings
            loadCarrierRatings();
        } catch (Exception e) {
            System.err.println("Error loading carriers: " + e.getMessage());
        }
    }
    
    private void loadCarrierRatings() {
        if (carrierRatingsListContainer == null) return;
        
        carrierRatingsListContainer.getChildren().clear();
        
        try {
            List<Object[]> ratingSummary = carrierService.getCarrierPerformanceSummary();
            if (ratingSummary == null || ratingSummary.isEmpty()) {
                Label emptyLabel = new Label("No ratings yet");
                emptyLabel.setStyle("-fx-text-fill: #757575; -fx-padding: 10;");
                carrierRatingsListContainer.getChildren().add(emptyLabel);
                return;
            }
            
            for (Object[] rating : ratingSummary) {
                VBox card = createRatingCard(rating);
                carrierRatingsListContainer.getChildren().add(card);
            }
        } catch (Exception e) {
            System.err.println("Error loading carrier ratings: " + e.getMessage());
        }
    }
    
    private HBox selectedCarrierCard = null;
    
    private HBox createCarrierCard(User carrier) {
        HBox card = new HBox(12);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(12, 16, 12, 16));
        card.setStyle("-fx-background-color: #FAFAFA; -fx-background-radius: 8; -fx-border-color: #E0E0E0; -fx-border-radius: 8; -fx-cursor: hand;");
        
        Label iconLabel = new Label("🚚");
        iconLabel.setStyle("-fx-font-size: 24px;");
        
        VBox infoBox = new VBox(2);
        HBox.setHgrow(infoBox, Priority.ALWAYS);
        
        Label nameLabel = new Label(carrier.getUsername());
        nameLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        
        Label statusLabel = new Label(carrier.isActive() ? "Active" : "Inactive");
        statusLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: " + (carrier.isActive() ? "#4CAF50" : "#C62828") + ";");
        
        infoBox.getChildren().addAll(nameLabel, statusLabel);
        
        card.getChildren().addAll(iconLabel, infoBox);
        
        card.setOnMouseClicked(e -> selectCarrier(carrier, card));
        card.setOnMouseEntered(e -> {
            if (selectedCarrier != carrier) {
                card.setStyle("-fx-background-color: #F5F5F5; -fx-background-radius: 8; -fx-border-color: #BDBDBD; -fx-border-radius: 8; -fx-cursor: hand;");
            }
        });
        card.setOnMouseExited(e -> {
            if (selectedCarrier != carrier) {
                card.setStyle("-fx-background-color: #FAFAFA; -fx-background-radius: 8; -fx-border-color: #E0E0E0; -fx-border-radius: 8; -fx-cursor: hand;");
            }
        });
        
        return card;
    }
    
    private void selectCarrier(User carrier, HBox card) {
        if (selectedCarrierCard != null) {
            selectedCarrierCard.setStyle("-fx-background-color: #FAFAFA; -fx-background-radius: 8; -fx-border-color: #E0E0E0; -fx-border-radius: 8; -fx-cursor: hand;");
        }
        selectedCarrier = carrier;
        selectedCarrierCard = card;
        card.setStyle("-fx-background-color: #E8F5E9; -fx-background-radius: 8; -fx-border-color: #4CAF50; -fx-border-width: 2; -fx-border-radius: 8; -fx-cursor: hand;");
    }
    
    private VBox createRatingCard(Object[] rating) {
        VBox card = new VBox(6);
        card.setPadding(new Insets(12, 14, 12, 14));
        card.setStyle("-fx-background-color: #FFF8E1; -fx-background-radius: 8; -fx-border-color: #FFE082; -fx-border-radius: 8;");
        
        // Header row with stars and carrier name
        HBox headerRow = new HBox(10);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        
        String carrierName = rating.length > 0 ? String.valueOf(rating[0]) : "Unknown";
        double avgRatingVal = rating.length > 1 && rating[1] != null ? ((Number) rating[1]).doubleValue() : 0;
        int totalRatings = rating.length > 2 && rating[2] != null ? ((Number) rating[2]).intValue() : 0;
        
        // Star display based on rating
        String stars = getStarDisplay(avgRatingVal);
        Label starsLabel = new Label(stars);
        starsLabel.setStyle("-fx-font-size: 14px;");
        
        Label nameLabel = new Label(carrierName);
        nameLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;");
        
        Label ratingLabel = new Label(String.format("%.1f", avgRatingVal));
        ratingLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #F57C00; -fx-font-weight: bold;");
        
        Label countLabel = new Label("(" + totalRatings + " reviews)");
        countLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #757575;");
        
        headerRow.getChildren().addAll(starsLabel, nameLabel, ratingLabel, countLabel);
        
        card.getChildren().add(headerRow);
        
        // Show latest comment if available
        String latestComment = rating.length > 3 && rating[3] != null ? String.valueOf(rating[3]) : null;
        if (latestComment != null && !latestComment.isEmpty()) {
            Label commentLabel = new Label("💬 \"" + latestComment + "\"");
            commentLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #616161; -fx-font-style: italic;");
            commentLabel.setWrapText(true);
            card.getChildren().add(commentLabel);
        }
        
        // Show order info if available
        Integer orderId = rating.length > 4 && rating[4] != null ? ((Number) rating[4]).intValue() : null;
        if (orderId != null) {
            Label orderLabel = new Label("Order #" + orderId);
            orderLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #9E9E9E;");
            card.getChildren().add(orderLabel);
        }
        
        return card;
    }
    
    private String getStarDisplay(double rating) {
        StringBuilder stars = new StringBuilder();
        int fullStars = (int) rating;
        boolean halfStar = (rating - fullStars) >= 0.5;
        
        for (int i = 0; i < fullStars; i++) {
            stars.append("⭐");
        }
        if (halfStar && fullStars < 5) {
            stars.append("✨");
        }
        // Fill empty stars
        int emptyStars = 5 - fullStars - (halfStar ? 1 : 0);
        for (int i = 0; i < emptyStars; i++) {
            stars.append("☆");
        }
        return stars.toString();
    }

    /**
     * Handles employing new carrier.
     * 
     * @param event Action event
     */
    @FXML
    private void handleEmployCarrier(ActionEvent event) {
        String username = carrierUsernameField.getText().trim();
        String email = carrierEmailField.getText().trim();
        String password = carrierPasswordField.getText();
        
        if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showError("Username, email and password are required");
            return;
        }
        
        // Validate email format
        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            showError("Please enter a valid email address");
            return;
        }
        
        try {
            User carrier = carrierService.employCarrier(username, email, password);
            if (carrier != null) {
                loadCarriers();
                carrierUsernameField.clear();
                carrierEmailField.clear();
                carrierPasswordField.clear();
            }
        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
        }
    }

    /**
     * Handles firing selected carrier.
     * Checks for active orders before allowing termination.
     * 
     * @param event Action event
     */
    @FXML
    private void handleFireCarrier(ActionEvent event) {
        if (selectedCarrier == null) {
            showError("Please select a carrier to fire");
            return;
        }
        
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, 
                "Fire carrier: " + selectedCarrier.getUsername() + "?\n\n" +
                "This action cannot be undone.");
        
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                if (carrierService.fireCarrier(selectedCarrier.getId())) {
                    loadCarriers();
                } else {
                    showError("Could not fire carrier. Please try again.");
                }
            } catch (IllegalStateException e) {
                // Carrier has active orders
                showError(e.getMessage());
            } catch (IllegalArgumentException e) {
                // Carrier not found or not a carrier
                showError(e.getMessage());
            }
        }
    }

    // ==================== ORDERS TAB ====================

    /**
     * Sets up orders tab with MaterialFX components.
     */
    private void setupOrdersTab() {
        loadOrders();
    }

    /**
     * Loads all orders into styled list.
     */
    private void loadOrders() {
        if (ordersListContainer == null) return;
        
        ordersListContainer.getChildren().clear();
        selectedOrder = null;
        
        try {
            List<Order> orders = orderService.getAllOrders();
            if (orders == null || orders.isEmpty()) {
                Label emptyLabel = new Label("No orders found");
                emptyLabel.setStyle("-fx-text-fill: #757575; -fx-padding: 20;");
                ordersListContainer.getChildren().add(emptyLabel);
                return;
            }
            
            for (Order order : orders) {
                HBox card = createOrderCard(order);
                ordersListContainer.getChildren().add(card);
            }
        } catch (Exception e) {
            System.err.println("Error loading orders: " + e.getMessage());
            Label errorLabel = new Label("Error loading orders");
            errorLabel.setStyle("-fx-text-fill: #C62828; -fx-padding: 20;");
            ordersListContainer.getChildren().add(errorLabel);
        }
    }
    
    private HBox selectedOrderCard = null;
    
    private HBox createOrderCard(Order order) {
        HBox card = new HBox(12);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(12, 16, 12, 16));
        card.setStyle("-fx-background-color: #FAFAFA; -fx-background-radius: 8; -fx-border-color: #E0E0E0; -fx-border-radius: 8; -fx-cursor: hand;");
        
        // Order number badge
        Label idLabel = new Label("#" + order.getId());
        idLabel.setMinWidth(50);
        idLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1976D2;");
        
        // Order info
        VBox infoBox = new VBox(2);
        HBox.setHgrow(infoBox, Priority.ALWAYS);
        
        Label customerLabel = new Label("Customer ID: " + order.getCustomerId());
        customerLabel.setStyle("-fx-font-size: 13px;");
        
        Label dateLabel = new Label(order.getOrderTime() != null ? order.getOrderTime().toString() : "N/A");
        dateLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #757575;");
        
        infoBox.getChildren().addAll(customerLabel, dateLabel);
        
        // Status badge
        String statusColor = getStatusColor(order.getStatus());
        Label statusLabel = new Label(order.getStatus().toUpperCase());
        statusLabel.setPadding(new Insets(4, 8, 4, 8));
        statusLabel.setStyle("-fx-background-color: " + statusColor + "; -fx-background-radius: 4; -fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold;");
        
        // Total
        Label totalLabel = new Label(String.format("₺%.2f", order.getTotalCost()));
        totalLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2E7D32;");
        
        card.getChildren().addAll(idLabel, infoBox, statusLabel, totalLabel);
        
        card.setOnMouseClicked(e -> selectOrder(order, card));
        card.setOnMouseEntered(e -> {
            if (selectedOrder != order) {
                card.setStyle("-fx-background-color: #F5F5F5; -fx-background-radius: 8; -fx-border-color: #BDBDBD; -fx-border-radius: 8; -fx-cursor: hand;");
            }
        });
        card.setOnMouseExited(e -> {
            if (selectedOrder != order) {
                card.setStyle("-fx-background-color: #FAFAFA; -fx-background-radius: 8; -fx-border-color: #E0E0E0; -fx-border-radius: 8; -fx-cursor: hand;");
            }
        });
        
        return card;
    }
    
    private void selectOrder(Order order, HBox card) {
        if (selectedOrderCard != null) {
            selectedOrderCard.setStyle("-fx-background-color: #FAFAFA; -fx-background-radius: 8; -fx-border-color: #E0E0E0; -fx-border-radius: 8; -fx-cursor: hand;");
        }
        selectedOrder = order;
        selectedOrderCard = card;
        card.setStyle("-fx-background-color: #E3F2FD; -fx-background-radius: 8; -fx-border-color: #1976D2; -fx-border-width: 2; -fx-border-radius: 8; -fx-cursor: hand;");
    }
    
    private String getStatusColor(String status) {
        if (status == null) return "#757575";
        switch (status.toLowerCase()) {
            case "pending": return "#FF9800";
            case "confirmed": return "#2196F3";
            case "assigned": return "#9C27B0";
            case "delivered": return "#4CAF50";
            case "cancelled": return "#F44336";
            default: return "#757575";
        }
    }

    // ==================== CUSTOMERS TAB ====================

    /**
     * Sets up customers tab.
     */
    private void setupCustomersTab() {
        loadCustomers();
    }

    /**
     * Loads all customers into the list.
     */
    private void loadCustomers() {
        if (customersListContainer == null) return;
        
        customersListContainer.getChildren().clear();
        selectedCustomer = null;
        
        // Hide details panel
        if (noCustomerSelected != null) {
            noCustomerSelected.setVisible(true);
            noCustomerSelected.setManaged(true);
        }
        if (customerDetailsContent != null) {
            customerDetailsContent.setVisible(false);
            customerDetailsContent.setManaged(false);
        }
        
        try {
            List<User> customers = userDAO.findAllCustomers();
            if (customers == null || customers.isEmpty()) {
                Label emptyLabel = new Label("No customers found");
                emptyLabel.setStyle("-fx-text-fill: #757575; -fx-padding: 20;");
                customersListContainer.getChildren().add(emptyLabel);
                return;
            }
            
            for (User customer : customers) {
                HBox card = createCustomerCard(customer);
                customersListContainer.getChildren().add(card);
            }
        } catch (Exception e) {
            System.err.println("Error loading customers: " + e.getMessage());
        }
    }

    /**
     * Creates a styled card for a customer.
     */
    private HBox createCustomerCard(User customer) {
        HBox card = new HBox(12);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(12, 16, 12, 16));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 10; " +
                "-fx-border-color: #e5e7eb; -fx-border-radius: 10; -fx-cursor: hand;");
        
        // Profile image or default
        Label avatar = new Label("👤");
        avatar.setStyle("-fx-font-size: 24px; -fx-min-width: 40; -fx-alignment: center;");
        
        VBox info = new VBox(4);
        HBox.setHgrow(info, Priority.ALWAYS);
        
        Label nameLabel = new Label(customer.getUsername());
        nameLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        
        String email = customer.getEmail() != null ? customer.getEmail() : "No email";
        Label emailLabel = new Label(email);
        emailLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #6b7280;");
        
        info.getChildren().addAll(nameLabel, emailLabel);
        
        // Order count badge
        int orderCount = getCustomerOrderCount(customer.getId());
        Label orderBadge = new Label(orderCount + " orders");
        orderBadge.setStyle("-fx-font-size: 11px; -fx-text-fill: white; -fx-background-color: #3b82f6; " +
                "-fx-padding: 4 8; -fx-background-radius: 12;");
        
        card.getChildren().addAll(avatar, info, orderBadge);
        
        // Click to select
        card.setOnMouseClicked(e -> selectCustomer(customer));
        card.setOnMouseEntered(e -> card.setStyle(card.getStyle().replace("white", "#f0f9ff")));
        card.setOnMouseExited(e -> card.setStyle(card.getStyle().replace("#f0f9ff", "white")));
        
        return card;
    }

    /**
     * Gets the order count for a customer.
     */
    private int getCustomerOrderCount(int customerId) {
        try {
            List<Order> orders = orderService.getOrdersByCustomerId(customerId);
            return orders != null ? orders.size() : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Selects a customer and displays their details.
     */
    private void selectCustomer(User customer) {
        selectedCustomer = customer;
        
        // Show details panel
        if (noCustomerSelected != null) {
            noCustomerSelected.setVisible(false);
            noCustomerSelected.setManaged(false);
        }
        if (customerDetailsContent != null) {
            customerDetailsContent.setVisible(true);
            customerDetailsContent.setManaged(true);
        }
        
        // Load profile image
        if (customerProfileImage != null) {
            if (customer.hasProfileImage()) {
                customerProfileImage.setImage(new Image(new java.io.ByteArrayInputStream(customer.getProfileImageData())));
            } else {
                // Default avatar
                try {
                    customerProfileImage.setImage(new Image(getClass().getResourceAsStream("/images/default.png")));
                } catch (Exception e) {
                    customerProfileImage.setImage(null);
                }
            }
        }
        
        // Basic info
        if (customerNameLabel != null) customerNameLabel.setText(customer.getUsername());
        if (customerEmailLabel != null) customerEmailLabel.setText(customer.getEmail() != null ? customer.getEmail() : "No email");
        if (customerPhoneLabel != null) customerPhoneLabel.setText(customer.getPhoneNumber() != null ? customer.getPhoneNumber() : "No phone");
        
        // Address
        if (customerAddressLabel != null) {
            String address = buildCustomerAddress(customer);
            customerAddressLabel.setText(address.isEmpty() ? "No address provided" : address);
        }
        
        // Stats
        List<Order> customerOrders = orderService.getOrdersByCustomerId(customer.getId());
        int orderCount = customerOrders != null ? customerOrders.size() : 0;
        double totalSpent = customerOrders != null ? customerOrders.stream()
                .filter(o -> !"cancelled".equals(o.getStatus()))
                .mapToDouble(Order::getTotalCost)
                .sum() : 0;
        
        if (customerOrdersLabel != null) customerOrdersLabel.setText(String.valueOf(orderCount));
        if (customerTotalSpentLabel != null) customerTotalSpentLabel.setText(String.format("₺%.0f", totalSpent));
        if (customerPointsLabel != null) customerPointsLabel.setText(String.valueOf(customer.getLoyaltyPoints()));
        
        // Account details
        if (customerCreatedAtLabel != null) {
            customerCreatedAtLabel.setText(customer.getCreatedAt() != null 
                ? customer.getCreatedAt().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")) 
                : "-");
        }
        if (customerVerifiedLabel != null) {
            customerVerifiedLabel.setText(customer.isEmailVerified() ? "✅ Yes" : "❌ No");
        }
        if (customerRegionLabel != null) {
            customerRegionLabel.setText(customer.getRegionId() > 0 ? "Region #" + customer.getRegionId() : "-");
        }
        
        // Load recent orders
        loadCustomerOrders(customerOrders);
    }

    /**
     * Builds address string for customer.
     */
    private String buildCustomerAddress(User customer) {
        StringBuilder sb = new StringBuilder();
        if (customer.getStreetAddress() != null && !customer.getStreetAddress().isEmpty()) {
            sb.append(customer.getStreetAddress());
        }
        if (customer.getApartment() != null && !customer.getApartment().isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append("Apt ").append(customer.getApartment());
        }
        if (customer.getDistrict() != null && !customer.getDistrict().isEmpty()) {
            if (sb.length() > 0) sb.append("\n");
            sb.append(customer.getDistrict());
        }
        if (customer.getCity() != null && !customer.getCity().isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(customer.getCity());
        }
        if (customer.getPostalCode() != null && !customer.getPostalCode().isEmpty()) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(customer.getPostalCode());
        }
        if (sb.length() == 0 && customer.getAddress() != null) {
            sb.append(customer.getAddress());
        }
        return sb.toString();
    }

    /**
     * Loads customer's recent orders into the details panel.
     */
    private void loadCustomerOrders(List<Order> orders) {
        if (customerOrdersContainer == null) return;
        
        customerOrdersContainer.getChildren().clear();
        
        if (orders == null || orders.isEmpty()) {
            Label noOrders = new Label("No orders yet");
            noOrders.setStyle("-fx-text-fill: #9ca3af; -fx-font-style: italic;");
            customerOrdersContainer.getChildren().add(noOrders);
            return;
        }
        
        // Show last 5 orders
        for (int i = 0; i < Math.min(orders.size(), 5); i++) {
            Order order = orders.get(i);
            HBox orderCard = createCustomerOrderCard(order);
            customerOrdersContainer.getChildren().add(orderCard);
        }
    }

    /**
     * Creates an order card for customer details view.
     */
    private HBox createCustomerOrderCard(Order order) {
        HBox card = new HBox(10);
        card.setPadding(new Insets(8, 12, 8, 12));
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle("-fx-background-color: #f9fafb; -fx-background-radius: 6;");
        
        Label orderIdLabel = new Label("#" + order.getId());
        orderIdLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");
        
        VBox details = new VBox(2);
        HBox.setHgrow(details, Priority.ALWAYS);
        
        String dateStr = order.getOrderTime() != null 
            ? order.getOrderTime().format(DateTimeFormatter.ofPattern("MMM dd, HH:mm")) 
            : "Unknown";
        Label dateLabel = new Label(dateStr);
        dateLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #6b7280;");
        
        int itemCount = order.getItems() != null ? order.getItems().size() : 0;
        Label itemsLabel = new Label(itemCount + " items");
        itemsLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #6b7280;");
        
        details.getChildren().addAll(dateLabel, itemsLabel);
        
        Label statusLabel = new Label(order.getStatus());
        statusLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: white; -fx-padding: 2 6; " +
                "-fx-background-color: " + getStatusColor(order.getStatus()) + "; -fx-background-radius: 8;");
        
        Label totalLabel = new Label(String.format("₺%.2f", order.getTotalCost()));
        totalLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2E7D32;");
        
        card.getChildren().addAll(orderIdLabel, details, statusLabel, totalLabel);
        return card;
    }

    /**
     * Handles customer search.
     */
    @FXML
    private void handleCustomerSearch(javafx.scene.input.KeyEvent event) {
        String query = customerSearchField.getText().toLowerCase().trim();
        
        if (customersListContainer == null) return;
        customersListContainer.getChildren().clear();
        
        try {
            List<User> customers = userDAO.findAllCustomers();
            if (customers == null) return;
            
            for (User customer : customers) {
                boolean matches = query.isEmpty() ||
                        customer.getUsername().toLowerCase().contains(query) ||
                        (customer.getEmail() != null && customer.getEmail().toLowerCase().contains(query)) ||
                        (customer.getPhoneNumber() != null && customer.getPhoneNumber().contains(query));
                
                if (matches) {
                    HBox card = createCustomerCard(customer);
                    customersListContainer.getChildren().add(card);
                }
            }
            
            if (customersListContainer.getChildren().isEmpty()) {
                Label noResults = new Label("No customers found");
                noResults.setStyle("-fx-text-fill: #757575; -fx-padding: 20;");
                customersListContainer.getChildren().add(noResults);
            }
        } catch (Exception e) {
            System.err.println("Error searching customers: " + e.getMessage());
        }
    }

    /**
     * Refreshes the customers list.
     */
    @FXML
    private void handleRefreshCustomers(ActionEvent event) {
        if (customerSearchField != null) customerSearchField.clear();
        loadCustomers();
    }

    /**
     * Sends a message to the selected customer.
     */
    @FXML
    private void handleMessageCustomer(ActionEvent event) {
        if (selectedCustomer == null) {
            showError("No customer selected");
            return;
        }
        
        // Open messaging dialog or full inbox
        try {
            MainApplication.openNewWindow("/fxml/OwnerMessagesView.fxml", "Group03 - GreenGrocer");
        } catch (Exception e) {
            showError("Could not open messaging: " + e.getMessage());
        }
    }
    
    /**
     * Views all carrier ratings in a detailed dialog.
     */
    @FXML
    private void handleViewAllRatings(ActionEvent event) {
        try {
            List<CarrierRating> allRatings = carrierService.getAllRatings();
            
            // Create dialog window
            Stage dialogStage = new Stage();
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.setTitle("All Carrier Ratings");
            dialogStage.setWidth(800);
            dialogStage.setHeight(600);
            
            VBox root = new VBox(16);
            root.setPadding(new Insets(20));
            root.setStyle("-fx-background-color: #fafafa;");
            
            Label titleLabel = new Label("All Carrier Ratings");
            titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #1f2937;");
            
            ScrollPane scrollPane = new ScrollPane();
            scrollPane.setFitToWidth(true);
            scrollPane.setStyle("-fx-background-color: transparent;");
            
            VBox ratingsContainer = new VBox(12);
            ratingsContainer.setPadding(new Insets(10));
            
            if (allRatings == null || allRatings.isEmpty()) {
                Label emptyLabel = new Label("No ratings found");
                emptyLabel.setStyle("-fx-text-fill: #9ca3af; -fx-font-size: 14px; -fx-padding: 20;");
                ratingsContainer.getChildren().add(emptyLabel);
            } else {
                for (CarrierRating rating : allRatings) {
                    VBox ratingCard = createDetailedRatingCard(rating);
                    ratingsContainer.getChildren().add(ratingCard);
                }
            }
            
            scrollPane.setContent(ratingsContainer);
            
            MFXButton closeButton = new MFXButton("Close");
            closeButton.getStyleClass().add("mfx-button-raised");
            closeButton.setOnAction(e -> dialogStage.close());
            
            root.getChildren().addAll(titleLabel, scrollPane, closeButton);
            VBox.setVgrow(scrollPane, Priority.ALWAYS);
            
            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            ThemeManager.getInstance().applyTheme(scene);
            dialogStage.setScene(scene);
            dialogStage.centerOnScreen();
            dialogStage.show();
        } catch (Exception e) {
            System.err.println("Error showing ratings: " + e.getMessage());
            showError("Could not load ratings: " + e.getMessage());
        }
    }
    
    /**
     * Creates a detailed rating card for display in the ratings dialog.
     */
    private VBox createDetailedRatingCard(CarrierRating rating) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(16));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 10; " +
                     "-fx-border-color: #e5e7eb; -fx-border-width: 1; -fx-border-radius: 10;");
        
        HBox headerRow = new HBox(12);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        
        // Carrier name
        Label carrierLabel = new Label("Carrier: " + (rating.getCarrierName() != null ? rating.getCarrierName() : "Unknown"));
        carrierLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1f2937;");
        
        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        // Rating stars
        String stars = getStarDisplay((double) rating.getRating());
        Label starsLabel = new Label(stars);
        starsLabel.setStyle("-fx-font-size: 16px;");
        
        Label ratingValueLabel = new Label(String.format("%d/5", rating.getRating()));
        ratingValueLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #f59e0b;");
        
        headerRow.getChildren().addAll(carrierLabel, spacer, starsLabel, ratingValueLabel);
        card.getChildren().add(headerRow);
        
        // Customer name
        Label customerLabel = new Label("Rated by: " + (rating.getCustomerName() != null ? rating.getCustomerName() : "Unknown"));
        customerLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #6b7280;");
        card.getChildren().add(customerLabel);
        
        // Comment
        if (rating.getComment() != null && !rating.getComment().isEmpty()) {
            Label commentLabel = new Label("💬 " + rating.getComment());
            commentLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #374151; -fx-wrap-text: true;");
            commentLabel.setWrapText(true);
            card.getChildren().add(commentLabel);
        }
        
        // Order and date info
        HBox footerRow = new HBox(16);
        Label orderLabel = new Label("Order #" + rating.getOrderId());
        orderLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #9ca3af;");
        
        if (rating.getRatedAt() != null) {
            Label dateLabel = new Label("Date: " + rating.getRatedAt().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")));
            dateLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #9ca3af;");
            footerRow.getChildren().addAll(orderLabel, dateLabel);
        } else {
            footerRow.getChildren().add(orderLabel);
        }
        card.getChildren().add(footerRow);
        
        return card;
    }
    
    /**
     * Removes the selected customer.
     */
    @FXML
    private void handleRemoveCustomer(ActionEvent event) {
        if (selectedCustomer == null) {
            showError("No customer selected");
            return;
        }
        
        // Check if customer has active/pending orders
        try {
            List<Order> orders = orderService.getOrdersByCustomerId(selectedCustomer.getId());
            if (orders != null && !orders.isEmpty()) {
                // Check for non-delivered orders
                long activeOrders = orders.stream()
                    .filter(o -> !"delivered".equalsIgnoreCase(o.getStatus()) && !"cancelled".equalsIgnoreCase(o.getStatus()))
                    .count();
                
                if (activeOrders > 0) {
                    showError("Cannot remove customer: " + activeOrders + " active order(s) found.\n\n" +
                             "Please wait for all orders to be delivered or cancelled before removing the customer.");
                    return;
                }
            }
        } catch (Exception e) {
            System.err.println("Error checking customer orders: " + e.getMessage());
            showError("Error checking customer orders. Please try again.");
            return;
        }
        
        // Show confirmation dialog
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Removal");
        confirm.setHeaderText("Remove Customer");
        confirm.setContentText("Are you sure you want to remove customer '" + selectedCustomer.getUsername() + "'?\n\n" +
                              "This action cannot be undone. All customer data will be permanently deleted.");
        
        java.util.Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                // Delete the customer
                boolean deleted = userDAO.delete(selectedCustomer.getId());
                
                if (deleted) {
                    showInfo("Customer '" + selectedCustomer.getUsername() + "' has been removed successfully.");
                    // Clear selection and refresh list
                    selectedCustomer = null;
                    if (noCustomerSelected != null) {
                        noCustomerSelected.setVisible(true);
                        noCustomerSelected.setManaged(true);
                    }
                    if (customerDetailsContent != null) {
                        customerDetailsContent.setVisible(false);
                        customerDetailsContent.setManaged(false);
                    }
                    loadCustomers();
                } else {
                    showError("Failed to remove customer. Please try again.");
                }
            } catch (Exception e) {
                System.err.println("Error removing customer: " + e.getMessage());
                showError("Error removing customer: " + e.getMessage());
            }
        }
    }

    // ==================== MESSAGES TAB ====================

    /**
     * Sets up messages tab with MaterialFX components.
     */
    private void setupMessagesTab() {
        // Set up MaterialFX list view
        // Load messages
        loadMessages();
    }

    /**
     * Loads all messages into the ScrollPane container.
     */
    private void loadMessages() {
        if (messagesListContainer == null) return;
        
        try {
            List<Message> messages = messageService.getAllMessagesForOwner();
            if (messages == null) {
                messages = new ArrayList<>();
            }
            
            // Clear and populate the container
            messagesListContainer.getChildren().clear();
            
            if (messages.isEmpty()) {
                Label emptyLabel = new Label("📭 No messages yet");
                emptyLabel.setStyle("-fx-text-fill: #9ca3af; -fx-font-size: 16px; -fx-padding: 40;");
                messagesListContainer.getChildren().add(emptyLabel);
            } else {
                for (Message message : messages) {
                    VBox messageCard = createMessageCard(message);
                    messagesListContainer.getChildren().add(messageCard);
                }
            }
            
            // Update message count label
            if (messageCountLabel != null) {
                messageCountLabel.setText(messages.size() + " message" + (messages.size() != 1 ? "s" : ""));
            }
            
            // Update stat count labels
            List<Message> finalMessages = messages;
            long unreadCount = finalMessages.stream().filter(m -> !m.isRead()).count();
            long urgentCount = finalMessages.stream()
                    .filter(m -> {
                        String content = ((m.getContent() != null ? m.getContent() : "") + " " + 
                                         (m.getSubject() != null ? m.getSubject() : "")).toLowerCase();
                        return content.contains("refund") || content.contains("urgent") || content.contains("problem");
                    })
                    .count();
            long todayCount = finalMessages.stream()
                    .filter(m -> m.getSentAt() != null && m.getSentAt().toLocalDate().equals(java.time.LocalDate.now()))
                    .count();
            
            if (unreadMessagesCount != null) {
                unreadMessagesCount.setText(String.valueOf(unreadCount));
            }
            if (urgentMessagesCount != null) {
                urgentMessagesCount.setText(String.valueOf(urgentCount));
            }
            if (todayMessagesCount != null) {
                todayMessagesCount.setText(String.valueOf(todayCount));
            }
        } catch (Exception e) {
            System.err.println("Error loading messages: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Creates a message card for display in the list.
     */
    private VBox createMessageCard(Message message) {
        VBox card = new VBox(8);
        card.setStyle("-fx-padding: 16; -fx-background-color: " + 
                (message.isRead() ? "white" : "#fefce8") + 
                "; -fx-background-radius: 8; -fx-border-color: #e5e7eb; -fx-border-radius: 8; -fx-cursor: hand;");
        
        // Header row: sender name + date
        HBox header = new HBox(10);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        
        String senderName = message.getSenderName() != null ? message.getSenderName() : "Unknown";
        Label nameLabel = new Label(senderName);
        nameLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #111827;");
        
        // Unread indicator
        if (!message.isRead()) {
            Label unreadDot = new Label("●");
            unreadDot.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 10px;");
            header.getChildren().add(unreadDot);
        }
        
        header.getChildren().add(nameLabel);
        
        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        header.getChildren().add(spacer);
        
        String dateStr = message.getSentAt() != null 
                ? message.getSentAt().format(java.time.format.DateTimeFormatter.ofPattern("MMM dd, HH:mm"))
                : "";
        Label dateLabel = new Label(dateStr);
        dateLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #9ca3af;");
        header.getChildren().add(dateLabel);
        
        // Subject
        String subject = message.getSubject() != null ? message.getSubject() : "No Subject";
        Label subjectLabel = new Label(subject);
        subjectLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #374151;");
        
        // Content preview
        String content = message.getContent() != null ? message.getContent() : "";
        String preview = content.length() > 100 ? content.substring(0, 100) + "..." : content;
        Label contentLabel = new Label(preview);
        contentLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #6b7280;");
        contentLabel.setWrapText(true);
        
        card.getChildren().addAll(header, subjectLabel, contentLabel);
        
        // Click to open full inbox with this message selected
        card.setOnMouseClicked(e -> {
            // Mark as read
            messageService.markAsRead(message.getId());
            // Open full inbox
            handleOpenFullInbox(null);
        });
        
        // Hover effect
        card.setOnMouseEntered(e -> card.setStyle(card.getStyle().replace("-fx-background-color: white", "-fx-background-color: #f9fafb")));
        card.setOnMouseExited(e -> {
            if (message.isRead()) {
                card.setStyle(card.getStyle().replace("-fx-background-color: #f9fafb", "-fx-background-color: white"));
            }
        });
        
        return card;
    }
    
    /**
     * Refreshes the messages list.
     */
    @FXML
    private void handleRefreshMessages(ActionEvent event) {
        loadMessages();
    }

    /**
     * Opens the full inbox dashboard with 3-pane layout.
     * 
     * @param event Action event
     */
    @FXML
    private void handleOpenFullInbox(ActionEvent event) {
        try {
            MainApplication.openNewWindow("/fxml/OwnerMessagesView.fxml", "Group03 - GreenGrocer");
        } catch (Exception e) {
            showError("Could not open inbox dashboard: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ==================== REPORTS TAB ====================

    /**
     * Sets up reports tab with charts and statistics.
     */
    private void setupReportsTab() {
        try {
            // Update date label
            if (reportDateLabel != null) {
                reportDateLabel.setText("Data as of " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")));
            }
            
            // Load key statistics
            loadKeyStatistics();
            
            // Load charts
            loadSalesPieChart();
            loadRevenueBarChart();
            loadTopProducts();
            loadCarrierPerformance();
            loadLowStockAlerts();
            
        } catch (Exception e) {
            System.err.println("Error setting up reports tab: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Loads key statistics (revenue, orders, customers, avg order value).
     */
    private void loadKeyStatistics() {
        try {
            List<Order> allOrders = orderService.getAllOrders();
            List<User> allCustomers = userDAO.findAllCustomers();
            
            // Calculate totals
            double totalRevenue = allOrders.stream()
                    .filter(o -> !"cancelled".equals(o.getStatus()))
                    .mapToDouble(Order::getTotalCost)
                    .sum();
            
            int totalOrders = (int) allOrders.stream()
                    .filter(o -> !"cancelled".equals(o.getStatus()))
                    .count();
            
            int customerCount = allCustomers != null ? allCustomers.size() : 0;
            
            double avgOrderValue = totalOrders > 0 ? totalRevenue / totalOrders : 0;
            
            // Update labels
            if (totalRevenueLabel != null) {
                totalRevenueLabel.setText(String.format("₺%.2f", totalRevenue));
            }
            if (totalOrdersLabel != null) {
                totalOrdersLabel.setText(String.valueOf(totalOrders));
            }
            if (totalCustomersLabel != null) {
                totalCustomersLabel.setText(String.valueOf(customerCount));
            }
            if (avgOrderValueLabel != null) {
                avgOrderValueLabel.setText(String.format("₺%.2f", avgOrderValue));
            }
            
        } catch (Exception e) {
            System.err.println("Error loading key statistics: " + e.getMessage());
        }
    }

    /**
     * Loads sales pie chart by product category (vegetables vs fruits).
     */
    private void loadSalesPieChart() {
        if (salesPieChart == null) return;
        
        try {
            salesPieChart.getData().clear();
            
            List<Order> allOrders = orderService.getAllOrders();
            
            // Calculate sales by type
            double vegetableSales = 0;
            double fruitSales = 0;
            
            for (Order order : allOrders) {
                if ("cancelled".equals(order.getStatus())) continue;
                
                for (OrderItem item : order.getItems()) {
                    String productName = item.getProductName().toLowerCase();
                    double itemTotal = item.getTotalPrice();
                    
                    // Simple categorization based on common products
                    if (isVegetable(productName)) {
                        vegetableSales += itemTotal;
                    } else {
                        fruitSales += itemTotal;
                    }
                }
            }
            
            // Add data to pie chart
            if (vegetableSales > 0 || fruitSales > 0) {
                javafx.scene.chart.PieChart.Data vegData = new javafx.scene.chart.PieChart.Data(
                        String.format("🥬 Vegetables (₺%.0f)", vegetableSales), vegetableSales);
                javafx.scene.chart.PieChart.Data fruitData = new javafx.scene.chart.PieChart.Data(
                        String.format("🍎 Fruits (₺%.0f)", fruitSales), fruitSales);
                
                salesPieChart.getData().addAll(vegData, fruitData);
                
                // Style the slices
                vegData.getNode().setStyle("-fx-pie-color: #4CAF50;");
                fruitData.getNode().setStyle("-fx-pie-color: #FF9800;");
            } else {
                // No data - add placeholder
                salesPieChart.getData().add(new javafx.scene.chart.PieChart.Data("No sales data", 1));
            }
            
        } catch (Exception e) {
            System.err.println("Error loading sales pie chart: " + e.getMessage());
        }
    }

    /**
     * Checks if a product is a vegetable based on name.
     */
    private boolean isVegetable(String productName) {
        String[] vegetables = {"tomato", "potato", "onion", "carrot", "cucumber", "pepper", 
                "eggplant", "zucchini", "lettuce", "spinach", "broccoli", "cabbage", 
                "artichoke", "corn", "celery", "pumpkin", "cauliflower", "leek", 
                "kohlrabi", "fennel", "red cabbage"};
        for (String veg : vegetables) {
            if (productName.contains(veg)) return true;
        }
        return false;
    }

    /**
     * Loads revenue bar chart for last 7 days.
     */
    private void loadRevenueBarChart() {
        if (revenueBarChart == null) return;
        
        try {
            revenueBarChart.getData().clear();
            
            List<Order> allOrders = orderService.getAllOrders();
            LocalDateTime now = LocalDateTime.now();
            
            // Calculate revenue for last 7 days
            javafx.scene.chart.XYChart.Series<String, Number> series = new javafx.scene.chart.XYChart.Series<>();
            series.setName("Daily Revenue");
            
            DateTimeFormatter dayFormatter = DateTimeFormatter.ofPattern("EEE");
            
            for (int i = 6; i >= 0; i--) {
                LocalDateTime dayStart = now.minusDays(i).toLocalDate().atStartOfDay();
                LocalDateTime dayEnd = dayStart.plusDays(1);
                
                double dayRevenue = allOrders.stream()
                        .filter(o -> !"cancelled".equals(o.getStatus()))
                        .filter(o -> o.getOrderTime() != null)
                        .filter(o -> o.getOrderTime().isAfter(dayStart) && o.getOrderTime().isBefore(dayEnd))
                        .mapToDouble(Order::getTotalCost)
                        .sum();
                
                String dayLabel = dayStart.format(dayFormatter);
                series.getData().add(new javafx.scene.chart.XYChart.Data<>(dayLabel, dayRevenue));
            }
            
            revenueBarChart.getData().add(series);
            
            // Style the bars
            for (javafx.scene.chart.XYChart.Data<String, Number> data : series.getData()) {
                if (data.getNode() != null) {
                    data.getNode().setStyle("-fx-bar-fill: #2E7D32;");
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error loading revenue bar chart: " + e.getMessage());
        }
    }

    /**
     * Loads top selling products list.
     */
    private void loadTopProducts() {
        if (topProductsContainer == null) return;
        
        try {
            topProductsContainer.getChildren().clear();
            
            List<Order> allOrders = orderService.getAllOrders();
            
            // Count sales by product
            java.util.Map<String, Double> productSales = new java.util.HashMap<>();
            java.util.Map<String, Double> productQuantities = new java.util.HashMap<>();
            
            for (Order order : allOrders) {
                if ("cancelled".equals(order.getStatus())) continue;
                
                for (OrderItem item : order.getItems()) {
                    String name = item.getProductName();
                    productSales.merge(name, item.getTotalPrice(), Double::sum);
                    productQuantities.merge(name, item.getQuantity(), Double::sum);
                }
            }
            
            // Sort by sales and get top 5
            java.util.List<java.util.Map.Entry<String, Double>> sortedProducts = productSales.entrySet().stream()
                    .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                    .limit(5)
                    .collect(java.util.stream.Collectors.toList());
            
            if (sortedProducts.isEmpty()) {
                Label noData = new Label("No sales data yet");
                noData.setStyle("-fx-text-fill: #9ca3af; -fx-font-style: italic;");
                topProductsContainer.getChildren().add(noData);
                return;
            }
            
            int rank = 1;
            for (java.util.Map.Entry<String, Double> entry : sortedProducts) {
                HBox row = createTopProductRow(rank, entry.getKey(), 
                        productQuantities.getOrDefault(entry.getKey(), 0.0), entry.getValue());
                topProductsContainer.getChildren().add(row);
                rank++;
            }
            
        } catch (Exception e) {
            System.err.println("Error loading top products: " + e.getMessage());
        }
    }

    /**
     * Creates a row for top product display.
     */
    private HBox createTopProductRow(int rank, String productName, double quantity, double revenue) {
        HBox row = new HBox(12);
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        row.setStyle("-fx-padding: 8 12; -fx-background-color: " + (rank <= 3 ? "#f0fdf4" : "#f9fafb") + "; -fx-background-radius: 8;");
        
        // Rank badge
        String rankEmoji = rank == 1 ? "🥇" : (rank == 2 ? "🥈" : (rank == 3 ? "🥉" : "#" + rank));
        Label rankLabel = new Label(rankEmoji);
        rankLabel.setStyle("-fx-font-size: 16px; -fx-min-width: 30;");
        
        // Product name
        Label nameLabel = new Label(productName);
        nameLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        HBox.setHgrow(nameLabel, Priority.ALWAYS);
        
        // Quantity
        Label qtyLabel = new Label(String.format("%.1f kg", quantity));
        qtyLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #6b7280;");
        
        // Revenue
        Label revenueLabel = new Label(String.format("₺%.2f", revenue));
        revenueLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2E7D32;");
        
        row.getChildren().addAll(rankLabel, nameLabel, qtyLabel, revenueLabel);
        return row;
    }

    /**
     * Loads carrier performance data.
     */
    private void loadCarrierPerformance() {
        if (carrierPerformanceContainer == null) return;
        
        try {
            carrierPerformanceContainer.getChildren().clear();
            
            List<User> carriers = userDAO.findAllCarriers();
            List<Order> allOrders = orderService.getAllOrders();
            
            if (carriers == null || carriers.isEmpty()) {
                Label noData = new Label("No carriers registered");
                noData.setStyle("-fx-text-fill: #9ca3af; -fx-font-style: italic;");
                carrierPerformanceContainer.getChildren().add(noData);
                return;
            }
            
            for (User carrier : carriers) {
                // Count deliveries for this carrier
                long deliveries = allOrders.stream()
                        .filter(o -> carrier.getId() == (o.getCarrierId() != null ? o.getCarrierId() : 0))
                        .filter(o -> "delivered".equals(o.getStatus()))
                        .count();
                
                long pending = allOrders.stream()
                        .filter(o -> carrier.getId() == (o.getCarrierId() != null ? o.getCarrierId() : 0))
                        .filter(o -> "assigned".equals(o.getStatus()))
                        .count();
                
                HBox row = createCarrierRow(carrier.getUsername(), deliveries, pending);
                carrierPerformanceContainer.getChildren().add(row);
            }
            
        } catch (Exception e) {
            System.err.println("Error loading carrier performance: " + e.getMessage());
        }
    }

    /**
     * Creates a row for carrier performance display.
     */
    private HBox createCarrierRow(String carrierName, long deliveries, long pending) {
        HBox row = new HBox(12);
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        row.setStyle("-fx-padding: 10 12; -fx-background-color: #f9fafb; -fx-background-radius: 8;");
        
        Label icon = new Label("🚴");
        icon.setStyle("-fx-font-size: 18px;");
        
        Label nameLabel = new Label(carrierName);
        nameLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        HBox.setHgrow(nameLabel, Priority.ALWAYS);
        
        VBox stats = new VBox(2);
        stats.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        
        Label deliveredLabel = new Label("✓ " + deliveries + " delivered");
        deliveredLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #2E7D32;");
        
        Label pendingLabel = new Label("⏳ " + pending + " pending");
        pendingLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #f59e0b;");
        
        stats.getChildren().addAll(deliveredLabel, pendingLabel);
        row.getChildren().addAll(icon, nameLabel, stats);
        return row;
    }

    /**
     * Loads low stock alerts.
     */
    private void loadLowStockAlerts() {
        if (lowStockContainer == null) return;
        
        try {
            lowStockContainer.getChildren().clear();
            
            List<Product> allProducts = productService.getAllProducts();
            
            // Filter products with low stock
            java.util.List<Product> lowStockProducts = allProducts.stream()
                    .filter(p -> p.getStock() <= p.getThreshold())
                    .sorted((a, b) -> Double.compare(a.getStock(), b.getStock()))
                    .limit(5)
                    .collect(java.util.stream.Collectors.toList());
            
            if (lowStockProducts.isEmpty()) {
                Label allGood = new Label("✓ All products well stocked!");
                allGood.setStyle("-fx-text-fill: #2E7D32; -fx-font-weight: bold;");
                lowStockContainer.getChildren().add(allGood);
                return;
            }
            
            for (Product product : lowStockProducts) {
                HBox row = createLowStockRow(product);
                lowStockContainer.getChildren().add(row);
            }
            
        } catch (Exception e) {
            System.err.println("Error loading low stock alerts: " + e.getMessage());
        }
    }

    /**
     * Creates a row for low stock alert display.
     */
    private HBox createLowStockRow(Product product) {
        HBox row = new HBox(12);
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        
        boolean critical = product.getStock() <= product.getThreshold() / 2;
        row.setStyle("-fx-padding: 8 12; -fx-background-color: " + 
                (critical ? "#fef2f2" : "#fffbeb") + "; -fx-background-radius: 8;");
        
        Label icon = new Label(critical ? "🔴" : "🟡");
        icon.setStyle("-fx-font-size: 14px;");
        
        Label nameLabel = new Label(product.getName());
        nameLabel.setStyle("-fx-font-size: 13px;");
        HBox.setHgrow(nameLabel, Priority.ALWAYS);
        
        String unitStr = product.getType() != null && product.getType().contains("piece") ? "pcs" : "kg";
        Label stockLabel = new Label(String.format("%.1f %s", product.getStock(), unitStr));
        stockLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: " + 
                (critical ? "#dc2626" : "#f59e0b") + ";");
        
        row.getChildren().addAll(icon, nameLabel, stockLabel);
        return row;
    }

    /**
     * Refreshes reports data.
     */
    @FXML
    private void handleRefreshReports(ActionEvent event) {
        setupReportsTab();
    }

    /**
     * Generates revenue report.
     */
    @FXML
    private void handleGenerateRevenueReport(ActionEvent event) {
        setupReportsTab();
    }

    /**
     * Generates product sales report.
     */
    @FXML
    private void handleGenerateProductReport(ActionEvent event) {
        setupReportsTab();
    }

    // ==================== COUPONS ====================

    /**
     * Handles creating new coupon.
     * 
     * @param event Action event
     */
    @FXML
    private void handleCreateCoupon(ActionEvent event) {
        // Open coupon creation dialog
        // Create coupon
        // Refresh
    }
    
    /**
     * Handles adding a coupon to the selected customer.
     */
    @FXML
    private void handleAddCouponToCustomer(ActionEvent event) {
        if (selectedCustomer == null) {
            showError("No customer selected");
            return;
        }
        
        // Create coupon creation dialog
        Stage dialogStage = new Stage();
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.setTitle("Create Coupon for " + selectedCustomer.getUsername());
        dialogStage.setWidth(450);
        dialogStage.setHeight(550);
        
        VBox root = new VBox(16);
        root.setPadding(new Insets(24));
        root.setStyle("-fx-background-color: #fafafa;");
        
        Label titleLabel = new Label("Create Coupon");
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #1f2937;");
        
        Label customerLabel = new Label("Customer: " + selectedCustomer.getUsername());
        customerLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #6b7280;");
        
        MFXTextField codeField = new MFXTextField();
        codeField.setFloatingText("Coupon Code");
        codeField.setPromptText("e.g., WELCOME10");
        codeField.setPrefWidth(400);
        
        MFXTextField discountField = new MFXTextField();
        discountField.setFloatingText("Discount Percentage");
        discountField.setPromptText("e.g., 10 (for 10%)");
        discountField.setPrefWidth(400);
        
        // Use TextField with prompt for date (MFXDatePicker may not be available)
        MFXTextField expirationField = new MFXTextField();
        expirationField.setFloatingText("Expiration Date (YYYY-MM-DD)");
        expirationField.setPromptText("e.g., 2025-12-31");
        expirationField.setPrefWidth(400);
        
        MFXTextField minOrderField = new MFXTextField();
        minOrderField.setFloatingText("Minimum Order Value (₺)");
        minOrderField.setPromptText("0 for no minimum");
        minOrderField.setPrefWidth(400);
        minOrderField.setText("0");
        
        MFXTextField maxDiscountField = new MFXTextField();
        maxDiscountField.setFloatingText("Maximum Discount (₺)");
        maxDiscountField.setPromptText("0 for no limit");
        maxDiscountField.setPrefWidth(400);
        maxDiscountField.setText("0");
        
        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 12px;");
        errorLabel.setVisible(false);
        
        HBox buttonRow = new HBox(12);
        buttonRow.setAlignment(Pos.CENTER_RIGHT);
        
        MFXButton cancelButton = new MFXButton("Cancel");
        cancelButton.getStyleClass().add("mfx-button-outlined");
        cancelButton.setOnAction(e -> dialogStage.close());
        
        MFXButton createButton = new MFXButton("Create Coupon");
        createButton.getStyleClass().add("mfx-button-raised");
        createButton.setOnAction(e -> {
            try {
                String code = codeField.getText().trim();
                String discountStr = discountField.getText().trim();
                String expirationStr = expirationField.getText().trim();
                String minOrderStr = minOrderField.getText().trim();
                String maxDiscountStr = maxDiscountField.getText().trim();
                
                // Validation
                if (code.isEmpty()) {
                    errorLabel.setText("Coupon code is required");
                    errorLabel.setVisible(true);
                    return;
                }
                
                if (discountStr.isEmpty()) {
                    errorLabel.setText("Discount percentage is required");
                    errorLabel.setVisible(true);
                    return;
                }
                
                if (expirationStr.isEmpty()) {
                    errorLabel.setText("Expiration date is required");
                    errorLabel.setVisible(true);
                    return;
                }
                
                double discount = Double.parseDouble(discountStr);
                LocalDate expirationDate = LocalDate.parse(expirationStr);
                double minOrder = minOrderStr.isEmpty() ? 0 : Double.parseDouble(minOrderStr);
                double maxDiscount = maxDiscountStr.isEmpty() ? 0 : Double.parseDouble(maxDiscountStr);
                
                // Create coupon
                Coupon coupon = couponService.createCoupon(code, discount, expirationDate, minOrder, maxDiscount, selectedCustomer.getId());
                
                if (coupon != null) {
                    showInfo("Coupon '" + code + "' created successfully for " + selectedCustomer.getUsername());
                    dialogStage.close();
                } else {
                    errorLabel.setText("Failed to create coupon");
                    errorLabel.setVisible(true);
                }
            } catch (IllegalArgumentException ex) {
                errorLabel.setText(ex.getMessage());
                errorLabel.setVisible(true);
            } catch (Exception ex) {
                errorLabel.setText("Error: " + ex.getMessage());
                errorLabel.setVisible(true);
                System.err.println("Error creating coupon: " + ex.getMessage());
                ex.printStackTrace();
            }
        });
        
        buttonRow.getChildren().addAll(cancelButton, createButton);
        
        root.getChildren().addAll(titleLabel, customerLabel, codeField, discountField, expirationField, 
                                  minOrderField, maxDiscountField, errorLabel, buttonRow);
        
        javafx.scene.Scene scene = new javafx.scene.Scene(root);
        ThemeManager.getInstance().applyTheme(scene);
        dialogStage.setScene(scene);
        dialogStage.centerOnScreen();
        dialogStage.show();
    }

    // ==================== COMMON ====================

    /**
     * Launches the Dashboard Pro view.
     * 
     * @param event Action event
     */
    @FXML
    private void handleLaunchDashboardPro(ActionEvent event) {
        try {
            MainApplication.openNewWindow("/fxml/OwnerDashboardPro.fxml", "Group03 - GreenGrocer");
        } catch (Exception e) {
            showError("Could not open Dashboard Pro: " + e.getMessage());
            e.printStackTrace();
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
     * Shows error alert.
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
     * Shows info alert.
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
     * Toggle between available themes
     * 
     * @param event Action event
     */
    @FXML
    private void handleToggleTheme(ActionEvent event) {
        try {
            ThemeManager.getInstance().toggleTheme(mainTabPane.getScene());
        } catch (Exception e) {
            System.err.println("Error toggling theme: " + e.getMessage());
        }
    }
}
