package com.greengrocer.controllers;

import com.greengrocer.services.OpenAIService;
import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.controls.MFXTextField;
import io.github.palexdev.materialfx.controls.MFXScrollPane;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;

/**
 * controller for AI chatbot window
 * 
 * @author Barkev Şarklı
 * @version 1.0
 */
public class ChatController implements Initializable
{
    @FXML private MFXTextField messageInput;
    @FXML private MFXButton sendButton;
    @FXML private MFXButton clearButton;
    @FXML private VBox chatMessagesBox;
    @FXML private MFXScrollPane chatScrollPane;
    @FXML private HBox statusBox;
    @FXML private Label statusLabel;

    private OpenAIService openAIService;
    private boolean isWaitingResponse = false;

    @Override
    public void initialize(URL location, ResourceBundle resources)
    {
        openAIService = OpenAIService.getInstance();
        
        // add welcome message
        addBotMessage("Hello! I'm your GreenGrocer assistant.\n\n" +
                     "I can help you with:\n" +
                     "- Fruit and vegetable recommendations\n" +
                     "- Recipe ideas and cooking tips\n" +
                     "- Nutritional information\n" +
                     "- Seasonal produce advice\n\n" +
                     "What would you like to know?");
        
        // check if API is configured
        if (!openAIService.isConfigured())
        {
            addBotMessage("Note: ChatBot is not fully configured. " +
                         "Please set your OPENAI_API_KEY environment variable to enable AI responses.");
        }
        
        // scroll to bottom when messages added
        chatMessagesBox.heightProperty().addListener((obs, oldVal, newVal) -> 
        {
            chatScrollPane.setVvalue(1.0);
        });
    }

    /**
     * handle send button click
     * @author Barkev Şarklı
     * @version 1.0
     */
    @FXML
    private void handleSendMessage()
    {
        String message = messageInput.getText();
        
        if (message == null || message.trim().isEmpty())
            return;

        if (isWaitingResponse)
            return;

        // clear input
        messageInput.clear();
        
        // add user message to chat
        addUserMessage(message.trim());
        
        // show typing indicator
        setTypingIndicator(true);
        
        // send to OpenAI asynchronously
        CompletableFuture.supplyAsync(() -> openAIService.chat(message.trim()))
            .thenAccept(response -> 
            {
                Platform.runLater(() -> 
                {
                    setTypingIndicator(false);
                    addBotMessage(response);
                });
            })
            .exceptionally(ex -> 
            {
                Platform.runLater(() -> 
                {
                    setTypingIndicator(false);
                    addBotMessage("Sorry, something went wrong. Please try again.");
                });
                return null;
            });
    }

    /**
     * handle quick suggestion buttons
     * @author Barkev Şarklı
     * @version 1.0
     */
    @FXML
    private void handleQuickSuggestion(javafx.event.ActionEvent event)
    {
        MFXButton button = (MFXButton) event.getSource();
        String text = button.getText();
        
        String question = "";
        if (text.contains("Salad"))
            question = "What vegetables do you recommend for a fresh healthy salad?";
        else if (text.contains("Seasonal"))
            question = "What fruits are in season right now?";
        else if (text.contains("Nutrition"))
            question = "What are some nutritional tips for eating more vegetables?";

        if (!question.isEmpty())
        {
            messageInput.setText(question);
            handleSendMessage();
        }
    }

    /**
     * handle clear chat button
     * @author Barkev Şarklı
     * @version 1.0
     */
    @FXML
    private void handleClearChat()
    {
        chatMessagesBox.getChildren().clear();
        openAIService.clearHistory();
        
        addBotMessage("Chat cleared. How can I help you today?");
    }

    /**
     * add a user message bubble
     * @param message message text
     * @author Barkev Şarklı
     * @version 1.0
     */
    private void addUserMessage(String message)
    {
        HBox messageBox = new HBox();
        messageBox.setAlignment(Pos.CENTER_RIGHT);
        messageBox.setPadding(new Insets(5, 0, 5, 50));
        
        VBox bubble = createMessageBubble(message, true);
        messageBox.getChildren().add(bubble);
        
        chatMessagesBox.getChildren().add(messageBox);
    }

    /**
     * add a bot message bubble
     * @param message message text
     * @author Barkev Şarklı
     * @version 1.0
     */
    private void addBotMessage(String message)
    {
        HBox messageBox = new HBox();
        messageBox.setAlignment(Pos.CENTER_LEFT);
        messageBox.setPadding(new Insets(5, 50, 5, 0));
        
        VBox bubble = createMessageBubble(message, false);
        messageBox.getChildren().add(bubble);
        
        chatMessagesBox.getChildren().add(messageBox);
    }

    /**
     * create a message bubble
     * @param message message text
     * @param isUser true if user message
     * @return styled VBox bubble
     * @author Barkev Şarklı
     * @version 1.0
     */
    private VBox createMessageBubble(String message, boolean isUser)
    {
        VBox bubble = new VBox();
        bubble.setMaxWidth(300);
        bubble.setPadding(new Insets(10, 14, 10, 14));
        
        Text text = new Text(message);
        text.setWrappingWidth(270);
        TextFlow textFlow = new TextFlow(text);
        
        if (isUser)
        {
            bubble.setStyle("-fx-background-color: #4CAF50; -fx-background-radius: 12 12 0 12;");
            text.setStyle("-fx-fill: white;");
        }
        else
        {
            bubble.setStyle("-fx-background-color: white; -fx-background-radius: 12 12 12 0; " +
                           "-fx-border-color: #E0E0E0; -fx-border-radius: 12 12 12 0;");
            text.setStyle("-fx-fill: #212121;");
        }
        
        bubble.getChildren().add(textFlow);
        return bubble;
    }

    /**
     * show/hide typing indicator
     * @param show true to show
     * @author Barkev Şarklı
     * @version 1.0
     */
    private void setTypingIndicator(boolean show)
    {
        isWaitingResponse = show;
        statusBox.setVisible(show);
        sendButton.setDisable(show);
        messageInput.setDisable(show);
        
        if (show)
            statusLabel.setText("Thinking...");
    }
}

