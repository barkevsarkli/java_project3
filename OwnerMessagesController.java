package com.greengrocer.controllers;

import com.greengrocer.app.SessionManager;
import com.greengrocer.dao.MessageDAO;
import com.greengrocer.models.*;
import com.greengrocer.services.MessageService;
import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.controls.MFXTextField;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Controller for the Owner Dashboard Messages View.
 * Implements 3-pane email-client style layout with:
 * - Left: Message list with filters
 * - Center: Active conversation with smart actions
 * - Right: Fresh Context sidebar (customer + order + carrier info)
 * 
 * @author Green Grocer Team
 * @version 2.0
 */
public class OwnerMessagesController implements Initializable {

    // Header elements
    @FXML private Label dashboardSubtitle;
    @FXML private Label urgentCountLabel;
    @FXML private Label pendingCountLabel;
    @FXML private Label resolvedTodayLabel;
    @FXML private MFXButton refreshBtn;

    // Filter buttons
    @FXML private MFXButton filterAllBtn;
    @FXML private MFXButton filterUrgentBtn;
    @FXML private MFXButton filterDeliveryBtn;
    @FXML private MFXButton filterQualityBtn;
    @FXML private MFXButton filterGeneralBtn;

    // Left pane - Messages list
    @FXML private VBox messagesListContainer;

    // Center pane - Conversation
    @FXML private VBox emptyConversationState;
    @FXML private VBox conversationView;
    @FXML private Label conversationTitle;
    @FXML private Label issueTagLabel;
    @FXML private Label conversationSubject;
    @FXML private Label conversationDate;
    @FXML private ScrollPane messagesScrollPane;
    @FXML private VBox messagesThreadContainer;
    @FXML private TextArea replyTextArea;
    @FXML private MFXButton sendReplyBtn;


    // Services
    private MessageService messageService;
    private MessageDAO messageDAO;
    private User currentUser;

    // State
    private List<Message> allMessages = new ArrayList<>();
    private Message selectedMessage;
    private String currentFilter = "general";

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' HH:mm");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM yyyy");

    // Issue type detection keywords
    private static final Map<String, String> ISSUE_KEYWORDS = new HashMap<>() {{
        put("quality", "rotten|brown|spoiled|bad|mushy|moldy|overripe|damaged|bruised");
        put("delivery", "late|delay|didn't arrive|not delivered|wrong address|missing");
        put("refund", "refund|money back|return|cancel|compensation");
    }};

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            messageService = new MessageService();
            messageDAO = new MessageDAO();
            currentUser = SessionManager.getInstance().getCurrentUser();

            if (currentUser == null || !currentUser.isOwner()) {
                System.err.println("OwnerMessagesController: User is not owner!");
                return;
            }

            loadMessages();
            updateStats();
            showEmptyState();
            
            // Auto-scroll to bottom of messages
            messagesScrollPane.vvalueProperty().addListener((obs, oldVal, newVal) -> {
                // Keep scroll at bottom when new messages are added
            });
            
        } catch (Exception e) {
            System.err.println("Error initializing OwnerMessagesController: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Loads all messages for the owner.
     */
    private void loadMessages() {
        try {
            allMessages = messageDAO.findByReceiverId(currentUser.getId());
            // Also include sent messages for complete conversations
            List<Message> sentMessages = messageDAO.findBySenderId(currentUser.getId());
            allMessages.addAll(sentMessages);
            
            // Sort by date descending
            allMessages.sort((m1, m2) -> m2.getSentAt().compareTo(m1.getSentAt()));
            
            // Remove duplicates and get unique conversations
            displayFilteredMessages(currentFilter);
            
        } catch (SQLException e) {
            System.err.println("Error loading messages: " + e.getMessage());
            showError("Could not load messages");
        }
    }

    /**
     * Displays messages based on current filter.
     */
    private void displayFilteredMessages(String filter) {
        messagesListContainer.getChildren().clear();
        currentFilter = filter;
        
        // Get unique conversations (group by sender/receiver pair)
        Map<Integer, Message> uniqueConversations = new LinkedHashMap<>();
        for (Message msg : allMessages) {
            int otherUserId = msg.getSenderId() == currentUser.getId() 
                    ? msg.getReceiverId() 
                    : msg.getSenderId();
            
            // Keep the most recent message for each conversation
            if (!uniqueConversations.containsKey(otherUserId) || 
                msg.getSentAt().isAfter(uniqueConversations.get(otherUserId).getSentAt())) {
                uniqueConversations.put(otherUserId, msg);
            }
        }
        
        List<Message> filteredMessages = new ArrayList<>(uniqueConversations.values());
        
        // Apply filters - only show general messages
        filteredMessages = filteredMessages.stream()
                .filter(m -> matchesFilter(m, "general"))
                .collect(Collectors.toList());
        
        if (filteredMessages.isEmpty()) {
            Label emptyLabel = new Label("No messages match this filter");
            emptyLabel.setStyle("-fx-text-fill: #9ca3af; -fx-font-size: 13px; -fx-padding: 20;");
            messagesListContainer.getChildren().add(emptyLabel);
            return;
        }
        
        for (Message message : filteredMessages) {
            VBox messageCard = createMessageCard(message);
            messagesListContainer.getChildren().add(messageCard);
        }
        
        updateFilterButtonStyles();
    }

    /**
     * Checks if a message matches the given filter.
     */
    private boolean matchesFilter(Message message, String filter) {
        String content = (message.getContent() + " " + message.getSubject()).toLowerCase();
        
        switch (filter) {
            case "urgent":
                return content.matches(".*(" + ISSUE_KEYWORDS.get("refund") + ").*") || !message.isRead();
            case "delivery":
                return content.matches(".*(" + ISSUE_KEYWORDS.get("delivery") + ").*");
            case "quality":
                return content.matches(".*(" + ISSUE_KEYWORDS.get("quality") + ").*");
            case "general":
                // Messages that don't match any specific category
                return !content.matches(".*(" + ISSUE_KEYWORDS.get("quality") + "|" + 
                       ISSUE_KEYWORDS.get("delivery") + "|" + ISSUE_KEYWORDS.get("refund") + ").*");
            default:
                return true;
        }
    }

    /**
     * Creates a message card for the list.
     */
    private VBox createMessageCard(Message message) {
        VBox card = new VBox(8);
        card.setStyle("-fx-padding: 14 16; -fx-cursor: hand; -fx-background-color: white; -fx-border-color: #e5e1d8; -fx-border-width: 0 0 1 0;");
        
        // Highlight unread messages
        if (!message.isRead() && message.getReceiverId() == currentUser.getId()) {
            card.setStyle(card.getStyle() + " -fx-background-color: #fefce8;");
        }
        
        // Determine issue type and icon
        String issueIcon = detectIssueIcon(message);
        String issueType = detectIssueType(message);
        
        // Sender name with icon
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        
        Label iconLabel = new Label(issueIcon);
        iconLabel.setStyle("-fx-font-size: 14px;");
        
        String senderName = message.getSenderName() != null ? message.getSenderName() : "Customer";
        Label nameLabel = new Label(senderName);
        nameLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #111827;");
        
        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Label timeLabel = new Label(formatMessageTime(message.getSentAt()));
        timeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #9ca3af;");
        
        header.getChildren().addAll(iconLabel, nameLabel, spacer, timeLabel);
        
        // Subject
        Label subjectLabel = new Label(message.getSubject() != null ? message.getSubject() : "No Subject");
        subjectLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #374151;");
        
        // Preview
        String preview = message.getContent() != null 
                ? (message.getContent().length() > 60 ? message.getContent().substring(0, 60) + "..." : message.getContent())
                : "";
        Label previewLabel = new Label(preview);
        previewLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #6b7280;");
        previewLabel.setWrapText(true);
        
        // Issue tag
        if (!issueType.isEmpty()) {
            HBox tagRow = new HBox(8);
            Label tag = new Label(issueType);
            tag.setStyle("-fx-font-size: 10px; -fx-padding: 2 8; -fx-background-radius: 10; " + getTagStyle(issueType));
            tagRow.getChildren().add(tag);
            card.getChildren().addAll(header, subjectLabel, previewLabel, tagRow);
        } else {
            card.getChildren().addAll(header, subjectLabel, previewLabel);
        }
        
        // Click handler
        card.setOnMouseClicked(e -> selectMessage(message));
        
        // Hover effect
        card.setOnMouseEntered(e -> {
            if (selectedMessage == null || selectedMessage.getId() != message.getId()) {
                card.setStyle(card.getStyle().replace("-fx-background-color: white;", "-fx-background-color: #f9fafb;"));
            }
        });
        card.setOnMouseExited(e -> {
            if (!message.isRead() && message.getReceiverId() == currentUser.getId()) {
                card.setStyle(card.getStyle().replace("-fx-background-color: #f9fafb;", "-fx-background-color: #fefce8;"));
            } else if (selectedMessage == null || selectedMessage.getId() != message.getId()) {
                card.setStyle(card.getStyle().replace("-fx-background-color: #f9fafb;", "-fx-background-color: white;"));
            }
        });
        
        return card;
    }

    /**
     * Detects the issue icon based on message content.
     */
    private String detectIssueIcon(Message message) {
        String content = (message.getContent() + " " + message.getSubject()).toLowerCase();
        
        if (content.matches(".*(" + ISSUE_KEYWORDS.get("quality") + ").*")) {
            return "🍎";
        } else if (content.matches(".*(" + ISSUE_KEYWORDS.get("delivery") + ").*")) {
            return "🚚";
        } else if (content.matches(".*(" + ISSUE_KEYWORDS.get("refund") + ").*")) {
            return "💸";
        }
        return "💬";
    }

    /**
     * Detects the issue type label.
     */
    private String detectIssueType(Message message) {
        String content = (message.getContent() + " " + message.getSubject()).toLowerCase();
        
        if (content.matches(".*(" + ISSUE_KEYWORDS.get("quality") + ").*")) {
            return "Quality Issue";
        } else if (content.matches(".*(" + ISSUE_KEYWORDS.get("delivery") + ").*")) {
            return "Delivery Issue";
        } else if (content.matches(".*(" + ISSUE_KEYWORDS.get("refund") + ").*")) {
            return "Refund Request";
        }
        return "";
    }

    /**
     * Gets the tag style based on issue type.
     */
    private String getTagStyle(String issueType) {
        switch (issueType) {
            case "Quality Issue":
                return "-fx-background-color: #fee2e2; -fx-text-fill: #991b1b;";
            case "Delivery Issue":
                return "-fx-background-color: #fef3c7; -fx-text-fill: #92400e;";
            case "Refund Request":
                return "-fx-background-color: #dbeafe; -fx-text-fill: #1e40af;";
            default:
                return "-fx-background-color: #f3f4f6; -fx-text-fill: #374151;";
        }
    }

    /**
     * Formats message time for display.
     */
    private String formatMessageTime(LocalDateTime time) {
        if (time == null) return "";
        
        LocalDateTime now = LocalDateTime.now();
        if (time.toLocalDate().equals(now.toLocalDate())) {
            return "Today, " + time.format(TIME_FORMATTER);
        } else if (time.toLocalDate().equals(now.toLocalDate().minusDays(1))) {
            return "Yesterday, " + time.format(TIME_FORMATTER);
        }
        return time.format(DateTimeFormatter.ofPattern("MMM dd, HH:mm"));
    }

    /**
     * Selects a message and shows the conversation.
     */
    private void selectMessage(Message message) {
        selectedMessage = message;
        
        // Mark as read
        if (!message.isRead() && message.getReceiverId() == currentUser.getId()) {
            try {
                messageDAO.markAsRead(message.getId());
                message.markAsRead();
            } catch (SQLException e) {
                System.err.println("Error marking message as read: " + e.getMessage());
            }
        }
        
        // Get customer ID for conversation thread
        int customerId = message.getSenderId() == currentUser.getId() 
                ? message.getReceiverId() 
                : message.getSenderId();
        
        // Show conversation
        showConversation(message);
        
        // Load full conversation thread
        loadConversationThread(customerId);
        
        updateStats();
    }

    /**
     * Shows the conversation view and hides empty state.
     */
    private void showConversation(Message message) {
        emptyConversationState.setVisible(false);
        emptyConversationState.setManaged(false);
        conversationView.setVisible(true);
        conversationView.setManaged(true);
        
        // Update header
        String senderName = message.getSenderName() != null ? message.getSenderName() : "Customer";
        conversationTitle.setText(senderName);
        conversationSubject.setText(message.getSubject() != null ? message.getSubject() : "No Subject");
        conversationDate.setText(formatMessageTime(message.getSentAt()));
        
        // Update issue tag
        String issueType = detectIssueType(message);
        if (!issueType.isEmpty()) {
            issueTagLabel.setText(detectIssueIcon(message) + " " + issueType);
            issueTagLabel.setStyle("-fx-font-size: 12px; -fx-padding: 4 10; -fx-background-radius: 12; " + getTagStyle(issueType));
            issueTagLabel.setVisible(true);
        } else {
            issueTagLabel.setVisible(false);
        }
    }

    /**
     * Loads the full conversation thread.
     */
    private void loadConversationThread(int otherUserId) {
        messagesThreadContainer.getChildren().clear();
        
        try {
            List<Message> thread = messageDAO.findConversationBetween(currentUser.getId(), otherUserId);
            // Sort chronologically (oldest first)
            thread.sort((m1, m2) -> m1.getSentAt().compareTo(m2.getSentAt()));
            
            for (Message msg : thread) {
                VBox bubble = createMessageBubble(msg);
                messagesThreadContainer.getChildren().add(bubble);
            }
            
            // Scroll to bottom
            Platform.runLater(() -> messagesScrollPane.setVvalue(1.0));
            
        } catch (SQLException e) {
            System.err.println("Error loading conversation thread: " + e.getMessage());
        }
    }

    /**
     * Creates a chat bubble for a message.
     */
    private VBox createMessageBubble(Message message) {
        boolean isMine = message.getSenderId() == currentUser.getId();
        
        VBox bubble = new VBox(4);
        bubble.setMaxWidth(500);
        
        // Sender info
        HBox senderRow = new HBox(8);
        senderRow.setAlignment(isMine ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        
        String senderName = isMine ? "You" : (message.getSenderName() != null ? message.getSenderName() : "Customer");
        Label nameLabel = new Label(senderName);
        nameLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: " + (isMine ? "#059669" : "#374151") + ";");
        
        Label timeLabel = new Label(message.getSentAt().format(TIME_FORMATTER));
        timeLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #9ca3af;");
        
        if (isMine) {
            senderRow.getChildren().addAll(timeLabel, nameLabel);
        } else {
            senderRow.getChildren().addAll(nameLabel, timeLabel);
        }
        
        // Content bubble
        VBox contentBox = new VBox(4);
        contentBox.setStyle("-fx-padding: 12 16; -fx-background-radius: 12; " + 
                (isMine 
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
        bubble.setAlignment(isMine ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        
        // Wrap in HBox for alignment
        HBox container = new HBox();
        container.setAlignment(isMine ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        container.getChildren().add(bubble);
        
        VBox wrapper = new VBox(container);
        wrapper.setAlignment(isMine ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        
        return wrapper;
    }


    /**
     * Formats order status for display.
     */
    private String formatStatus(String status) {
        if (status == null) return "Unknown";
        switch (status.toLowerCase()) {
            case "delivered": return "Delivered ✓";
            case "assigned": return "Out for Delivery 🚚";
            case "confirmed": return "Confirmed";
            case "pending": return "Pending";
            case "cancelled": return "Cancelled ✗";
            default: return status;
        }
    }

    /**
     * Shows empty conversation state.
     */
    private void showEmptyState() {
        emptyConversationState.setVisible(true);
        emptyConversationState.setManaged(true);
        conversationView.setVisible(false);
        conversationView.setManaged(false);
    }

    /**
     * Updates header stats.
     */
    private void updateStats() {
        try {
            int unreadCount = messageDAO.getUnreadCount(currentUser.getId());
            urgentCountLabel.setText(String.valueOf(Math.min(unreadCount, 99)));
            pendingCountLabel.setText(String.valueOf(allMessages.size()));
            
            // Count resolved today (simplified - just count read messages from today)
            long resolvedToday = allMessages.stream()
                    .filter(m -> m.isRead() && m.getSentAt().toLocalDate().equals(LocalDateTime.now().toLocalDate()))
                    .count();
            resolvedTodayLabel.setText(String.valueOf(resolvedToday));
            
        } catch (SQLException e) {
            System.err.println("Error updating stats: " + e.getMessage());
        }
    }

    /**
     * Updates filter button styles.
     */
    private void updateFilterButtonStyles() {
        // Only general filter is shown, so no need to update button styles
    }

    // ==================== ACTION HANDLERS ====================

    @FXML
    private void handleRefresh(ActionEvent event) {
        loadMessages();
        updateStats();
        showEmptyState();
    }

    @FXML
    private void handleFilterAll(ActionEvent event) {
        displayFilteredMessages("all");
    }

    @FXML
    private void handleFilterUrgent(ActionEvent event) {
        displayFilteredMessages("urgent");
    }

    @FXML
    private void handleFilterDelivery(ActionEvent event) {
        displayFilteredMessages("delivery");
    }

    @FXML
    private void handleFilterQuality(ActionEvent event) {
        displayFilteredMessages("quality");
    }

    @FXML
    private void handleFilterGeneral(ActionEvent event) {
        displayFilteredMessages("general");
    }

    @FXML
    private void handleSendReply(ActionEvent event) {
        String content = replyTextArea.getText().trim();
        if (content.isEmpty() || selectedMessage == null) {
            return;
        }
        
        int receiverId = selectedMessage.getSenderId() == currentUser.getId() 
                ? selectedMessage.getReceiverId() 
                : selectedMessage.getSenderId();
        
        Message reply = messageService.replyToMessage(selectedMessage.getId(), content);
        if (reply != null) {
            replyTextArea.clear();
            loadConversationThread(receiverId);
        } else {
            showError("Failed to send reply");
        }
    }

    @FXML
    private void handleAttachImage(ActionEvent event) {
        showInfo("Image attachment feature coming soon!");
    }

    @FXML
    private void handleUseTemplate(ActionEvent event) {
        // Show template picker
        String[] templates = {
            "Thank you for reaching out. We apologize for the inconvenience and will resolve this immediately.",
            "We have processed a partial refund for the affected items. Please allow 3-5 business days.",
            "A replacement order has been created and will be delivered within 24 hours.",
            "We have notified the carrier about this issue. They will contact you shortly."
        };
        
        // For now, just use the first template
        replyTextArea.setText(templates[0]);
    }

    @FXML
    private void handleCallCarrier(ActionEvent event) {
        showInfo("Carrier contact feature - Would initiate call");
    }

    // ==================== HELPER METHODS ====================

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
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
}

