package com.greengrocer.services;

import com.greengrocer.dao.DatabaseAdapter;
import java.io.InputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * service for loading default product images from resources.
 * 
 * @author Barkev Şarklı
 * @since 23-12-2025
 */
public class ImageLoaderService {
    
    private static ImageLoaderService instance;
    private final DatabaseAdapter db;
    private boolean imagesLoaded = false;
    
    // Map of product names (lowercase) to image file names
    private static final Map<String, String> PRODUCT_IMAGES = new HashMap<>();
    
    static {
        PRODUCT_IMAGES.put("apple", "apple.png");
        PRODUCT_IMAGES.put("artichoke", "artichoke.png");
        PRODUCT_IMAGES.put("avocado", "avocado.png");
        PRODUCT_IMAGES.put("banana", "banana.png");
        PRODUCT_IMAGES.put("broccoli", "broccoli.png");
        PRODUCT_IMAGES.put("cabbage", "cabbage.png");
        PRODUCT_IMAGES.put("carrot", "carrot.png");
        PRODUCT_IMAGES.put("cauliflower", "cauliflower.png");
        PRODUCT_IMAGES.put("celery root", "celery-root.png");
        PRODUCT_IMAGES.put("cherry", "cherry.png");

        PRODUCT_IMAGES.put("coconut", "coconut.png");
        PRODUCT_IMAGES.put("corn", "corn.png");
        PRODUCT_IMAGES.put("cucumber", "cucumber.png");
        PRODUCT_IMAGES.put("dragonfruit", "dragon-fruit.png");
        PRODUCT_IMAGES.put("dragon fruit", "dragon-fruit.png");
        PRODUCT_IMAGES.put("eggplant", "eggplant.png");

        PRODUCT_IMAGES.put("fennel", "fennel.png");
        PRODUCT_IMAGES.put("grape", "grape.png");
        PRODUCT_IMAGES.put("kiwi", "kiwi.png");
        PRODUCT_IMAGES.put("kohlrabi", "kohlrabi.png");
        PRODUCT_IMAGES.put("leek", "leek.png");
        PRODUCT_IMAGES.put("lemon", "lemon.png");
        PRODUCT_IMAGES.put("mandarin", "mandarin.png");
        PRODUCT_IMAGES.put("lettuce", "lettuce.png");
        PRODUCT_IMAGES.put("mango", "mango.png");
        PRODUCT_IMAGES.put("melon", "melon.png");
        PRODUCT_IMAGES.put("onion", "onion.png");
        PRODUCT_IMAGES.put("orange", "orange.png");
        PRODUCT_IMAGES.put("papaya", "papaya.png");
        PRODUCT_IMAGES.put("passion fruit", "passion-fruit.png");
        PRODUCT_IMAGES.put("passionfruit", "passion-fruit.png");
        PRODUCT_IMAGES.put("peach", "peach.png");
        PRODUCT_IMAGES.put("pear", "pear.png");
        PRODUCT_IMAGES.put("persimmon", "persimmon.png");
        PRODUCT_IMAGES.put("pineapple", "pineapple.png");
        PRODUCT_IMAGES.put("pepper", "pepper.png");
        PRODUCT_IMAGES.put("pomegranate", "pomegranate.png");
        PRODUCT_IMAGES.put("potato", "patato.png");
        PRODUCT_IMAGES.put("pumpkin", "pumpkin.png");
        PRODUCT_IMAGES.put("red cabbage", "red-cabbage.png");
        PRODUCT_IMAGES.put("redcabbage", "red-cabbage.png");
        PRODUCT_IMAGES.put("spinach", "spinach.png");
        PRODUCT_IMAGES.put("strawberry", "strawberry.png");
        PRODUCT_IMAGES.put("tomato", "tomato.png");
        PRODUCT_IMAGES.put("watermelon", "watermelon.png");
        PRODUCT_IMAGES.put("zucchini", "zucchini.png");
    }
    
    private ImageLoaderService() {
        this.db = DatabaseAdapter.getInstance();
    }
    
    /**
     * Gets the singleton instance.
     * 
     * @return ImageLoaderService instance
     */
    public static synchronized ImageLoaderService getInstance() {
        if (instance == null) {
            instance = new ImageLoaderService();
        }
        return instance;
    }
    
    /**
     * Loads all default product images into the database.
     * Only loads images for products that don't already have one.
     * This should be called on application startup.
     */
    public void loadDefaultImages() {
        if (imagesLoaded) {
            System.out.println("ImageLoaderService | Images already loaded this session");
            return;
        }
        
        System.out.println("ImageLoaderService | Loading default product images...");
        int loaded = 0;
        int skipped = 0;
        
        for (Map.Entry<String, String> entry : PRODUCT_IMAGES.entrySet()) {
            String productName = entry.getKey();
            String imageFileName = entry.getValue();
            
            try {
                // Check if product exists and needs an image
                if (productNeedsImage(productName)) {
                    byte[] imageData = loadImageFromResources(imageFileName);
                    
                    if (imageData != null && imageData.length > 0) {
                        if (updateProductImage(productName, imageData)) {
                            System.out.println("  ✓ Loaded image for: " + productName + 
                                             " (" + (imageData.length / 1024) + " KB)");
                            loaded++;
                        }
                    } else {
                        System.out.println("  ✗ Image file not found: " + imageFileName);
                    }
                } else {
                    skipped++;
                }
            } catch (Exception e) {
                System.err.println("  ✗ Error loading image for " + productName + ": " + e.getMessage());
            }
        }
        
        imagesLoaded = true;
        System.out.println("ImageLoaderService | Complete: " + loaded + " loaded, " + skipped + " skipped");
    }
    
    /**
     * Checks if a product exists and needs an image (has null image).
     * Checks all product tables: products, vegetables_piece, fruits_piece
     * 
     * @param productName Product name to check
     * @return true if product exists and has no image in any table
     */
    private boolean productNeedsImage(String productName) {
        // Check main products table
        if (checkTableNeedsImage("products", productName)) {
            return true;
        }
        // Check vegetables_piece table
        if (checkTableNeedsImage("vegetables_piece", productName)) {
            return true;
        }
        // Check fruits_piece table
        if (checkTableNeedsImage("fruits_piece", productName)) {
            return true;
        }
        return false;
    }
    
    /**
     * Checks if a specific table has a product that needs an image.
     */
    private boolean checkTableNeedsImage(String tableName, String productName) {
        String sql = "SELECT id, image FROM " + tableName + " WHERE LOWER(name) = LOWER(?)";
        ResultSet rs = null;
        
        try {
            rs = db.executeQuery(sql, productName);
            if (rs.next()) {
                byte[] existingImage = rs.getBytes("image");
                return existingImage == null || existingImage.length == 0;
            }
        } catch (SQLException e) {
            // Table might not have the product, that's okay
        } finally {
            DatabaseAdapter.closeResultSet(rs);
        }
        return false;
    }
    
    /**
     * Loads an image file from the resources/images folder.
     * 
     * @param fileName Image file name
     * @return byte array of image data, or null if not found
     */
    private byte[] loadImageFromResources(String fileName) {
        String resourcePath = "/images/" + fileName;
        
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is == null) {
                System.err.println("  Resource not found: " + resourcePath);
                return null;
            }
            return is.readAllBytes();
        } catch (Exception e) {
            System.err.println("Error reading image resource: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Updates a product's image in the database.
     * Updates all relevant tables: products, vegetables_piece, fruits_piece
     * 
     * @param productName Product name
     * @param imageData Image byte array
     * @return true if update successful in any table
     */
    private boolean updateProductImage(String productName, byte[] imageData) {
        int totalAffected = 0;
        
        // Update main products table
        totalAffected += updateTableImage("products", productName, imageData);
        
        // Update vegetables_piece table
        totalAffected += updateTableImage("vegetables_piece", productName, imageData);
        
        // Update fruits_piece table
        totalAffected += updateTableImage("fruits_piece", productName, imageData);
        
        return totalAffected > 0;
    }
    
    /**
     * Updates a specific table with the product image.
     */
    private int updateTableImage(String tableName, String productName, byte[] imageData) {
        String sql = "UPDATE " + tableName + " SET image = ? WHERE LOWER(name) = LOWER(?)";
        
        try {
            return db.executeUpdate(sql, imageData, productName);
        } catch (SQLException e) {
            // Table might not have the product, that's okay
            return 0;
        }
    }
    
    /**
     * Forces reload of all images, even if they already exist.
     * Use with caution - will overwrite existing images.
     */
    public void forceReloadAllImages() {
        System.out.println("ImageLoaderService | Force reloading all images...");
        int loaded = 0;
        
        for (Map.Entry<String, String> entry : PRODUCT_IMAGES.entrySet()) {
            String productName = entry.getKey();
            String imageFileName = entry.getValue();
            
            try {
                byte[] imageData = loadImageFromResources(imageFileName);
                
                if (imageData != null && imageData.length > 0) {
                    if (updateProductImage(productName, imageData)) {
                        System.out.println("  ✓ Reloaded image for: " + productName);
                        loaded++;
                    }
                }
            } catch (Exception e) {
                System.err.println("  ✗ Error reloading image for " + productName + ": " + e.getMessage());
            }
        }
        
        System.out.println("ImageLoaderService | Force reload complete: " + loaded + " images updated");
    }
    
    /**
     * Adds a new product-image mapping at runtime.
     * 
     * @param productName Product name (case-insensitive)
     * @param imageFileName Image file name in resources/images folder
     */
    public void registerProductImage(String productName, String imageFileName) {
        PRODUCT_IMAGES.put(productName.toLowerCase(), imageFileName);
    }
    
    /**
     * Gets the number of registered product-image mappings.
     * 
     * @return count of mappings
     */
    public int getRegisteredImageCount() {
        return PRODUCT_IMAGES.size();
    }
}

