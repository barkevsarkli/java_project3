package com.greengrocer.controllers;

import com.greengrocer.app.SessionManager;
import com.greengrocer.models.Message;
import com.greengrocer.models.User;
import com.greengrocer.services.MessageService;
import io.github.palexdev.materialfx.controls.*;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Modern controller for customer messaging with chat-style interface.
 * Supports messaging to owner (support) and assigned carrier.
 * 
 * @author Barkev Şarklı
 * @version 2.0
 * @since 02.01.2026
 */
public class CustomerMessagingController implements Initializable {

    // Header Components
    @FXML private Label headerSubtitle;
    @FXML private Label unreadBadge;
    @FXML private MFXButton refreshButton;

    // Sidebar Components
    @FXML private MFXButton messageOwnerButton;
    @FXML private MFXButton messageCarrierButton;
    @FXML private Label carrierInfoLabel;
    @FXML private MFXButton tabAllBtn;
    @FXML private MFXButton tabUnreadBtn;
    @FXML private MFXButton tabSentBtn;
    @FXML private VBox conversationsContainer;

    // Message Thread Components
    @FXML private HBox messageHeader;
    @FXML private Label threadTitleLabel;
    @FXML private Label threadStatusLabel;
    @FXML private VBox emptyState;
    @FXML private VBox messageThreadView;
    @FXML private ScrollPane messagesScrollPane;
    @FXML private VBox messagesContainer;
    @FXML private MFXTextField replyTextField;
    @FXML private MFXButton sendReplyButton;

    // Compose View Components
    @FXML private VBox composeView;
    @FXML private Label composeRecipientLabel;
    @FXML private MFXTextField subjectField;
    @FXML private TextArea messageContentArea;
    @FXML private MFXButton sendMessageButton;

    // Services
    private MessageService messageService;
    private User currentUser;

    // State
    private Message selectedMessage;
    private String currentFilter = "all";
    private User assignedCarrier;
    private String composeRecipient; // "owner" or "carrier"
    private int selectedOrderId = -1; // Selected order for carrier messaging
    private List<Object[]> ordersWithCarriers = new ArrayList<>(); // [orderId, carrierId, carrierName, status]

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            messageService = new MessageService();
            currentUser = SessionManager.getInstance().getCurrentUser();

            if (currentUser == null) {
                System.err.println("CustomerMessagingController: No current user found!");
                return;
            }

            checkCarrierStatus();
            loadMessages();
            updateUnreadBadge();
            updateTabStyles(); // Initialize tab styles
            showEmptyState();
        } catch (Exception e) {
            System.err.println("Error initializing CustomerMessagingController: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Checks if customer has assigned carriers and updates UI accordingly.
     */
    private void checkCarrierStatus() {
        ordersWithCarriers = messageService.getMyOrdersWithCarriers();
        assignedCarrier = messageService.getMyAssignedCarrier();
        
        if (!ordersWithCarriers.isEmpty()) {
            messageCarrierButton.setDisable(false);
            if (ordersWithCarriers.size() == 1) {
                String carrierName = (String) ordersWithCarriers.get(0)[2];
                carrierInfoLabel.setText("✓ Carrier: " + carrierName);
            } else {
                carrierInfoLabel.setText("✓ " + ordersWithCarriers.size() + " active orders with carriers");
            }
            carrierInfoLabel.setStyle("-fx-text-fill: #10b981; -fx-font-size: 11px;");
        } else {
            messageCarrierButton.setDisable(true);
            carrierInfoLabel.setText("No active carrier assigned");
            carrierInfoLabel.setStyle("-fx-text-fill: #9ca3af; -fx-font-size: 11px;");
        }
    }

    /**
     * Loads and displays messages based on current filter.
     */
    private void loadMessages() {
        List<Message> messages;
        
        switch (currentFilter) {
            case "unread":
                messages = messageService.getUnreadMessages();
                break;
            case "sent":
                messages = messageService.getSentMessages();
                break;
            default: // "all"
                List<Message> received = messageService.getReceivedMessages();
                List<Message> sent = messageService.getSentMessages();
                messages = new ArrayList<>(received);
                messages.addAll(sent);
                break;
        }

        // Sort by date (newest first)
        messages.sort((a, b) -> b.getSentAt().compareTo(a.getSentAt()));

        displayConversations(messages);
    }

    /**
     * Displays conversations in the sidebar.
     */
    private void displayConversations(List<Message> messages) {
        conversationsContainer.getChildren().clear();

        if (messages.isEmpty()) {
            Label emptyLabel = new Label("No messages yet");
            emptyLabel.setStyle("-fx-text-fill: #9ca3af; -fx-font-size: 14px; -fx-padding: 20;");
            conversationsContainer.getChildren().add(emptyLabel);
            return;
        }

        for (Message message : messages) {
            VBox messageCard = createMessageCard(message);
            conversationsContainer.getChildren().add(messageCard);
        }
    }

    /**
     * Creates a message card for the sidebar.
     */
    private VBox createMessageCard(Message message) {
        VBox card = new VBox(6);
        card.setStyle("-fx-padding: 16 20; -fx-cursor: hand; -fx-background-color: white;");

        boolean isSent = message.getSenderId() == currentUser.getId();
        boolean isUnread = !message.isRead() && !isSent;

        // Add hover effect
        card.setOnMouseEntered(e -> card.setStyle("-fx-padding: 16 20; -fx-cursor: hand; -fx-background-color: #f3f4f6;"));
        card.setOnMouseExited(e -> {
            String bg = isUnread ? "#eff6ff" : "white";
            card.setStyle("-fx-padding: 16 20; -fx-cursor: hand; -fx-background-color: " + bg + ";");
        });

        // Click handler
        card.setOnMouseClicked(e -> handleSelectMessage(message));

        if (isUnread) {
            card.setStyle("-fx-padding: 16 20; -fx-cursor: hand; -fx-background-color: #eff6ff;");
        }

        // Header row
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);

        String senderName = message.getSenderName() != null ? message.getSenderName() : "Unknown";
        String receiverName = message.getReceiverName() != null ? message.getReceiverName() : "Unknown";
        Label nameLabel = new Label(isSent ? "To: " + receiverName : senderName);
        nameLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #111827;");

        if (isUnread) {
            Label unreadDot = new Label("●");
            unreadDot.setStyle("-fx-text-fill: #3b82f6; -fx-font-size: 14px;");
            header.getChildren().add(unreadDot);
        }

        header.getChildren().add(nameLabel);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        header.getChildren().add(spacer);

        Label timeLabel = new Label(message.getSentAt().format(TIME_FORMATTER));
        timeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #6b7280;");
        header.getChildren().add(timeLabel);

        card.getChildren().add(header);

        // Subject
        Label subjectLabel = new Label(message.getSubject());
        subjectLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #374151; -fx-font-weight: " + (isUnread ? "bold" : "normal") + ";");
        subjectLabel.setMaxWidth(Double.MAX_VALUE);
        subjectLabel.setWrapText(false);
        card.getChildren().add(subjectLabel);

        // Content preview
        String preview = message.getContent();
        if (preview.length() > 80) {
            preview = preview.substring(0, 80) + "...";
        }
        Label contentLabel = new Label(preview);
        contentLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #6b7280;");
        contentLabel.setWrapText(true);
        contentLabel.setMaxWidth(Double.MAX_VALUE);
        card.getChildren().add(contentLabel);

        // Separator
        Separator separator = new Separator();
        separator.setStyle("-fx-background-color: #e5e7eb;");
        VBox.setMargin(separator, new Insets(12, -20, 0, -20));
        card.getChildren().add(separator);

        return card;
    }

    /**
     * Handles selecting a message from the sidebar.
     */
    private void handleSelectMessage(Message message) {
        selectedMessage = message;
        
        // Mark as read
        if (!message.isRead() && message.getReceiverId() == currentUser.getId()) {
            messageService.markAsRead(message.getId());
            message.setRead(true);
            updateUnreadBadge();
            loadMessages(); // Refresh to update unread indicators
        }

        showMessageThread();
        displayMessageThread(message);
    }

    /**
     * Displays the message thread view.
     */
    private void showMessageThread() {
        emptyState.setVisible(false);
        emptyState.setManaged(false);
        messageThreadView.setVisible(true);
        messageThreadView.setManaged(true);
        composeView.setVisible(false);
        composeView.setManaged(false);
    }

    /**
     * Shows the empty state.
     */
    private void showEmptyState() {
        emptyState.setVisible(true);
        emptyState.setManaged(true);
        messageThreadView.setVisible(false);
        messageThreadView.setManaged(false);
        composeView.setVisible(false);
        composeView.setManaged(false);
    }

    /**
     * Shows the compose view.
     */
    private void showComposeView() {
        emptyState.setVisible(false);
        emptyState.setManaged(false);
        messageThreadView.setVisible(false);
        messageThreadView.setManaged(false);
        composeView.setVisible(true);
        composeView.setManaged(true);
    }

    /**
     * Displays the message thread (conversation history).
     */
    private void displayMessageThread(Message message) {
        messagesContainer.getChildren().clear();

        // Update thread header
        boolean isSent = message.getSenderId() == currentUser.getId();
        String senderName = message.getSenderName() != null ? message.getSenderName() : "Unknown";
        String receiverName = message.getReceiverName() != null ? message.getReceiverName() : "Unknown";
        String otherParty = isSent ? receiverName : senderName;
        threadTitleLabel.setText("Conversation with " + otherParty);
        threadStatusLabel.setText(message.getSentAt() != null ? message.getSentAt().format(DATE_TIME_FORMATTER) : "Unknown date");

        // Get conversation thread
        List<Message> thread = messageService.getConversationThread(message.getId());
        if (thread.isEmpty()) {
            thread = List.of(message);
        }

        // Sort chronologically (oldest first)
        thread.sort(Comparator.comparing(Message::getSentAt));

        // Display messages
        for (Message msg : thread) {
            VBox messageBox = createChatBubble(msg);
            messagesContainer.getChildren().add(messageBox);
        }

        // Scroll to bottom
        Platform.runLater(() -> messagesScrollPane.setVvalue(1.0));
    }

    /**
     * Creates a chat bubble for a message.
     */
    private VBox createChatBubble(Message message) {
        VBox container = new VBox(4);
        boolean isSentByMe = message.getSenderId() == currentUser.getId();

        HBox wrapper = new HBox();
        wrapper.setAlignment(isSentByMe ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

        VBox bubble = new VBox(8);
        bubble.setMaxWidth(500);
        bubble.setPadding(new Insets(12, 16, 12, 16));

        if (isSentByMe) {
            bubble.setStyle("-fx-background-color: #2E7D32; -fx-background-radius: 16 16 4 16;");
        } else {
            bubble.setStyle("-fx-background-color: white; -fx-background-radius: 16 16 16 4; -fx-border-color: #e5e7eb; -fx-border-width: 1; -fx-border-radius: 16 16 16 4;");
        }

        // Sender name (if not sent by me)
        if (!isSentByMe) {
            Label senderLabel = new Label(message.getSenderName());
            senderLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #2E7D32;");
            bubble.getChildren().add(senderLabel);
        }

        // Subject (if it's the first message)
        if (message.getParentMessageId() == null) {
            Label subjectLabel = new Label("📌 " + message.getSubject());
            subjectLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: " + (isSentByMe ? "white" : "#111827") + ";");
            subjectLabel.setWrapText(true);
            bubble.getChildren().add(subjectLabel);
        }

        // Content
        Label contentLabel = new Label(message.getContent());
        contentLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: " + (isSentByMe ? "white" : "#374151") + "; -fx-line-spacing: 2px;");
        contentLabel.setWrapText(true);
        bubble.getChildren().add(contentLabel);

        // Timestamp
        Label timeLabel = new Label(message.getSentAt().format(TIME_FORMATTER));
        timeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: " + (isSentByMe ? "#c7e7c9" : "#9ca3af") + ";");
        timeLabel.setAlignment(isSentByMe ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        bubble.getChildren().add(timeLabel);

        wrapper.getChildren().add(bubble);
        container.getChildren().add(wrapper);

        return container;
    }

    /**
     * Handles sending a reply to the current message.
     */
    @FXML
    private void handleSendReply(ActionEvent event) {
        if (selectedMessage == null) return;

        String replyContent = replyTextField.getText().trim();
        if (replyContent.isEmpty()) {
            showError("Please enter a message");
            return;
        }

        Message reply = messageService.replyToMessage(selectedMessage.getId(), replyContent);
        if (reply != null) {
            replyTextField.clear();
            displayMessageThread(selectedMessage); // Refresh thread
        } else {
            showError("Failed to send reply");
        }
    }

    /**
     * Handles canceling reply.
     */
    @FXML
    private void handleCancelReply(ActionEvent event) {
        replyTextField.clear();
    }

    /**
     * Handles starting a new message to owner.
     */
    @FXML
    private void handleNewMessageToOwner(ActionEvent event) {
        composeRecipient = "owner";
        composeRecipientLabel.setText("To: Support Team (Owner)");
        subjectField.clear();
        messageContentArea.clear();
        showComposeView();
    }

    /**
     * Handles starting a new message to carrier.
     * Shows order selection dialog if there are multiple orders.
     */
    @FXML
    private void handleNewMessageToCarrier(ActionEvent event) {
        if (ordersWithCarriers.isEmpty()) {
            showError("You don't have any active orders with assigned carriers");
            return;
        }

        // If only one order, use it directly
        if (ordersWithCarriers.size() == 1) {
            Object[] orderInfo = ordersWithCarriers.get(0);
            selectedOrderId = (int) orderInfo[0];
            String carrierName = (String) orderInfo[2];
            
            composeRecipient = "carrier";
            composeRecipientLabel.setText("To: Carrier " + carrierName + " (Order #" + selectedOrderId + ")");
            subjectField.clear();
            messageContentArea.clear();
            showComposeView();
            return;
        }
        
        // Multiple orders - show selection dialog
        showOrderSelectionDialog();
    }
    
    /**
     * Shows a dialog to select which order/carrier to message.
     */
    private void showOrderSelectionDialog() {
        Alert dialog = new Alert(Alert.AlertType.CONFIRMATION);
        dialog.setTitle("Select Order");
        dialog.setHeaderText("Which order would you like to message about?");
        dialog.setContentText("You have multiple active orders. Please select one:");
        
        // Create custom content with order options
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        
        ToggleGroup orderGroup = new ToggleGroup();
        
        for (Object[] orderInfo : ordersWithCarriers) {
            int orderId = (int) orderInfo[0];
            String carrierName = (String) orderInfo[2];
            String status = (String) orderInfo[3];
            
            RadioButton rb = new RadioButton("Order #" + orderId + " - Carrier: " + carrierName + 
                    " (" + status.toUpperCase() + ")");
            rb.setToggleGroup(orderGroup);
            rb.setUserData(orderInfo);
            rb.setStyle("-fx-font-size: 13px;");
            
            content.getChildren().add(rb);
        }
        
        // Select first by default
        if (!content.getChildren().isEmpty()) {
            ((RadioButton) content.getChildren().get(0)).setSelected(true);
        }
        
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setPrefWidth(400);
        
        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            RadioButton selected = (RadioButton) orderGroup.getSelectedToggle();
            if (selected != null) {
                Object[] orderInfo = (Object[]) selected.getUserData();
                selectedOrderId = (int) orderInfo[0];
                String carrierName = (String) orderInfo[2];
                
                composeRecipient = "carrier";
                composeRecipientLabel.setText("To: Carrier " + carrierName + " (Order #" + selectedOrderId + ")");
                subjectField.clear();
                messageContentArea.clear();
                showComposeView();
            }
        }
    }

    /**
     * Handles sending a new message.
     */
    @FXML
    private void handleSendMessage(ActionEvent event) {
        String subject = subjectField.getText().trim();
        String content = messageContentArea.getText().trim();

        if (subject.isEmpty()) {
            showError("Please enter a subject");
            return;
        }

        if (content.isEmpty()) {
            showError("Please enter a message");
            return;
        }

        Message message = null;
        if ("owner".equals(composeRecipient)) {
            message = messageService.sendMessageToOwner(subject, content);
        } else if ("carrier".equals(composeRecipient)) {
            // Use the selected order ID to send to the correct carrier
            if (selectedOrderId > 0) {
                message = messageService.sendMessageToCarrierForOrder(selectedOrderId, subject, content);
            } else {
                // Fallback to default behavior (most recent order)
                message = messageService.sendMessageToMyCarrier(subject, content);
            }
        }

        if (message != null) {
            subjectField.clear();
            messageContentArea.clear();
            selectedOrderId = -1; // Reset selection
            loadMessages();
            updateUnreadBadge();
            showEmptyState();
        } else {
            showError("Failed to send message");
        }
    }

    /**
     * Handles canceling compose.
     */
    @FXML
    private void handleCancelCompose(ActionEvent event) {
        subjectField.clear();
        messageContentArea.clear();
        showEmptyState();
    }

    /**
     * Handles showing all messages.
     */
    @FXML
    private void handleShowAllMessages(ActionEvent event) {
        currentFilter = "all";
        updateTabStyles();
        loadMessages();
    }

    /**
     * Handles showing unread messages only.
     */
    @FXML
    private void handleShowUnreadMessages(ActionEvent event) {
        currentFilter = "unread";
        updateTabStyles();
        loadMessages();
    }

    /**
     * Handles showing sent messages only.
     */
    @FXML
    private void handleShowSentMessages(ActionEvent event) {
        currentFilter = "sent";
        updateTabStyles();
        loadMessages();
    }

    /**
     * Updates tab button styles based on current filter.
     */
    private void updateTabStyles() {
        String activeStyle = "-fx-background-color: #2E7D32; -fx-text-fill: white; -fx-font-size: 13px; -fx-padding: 8 16; -fx-background-radius: 6;";
        String inactiveStyle = "-fx-background-color: transparent; -fx-text-fill: #6b7280; -fx-font-size: 13px; -fx-padding: 8 16;";

        tabAllBtn.setStyle(currentFilter.equals("all") ? activeStyle : inactiveStyle);
        tabUnreadBtn.setStyle(currentFilter.equals("unread") ? activeStyle : inactiveStyle);
        tabSentBtn.setStyle(currentFilter.equals("sent") ? activeStyle : inactiveStyle);
    }

    /**
     * Handles refreshing messages.
     */
    @FXML
    private void handleRefresh(ActionEvent event) {
        checkCarrierStatus();
        loadMessages();
        updateUnreadBadge();
    }

    /**
     * Updates the unread badge.
     */
    private void updateUnreadBadge() {
        int unreadCount = messageService.getUnreadCount();
        if (unreadCount > 0) {
            unreadBadge.setText(unreadCount + " unread");
            unreadBadge.setVisible(true);
        } else {
            unreadBadge.setVisible(false);
        }
    }

    /**
     * Shows error alert.
     */
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Shows success alert.
     */
    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
