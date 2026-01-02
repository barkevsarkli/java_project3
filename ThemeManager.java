package com.greengrocer.app;

import javafx.scene.Scene;

/**
 * Manages application theme with support for multiple color schemes
 * Updated for "Green as your money" branding
 * 
 * @author Barkev Şarklı
 * @version 4.0
 */
public class ThemeManager
{
    private static ThemeManager instance;
    
    // Available themes
    public enum Theme {
        FRESH_GREEN("/css/unified-fresh-theme.css"),    // White-Orange-Green (Default)
        BLUE_PURPLE("/css/blue-purple-theme.css");       // Blue-Purple-Gray
        
        private final String cssPath;
        
        Theme(String cssPath) {
            this.cssPath = cssPath;
        }
        
        public String getCssPath() {
            return cssPath;
        }
    }
    
    private Theme currentTheme = Theme.FRESH_GREEN;  // Default theme

    private ThemeManager()
    {
    }

    /**
     * Get singleton instance
     * @return ThemeManager instance
     */
    public static synchronized ThemeManager getInstance()
    {
        if (instance == null)
            instance = new ThemeManager();

        return instance;
    }

    /**
     * Get current theme
     * @return current theme
     */
    public Theme getCurrentTheme()
    {
        return currentTheme;
    }

    /**
     * Set and apply a new theme
     * @param theme theme to apply
     * @param scene scene to apply theme to
     */
    public void setTheme(Theme theme, Scene scene)
    {
        this.currentTheme = theme;
        applyTheme(scene);
    }

    /**
     * Toggle between available themes
     * @param scene scene to apply theme to
     */
    public void toggleTheme(Scene scene)
    {
        System.out.println("DEBUG: toggleTheme() called");
        System.out.println("DEBUG: Current theme before toggle: " + currentTheme);
        System.out.println("DEBUG: Scene is null? " + (scene == null));
        
        // Switch to next theme
        currentTheme = (currentTheme == Theme.FRESH_GREEN) 
                        ? Theme.BLUE_PURPLE 
                        : Theme.FRESH_GREEN;
        
        System.out.println("DEBUG: New theme after toggle: " + currentTheme);
        applyTheme(scene);
    }

    /**
     * Apply current theme to a scene
     * @param scene scene to apply theme to
     */
    public void applyTheme(Scene scene)
    {
        System.out.println("DEBUG: applyTheme() called");
        System.out.println("DEBUG: Scene is null? " + (scene == null));
        
        if (scene == null) {
            System.err.println("ERROR: Scene is null! Cannot apply theme.");
            return;
        }

        System.out.println("DEBUG: Clearing existing stylesheets...");
        System.out.println("DEBUG: Current stylesheets count: " + scene.getStylesheets().size());
        scene.getStylesheets().clear();
        
        // Add the current theme
        try {
            String themeCss = getClass().getResource(currentTheme.getCssPath()).toExternalForm();
            System.out.println("DEBUG: Loading theme CSS: " + themeCss);
            scene.getStylesheets().add(themeCss);
            System.out.println("DEBUG: Theme applied successfully!");
            System.out.println("DEBUG: Stylesheets count after: " + scene.getStylesheets().size());
        } catch (Exception e) {
            System.err.println("ERROR: Failed to load theme: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
