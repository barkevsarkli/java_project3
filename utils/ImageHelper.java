package com.greengrocer.utils;

import javafx.scene.image.Image;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * utility class for image handling.
 * 
 * @author Samira Çeçen
 * @since 24-12-2025
 */
public class ImageHelper {

    /** Default image width for product display */
    public static final int DEFAULT_WIDTH = 100;
    
    /** Default image height for product display */
    public static final int DEFAULT_HEIGHT = 100;

    /**
     * converts a file to byte array for database storage.
     * 
     * @param file image file
     * @return byte array of image data
     * @throws IOException if file cannot be read
     */
    public static byte[] fileToByteArray(File file) throws IOException {
        // Read file bytes using Files.readAllBytes
        // Return byte array
        return Files.readAllBytes(file.toPath());
    }

    /**
     * converts a path to byte array for database storage.
     * 
     * @param path path to image file
     * @return byte array of image data
     * @throws IOException if file cannot be read
     */
    public static byte[] pathToByteArray(Path path) throws IOException {
        // Read path bytes
        // Return byte array
        return Files.readAllBytes(path);
    }

    /**
     * converts byte array to javafx image.
     * 
     * @param imageData byte array of image data
     * @return javafx image or null if data is null/empty
     */
    public static Image byteArrayToImage(byte[] imageData) {
        // Check if data is null or empty
        // Create ByteArrayInputStream
        // Create Image from stream
        // Return image
        if (imageData == null || imageData.length == 0) {
            return null;
        }
        
        try (ByteArrayInputStream bis = new ByteArrayInputStream(imageData)) {
            return new Image(bis);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * converts byte array to javafx image with specified dimensions.
     * 
     * @param imageData byte array of image data
     * @param width desired width
     * @param height desired height
     * @param preserveRatio whether to preserve aspect ratio
     * @return javafx image or null if data is null/empty
     */
    public static Image byteArrayToImage(byte[] imageData, double width, double height, boolean preserveRatio) {
        // Check if data is null or empty
        // Create ByteArrayInputStream
        // Create Image with dimensions
        // Return image
        if (imageData == null || imageData.length == 0) {
            return null;
        }
        
        try (ByteArrayInputStream bis = new ByteArrayInputStream(imageData)) {
            return new Image(bis, width, height, preserveRatio, true);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * saves byte array to file.
     * 
     * @param imageData byte array of image data
     * @param destination destination file
     * @throws IOException if file cannot be written
     */
    public static void saveToFile(byte[] imageData, File destination) throws IOException {
        // Write bytes to file
        Files.write(destination.toPath(), imageData);
    }

    /**
     * gets a placeholder image for products without images.
     * 
     * @return default placeholder image
     */
    public static Image getPlaceholderImage() {
        // Try to load placeholder from resources
        // Return default image or null
        try {
            return new Image(ImageHelper.class.getResourceAsStream("/images/broccoli.jpg"));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * checks if file is a valid image format.
     * 
     * @param file file to check
     * @return true if valid image format
     */
    public static boolean isValidImageFile(File file) {
        // Check file extension
        // Supported: jpg, jpeg, png, gif
        if (file == null || !file.exists()) return false;
        
        String name = file.getName().toLowerCase();
        return name.endsWith(".jpg") || name.endsWith(".jpeg") ||
               name.endsWith(".png") || name.endsWith(".gif");
    }

    /**
     * gets image file extension.
     * 
     * @param file image file
     * @return extension (e.g., "jpg", "png")
     */
    public static String getExtension(File file) {
        // Extract extension from filename
        String name = file.getName();
        int lastDot = name.lastIndexOf('.');
        if (lastDot > 0) {
            return name.substring(lastDot + 1).toLowerCase();
        }
        return "";
    }
}

