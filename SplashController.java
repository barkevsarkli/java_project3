package com.greengrocer.controllers;

import com.greengrocer.app.MainApplication;
import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import javafx.util.Duration;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controller for the splash screen with snake eating apples animation.
 * Shows a simple, beginner-friendly animation before the login screen.
 * 
 * @author Barkev Şarklı 
 * @version 2.0
 */
public class SplashController implements Initializable {
    
    @FXML
    private StackPane splashContainer;
    
    @FXML
    private Pane gamePane;
    
    @FXML
    private Circle snakeHead;
    
    @FXML
    private Circle snakeBody1;
    
    @FXML
    private Circle snakeBody2;
    
    @FXML
    private Circle apple;
    
    @FXML
    private Label appleLabel;
    
    @FXML
    private Label titleLabel;
    
    @FXML
    private Label loadingLabel;
    
    @FXML
    private Circle dot1, dot2, dot3, dot4, dot5;
    
    /** Total animation duration before transitioning to login */
    private static final int SPLASH_DURATION_MS = 4000;
    
    /** Snake movement speed */
    private static final double SNAKE_SPEED = 2.5;
    
    /** Animation timeline */
    private Timeline snakeAnimation;
    
    /** Current snake position */
    private double snakeX = 50;
    
    /** Number of apples eaten (snake grows) */
    private int applesEaten = 0;

    /**
     * Initializes the splash screen and starts animations.
     * 
     * @param location FXML location
     * @param resources Resource bundle
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Start title fade-in animation
        playTitleAnimation();
        
        // Start snake animation
        playSnakeAnimation();
        
        // Start loading dots animation
        playLoadingDotsAnimation();
        
        // Schedule transition to login after splash duration
        scheduleLoginTransition();
    }
    
    /**
     * Plays the title fade-in animation with a slight scale effect.
     */
    private void playTitleAnimation() {
        // Fade in the title
        FadeTransition fadeIn = new FadeTransition(Duration.millis(1000), titleLabel);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        
        // Scale animation for title
        ScaleTransition scaleUp = new ScaleTransition(Duration.millis(1200), titleLabel);
        scaleUp.setFromX(0.8);
        scaleUp.setFromY(0.8);
        scaleUp.setToX(1.0);
        scaleUp.setToY(1.0);
        
        // Play both together
        ParallelTransition titleAnim = new ParallelTransition(fadeIn, scaleUp);
        titleAnim.play();
    }
    
    /**
     * Plays the snake eating apples animation using TranslateTransition.
     * The snake moves across the screen, eats apples, and grows.
     */
    private void playSnakeAnimation() {
        // Main snake movement animation
        snakeAnimation = new Timeline();
        snakeAnimation.setCycleCount(Timeline.INDEFINITE);
        
        // KeyFrame to update snake position
        KeyFrame moveFrame = new KeyFrame(Duration.millis(16), event -> {
            // Move snake head forward
            snakeX += SNAKE_SPEED;
            snakeHead.setLayoutX(snakeX);
            
            // Move body segments (follow the head with delay)
            snakeBody1.setLayoutX(snakeX - 20);
            snakeBody2.setLayoutX(snakeX - 36);
            
            // Check if snake reached apple
            if (Math.abs(snakeX - apple.getLayoutX()) < 20) {
                eatApple();
            }
            
            // Reset snake position if it goes off screen
            if (snakeX > 420) {
                resetSnakePosition();
            }
        });
        
        snakeAnimation.getKeyFrames().add(moveFrame);
        snakeAnimation.play();
    }
    
    /**
     * Handles the apple eating animation and effect.
     */
    private void eatApple() {
        applesEaten++;
        
        // Apple disappear animation
        ScaleTransition shrink = new ScaleTransition(Duration.millis(150), apple);
        shrink.setToX(0);
        shrink.setToY(0);
        
        ScaleTransition shrinkLabel = new ScaleTransition(Duration.millis(150), appleLabel);
        shrinkLabel.setToX(0);
        shrinkLabel.setToY(0);
        
        // Snake grow animation (pulse effect)
        ScaleTransition growHead = new ScaleTransition(Duration.millis(200), snakeHead);
        growHead.setToX(1.3);
        growHead.setToY(1.3);
        growHead.setAutoReverse(true);
        growHead.setCycleCount(2);
        
        // Update loading message
        String[] messages = {
            "Gathering fresh vegetables...",
            "Picking ripe fruits...",
            "Almost ready!",
            "Welcome to Green as your money!"
        };
        
        int msgIndex = Math.min(applesEaten - 1, messages.length - 1);
        loadingLabel.setText(messages[msgIndex]);
        
        shrink.play();
        shrinkLabel.play();
        growHead.play();
        
        // Respawn apple after short delay
        PauseTransition pause = new PauseTransition(Duration.millis(400));
        pause.setOnFinished(e -> respawnApple());
        pause.play();
    }
    
    /**
     * Respawns the apple at a new random position.
     */
    private void respawnApple() {
        // Random Y position
        double newY = 50 + Math.random() * 100;
        apple.setLayoutY(newY);
        appleLabel.setLayoutY(newY - 15);
        
        // Reset scale
        apple.setScaleX(1);
        apple.setScaleY(1);
        appleLabel.setScaleX(1);
        appleLabel.setScaleY(1);
        
        // Fade in animation
        FadeTransition fadeIn = new FadeTransition(Duration.millis(200), apple);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        
        FadeTransition fadeInLabel = new FadeTransition(Duration.millis(200), appleLabel);
        fadeInLabel.setFromValue(0);
        fadeInLabel.setToValue(1);
        
        fadeIn.play();
        fadeInLabel.play();
    }
    
    /**
     * Resets snake position to the left side of the screen.
     */
    private void resetSnakePosition() {
        snakeX = -50;
        
        // Randomize Y position
        double newY = 50 + Math.random() * 100;
        snakeHead.setLayoutY(newY);
        snakeBody1.setLayoutY(newY);
        snakeBody2.setLayoutY(newY);
    }
    
    /**
     * Plays the loading dots pulse animation.
     */
    private void playLoadingDotsAnimation() {
        Circle[] dots = {dot1, dot2, dot3, dot4, dot5};
        
        for (int i = 0; i < dots.length; i++) {
            // Create pulse animation for each dot
            ScaleTransition pulse = new ScaleTransition(Duration.millis(400), dots[i]);
            pulse.setFromX(1);
            pulse.setFromY(1);
            pulse.setToX(1.5);
            pulse.setToY(1.5);
            pulse.setAutoReverse(true);
            pulse.setCycleCount(Animation.INDEFINITE);
            pulse.setDelay(Duration.millis(i * 100));
            pulse.play();
        }
    }
    
    /**
     * Schedules the transition to login screen after splash duration.
     */
    private void scheduleLoginTransition() {
        PauseTransition delay = new PauseTransition(Duration.millis(SPLASH_DURATION_MS));
        
        delay.setOnFinished(event -> {
            // Stop snake animation
            if (snakeAnimation != null) {
                snakeAnimation.stop();
            }
            
            // Fade out splash screen
            FadeTransition fadeOut = new FadeTransition(Duration.millis(500), splashContainer);
            fadeOut.setFromValue(1);
            fadeOut.setToValue(0);
            
            fadeOut.setOnFinished(e -> {
                // Navigate to login screen
                try {
                    MainApplication.switchScene("/fxml/LoginView.fxml", 
                            "Group03 - GreenGrocer");
                } catch (Exception ex) {
                    System.err.println("Failed to navigate to login: " + ex.getMessage());
                    ex.printStackTrace();
                }
            });
            
            fadeOut.play();
        });
        
        delay.play();
    }
}

