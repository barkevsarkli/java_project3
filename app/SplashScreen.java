package com.greengrocer.app;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * splash screen with snake game themed loading animation.
 * 
 * @author Barkev Şarklı
 * @since 23-12-2025
 */
public class SplashScreen {
    
    // MD3 Color Palette
    private static final String CREAM_BG = "#FAF3E0";
    private static final String SAGE_GREEN = "#9CAF88";
    private static final String OLIVE_GREEN = "#708238";
    private static final String DARK_ANTHRACITE = "#2C3E2D";
    private static final String APPLE_RED = "#E9762B";  // Using vibrant orange for apples
    
    private Stage splashStage;
    private StackPane root;
    private Pane gamePane;
    private Label loadingLabel;
    private ProgressBar progressBar;
    
    // Snake game variables
    private List<Rectangle> snakeBody = new ArrayList<>();
    private Circle apple;
    private double snakeHeadX = 200;
    private double snakeHeadY = 100;
    private double targetX = 300;
    private double targetY = 100;
    private double velocityX = 2;
    private double velocityY = 0;
    private Timeline snakeAnimation;
    private Timeline loadingAnimation;
    
    private final String[] loadingMessages = {
        "Connecting to database...",
        "Loading product information...",
        "Preparing fresh fruits and vegetables...",
        "Checking user profiles...",
        "Starting cart system...",
        "Green as Your Money is Ready!"
    };
    
    private int currentMessageIndex = 0;
    private Runnable onComplete;
    
    public SplashScreen() {
        createSplashScreen();
    }
    
    private void createSplashScreen() {
        splashStage = new Stage();
        splashStage.initStyle(StageStyle.UNDECORATED);
        
        root = new StackPane();
        root.setStyle("-fx-background-color: " + CREAM_BG + "; -fx-background-radius: 20;");
        root.setPrefSize(960, 540);
        
        // Main container
        VBox mainContainer = new VBox(25);
        mainContainer.setAlignment(Pos.CENTER);
        mainContainer.setPadding(new Insets(40));
        
        // Logo and Title
        VBox headerBox = createHeader();
        
        // Snake Game Area
        Pane gamePaneLocal = createGamePane();
        
        // Loading Status
        VBox loadingBox = createLoadingSection();
        
        mainContainer.getChildren().addAll(headerBox, gamePaneLocal, loadingBox);
        
        // Shadow effect
        DropShadow shadow = new DropShadow();
        shadow.setRadius(30);
        shadow.setColor(Color.web(DARK_ANTHRACITE, 0.3));
        root.setEffect(shadow);
        
        // Decorative corner elements
        addCornerDecorations();
        
        root.getChildren().add(mainContainer);
        
        Scene scene = new Scene(root, 960, 540);
        scene.setFill(Color.TRANSPARENT);
        splashStage.setScene(scene);
        splashStage.centerOnScreen();
    }
    
    private VBox createHeader() {
        VBox headerBox = new VBox(5);
        headerBox.setAlignment(Pos.CENTER);
        
        // Leaf icon - replaced with text
        Label leafIcon = new Label("GREEN");
        leafIcon.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: " + OLIVE_GREEN + ";");
        
        // Main title
        Label titleLabel = new Label("Green as Your Money");
        titleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 36));
        titleLabel.setTextFill(Color.web(DARK_ANTHRACITE));
        
        // Subtitle
        Label subtitleLabel = new Label("Local Greengrocer • Fresh Products");
        subtitleLabel.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 16));
        subtitleLabel.setTextFill(Color.web(OLIVE_GREEN));
        
        headerBox.getChildren().addAll(leafIcon, titleLabel, subtitleLabel);
        return headerBox;
    }
    
    private Pane createGamePane() {
        gamePane = new Pane();
        gamePane.setPrefSize(500, 180);
        gamePane.setMaxSize(500, 180);
        gamePane.setStyle(
            "-fx-background-color: linear-gradient(to bottom, " + CREAM_BG + ", #EDE8D5);" +
            "-fx-background-radius: 20;" +
            "-fx-border-color: " + SAGE_GREEN + ";" +
            "-fx-border-radius: 20;" +
            "-fx-border-width: 2;"
        );
        
        // Initialize snake
        initializeSnake();
        
        // Create apple
        createApple();
        
        return gamePane;
    }
    
    private void initializeSnake() {
        snakeBody.clear();
        
        // Snake body segments
        for (int i = 0; i < 8; i++) {
            Rectangle segment = new Rectangle(16, 16);
            segment.setArcWidth(8);
            segment.setArcHeight(8);
            
            // Gradient color - fades from head to tail
            double opacity = 1.0 - (i * 0.08);
            segment.setFill(Color.web(OLIVE_GREEN, opacity));
            segment.setStroke(Color.web(DARK_ANTHRACITE, 0.3));
            segment.setStrokeWidth(1);
            
            segment.setX(snakeHeadX - (i * 14));
            segment.setY(snakeHeadY);
            
            // Shadow effect
            DropShadow segmentShadow = new DropShadow();
            segmentShadow.setRadius(3);
            segmentShadow.setColor(Color.web(DARK_ANTHRACITE, 0.2));
            segment.setEffect(segmentShadow);
            
            snakeBody.add(segment);
            gamePane.getChildren().add(segment);
        }
    }
    
    private void createApple() {
        apple = new Circle(10);
        apple.setFill(Color.web(APPLE_RED));
        apple.setStroke(Color.web(DARK_ANTHRACITE, 0.5));
        apple.setStrokeWidth(2);
        
        // Glow effect
        Glow glow = new Glow(0.3);
        apple.setEffect(glow);
        
        positionApple();
        gamePane.getChildren().add(apple);
        
        // Apple pulse animation
        ScaleTransition pulse = new ScaleTransition(Duration.millis(500), apple);
        pulse.setFromX(1.0);
        pulse.setFromY(1.0);
        pulse.setToX(1.15);
        pulse.setToY(1.15);
        pulse.setAutoReverse(true);
        pulse.setCycleCount(Animation.INDEFINITE);
        pulse.play();
    }
    
    private void positionApple() {
        Random random = new Random();
        targetX = 50 + random.nextDouble() * 400;
        targetY = 30 + random.nextDouble() * 120;
        apple.setCenterX(targetX);
        apple.setCenterY(targetY);
    }
    
    private VBox createLoadingSection() {
        VBox loadingBox = new VBox(12);
        loadingBox.setAlignment(Pos.CENTER);
        loadingBox.setMaxWidth(400);
        
        // Progress bar
        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(350);
        progressBar.setPrefHeight(6);
        progressBar.setStyle(
            "-fx-accent: " + OLIVE_GREEN + ";" +
            "-fx-background-color: " + SAGE_GREEN + "33;" +
            "-fx-background-radius: 3;"
        );
        
        // Loading message
        loadingLabel = new Label(loadingMessages[0]);
        loadingLabel.setFont(Font.font("Segoe UI", FontWeight.MEDIUM, 14));
        loadingLabel.setTextFill(Color.web(SAGE_GREEN).darker());
        
        // Dot animation
        HBox dotsBox = new HBox(5);
        dotsBox.setAlignment(Pos.CENTER);
        for (int i = 0; i < 3; i++) {
            Circle dot = new Circle(4);
            dot.setFill(Color.web(OLIVE_GREEN));
            
            FadeTransition fade = new FadeTransition(Duration.millis(600), dot);
            fade.setFromValue(0.3);
            fade.setToValue(1.0);
            fade.setDelay(Duration.millis(i * 200));
            fade.setAutoReverse(true);
            fade.setCycleCount(Animation.INDEFINITE);
            fade.play();
            
            dotsBox.getChildren().add(dot);
        }
        
        loadingBox.getChildren().addAll(progressBar, loadingLabel, dotsBox);
        return loadingBox;
    }
    
    private void addCornerDecorations() {
        // Top left leaf
        Label topLeft = new Label("*");
        topLeft.setStyle("-fx-font-size: 32px; -fx-opacity: 0.5; -fx-text-fill: " + OLIVE_GREEN + ";");
        StackPane.setAlignment(topLeft, Pos.TOP_LEFT);
        StackPane.setMargin(topLeft, new Insets(15));
        
        // Top right leaf
        Label topRight = new Label("*");
        topRight.setStyle("-fx-font-size: 32px; -fx-opacity: 0.5; -fx-text-fill: " + OLIVE_GREEN + ";");
        StackPane.setAlignment(topRight, Pos.TOP_RIGHT);
        StackPane.setMargin(topRight, new Insets(15));
        
        // Bottom left
        Label bottomLeft = new Label("*");
        bottomLeft.setStyle("-fx-font-size: 28px; -fx-opacity: 0.4; -fx-text-fill: " + OLIVE_GREEN + ";");
        StackPane.setAlignment(bottomLeft, Pos.BOTTOM_LEFT);
        StackPane.setMargin(bottomLeft, new Insets(15));
        
        // Bottom right
        Label bottomRight = new Label("*");
        bottomRight.setStyle("-fx-font-size: 28px; -fx-opacity: 0.4; -fx-text-fill: " + OLIVE_GREEN + ";");
        StackPane.setAlignment(bottomRight, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(bottomRight, new Insets(15));
        
        root.getChildren().addAll(topLeft, topRight, bottomLeft, bottomRight);
    }
    
    public void show(Runnable onComplete) {
        this.onComplete = onComplete;
        splashStage.show();
        startAnimations();
    }
    
    private void startAnimations() {
        // Snake animation
        snakeAnimation = new Timeline(new KeyFrame(Duration.millis(30), e -> updateSnake()));
        snakeAnimation.setCycleCount(Animation.INDEFINITE);
        snakeAnimation.play();
        
        // Loading animation
        loadingAnimation = new Timeline(new KeyFrame(Duration.millis(800), e -> updateLoading()));
        loadingAnimation.setCycleCount(loadingMessages.length);
        loadingAnimation.setOnFinished(e -> finishLoading());
        loadingAnimation.play();
        
        // Progress bar animation
        Timeline progressAnimation = new Timeline(
            new KeyFrame(Duration.ZERO, new KeyValue(progressBar.progressProperty(), 0)),
            new KeyFrame(Duration.seconds(4.5), new KeyValue(progressBar.progressProperty(), 1))
        );
        progressAnimation.play();
    }
    
    private void updateSnake() {
        // Move towards target
        double dx = targetX - snakeHeadX;
        double dy = targetY - snakeHeadY;
        double distance = Math.sqrt(dx * dx + dy * dy);
        
        if (distance < 15) {
            // Apple eaten - add segment and reposition apple
            eatApple();
        } else {
            // Move towards target
            velocityX = (dx / distance) * 3;
            velocityY = (dy / distance) * 3;
        }
        
        // Move head
        snakeHeadX += velocityX;
        snakeHeadY += velocityY;
        
        // Boundary check
        snakeHeadX = Math.max(20, Math.min(480, snakeHeadX));
        snakeHeadY = Math.max(20, Math.min(160, snakeHeadY));
        
        // Update body (each segment follows the previous one)
        for (int i = snakeBody.size() - 1; i > 0; i--) {
            Rectangle current = snakeBody.get(i);
            Rectangle previous = snakeBody.get(i - 1);
            current.setX(previous.getX());
            current.setY(previous.getY());
        }
        
        // Update head
        if (!snakeBody.isEmpty()) {
            snakeBody.get(0).setX(snakeHeadX);
            snakeBody.get(0).setY(snakeHeadY);
        }
    }
    
    private void eatApple() {
        // Add new segment
        if (snakeBody.size() < 18) {
            Rectangle lastSegment = snakeBody.get(snakeBody.size() - 1);
            Rectangle newSegment = new Rectangle(16, 16);
            newSegment.setArcWidth(8);
            newSegment.setArcHeight(8);
            
            double opacity = 1.0 - (snakeBody.size() * 0.04);
            newSegment.setFill(Color.web(OLIVE_GREEN, Math.max(0.3, opacity)));
            newSegment.setStroke(Color.web(DARK_ANTHRACITE, 0.2));
            newSegment.setStrokeWidth(1);
            newSegment.setX(lastSegment.getX());
            newSegment.setY(lastSegment.getY());
            
            snakeBody.add(newSegment);
            gamePane.getChildren().add(newSegment);
        }
        
        // Reposition apple
        positionApple();
        
        // Eat effect
        ScaleTransition eatEffect = new ScaleTransition(Duration.millis(100), apple);
        eatEffect.setFromX(0.5);
        eatEffect.setFromY(0.5);
        eatEffect.setToX(1.0);
        eatEffect.setToY(1.0);
        eatEffect.play();
    }
    
    private void updateLoading() {
        currentMessageIndex++;
        if (currentMessageIndex < loadingMessages.length) {
            // Fade transition
            FadeTransition fadeOut = new FadeTransition(Duration.millis(150), loadingLabel);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);
            fadeOut.setOnFinished(e -> {
                loadingLabel.setText(loadingMessages[currentMessageIndex]);
                FadeTransition fadeIn = new FadeTransition(Duration.millis(150), loadingLabel);
                fadeIn.setFromValue(0.0);
                fadeIn.setToValue(1.0);
                fadeIn.play();
            });
            fadeOut.play();
        }
    }
    
    private void finishLoading() {
        // Stop snake animation
        if (snakeAnimation != null) {
            snakeAnimation.stop();
        }
        
        // Fade-out animation
        FadeTransition fadeOut = new FadeTransition(Duration.millis(800), root);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setOnFinished(e -> {
            splashStage.close();
            if (onComplete != null) {
                Platform.runLater(onComplete);
            }
        });
        fadeOut.play();
    }
    
    public void close() {
        if (snakeAnimation != null) snakeAnimation.stop();
        if (loadingAnimation != null) loadingAnimation.stop();
        splashStage.close();
    }
}

