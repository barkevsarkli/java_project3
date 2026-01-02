package com.greengrocer.app;

import com.greengrocer.services.ImageLoaderService;
import io.github.palexdev.materialfx.theming.JavaFXThemes;
import io.github.palexdev.materialfx.theming.MaterialFXStylesheets;
import io.github.palexdev.materialfx.theming.UserAgentBuilder;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

/**
 * Main application, initializes MaterialFX and JavaFX application.
 * Updated for "Green as Your Money" branding with MD3 styling.
 * 
 * @author Barkev Şarklı
 * @version 2.0
 * @since 24.12.2025
 */
public class MainApplication extends Application
{

    //initial stage 
    private static Stage primaryStage;
    
    private static final double DEFAULT_WIDTH = 960; //window width
    private static final double DEFAULT_HEIGHT = 540; //window height

    /**
     * main method, to start application
     * 
     * @param args Command line arguments
     */
    public static void main(String[] args)
    {
        launch(args);
    }

    /**
     * Creating magic with JavaFX and MaterialFX frameworks
     * Shows splash screen with snake animation first, then login
     * 
     * @author Barkev Şarklı
     * @version 2.0
     * @param stage initial stage
     */
    @Override
    public void start(Stage stage)
    {
        initializeMaterialFX();
        primaryStage = stage;
        
        // Show the splash screen with snake animation
        SplashScreen splashScreen = new SplashScreen();
        splashScreen.show(() -> {
            // After splash screen finishes, show login
            showLoginScreen();
        });
    }
    
    /**
     * Shows the login screen after splash animation completes.
     * Also loads default product images into database.
     */
    private void showLoginScreen()
    {
        // Load default product images from resources into database
        try {
            // Use forceReloadAllImages() to ensure images are updated
            ImageLoaderService.getInstance().forceReloadAllImages();
        } catch (Exception e) {
            System.err.println("Warning: Could not load default images: " + e.getMessage());
        }
        
        try
        {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/LoginView.fxml"));
            Parent root = loader.load();
            
            Scene scene = new Scene(root, DEFAULT_WIDTH, DEFAULT_HEIGHT);
            
            // Apply theme using ThemeManager
            ThemeManager.getInstance().applyTheme(scene);
            
            primaryStage.setTitle("Group03 - GreenGrocer");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(800);
            primaryStage.setMinHeight(450);
            primaryStage.centerOnScreen();
            primaryStage.show();
            
        }
        catch (IOException e)
        {
            System.err.println("Failed to load login view: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * initialize MaterialFX theme
     * @author Barkev Şarklı
     * @version 1.0
     */
    private void initializeMaterialFX()
    {
        UserAgentBuilder.builder()
                .themes(JavaFXThemes.MODENA)
                .themes(MaterialFXStylesheets.forAssemble(true))
                .setDeploy(true)
                .setResolveAssets(true)
                .build()
                .setGlobal();
    }

    /**
     * switch the current scene with new view
     * 
     * @param fxmlPath path to the FXML file
     * @param title title of the window new window
     * @author Barkev Şarklı
     * @version 2.0
     * @throws IOException if FXML cannot be loaded
     */
    public static void switchScene(String fxmlPath, String title) throws IOException
    {
        FXMLLoader loader = new FXMLLoader(MainApplication.class.getResource(fxmlPath));
        Parent root = loader.load();
        
        Scene scene = primaryStage.getScene();
        scene.setRoot(root);
        
        // Apply theme using ThemeManager (supports dark/light mode)
        ThemeManager.getInstance().applyTheme(scene);
        
        primaryStage.setTitle(title);
        primaryStage.centerOnScreen();
    }

    /**
     * open a new window with specified view
     * 
     * @param fxmlPath Path to the FXML file
     * @param title Window title
     * @author Barkev Şarklı
     * @version 2.0
     * @return The new Stage
     * @throws IOException if FXML cannot be loaded
     */
    public static Stage openNewWindow(String fxmlPath, String title) throws IOException
    {
        FXMLLoader loader = new FXMLLoader(MainApplication.class.getResource(fxmlPath));
        Parent root = loader.load();
        
        Stage newStage = new Stage();
        Scene scene = new Scene(root);
        
        // Apply theme using ThemeManager
        ThemeManager.getInstance().applyTheme(scene);
        
        newStage.setTitle(title);
        newStage.setScene(scene);
        newStage.centerOnScreen();
        newStage.show();
        
        return newStage;
    }

    /**
     * open a new window with specified view and size
     * 
     * @param fxmlPath Path to the FXML file
     * @param title title of the window
     * @param width width of the window
     * @param height height of the window
     * @author Barkev Şarklı
     * @version 2.0
     * @return The new Stage
     * @throws IOException if FXML cannot be loaded
     */
    public static Stage openNewWindow(String fxmlPath, String title, double width, double height) throws IOException
    {
        FXMLLoader loader = new FXMLLoader(MainApplication.class.getResource(fxmlPath));
        Parent root = loader.load();
        
        Stage newStage = new Stage();
        Scene scene = new Scene(root, width, height);
        
        // Apply theme using ThemeManager
        ThemeManager.getInstance().applyTheme(scene);
        
        newStage.setTitle(title);
        newStage.setScene(scene);
        newStage.centerOnScreen();
        newStage.show();
        
        return newStage;
    }

    /**
     * open the AI chat assistant window
     * @author Barkev Şarklı
     * @version 2.0
     */
    public static void openChatWindow()
    {
        System.out.println("DEBUG: openChatWindow() called");
        try
        {
            System.out.println("DEBUG: Loading ChatView.fxml...");
            openNewWindow("/views/ChatView.fxml", "Group03 - GreenGrocer", 450, 600);
            System.out.println("DEBUG: ChatView loaded successfully!");
        }
        catch (IOException e)
        {
            System.err.println("Failed to open chat window: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * get the primary stage
     * 
     * @return primary stage
     * @author Barkev Şarklı
     * @version 1.0
     */
    public static Stage getPrimaryStage()
    {
        return primaryStage;
    }

    /**
     * perform cleanup operations
     * @author Barkev Şarklı
     * @version 1.0
     */
    @Override
    public void stop()
    {
        SessionManager.getInstance().logout();
    }
}
