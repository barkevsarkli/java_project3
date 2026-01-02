package com.greengrocer.controllers;

import com.greengrocer.app.MainApplication;
import com.greengrocer.app.SessionManager;
import com.greengrocer.app.ThemeManager;
import com.greengrocer.models.CarrierRating;
import com.greengrocer.models.Message;
import com.greengrocer.models.Order;
import com.greengrocer.models.User;
import com.greengrocer.services.CarrierService;
import com.greengrocer.services.MessageService;
import com.greengrocer.services.OrderService;

import io.github.palexdev.materialfx.controls.*;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controller for the carrier main view using MaterialFX components.
 * Handles order selection, delivery management, and completion.
 *
 * @author Ovsanna Artan
 * @version 1.0.0
 */
public class CarrierMainController implements Initializable {

    @FXML
    private Label carrierNameLabel;

    @FXML
    private VBox availableOrdersContainer;

    @FXML
    private VBox currentOrdersContainer;

    @FXML
    private VBox completedOrdersContainer;

    @FXML
    private MFXButton expandCompletedBtn;
    
    @FXML
    private Label unreadBadgeLabel;

    private OrderService orderService;
    private CarrierService carrierService;
    private MessageService messageService;
    private User currentCarrier;
    
    // Selection tracking
    private Order selectedAvailableOrder = null;
    private Order selectedCurrentOrder = null;
    private Order selectedCompletedOrder = null;
    private HBox selectedAvailableCard = null;
    private HBox selectedCurrentCard = null;
    private HBox selectedCompletedCard = null;

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        orderService = new OrderService();
        carrierService = new CarrierService();
        messageService = new MessageService();
        currentCarrier = SessionManager.getInstance().getCurrentUser();

        carrierNameLabel.setText(currentCarrier.getUsername());

        loadOrders();
        updateUnreadBadge();
    }
    
    /**
     * Updates the unread message badge.
     */
    private void updateUnreadBadge() {
        int unreadCount = messageService.getUnreadCount();
        if (unreadBadgeLabel != null) {
            if (unreadCount > 0) {
                unreadBadgeLabel.setText(String.valueOf(unreadCount));
                unreadBadgeLabel.setVisible(true);
                unreadBadgeLabel.setManaged(true);
            } else {
                unreadBadgeLabel.setVisible(false);
                unreadBadgeLabel.setManaged(false);
            }
        }
    }

    /**
     * Loads all orders into styled lists.
     */
    private void loadOrders() {
        loadAvailableOrders();
        loadCurrentOrders();
        loadCompletedOrders();
    }
    
    private void loadAvailableOrders() {
        availableOrdersContainer.getChildren().clear();
        selectedAvailableOrder = null;
        selectedAvailableCard = null;
        
        List<Order> available = orderService.getAvailableOrdersForCarrier();
        if (available == null || available.isEmpty()) {
            Label emptyLabel = new Label("No available orders");
            emptyLabel.setStyle("-fx-text-fill: #757575; -fx-padding: 16;");
            availableOrdersContainer.getChildren().add(emptyLabel);
            return;
        }
        
        for (Order order : available) {
            HBox card = createOrderCard(order, "available");
            availableOrdersContainer.getChildren().add(card);
        }
    }
    
    private void loadCurrentOrders() {
        currentOrdersContainer.getChildren().clear();
        selectedCurrentOrder = null;
        selectedCurrentCard = null;
        
        List<Order> current = orderService.getCurrentOrdersForCarrier(currentCarrier.getId());
        if (current == null || current.isEmpty()) {
            Label emptyLabel = new Label("No current deliveries");
            emptyLabel.setStyle("-fx-text-fill: #757575; -fx-padding: 16;");
            currentOrdersContainer.getChildren().add(emptyLabel);
            return;
        }
        
        for (Order order : current) {
            HBox card = createOrderCard(order, "current");
            currentOrdersContainer.getChildren().add(card);
        }
    }
    
    private void loadCompletedOrders() {
        completedOrdersContainer.getChildren().clear();
        selectedCompletedOrder = null;
        selectedCompletedCard = null;
        
        List<Order> completed = orderService.getCompletedOrdersForCarrier(currentCarrier.getId());
        if (completed == null || completed.isEmpty()) {
            Label emptyLabel = new Label("No completed deliveries");
            emptyLabel.setStyle("-fx-text-fill: #757575; -fx-padding: 16;");
            completedOrdersContainer.getChildren().add(emptyLabel);
            return;
        }
        
        for (Order order : completed) {
            HBox card = createOrderCard(order, "completed");
            completedOrdersContainer.getChildren().add(card);
        }
    }
    
    private HBox createOrderCard(Order order, String type) {
        HBox card = new HBox(12);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(12, 16, 12, 16));
        card.setStyle("-fx-background-color: #FAFAFA; -fx-background-radius: 8; -fx-border-color: #E0E0E0; -fx-border-radius: 8; -fx-cursor: hand;");
        
        // Order ID badge
        Label idLabel = new Label("#" + order.getId());
        idLabel.setMinWidth(50);
        idLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1976D2;");
        
        // Order info
        VBox infoBox = new VBox(2);
        HBox.setHgrow(infoBox, Priority.ALWAYS);
        
        Label customerLabel = new Label("👤 " + (order.getCustomerName() != null ? order.getCustomerName() : "Customer #" + order.getCustomerId()));
        customerLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;");
        
        String deliveryTime = order.getRequestedDeliveryTime() != null 
                ? "📅 " + order.getRequestedDeliveryTime().format(DATE_FORMATTER)
                : "📅 Not scheduled";
        Label deliveryLabel = new Label(deliveryTime);
        deliveryLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #757575;");
        
        infoBox.getChildren().addAll(customerLabel, deliveryLabel);
        
        // Status/Type indicator
        String statusColor;
        String statusText;
        switch (type) {
            case "available":
                statusColor = "#FF9800";
                statusText = "AVAILABLE";
                break;
            case "current":
                statusColor = "#2196F3";
                statusText = "IN DELIVERY";
                break;
            case "completed":
                statusColor = "#4CAF50";
                statusText = "DELIVERED";
                break;
            default:
                statusColor = "#757575";
                statusText = "UNKNOWN";
        }
        
        Label statusLabel = new Label(statusText);
        statusLabel.setPadding(new Insets(4, 8, 4, 8));
        statusLabel.setStyle("-fx-background-color: " + statusColor + "; -fx-background-radius: 4; -fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold;");
        
        // Total
        Label totalLabel = new Label(String.format("₺%.2f", order.getTotalCost()));
        totalLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2E7D32;");
        
        card.getChildren().addAll(idLabel, infoBox, statusLabel, totalLabel);
        
        // Selection handling based on type
        card.setOnMouseClicked(e -> selectOrder(order, card, type));
        card.setOnMouseEntered(e -> {
            boolean isSelected = (type.equals("available") && selectedAvailableOrder == order) ||
                                (type.equals("current") && selectedCurrentOrder == order) ||
                                (type.equals("completed") && selectedCompletedOrder == order);
            if (!isSelected) {
                card.setStyle("-fx-background-color: #F5F5F5; -fx-background-radius: 8; -fx-border-color: #BDBDBD; -fx-border-radius: 8; -fx-cursor: hand;");
            }
        });
        card.setOnMouseExited(e -> {
            boolean isSelected = (type.equals("available") && selectedAvailableOrder == order) ||
                                (type.equals("current") && selectedCurrentOrder == order) ||
                                (type.equals("completed") && selectedCompletedOrder == order);
            if (!isSelected) {
                card.setStyle("-fx-background-color: #FAFAFA; -fx-background-radius: 8; -fx-border-color: #E0E0E0; -fx-border-radius: 8; -fx-cursor: hand;");
            }
        });
        
        return card;
    }
    
    private void selectOrder(Order order, HBox card, String type) {
        // Clear previous selection for this type
        switch (type) {
            case "available":
                if (selectedAvailableCard != null) {
                    selectedAvailableCard.setStyle("-fx-background-color: #FAFAFA; -fx-background-radius: 8; -fx-border-color: #E0E0E0; -fx-border-radius: 8; -fx-cursor: hand;");
                }
                selectedAvailableOrder = order;
                selectedAvailableCard = card;
                break;
            case "current":
                if (selectedCurrentCard != null) {
                    selectedCurrentCard.setStyle("-fx-background-color: #FAFAFA; -fx-background-radius: 8; -fx-border-color: #E0E0E0; -fx-border-radius: 8; -fx-cursor: hand;");
                }
                selectedCurrentOrder = order;
                selectedCurrentCard = card;
                break;
            case "completed":
                if (selectedCompletedCard != null) {
                    selectedCompletedCard.setStyle("-fx-background-color: #FAFAFA; -fx-background-radius: 8; -fx-border-color: #E0E0E0; -fx-border-radius: 8; -fx-cursor: hand;");
                }
                selectedCompletedOrder = order;
                selectedCompletedCard = card;
                break;
        }
        card.setStyle("-fx-background-color: #E3F2FD; -fx-background-radius: 8; -fx-border-color: #1976D2; -fx-border-width: 2; -fx-border-radius: 8; -fx-cursor: hand;");
    }

    @FXML
    private void handleSelectOrder(ActionEvent event) {
        if (selectedAvailableOrder == null) {
            showError("Please select an order to pick up");
            return;
        }

        try {
            boolean success = orderService.assignOrderToCarrier(selectedAvailableOrder.getId());
            if (success) {
                loadOrders();
            } else {
                showError("Order could not be assigned");
            }
        } catch (IllegalStateException e) {
            showError(e.getMessage());
        }
    }

    @FXML
    private void handleCompleteOrder(ActionEvent event) {
        if (selectedCurrentOrder == null) {
            showError("Please select an order to complete");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Complete Delivery");
        confirm.setHeaderText("Confirm Delivery");
        confirm.setContentText(
                "Order #" + selectedCurrentOrder.getId() +
                "\nCustomer: " + selectedCurrentOrder.getCustomerName() +
                "\nTotal: " + String.format("%.2f TL", selectedCurrentOrder.getTotalCost()) +
                "\n\nMark this order as delivered?"
        );

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            boolean success =
                    orderService.markOrderDelivered(selectedCurrentOrder.getId(), LocalDateTime.now());

            if (success) {
                loadOrders();
            } else {
                showError("Could not complete order");
            }
        }
    }

    @FXML
    private void handleViewDetails(ActionEvent event) {
        Order selected = selectedAvailableOrder;
        
        if (selected == null) {
            selected = selectedCurrentOrder;
        }
        if (selected == null) {
            selected = selectedCompletedOrder;
        }

        if (selected != null) {
            showOrderDetails(selected);
        } else {
            showError("Please select an order to view details");
        }
    }

    private void showOrderDetails(Order order) {

        StringBuilder details = new StringBuilder();
        details.append("Order #").append(order.getId()).append("\n\n");
        details.append("Customer: ").append(order.getCustomerName()).append("\n");
        details.append("Address: ").append(order.getCustomerAddress()).append("\n\n");
        details.append("Products:\n").append(order.getProductsSummary()).append("\n\n");
        details.append("Total: ").append(String.format("%.2f TL", order.getTotalCost())).append("\n");
        details.append("Requested Delivery: ")
                .append(order.getRequestedDeliveryTime().format(DATE_FORMATTER));

        if (order.getActualDeliveryTime() != null) {
            details.append("\nDelivered: ")
                    .append(order.getActualDeliveryTime().format(DATE_FORMATTER));
        }

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Order Details");
        alert.setHeaderText("Order #" + order.getId());
        alert.setContentText(details.toString());
        alert.showAndWait();
    }

    @FXML
    private void handleRefresh(ActionEvent event) {
        loadOrders();
        updateUnreadBadge();
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        SessionManager.getInstance().logout();
        try {
            MainApplication.switchScene(
                    "/fxml/LoginView.fxml",
                    "Group03 - GreenGrocer");
        } catch (Exception e) {
            showError("Could not logout");
        }
    }
    
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Info");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Shows the carrier profile popup with stats and ratings.
     */
    @FXML
    private void handleShowProfile(ActionEvent event) {
        Stage profileStage = new Stage();
        profileStage.initModality(Modality.APPLICATION_MODAL);
        profileStage.setTitle("My Profile - " + currentCarrier.getUsername());
        
        VBox root = new VBox(20);
        root.setPadding(new Insets(24));
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #e8f5e9, #f5f5f5);");
        
        // Header with user info
        VBox headerBox = new VBox(10);
        headerBox.setAlignment(Pos.CENTER);
        headerBox.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-padding: 20;");
        
        Label avatarLabel = new Label("🚚");
        avatarLabel.setStyle("-fx-font-size: 48px;");
        
        Label usernameLabel = new Label(currentCarrier.getUsername());
        usernameLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #2E7D32;");
        
        Label roleLabel = new Label("Delivery Carrier");
        roleLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #666;");
        
        Label emailLabel = new Label("📧 " + (currentCarrier.getEmail() != null ? currentCarrier.getEmail() : "No email set"));
        emailLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #444;");
        
        headerBox.getChildren().addAll(avatarLabel, usernameLabel, roleLabel, emailLabel);
        
        // Stats section
        VBox statsBox = new VBox(12);
        statsBox.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-padding: 16;");
        
        Label statsTitle = new Label("📊 Performance Statistics");
        statsTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1976D2;");
        
        // Get carrier statistics
        double avgRating = carrierService.getAverageRating(currentCarrier.getId());
        List<CarrierRating> ratings = carrierService.getCarrierRatings(currentCarrier.getId());
        int totalRatings = ratings.size();
        int completedDeliveries = orderService.getCompletedOrdersForCarrier(currentCarrier.getId()).size();
        
        // Generate star display
        StringBuilder stars = new StringBuilder();
        int fullStars = (int) avgRating;
        for (int i = 0; i < 5; i++) {
            stars.append(i < fullStars ? "★" : "☆");
        }
        
        HBox ratingRow = createStatRow("⭐ Average Rating", 
            String.format("%s %.1f/5", stars.toString(), avgRating));
        HBox totalRatingsRow = createStatRow("📝 Total Ratings", String.valueOf(totalRatings));
        HBox deliveriesRow = createStatRow("Completed Deliveries", String.valueOf(completedDeliveries));
        
        statsBox.getChildren().addAll(statsTitle, ratingRow, totalRatingsRow, deliveriesRow);
        
        // Recent ratings section
        VBox ratingsBox = new VBox(10);
        ratingsBox.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-padding: 16;");
        
        Label ratingsTitle = new Label("💬 Recent Ratings");
        ratingsTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #FF9800;");
        
        VBox ratingsListBox = new VBox(8);
        
        if (ratings.isEmpty()) {
            Label noRatings = new Label("No ratings yet. Keep delivering! 🚀");
            noRatings.setStyle("-fx-text-fill: #999; -fx-font-style: italic;");
            ratingsListBox.getChildren().add(noRatings);
        } else {
            // Show last 5 ratings
            int count = 0;
            for (CarrierRating rating : ratings) {
                if (count >= 5) break;
                HBox ratingCard = createRatingCard(rating);
                ratingsListBox.getChildren().add(ratingCard);
                count++;
            }
        }
        
        ScrollPane ratingsScroll = new ScrollPane(ratingsListBox);
        ratingsScroll.setFitToWidth(true);
        ratingsScroll.setPrefHeight(180);
        ratingsScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        
        ratingsBox.getChildren().addAll(ratingsTitle, ratingsScroll);
        
        // Close button
        MFXButton closeBtn = new MFXButton("Close");
        closeBtn.setStyle("-fx-background-color: #2E7D32; -fx-text-fill: white; -fx-font-size: 14px;");
        closeBtn.setPrefWidth(120);
        closeBtn.setOnAction(e -> profileStage.close());
        
        HBox buttonBox = new HBox(closeBtn);
        buttonBox.setAlignment(Pos.CENTER);
        
        root.getChildren().addAll(headerBox, statsBox, ratingsBox, buttonBox);
        
        Scene scene = new Scene(root, 420, 580);
        profileStage.setScene(scene);
        profileStage.setResizable(false);
        profileStage.showAndWait();
    }
    
    /**
     * Creates a stat row for the profile.
     */
    private HBox createStatRow(String label, String value) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        
        Label labelText = new Label(label);
        labelText.setStyle("-fx-font-size: 14px; -fx-text-fill: #333;");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Label valueText = new Label(value);
        valueText.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2E7D32;");
        
        row.getChildren().addAll(labelText, spacer, valueText);
        return row;
    }
    
    /**
     * Creates a rating card for display.
     */
    private HBox createRatingCard(CarrierRating rating) {
        HBox card = new HBox(12);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(10));
        card.setStyle("-fx-background-color: #f5f5f5; -fx-background-radius: 8;");
        
        // Stars
        StringBuilder stars = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            stars.append(i < rating.getRating() ? "★" : "☆");
        }
        Label starsLabel = new Label(stars.toString());
        starsLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #FF9800;");
        
        VBox textBox = new VBox(4);
        HBox.setHgrow(textBox, Priority.ALWAYS);
        
        Label orderLabel = new Label("Order #" + rating.getOrderId());
        orderLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");
        
        String comment = rating.getComment();
        if (comment != null && !comment.trim().isEmpty()) {
            Label commentLabel = new Label("\"" + comment + "\"");
            commentLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #333; -fx-font-style: italic;");
            commentLabel.setWrapText(true);
            textBox.getChildren().addAll(orderLabel, commentLabel);
        } else {
            textBox.getChildren().add(orderLabel);
        }
        
        card.getChildren().addAll(starsLabel, textBox);
        return card;
    }
    
    /**
     * Shows the messages popup for the carrier.
     */
    @FXML
    private void handleShowMessages(ActionEvent event) {
        Stage messagesStage = new Stage();
        messagesStage.initModality(Modality.APPLICATION_MODAL);
        messagesStage.setTitle("Group03 - GreenGrocer");
        
        VBox root = new VBox(16);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #e3f2fd, #f5f5f5);");
        
        // Header
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        Label titleLabel = new Label("💬 My Messages");
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #1976D2;");
        
        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);
        
        MFXButton refreshBtn = new MFXButton("🔄 Refresh");
        refreshBtn.setStyle("-fx-background-color: #1976D2; -fx-text-fill: white;");
        
        header.getChildren().addAll(titleLabel, headerSpacer, refreshBtn);
        
        // Messages container
        VBox messagesBox = new VBox(10);
        messagesBox.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-padding: 16;");
        
        ScrollPane messagesScroll = new ScrollPane(messagesBox);
        messagesScroll.setFitToWidth(true);
        messagesScroll.setPrefHeight(350);
        messagesScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        VBox.setVgrow(messagesScroll, Priority.ALWAYS);
        
        // Load messages
        Runnable loadMessagesAction = () -> {
            messagesBox.getChildren().clear();
            List<Message> received = messageService.getReceivedMessages();
            List<Message> sent = messageService.getSentMessages();
            
            // Combine and sort by date
            java.util.ArrayList<Message> allMessages = new java.util.ArrayList<>(received);
            allMessages.addAll(sent);
            allMessages.sort((a, b) -> b.getSentAt().compareTo(a.getSentAt()));
            
            if (allMessages.isEmpty()) {
                Label emptyLabel = new Label("📭 No messages yet\nCustomers will contact you about their deliveries here.");
                emptyLabel.setStyle("-fx-text-fill: #9ca3af; -fx-font-size: 14px; -fx-padding: 30; -fx-text-alignment: center;");
                emptyLabel.setWrapText(true);
                messagesBox.getChildren().add(emptyLabel);
            } else {
                for (Message msg : allMessages) {
                    VBox msgCard = createMessageCard(msg, messagesStage);
                    messagesBox.getChildren().add(msgCard);
                }
            }
            updateUnreadBadge();
        };
        
        loadMessagesAction.run();
        refreshBtn.setOnAction(e -> loadMessagesAction.run());
        
        // Close button
        MFXButton closeBtn = new MFXButton("Close");
        closeBtn.setStyle("-fx-background-color: #757575; -fx-text-fill: white; -fx-font-size: 14px;");
        closeBtn.setPrefWidth(100);
        closeBtn.setOnAction(e -> messagesStage.close());
        
        HBox buttonBox = new HBox(closeBtn);
        buttonBox.setAlignment(Pos.CENTER);
        
        root.getChildren().addAll(header, messagesScroll, buttonBox);
        
        Scene scene = new Scene(root, 500, 500);
        messagesStage.setScene(scene);
        messagesStage.setResizable(false);
        messagesStage.showAndWait();
        
        updateUnreadBadge();
    }
    
    /**
     * Creates a message card for the messages popup.
     */
    private VBox createMessageCard(Message message, Stage parentStage) {
        // Check if this message was sent BY the carrier (not received)
        boolean isSentByMe = message.getSenderId() == currentCarrier.getId();
        boolean isReceivedByMe = message.getReceiverId() == currentCarrier.getId();
        boolean isUnread = !message.isRead() && isReceivedByMe;
        
        VBox card = new VBox(8);
        card.setPadding(new Insets(12));
        card.setStyle("-fx-background-color: " + (isUnread ? "#fefce8" : "#f9fafb") + 
                     "; -fx-background-radius: 10; -fx-border-color: " + 
                     (isSentByMe ? "#d1fae5" : "#e5e7eb") + "; -fx-border-radius: 10; -fx-border-width: 1;");
        
        // Header row
        HBox headerRow = new HBox(8);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        
        Label directionLabel = new Label(isSentByMe ? "📤 Sent to Customer" : "📥 From Customer");
        directionLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: " + (isSentByMe ? "#059669" : "#1976D2") + "; -fx-font-weight: bold;");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        String timeStr = message.getSentAt().format(DateTimeFormatter.ofPattern("MMM dd, HH:mm"));
        Label timeLabel = new Label(timeStr);
        timeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #9ca3af;");
        
        if (isUnread) {
            Label unreadDot = new Label("● NEW");
            unreadDot.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 10px; -fx-font-weight: bold;");
            headerRow.getChildren().addAll(directionLabel, unreadDot, spacer, timeLabel);
        } else {
            headerRow.getChildren().addAll(directionLabel, spacer, timeLabel);
        }
        
        // Subject
        Label subjectLabel = new Label(message.getSubject() != null ? message.getSubject() : "No Subject");
        subjectLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1f2937;");
        subjectLabel.setWrapText(true);
        
        // Content preview
        String content = message.getContent();
        String preview = content != null ? 
            (content.length() > 100 ? content.substring(0, 100) + "..." : content) : "";
        Label contentLabel = new Label(preview);
        contentLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #4b5563;");
        contentLabel.setWrapText(true);
        
        card.getChildren().addAll(headerRow, subjectLabel, contentLabel);
        
        // Determine the other user ID for conversation thread
        int otherUserId = isSentByMe ? message.getReceiverId() : message.getSenderId();
        
        // Add click handler to show full conversation thread
        card.setOnMouseClicked(e -> {
            if (isUnread && isReceivedByMe) {
                messageService.markAsRead(message.getId());
                card.setStyle("-fx-background-color: #f9fafb; -fx-background-radius: 10; -fx-border-color: #e5e7eb; -fx-border-radius: 10; -fx-border-width: 1;");
                updateUnreadBadge();
            }
            showConversationThread(message, otherUserId, parentStage);
        });
        
        // Reply button ONLY for messages received from customers (not sent by carrier)
        if (isReceivedByMe && !isSentByMe) {
            MFXButton replyBtn = new MFXButton("↩ Reply to Customer");
            replyBtn.setStyle("-fx-background-color: #1976D2; -fx-text-fill: white; -fx-font-size: 12px;");
            replyBtn.setOnAction(e -> {
                e.consume(); // Prevent card click event
                showReplyDialog(message, parentStage);
            });
            
            HBox btnRow = new HBox(replyBtn);
            btnRow.setAlignment(Pos.CENTER_RIGHT);
            card.getChildren().add(btnRow);
        } else if (isSentByMe) {
            // Show "Sent" indicator for sent messages - no reply button
            Label sentIndicator = new Label("✓ Delivered");
            sentIndicator.setStyle("-fx-font-size: 11px; -fx-text-fill: #10b981;");
            HBox statusRow = new HBox(sentIndicator);
            statusRow.setAlignment(Pos.CENTER_RIGHT);
            card.getChildren().add(statusRow);
        }
        
        return card;
    }
    
    /**
     * Shows the full conversation thread for a message.
     */
    private void showConversationThread(Message message, int otherUserId, Stage parentStage) {
        Stage threadStage = new Stage();
        threadStage.initModality(Modality.APPLICATION_MODAL);
        threadStage.initOwner(parentStage);
        threadStage.setTitle("Group03 - GreenGrocer");
        
        VBox root = new VBox(16);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: white;");
        
        // Header
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        
        String otherUserName = message.getSenderId() == currentCarrier.getId() 
            ? (message.getReceiverName() != null ? message.getReceiverName() : "Customer")
            : (message.getSenderName() != null ? message.getSenderName() : "Customer");
        
        Label titleLabel = new Label("💬 Conversation with " + otherUserName);
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1976D2;");
        
        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);
        
        header.getChildren().addAll(titleLabel, headerSpacer);
        
        // Messages container
        VBox messagesContainer = new VBox(10);
        messagesContainer.setStyle("-fx-background-color: #f9fafb; -fx-background-radius: 8; -fx-padding: 12;");
        
        ScrollPane messagesScroll = new ScrollPane(messagesContainer);
        messagesScroll.setFitToWidth(true);
        messagesScroll.setPrefHeight(400);
        messagesScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        VBox.setVgrow(messagesScroll, Priority.ALWAYS);
        
        // Load conversation thread
        try {
            com.greengrocer.dao.MessageDAO messageDAO = new com.greengrocer.dao.MessageDAO();
            List<Message> thread = messageDAO.findConversationBetween(currentCarrier.getId(), otherUserId);
            
            // Sort chronologically (oldest first)
            thread.sort((m1, m2) -> m1.getSentAt().compareTo(m2.getSentAt()));
            
            if (thread.isEmpty()) {
                Label emptyLabel = new Label("No messages in this conversation");
                emptyLabel.setStyle("-fx-text-fill: #9ca3af; -fx-font-size: 14px; -fx-padding: 20;");
                messagesContainer.getChildren().add(emptyLabel);
            } else {
                for (Message msg : thread) {
                    VBox msgBubble = createThreadMessageBubble(msg);
                    messagesContainer.getChildren().add(msgBubble);
                }
                
                // Scroll to bottom
                Platform.runLater(() -> messagesScroll.setVvalue(1.0));
            }
        } catch (java.sql.SQLException e) {
            System.err.println("Error loading conversation thread: " + e.getMessage());
            Label errorLabel = new Label("Error loading conversation");
            errorLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 14px; -fx-padding: 20;");
            messagesContainer.getChildren().add(errorLabel);
        }
        
        // Close button
        MFXButton closeBtn = new MFXButton("Close");
        closeBtn.setStyle("-fx-background-color: #757575; -fx-text-fill: white; -fx-font-size: 14px;");
        closeBtn.setPrefWidth(100);
        closeBtn.setOnAction(e -> threadStage.close());
        
        HBox buttonBox = new HBox(closeBtn);
        buttonBox.setAlignment(Pos.CENTER);
        
        root.getChildren().addAll(header, messagesScroll, buttonBox);
        
        Scene scene = new Scene(root, 600, 500);
        threadStage.setScene(scene);
        threadStage.setResizable(false);
        threadStage.showAndWait();
    }
    
    /**
     * Creates a message bubble for the conversation thread view.
     */
    private VBox createThreadMessageBubble(Message message) {
        boolean isSentByMe = message.getSenderId() == currentCarrier.getId();
        
        VBox bubble = new VBox(4);
        bubble.setMaxWidth(500);
        bubble.setPadding(new Insets(10, 14, 10, 14));
        
        // Sender info
        HBox senderRow = new HBox(8);
        senderRow.setAlignment(isSentByMe ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        
        String senderName = isSentByMe ? "You" : (message.getSenderName() != null ? message.getSenderName() : "Customer");
        Label nameLabel = new Label(senderName);
        nameLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: " + (isSentByMe ? "#059669" : "#374151") + ";");
        
        String timeStr = message.getSentAt().format(DateTimeFormatter.ofPattern("MMM dd, HH:mm"));
        Label timeLabel = new Label(timeStr);
        timeLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #9ca3af;");
        
        if (isSentByMe) {
            senderRow.getChildren().addAll(timeLabel, nameLabel);
        } else {
            senderRow.getChildren().addAll(nameLabel, timeLabel);
        }
        
        // Content bubble
        VBox contentBox = new VBox(4);
        contentBox.setStyle("-fx-padding: 12 16; -fx-background-radius: 12; " + 
                (isSentByMe 
                    ? "-fx-background-color: #d1fae5; -fx-border-color: #a7f3d0; -fx-border-radius: 12;"
                    : "-fx-background-color: white; -fx-border-color: #e5e7eb; -fx-border-radius: 12;"));
        
        if (message.getSubject() != null && !message.getSubject().isEmpty()) {
            Label subjectLabel = new Label(message.getSubject());
            subjectLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #374151;");
            contentBox.getChildren().add(subjectLabel);
        }
        
        Label contentLabel = new Label(message.getContent());
        contentLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #1f2937;");
        contentLabel.setWrapText(true);
        contentBox.getChildren().add(contentLabel);
        
        bubble.getChildren().addAll(senderRow, contentBox);
        bubble.setAlignment(isSentByMe ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        
        // Wrap in HBox for alignment
        HBox container = new HBox();
        container.setAlignment(isSentByMe ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        container.getChildren().add(bubble);
        
        VBox wrapper = new VBox(container);
        wrapper.setAlignment(isSentByMe ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        
        return wrapper;
    }
    
    /**
     * Shows a reply dialog for a message.
     */
    private void showReplyDialog(Message originalMessage, Stage parentStage) {
        Stage replyStage = new Stage();
        replyStage.initModality(Modality.APPLICATION_MODAL);
        replyStage.initOwner(parentStage);
        replyStage.setTitle("Group03 - GreenGrocer");
        
        VBox root = new VBox(16);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: white;");
        
        // Original message preview
        VBox originalBox = new VBox(4);
        originalBox.setStyle("-fx-background-color: #f3f4f6; -fx-padding: 12; -fx-background-radius: 8;");
        
        Label originalLabel = new Label("Replying to:");
        originalLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #6b7280;");
        
        Label originalSubject = new Label(originalMessage.getSubject());
        originalSubject.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #374151;");
        
        originalBox.getChildren().addAll(originalLabel, originalSubject);
        
        // Reply content
        Label replyLabel = new Label("Your Reply:");
        replyLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1f2937;");
        
        TextArea replyArea = new TextArea();
        replyArea.setPromptText("Type your reply here...");
        replyArea.setPrefRowCount(5);
        replyArea.setWrapText(true);
        replyArea.setStyle("-fx-font-size: 13px;");
        
        // Buttons
        HBox buttonRow = new HBox(10);
        buttonRow.setAlignment(Pos.CENTER_RIGHT);
        
        MFXButton cancelBtn = new MFXButton("Cancel");
        cancelBtn.setStyle("-fx-background-color: #e5e7eb; -fx-text-fill: #374151;");
        cancelBtn.setOnAction(e -> replyStage.close());
        
        MFXButton sendBtn = new MFXButton("📤 Send Reply");
        sendBtn.setStyle("-fx-background-color: #1976D2; -fx-text-fill: white;");
        sendBtn.setOnAction(e -> {
            String content = replyArea.getText().trim();
            if (content.isEmpty()) {
                showError("Please enter a reply message");
                return;
            }
            
            Message reply = messageService.replyToMessage(originalMessage.getId(), content);
            if (reply != null) {
                replyStage.close();
            } else {
                showError("Failed to send reply");
            }
        });
        
        buttonRow.getChildren().addAll(cancelBtn, sendBtn);
        
        root.getChildren().addAll(originalBox, replyLabel, replyArea, buttonRow);
        
        Scene scene = new Scene(root, 400, 300);
        replyStage.setScene(scene);
        replyStage.setResizable(false);
        replyStage.showAndWait();
    }

    /**
     * Toggle between available themes
     * 
     * @param event Action event
     */
    @FXML
    private void handleToggleTheme(ActionEvent event) {
        try {
            ThemeManager.getInstance().toggleTheme(carrierNameLabel.getScene());
        } catch (Exception e) {
            System.err.println("Error toggling theme: " + e.getMessage());
        }
    }
}
