package com.greengrocer.controllers;

import com.greengrocer.app.SessionManager;
import com.greengrocer.models.CartItem;
import com.greengrocer.models.Coupon;
import com.greengrocer.models.Order;
import com.greengrocer.models.Product;
import com.greengrocer.models.User;
import com.greengrocer.services.*;
import io.github.palexdev.materialfx.controls.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controller for the shopping cart view using MaterialFX components.
 * Handles cart management, checkout, and order placement.
 * 
 * Fixed issues:
 * - Re-validates coupon at checkout time
 * - Validates stock before order submission
 * - Proper error handling for race conditions
 * 
 * @author Emir Kıraç Varol
 * @version 2.0.0
 */
public class ShoppingCartController implements Initializable {

    /** Cart items container (VBox for styled list) */
    @FXML
    private VBox cartItemsContainer;
    
    /** Currently selected cart item */
    private CartItem selectedItem = null;
    
    /** Currently selected item card (for styling) */
    private HBox selectedCard = null;
    
    /** Subtotal label */
    @FXML
    private Label subtotalLabel;
    
    /** Discount label */
    @FXML
    private Label discountLabel;
    
    /** VAT label */
    @FXML
    private Label vatLabel;
    
    /** Total label */
    @FXML
    private Label totalLabel;
    
    /** Coupon code input (MaterialFX) */
    @FXML
    private MFXTextField couponField;
    
    /** Coupon status label */
    @FXML
    private Label couponStatusLabel;
    
    /** Delivery date picker (MaterialFX) */
    @FXML
    private MFXDatePicker deliveryDatePicker;
    
    /** Delivery time combo box (MaterialFX) */
    @FXML
    private MFXComboBox<String> deliveryTimeCombo;
    
    /** Minimum value warning label */
    @FXML
    private Label minValueLabel;
    
    /** Loyalty discount label */
    @FXML
    private Label loyaltyDiscountLabel;

    /** Cart service */
    private CartService cartService;
    
    /** Order service */
    private OrderService orderService;
    
    /** Coupon service */
    private CouponService couponService;
    
    /** Loyalty service */
    private LoyaltyService loyaltyService;
    
    /** Invoice service */
    private InvoiceService invoiceService;
    
    /** Product service */
    private ProductService productService;

    /** Currently applied coupon (cached for re-validation) */
    private Coupon appliedCoupon;

    /**
     * Initializes the controller.
     * 
     * @param location FXML location
     * @param resources Resource bundle
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        cartService = new CartService();
        orderService = new OrderService();
        couponService = new CouponService();
        loyaltyService = new LoyaltyService();
        invoiceService = new InvoiceService();
        productService = new ProductService();
        
        // Load cart items into styled list
        refreshCart();
        
        // Set up delivery options
        setupDeliveryOptions();
        
        // Apply loyalty discount
        applyLoyaltyDiscount();
        
        // Update display
        updateTotals();
    }

    /**
     * Builds the cart items list with styled item cards.
     */
    private void buildCartItemsList() {
        cartItemsContainer.getChildren().clear();
        selectedItem = null;
        selectedCard = null;
        
        List<CartItem> items = cartService.getCartItems();
        
        if (items.isEmpty()) {
            // Show empty cart message
            Label emptyLabel = new Label("Your cart is empty");
            emptyLabel.setStyle("-fx-font-size: 18px; -fx-text-fill: #757575; -fx-padding: 40px;");
            cartItemsContainer.setAlignment(Pos.CENTER);
            cartItemsContainer.getChildren().add(emptyLabel);
            return;
        }
        
        cartItemsContainer.setAlignment(Pos.TOP_LEFT);
        
        int itemNumber = 1;
        for (CartItem item : items) {
            HBox itemCard = createItemCard(item, itemNumber++);
            cartItemsContainer.getChildren().add(itemCard);
        }
    }
    
    /**
     * Creates a styled card for a cart item.
     */
    private HBox createItemCard(CartItem item, int itemNumber) {
        HBox card = new HBox(16);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(16, 20, 16, 20));
        card.setStyle("-fx-background-color: #FAFAFA; -fx-background-radius: 12px; " +
                     "-fx-border-color: #E0E0E0; -fx-border-radius: 12px; -fx-cursor: hand;");
        
        // Number badge
        Label numberLabel = new Label(String.valueOf(itemNumber));
        numberLabel.setMinSize(36, 36);
        numberLabel.setMaxSize(36, 36);
        numberLabel.setAlignment(Pos.CENTER);
        numberLabel.setStyle("-fx-background-color: #4CAF50; -fx-background-radius: 18px; " +
                            "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");
        
        // Product image
        StackPane imageContainer = createProductImage(item);
        
        // Product info
        VBox infoBox = new VBox(4);
        HBox.setHgrow(infoBox, Priority.ALWAYS);
        
        Label nameLabel = new Label(item.getProductName());
        nameLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #212121;");
        
        // Show product type badge
        String typeEmoji = "vegetable".equalsIgnoreCase(item.getProductType()) ? "🥬" : "🍎";
        Label typeLabel = new Label(typeEmoji + " " + 
                (item.getProductType() != null ? item.getProductType().substring(0, 1).toUpperCase() + 
                item.getProductType().substring(1) : ""));
        typeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #9E9E9E;");
        
        Label detailsLabel = new Label(String.format("%.2f kg × %.2f TL/kg", 
                item.getQuantity(), item.getUnitPrice()));
        detailsLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #757575;");
        
        infoBox.getChildren().addAll(nameLabel, typeLabel, detailsLabel);
        
        // Price
        VBox priceBox = new VBox(2);
        priceBox.setAlignment(Pos.CENTER_RIGHT);
        
        Label priceLabel = new Label(String.format("%.2f TL", item.getTotalPrice()));
        priceLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2E7D32;");
        
        priceBox.getChildren().add(priceLabel);
        
        card.getChildren().addAll(numberLabel, imageContainer, infoBox, priceBox);
        
        // Click to select
        card.setOnMouseClicked(e -> selectItem(item, card));
        
        // Hover effect
        card.setOnMouseEntered(e -> {
            if (card != selectedCard) {
                card.setStyle("-fx-background-color: #F5F5F5; -fx-background-radius: 12px; " +
                             "-fx-border-color: #BDBDBD; -fx-border-radius: 12px; -fx-cursor: hand;");
            }
        });
        card.setOnMouseExited(e -> {
            if (card != selectedCard) {
                card.setStyle("-fx-background-color: #FAFAFA; -fx-background-radius: 12px; " +
                             "-fx-border-color: #E0E0E0; -fx-border-radius: 12px; -fx-cursor: hand;");
            }
        });
        
        return card;
    }
    
    /**
     * Creates a styled product image container with rounded corners.
     */
    private StackPane createProductImage(CartItem item) {
        StackPane imageContainer = new StackPane();
        imageContainer.setMinSize(64, 64);
        imageContainer.setMaxSize(64, 64);
        imageContainer.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 10px; " +
                               "-fx-border-color: #E0E0E0; -fx-border-radius: 10px;");
        
        // Try to get product image
        Image productImage = null;
        if (item.getProduct() != null) {
            productImage = item.getProduct().getDisplayImage();
        }
        
        if (productImage != null) {
            // Display actual product image
            ImageView imageView = new ImageView(productImage);
            imageView.setFitWidth(56);
            imageView.setFitHeight(56);
            imageView.setPreserveRatio(true);
            imageView.setSmooth(true);
            
            // Apply rounded corners using clip
            Rectangle clip = new Rectangle(56, 56);
            clip.setArcWidth(16);
            clip.setArcHeight(16);
            imageView.setClip(clip);
            
            imageContainer.getChildren().add(imageView);
        } else {
            // Fallback: show emoji based on product type
            String icon = "vegetable".equalsIgnoreCase(item.getProductType()) ? "🥬" : "🍎";
            Label iconLabel = new Label(icon);
            iconLabel.setStyle("-fx-font-size: 32px;");
            imageContainer.getChildren().add(iconLabel);
        }
        
        return imageContainer;
    }
    
    /**
     * Handles item selection.
     */
    private void selectItem(CartItem item, HBox card) {
        // Deselect previous
        if (selectedCard != null) {
            selectedCard.setStyle("-fx-background-color: #FAFAFA; -fx-background-radius: 12px; " +
                                 "-fx-border-color: #E0E0E0; -fx-border-radius: 12px; -fx-cursor: hand;");
        }
        
        // Select new
        selectedItem = item;
        selectedCard = card;
        card.setStyle("-fx-background-color: #E8F5E9; -fx-background-radius: 12px; " +
                     "-fx-border-color: #4CAF50; -fx-border-width: 2px; -fx-border-radius: 12px; -fx-cursor: hand;");
    }

    /**
     * Sets up delivery date and time options.
     */
    private void setupDeliveryOptions() {
        // Set minimum date to tomorrow
        // Set maximum date to 2 days from now
        
        deliveryDatePicker.setValue(LocalDate.now().plusDays(1));
        
        // Add time slots to MaterialFX combo box
        ObservableList<String> timeSlots = FXCollections.observableArrayList(
                "09:00", "10:00", "11:00", "12:00", "13:00", "14:00",
                "15:00", "16:00", "17:00", "18:00", "19:00", "20:00"
        );
        deliveryTimeCombo.setItems(timeSlots);
        deliveryTimeCombo.selectFirst();
    }

    /**
     * Applies loyalty discount to cart.
     */
    private void applyLoyaltyDiscount() {
        double discount = loyaltyService.calculateCurrentUserDiscount();
        if (discount > 0) {
            cartService.applyLoyaltyDiscount(discount);
            loyaltyDiscountLabel.setText(String.format("🎉 Loyalty Discount: %.0f%%", discount));
            loyaltyDiscountLabel.setStyle("-fx-text-fill: #4CAF50; -fx-font-weight: bold;");
        } else {
            loyaltyDiscountLabel.setText("");
        }
    }

    /**
     * Refreshes cart display.
     */
    private void refreshCart() {
        buildCartItemsList();
        updateTotals();
    }

    /**
     * Updates totals display.
     */
    private void updateTotals() {
        subtotalLabel.setText(String.format("%.2f TL", cartService.getSubtotal()));
        discountLabel.setText(String.format("-%.2f TL", cartService.getDiscountAmount()));
        vatLabel.setText(String.format("%.2f TL", cartService.getVatAmount()));
        totalLabel.setText(String.format("%.2f TL", cartService.getTotal()));
        
        // Check minimum value
        if (!cartService.meetsMinimumValue() && !cartService.isCartEmpty()) {
            minValueLabel.setText(String.format("⚠️ Minimum order: %.2f TL", cartService.getMinimumCartValue()));
            minValueLabel.setStyle("-fx-text-fill: #FF9800;");
        } else {
            minValueLabel.setText("");
        }
    }

    /**
     * Handles removing item from cart.
     * 
     * @param event Action event
     */
    @FXML
    private void handleRemoveItem(ActionEvent event) {
        if (selectedItem == null) {
            showWarning("Please select an item to remove");
            return;
        }
        
        cartService.removeFromCart(selectedItem.getProductId());
        refreshCart();
        
        // Re-validate coupon if order value changed below minimum
        if (appliedCoupon != null && !appliedCoupon.canApplyTo(cartService.getSubtotal())) {
            removeCouponWithMessage("Coupon removed - order no longer meets minimum value");
        }
    }

    /**
     * Handles updating item quantity.
     */
    @FXML
    private void handleUpdateQuantity() {
        if (selectedItem == null) {
            showWarning("Please select an item to update");
            return;
        }
        
        TextInputDialog dialog = new TextInputDialog(String.valueOf(selectedItem.getQuantity()));
        dialog.setTitle("Update Quantity");
        dialog.setHeaderText("Enter new quantity (kg) for " + selectedItem.getProductName() + ":");
        dialog.showAndWait().ifPresent(input -> {
            try {
                double quantity = cartService.parseQuantity(input);
                
                // Check if quantity exceeds threshold (triggers price doubling)
                Product product = productService.getProductById(selectedItem.getProductId());
                if (product != null && product.isLargeOrder(quantity)) {
                    showSuccess("⚠️ Threshold Exceeded!\n\n" +
                            "You have exceeded the threshold (" + product.getThreshold() + " kg) for " + product.getName() + ".\n" +
                            "Because you achieved the threshold, the prices are doubled for this product.");
                }
                
                cartService.updateQuantity(selectedItem.getProductId(), quantity);
                refreshCart();
                
                // Re-validate coupon if order value changed
                if (appliedCoupon != null && !appliedCoupon.canApplyTo(cartService.getSubtotal())) {
                    removeCouponWithMessage("Coupon removed - order no longer meets minimum value");
                }
            } catch (IllegalArgumentException e) {
                showError(e.getMessage());
            }
        });
    }

    /**
     * Handles applying coupon code.
     * Only one coupon can be used per order.
     * 
     * @param event Action event
     */
    @FXML
    private void handleApplyCoupon(ActionEvent event) {
        String code = couponField.getText().trim();
        if (code.isEmpty()) {
            couponStatusLabel.setText("Enter a coupon code");
            couponStatusLabel.setStyle("-fx-text-fill: #757575;");
            return;
        }
        
        // Check if a coupon is already applied - only one coupon allowed
        if (appliedCoupon != null) {
            couponStatusLabel.setText("✗ Only one coupon can be used per order. Remove current coupon first.");
            couponStatusLabel.setStyle("-fx-text-fill: #FF9800;");
            return;
        }
        
        // Validate coupon
        Coupon coupon = couponService.validateCoupon(code);
        
        if (coupon == null) {
            couponStatusLabel.setText("✗ Invalid or expired coupon");
            couponStatusLabel.setStyle("-fx-text-fill: #F44336;");
            return;
        }
        
        // Check minimum order value
        if (!coupon.canApplyTo(cartService.getSubtotal())) {
            couponStatusLabel.setText(String.format("✗ Minimum order: %.2f TL", 
                    coupon.getMinimumOrderValue()));
            couponStatusLabel.setStyle("-fx-text-fill: #F44336;");
            return;
        }
        
        // Apply coupon
        appliedCoupon = coupon;
        cartService.applyCoupon(code, coupon.getDiscountPercentage());
        
        // Disable the coupon field to prevent adding more
        couponField.setDisable(true);
        
        // Show success message
        String discountInfo = coupon.getDiscountDescription();
        couponStatusLabel.setText("✓ Coupon applied: " + discountInfo);
        couponStatusLabel.setStyle("-fx-text-fill: #4CAF50;");
        
        // Show expiration warning if expiring soon
        if (coupon.isExpiringSoon(3)) {
            couponStatusLabel.setText(couponStatusLabel.getText() + 
                    " ⚠️ " + coupon.getExpirationInfo());
        }
        
        updateTotals();
    }

    /**
     * Handles removing applied coupon.
     */
    @FXML
    private void handleRemoveCoupon(ActionEvent event) {
        removeCouponWithMessage("Coupon removed");
    }

    /**
     * Removes coupon and shows message.
     * Re-enables the coupon field for new coupon entry.
     */
    private void removeCouponWithMessage(String message) {
        appliedCoupon = null;
        cartService.removeCoupon();
        couponField.clear();
        couponField.setDisable(false);  // Re-enable for new coupon
        couponStatusLabel.setText(message);
        couponStatusLabel.setStyle("-fx-text-fill: #757575;");
        updateTotals();
    }

    /**
     * Handles checkout/place order.
     * Includes re-validation of coupon and stock.
     * 
     * @param event Action event
     */
    @FXML
    private void handleCheckout(ActionEvent event) {
        // Validation 1: Cart not empty
        if (cartService.isCartEmpty()) {
            showError("Your cart is empty");
            return;
        }
        
        // Validation 2: Minimum value
        if (!cartService.meetsMinimumValue()) {
            showError(String.format("Minimum order value is %.2f TL", cartService.getMinimumCartValue()));
            return;
        }
        
        // Validation 3: Stock availability (pre-check)
        if (!cartService.validateStock()) {
            String issues = cartService.getStockIssuesSummary();
            showError("Some items are no longer available:\n\n" + issues + 
                     "\nPlease update your cart.");
            refreshCart();
            return;
        }
        
        // Validation 4: Re-validate coupon at checkout time
        String couponCode = cartService.getCurrentCart().getAppliedCouponCode();
        if (couponCode != null) {
            Coupon freshCoupon = couponService.validateCoupon(couponCode);
            
            if (freshCoupon == null) {
                // Coupon expired or became invalid
                removeCouponWithMessage("Your coupon has expired or is no longer valid");
                showWarning("Your coupon '" + couponCode + "' has expired.\n" +
                           "Please review your new order total and try again.");
                return;
            }
            
            if (!freshCoupon.canApplyTo(cartService.getSubtotal())) {
                // Coupon no longer meets minimum
                removeCouponWithMessage("Coupon minimum not met");
                showWarning("Your coupon requires a minimum order of " + 
                           String.format("%.2f TL", freshCoupon.getMinimumOrderValue()) + 
                           ".\nPlease review your order.");
                return;
            }
        }
        
        // Validation 5: Delivery time
        LocalDateTime deliveryTime = getSelectedDeliveryTime();
        if (deliveryTime == null) {
            showError("Please select a valid delivery date and time");
            return;
        }
        
        if (!orderService.isValidDeliveryTime(deliveryTime)) {
            showError("Please select a delivery time between 1 and 48 hours from now");
            return;
        }
        
        // Show confirmation dialog
        if (!showOrderConfirmation()) {
            return;
        }
        
        // Attempt to create order
        try {
            Order order = orderService.createOrder(cartService.getCurrentCart(), deliveryTime);
            
            if (order != null) {
                // Order successful - coupon is already marked as used by DAO
                
                // Generate and store invoice
                User customer = SessionManager.getInstance().getCurrentUser();
                try {
                    byte[] invoice = invoiceService.generateInvoice(order, customer);
                    if (invoice != null) {
                        invoiceService.saveInvoice(order.getId(), invoice);
                    }
                } catch (Exception e) {
                    // Invoice generation failure shouldn't stop the order
                    System.err.println("Invoice generation failed: " + e.getMessage());
                }
                
                // Add loyalty points
                try {
                    loyaltyService.addPointsToUser(customer.getId(), 
                            loyaltyService.calculatePointsEarned(order.getTotalCost()));
                } catch (Exception e) {
                    // Loyalty points failure shouldn't stop the order
                    System.err.println("Loyalty points update failed: " + e.getMessage());
                }
                
                // Cart is already cleared by OrderService
                
                // Show success with order details
                showSuccess("🎉 Order placed successfully!\n\n" +
                        "Order #" + order.getId() + "\n" +
                        "Total: " + String.format("%.2f TL", order.getTotalCost()) + "\n" +
                        "Delivery: " + deliveryTime.toLocalDate() + " at " + deliveryTime.toLocalTime() + "\n\n" +
                        "Thank you for your order!");
                
                // Close cart window
                ((Stage) cartItemsContainer.getScene().getWindow()).close();
                
            } else {
                showError("Could not create order. Please try again.");
            }
            
        } catch (IllegalArgumentException e) {
            // Handle specific errors from OrderService
            String message = e.getMessage();
            
            if (message.contains("stock") || message.contains("Stock")) {
                // Stock issue - refresh cart and show error
                refreshCart();
                showError("Order failed: " + message + "\n\nYour cart has been updated.");
            } else if (message.contains("coupon") || message.contains("Coupon")) {
                // Coupon issue
                removeCouponWithMessage("Coupon is no longer valid");
                showWarning(message + "\n\nPlease review your order total.");
            } else {
                showError("Order failed: " + message);
            }
        } catch (Exception e) {
            showError("An unexpected error occurred: " + e.getMessage());
        }
    }

    /**
     * Gets selected delivery date and time.
     * 
     * @return LocalDateTime of delivery or null if invalid
     */
    private LocalDateTime getSelectedDeliveryTime() {
        try {
            LocalDate date = deliveryDatePicker.getValue();
            String timeStr = deliveryTimeCombo.getSelectedItem();
            
            if (date == null || timeStr == null) {
                return null;
            }
            
            LocalTime time = LocalTime.parse(timeStr);
            return LocalDateTime.of(date, time);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Shows order confirmation dialog.
     * 
     * @return true if user confirms
     */
    private boolean showOrderConfirmation() {
        // Build summary
        StringBuilder summary = new StringBuilder();
        summary.append("Order Summary:\n\n");
        
        for (CartItem item : cartService.getCartItems()) {
            summary.append(String.format("• %s - %.2f kg - %.2f TL\n",
                    item.getProductName(), item.getQuantity(), item.getTotalPrice()));
        }
        
        summary.append(String.format("\nSubtotal: %.2f TL", cartService.getSubtotal()));
        
        if (cartService.getDiscountAmount() > 0) {
            summary.append(String.format("\nDiscount: -%.2f TL", cartService.getDiscountAmount()));
        }
        
        summary.append(String.format("\nVAT (18%%): %.2f TL", cartService.getVatAmount()));
        summary.append(String.format("\n\n💰 Total: %.2f TL", cartService.getTotal()));
        summary.append(String.format("\n\nDelivery: %s", getSelectedDeliveryTime()));
        
        // Show coupon info if applied
        String couponCode = cartService.getCurrentCart().getAppliedCouponCode();
        if (couponCode != null) {
            summary.append("\n🎟️ Coupon: " + couponCode);
        }
        
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Order");
        alert.setHeaderText("Please confirm your order");
        alert.setContentText(summary.toString());
        
        return alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }

    /**
     * Handles clear cart.
     * 
     * @param event Action event
     */
    @FXML
    private void handleClearCart(ActionEvent event) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, 
                "Clear all items from cart?\n\nThis will also remove any applied coupons.");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            cartService.clearCart();
            appliedCoupon = null;
            couponField.clear();
            couponStatusLabel.setText("");
            refreshCart();
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
     * Shows warning alert.
     * 
     * @param message Warning message
     */
    private void showWarning(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Warning");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Shows success alert.
     * 
     * @param message Success message
     */
    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
