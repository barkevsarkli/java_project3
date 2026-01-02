package com.greengrocer.controllers;

import com.greengrocer.dao.OrderDAO;
import com.greengrocer.dao.UserDAO;
import com.greengrocer.models.*;
import com.greengrocer.services.*;
import io.github.palexdev.materialfx.controls.*;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Controller for the Owner Dashboard Pro view.
 * Features unified inbox, fleet management with smart fire button logic,
 * and data visualizations.
 * 
 * @author Barkev Şarklı
 * @version 1.0
 */
public class OwnerDashboardProController implements Initializable {

    // Header
    @FXML private Label flatRateLabel;
    @FXML private MFXButton settingsBtn;
    
    // Inbox - Message List
    @FXML private Label unreadBadge;
    @FXML private MFXButton filterAllBtn;
    @FXML private MFXButton filterUnreadBtn;
    @FXML private VBox messageListContainer;
    
    // Inbox - Conversation
    @FXML private HBox conversationHeader;
    @FXML private Label conversationTitle;
    @FXML private Label conversationSubtitle;
    @FXML private ScrollPane conversationScrollPane;
    @FXML private VBox conversationContainer;
    @FXML private MFXTextField replyField;
    @FXML private MFXButton sendReplyBtn;
    
    // Fleet Management
    @FXML private Label activeCarriersLabel;
    @FXML private VBox fleetContainer;
    
    // Charts
    @FXML private BarChart<String, Number> produceBarChart;
    @FXML private CategoryAxis produceXAxis;
    @FXML private NumberAxis produceYAxis;
    @FXML private VBox performanceContainer;
    
    // Services
    private MessageService messageService;
    private OrderService orderService;
    private CarrierService carrierService;
    private ProductService productService;
    private UserDAO userDAO;
    private OrderDAO orderDAO;
    
    // State
    private DeliverySettings deliverySettings;
    private Message selectedMessage;
    private String currentFilter = "all";
    
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MMM dd");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialize services
        messageService = new MessageService();
        orderService = new OrderService();
        carrierService = new CarrierService();
        productService = new ProductService();
        userDAO = new UserDAO();
        orderDAO = new OrderDAO();
        
        // Initialize delivery settings
        deliverySettings = new DeliverySettings();
        updateFlatRateDisplay();
        
        // Load all data
        loadMessages();
        loadFleet();
        loadProduceChart();
        loadCarrierPerformance();
    }
    
    // ==================== FLAT RATE SETTINGS ====================
    
    private void updateFlatRateDisplay() {
        if (flatRateLabel != null) {
            flatRateLabel.setText(deliverySettings.getFormattedRate());
        }
    }
    
    @FXML
    private void handleOpenSettings(ActionEvent event) {
        // Create settings dialog
        TextInputDialog dialog = new TextInputDialog(String.valueOf(deliverySettings.getFlatRate()));
        dialog.setTitle("Delivery Settings");
        dialog.setHeaderText("Configure Flat Rate Delivery Fee");
        dialog.setContentText("Enter flat rate (₺):");
        
        dialog.showAndWait().ifPresent(result -> {
            try {
                double newRate = Double.parseDouble(result);
                if (newRate >= 0) {
                    deliverySettings.setFlatRate(newRate);
                    updateFlatRateDisplay();
                }
            } catch (NumberFormatException e) {
                showError("Invalid number format");
            }
        });
    }
    
    // ==================== INBOX - MESSAGE LIST ====================
    
    private void loadMessages() {
        if (messageListContainer == null) return;
        
        messageListContainer.getChildren().clear();
        
        List<Message> messages = messageService.getAllMessagesForOwner();
        
        // Filter messages based on current filter
        if (messages != null) {
            messages = filterMessages(messages);
        }
        
        // Update unread badge
        long unreadCount = messages != null ? 
            messages.stream().filter(m -> !m.isRead()).count() : 0;
        if (unreadBadge != null) {
            unreadBadge.setText(String.valueOf(unreadCount));
            unreadBadge.setVisible(unreadCount > 0);
        }
        
        if (messages == null || messages.isEmpty()) {
            Label emptyLabel = new Label("No messages");
            emptyLabel.setStyle("-fx-text-fill: #9CA3AF; -fx-padding: 20;");
            messageListContainer.getChildren().add(emptyLabel);
            return;
        }
        
        for (Message message : messages) {
            HBox card = createMessageCard(message);
            messageListContainer.getChildren().add(card);
        }
    }
    
    private List<Message> filterMessages(List<Message> messages) {
        if ("unread".equals(currentFilter)) {
            return messages.stream()
                .filter(m -> !m.isRead())
                .collect(Collectors.toList());
        }
        return messages;
    }
    
    private HBox createMessageCard(Message message) {
        HBox card = new HBox(10);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(10, 12, 10, 12));
        
        String bgColor = message.isRead() ? "#FFFFFF" : "#F0FDF4";
        card.setStyle("-fx-background-color: " + bgColor + "; -fx-background-radius: 8; -fx-cursor: hand;");
        
        VBox content = new VBox(4);
        HBox.setHgrow(content, Priority.ALWAYS);
        
        String senderName = message.getSenderName() != null ? message.getSenderName() : "User #" + message.getSenderId();
        Label nameLabel = new Label(senderName);
        nameLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #1F2937;");
        
        String preview = message.getContent();
        if (preview != null && preview.length() > 40) {
            preview = preview.substring(0, 37) + "...";
        }
        Label previewLabel = new Label(preview != null ? preview : "");
        previewLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #6B7280;");
        
        content.getChildren().addAll(nameLabel, previewLabel);
        
        // Time label
        String timeStr = message.getSentAt() != null ? message.getSentAt().format(TIME_FORMAT) : "";
        Label timeLabel = new Label(timeStr);
        timeLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #9CA3AF;");
        
        // Unread dot
        if (!message.isRead()) {
            Label dot = new Label("●");
            dot.setStyle("-fx-text-fill: #1B4332; -fx-font-size: 8px;");
            card.getChildren().addAll(dot, content, timeLabel);
        } else {
            card.getChildren().addAll(content, timeLabel);
        }
        
        // Click handler
        card.setOnMouseClicked(e -> selectMessage(message));
        card.setOnMouseEntered(e -> card.setStyle(card.getStyle().replace(bgColor, "#E5E7EB")));
        card.setOnMouseExited(e -> card.setStyle(card.getStyle().replace("#E5E7EB", bgColor)));
        
        return card;
    }
    
    @FXML
    private void handleFilterAll(ActionEvent event) {
        currentFilter = "all";
        updateFilterButtons();
        loadMessages();
    }
    
    @FXML
    private void handleFilterUnread(ActionEvent event) {
        currentFilter = "unread";
        updateFilterButtons();
        loadMessages();
    }
    
    private void updateFilterButtons() {
        String activeStyle = "-fx-background-color: #1B4332; -fx-text-fill: white; -fx-font-size: 11px; -fx-padding: 4 10;";
        String inactiveStyle = "-fx-background-color: #E5E7EB; -fx-text-fill: #6B7280; -fx-font-size: 11px; -fx-padding: 4 10;";
        
        if (filterAllBtn != null) filterAllBtn.setStyle("all".equals(currentFilter) ? activeStyle : inactiveStyle);
        if (filterUnreadBtn != null) filterUnreadBtn.setStyle("unread".equals(currentFilter) ? activeStyle : inactiveStyle);
    }
    
    // ==================== INBOX - CONVERSATION ====================
    
    private void selectMessage(Message message) {
        selectedMessage = message;
        
        // Mark as read
        if (!message.isRead()) {
            messageService.markAsRead(message.getId());
            message.setRead(true);
            loadMessages(); // Refresh list
        }
        
        // Update conversation header
        String senderName = message.getSenderName() != null ? message.getSenderName() : "User #" + message.getSenderId();
        if (conversationTitle != null) conversationTitle.setText("Conversation with " + senderName);
        if (conversationSubtitle != null) {
            String dateStr = message.getSentAt() != null ? message.getSentAt().format(DATE_FORMAT) : "";
            conversationSubtitle.setText("Started " + dateStr);
        }
        
        // Load conversation thread
        loadConversation(message);
    }
    
    private void loadConversation(Message message) {
        if (conversationContainer == null) return;
        
        conversationContainer.getChildren().clear();
        
        // Get conversation thread using the message ID
        List<Message> thread = messageService.getConversationThread(message.getId());
        
        // Get owner ID (the current user viewing)
        User owner = userDAO.findOwner();
        int ownerId = owner != null ? owner.getId() : 0;
        
        for (Message msg : thread) {
            VBox bubble = createMessageBubble(msg, msg.getSenderId() == ownerId);
            conversationContainer.getChildren().add(bubble);
        }
        
        // Scroll to bottom
        if (conversationScrollPane != null) {
            conversationScrollPane.setVvalue(1.0);
        }
    }
    
    private VBox createMessageBubble(Message message, boolean isSent) {
        VBox bubble = new VBox(4);
        bubble.setMaxWidth(400);
        bubble.setPadding(new Insets(10, 14, 10, 14));
        
        if (isSent) {
            bubble.setAlignment(Pos.CENTER_RIGHT);
            bubble.setStyle("-fx-background-color: #1B4332; -fx-background-radius: 12 12 0 12;");
        } else {
            bubble.setAlignment(Pos.CENTER_LEFT);
            bubble.setStyle("-fx-background-color: #E5E7EB; -fx-background-radius: 12 12 12 0;");
        }
        
        Label content = new Label(message.getContent());
        content.setWrapText(true);
        content.setStyle("-fx-font-size: 13px; -fx-text-fill: " + (isSent ? "white" : "#1F2937") + ";");
        
        String timeStr = message.getSentAt() != null ? message.getSentAt().format(TIME_FORMAT) : "";
        Label time = new Label(timeStr);
        time.setStyle("-fx-font-size: 10px; -fx-text-fill: " + (isSent ? "#A7C4BC" : "#9CA3AF") + ";");
        
        bubble.getChildren().addAll(content, time);
        
        // Wrap in HBox for alignment
        HBox wrapper = new HBox(bubble);
        wrapper.setAlignment(isSent ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        
        VBox container = new VBox(wrapper);
        return container;
    }
    
    @FXML
    private void handleSendReply(ActionEvent event) {
        if (selectedMessage == null || replyField == null) return;
        
        String content = replyField.getText().trim();
        if (content.isEmpty()) return;
        
        Message reply = messageService.replyToMessage(selectedMessage.getId(), content);
        if (reply != null) {
            replyField.clear();
            loadConversation(selectedMessage);
        }
    }
    
    // ==================== FLEET MANAGEMENT ====================
    
    private void loadFleet() {
        if (fleetContainer == null) return;
        
        fleetContainer.getChildren().clear();
        
        List<User> carriers = userDAO.findAllCarriers();
        
        int activeCount = 0;
        
        if (carriers == null || carriers.isEmpty()) {
            Label emptyLabel = new Label("No carriers registered");
            emptyLabel.setStyle("-fx-text-fill: #9CA3AF; -fx-padding: 20;");
            fleetContainer.getChildren().add(emptyLabel);
        } else {
            for (User carrier : carriers) {
                boolean hasActive = hasActiveOrders(carrier.getId());
                if (hasActive) activeCount++;
                
                HBox card = createCarrierCard(carrier, hasActive);
                fleetContainer.getChildren().add(card);
            }
        }
        
        if (activeCarriersLabel != null) {
            activeCarriersLabel.setText(activeCount + " active");
        }
    }
    
    private boolean hasActiveOrders(int carrierId) {
        try {
            return orderDAO.hasActiveOrders(carrierId);
        } catch (Exception e) {
            return false;
        }
    }
    
    private HBox createCarrierCard(User carrier, boolean hasActiveOrders) {
        HBox card = new HBox(12);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(12, 14, 12, 14));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-border-color: #E5E7EB; -fx-border-radius: 8;");
        
        // Avatar
        Label avatar = new Label("🚴");
        avatar.setStyle("-fx-font-size: 24px;");
        
        // Info
        VBox info = new VBox(2);
        HBox.setHgrow(info, Priority.ALWAYS);
        
        Label nameLabel = new Label(carrier.getUsername());
        nameLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1F2937;");
        
        String vehicle = carrier.getVehicleType() != null ? carrier.getVehicleType() : "Not set";
        Label vehicleLabel = new Label(vehicle);
        vehicleLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #6B7280;");
        
        info.getChildren().addAll(nameLabel, vehicleLabel);
        
        // Status badge
        String status = carrier.getCarrierStatus() != null ? carrier.getCarrierStatus() : "offline";
        Label statusBadge = new Label(status.toUpperCase());
        String badgeColor = "available".equals(status) ? "#10B981" : ("busy".equals(status) ? "#F59E0B" : "#9CA3AF");
        statusBadge.setStyle("-fx-font-size: 10px; -fx-text-fill: white; -fx-background-color: " + badgeColor + 
                "; -fx-padding: 2 8; -fx-background-radius: 10;");
        
        // Active orders count
        int activeOrderCount = getActiveOrderCount(carrier.getId());
        Label activeLabel = new Label(activeOrderCount + " active");
        activeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #6B7280;");
        
        // Fire button
        MFXButton fireBtn = new MFXButton("Remove");
        if (hasActiveOrders) {
            // Disabled state - grayed out
            fireBtn.setStyle("-fx-background-color: #E5E7EB; -fx-text-fill: #9CA3AF; -fx-font-size: 11px;");
            fireBtn.setDisable(true);
            Tooltip tooltip = new Tooltip("Carrier must deliver all packages first before being removed");
            Tooltip.install(fireBtn, tooltip);
        } else {
            // Active state - red
            fireBtn.setStyle("-fx-background-color: #B91C1C; -fx-text-fill: white; -fx-font-size: 11px;");
            fireBtn.setOnAction(e -> handleFireCarrier(carrier));
        }
        
        card.getChildren().addAll(avatar, info, statusBadge, activeLabel, fireBtn);
        return card;
    }
    
    private int getActiveOrderCount(int carrierId) {
        try {
            List<Order> orders = orderDAO.findByCarrierId(carrierId);
            if (orders == null) return 0;
            return (int) orders.stream()
                .filter(o -> "assigned".equals(o.getStatus()) || "confirmed".equals(o.getStatus()))
                .count();
        } catch (Exception e) {
            return 0;
        }
    }
    
    private void handleFireCarrier(User carrier) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Remove Carrier");
        confirm.setHeaderText("Remove " + carrier.getUsername() + "?");
        confirm.setContentText("This action cannot be undone.");
        
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    carrierService.fireCarrier(carrier.getId());
                    loadFleet();
                } catch (Exception e) {
                    showError("Could not remove carrier: " + e.getMessage());
                }
            }
        });
    }
    
    // ==================== DATA VISUALIZATIONS ====================
    
    private void loadProduceChart() {
        if (produceBarChart == null) return;
        
        produceBarChart.getData().clear();
        
        // Get sales data
        List<Order> allOrders = orderService.getAllOrders();
        Map<String, Double> productRevenue = new HashMap<>();
        
        if (allOrders != null) {
            for (Order order : allOrders) {
                if ("cancelled".equals(order.getStatus())) continue;
                
                for (OrderItem item : order.getItems()) {
                    String name = item.getProductName();
                    productRevenue.merge(name, item.getTotalPrice(), Double::sum);
                }
            }
        }
        
        // Sort and get top 10
        List<Map.Entry<String, Double>> sorted = productRevenue.entrySet().stream()
            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(Collectors.toList());
        
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Revenue");
        
        for (Map.Entry<String, Double> entry : sorted) {
            series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
        }
        
        produceBarChart.getData().add(series);
        
        // Style bars with forest green
        for (XYChart.Data<String, Number> data : series.getData()) {
            if (data.getNode() != null) {
                data.getNode().setStyle("-fx-bar-fill: #1B4332;");
            }
        }
    }
    
    private void loadCarrierPerformance() {
        if (performanceContainer == null) return;
        
        performanceContainer.getChildren().clear();
        
        List<User> carriers = userDAO.findAllCarriers();
        
        if (carriers == null || carriers.isEmpty()) {
            Label emptyLabel = new Label("No carrier data");
            emptyLabel.setStyle("-fx-text-fill: #9CA3AF;");
            performanceContainer.getChildren().add(emptyLabel);
            return;
        }
        
        for (User carrier : carriers) {
            HBox row = createPerformanceRow(carrier);
            performanceContainer.getChildren().add(row);
        }
    }
    
    private HBox createPerformanceRow(User carrier) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(8));
        row.setStyle("-fx-background-color: #F9FAFB; -fx-background-radius: 6;");
        
        // Name
        Label nameLabel = new Label(carrier.getUsername());
        nameLabel.setPrefWidth(120);
        nameLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #1F2937;");
        
        // Rating (stars)
        double avgRating = getAverageRating(carrier.getId());
        Label ratingLabel = new Label(String.format("%.1f ⭐", avgRating));
        ratingLabel.setPrefWidth(80);
        ratingLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #F59E0B;");
        
        // On-time percentage
        double onTimePercent = getOnTimePercentage(carrier.getId());
        Label onTimeLabel = new Label(String.format("%.0f%%", onTimePercent));
        onTimeLabel.setPrefWidth(80);
        String onTimeColor = onTimePercent >= 90 ? "#10B981" : (onTimePercent >= 70 ? "#F59E0B" : "#EF4444");
        onTimeLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: " + onTimeColor + ";");
        
        // Total deliveries
        int totalDeliveries = getTotalDeliveries(carrier.getId());
        Label deliveriesLabel = new Label(String.valueOf(totalDeliveries));
        deliveriesLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #6B7280;");
        
        row.getChildren().addAll(nameLabel, ratingLabel, onTimeLabel, deliveriesLabel);
        return row;
    }
    
    private double getAverageRating(int carrierId) {
        try {
            List<CarrierRating> ratings = carrierService.getCarrierRatings(carrierId);
            if (ratings == null || ratings.isEmpty()) return 0;
            return ratings.stream().mapToInt(CarrierRating::getRating).average().orElse(0);
        } catch (Exception e) {
            return 0;
        }
    }
    
    private double getOnTimePercentage(int carrierId) {
        try {
            return carrierService.getOnTimePercentage(carrierId);
        } catch (Exception e) {
            return 0;
        }
    }
    
    private int getTotalDeliveries(int carrierId) {
        try {
            List<Order> orders = orderDAO.findByCarrierId(carrierId);
            if (orders == null) return 0;
            return (int) orders.stream().filter(o -> "delivered".equals(o.getStatus())).count();
        } catch (Exception e) {
            return 0;
        }
    }
    
    // ==================== COMMON ====================
    
    @FXML
    private void handleRefreshAll(ActionEvent event) {
        loadMessages();
        loadFleet();
        loadProduceChart();
        loadCarrierPerformance();
    }
    
    @FXML
    private void handleClose(ActionEvent event) {
        Stage stage = (Stage) flatRateLabel.getScene().getWindow();
        stage.close();
    }
    
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

