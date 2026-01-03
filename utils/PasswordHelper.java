package com.greengrocer.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * utility class for password hashing and verification.
 * 
 * @author Barkev Şarklı
 * @since 23-12-2025
 */
public class PasswordHelper {

    /** Salt length in bytes */
    private static final int SALT_LENGTH = 16;
    
    /** Hash algorithm */
    private static final String HASH_ALGORITHM = "SHA-256";
    
    /** Separator between salt and hash */
    private static final String SEPARATOR = ":";

    /**
     * private constructor to prevent instantiation.
     */
    private PasswordHelper() {
        // Utility class
    }

    /**
     * hashes a password with a random salt.
     * 
     * @param password plain text password
     * @return salted hash in format "salt:hash"
     */
    public static String hashPassword(String password) {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
        
        try {
            // Generate random salt
            SecureRandom random = new SecureRandom();
            byte[] salt = new byte[SALT_LENGTH];
            random.nextBytes(salt);
            
            // Hash password with salt
            byte[] hash = hashWithSalt(password, salt);
            
            // Encode and combine
            String saltBase64 = Base64.getEncoder().encodeToString(salt);
            String hashBase64 = Base64.getEncoder().encodeToString(hash);
            
            return saltBase64 + SEPARATOR + hashBase64;
            
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Hash algorithm not available", e);
        }
    }

    /**
     * verifies a password against a stored hash.
     * 
     * @param password plain text password to verify
     * @param storedHash stored salted hash
     * @return true if password matches
     */
    public static boolean verifyPassword(String password, String storedHash) {
        if (password == null || storedHash == null) {
            return false;
        }
        
        // Handle legacy plain text passwords (for migration)
        if (!storedHash.contains(SEPARATOR)) {
            // Old format - plain text comparison
            return password.equals(storedHash);
        }
        
        try {
            // Split salt and hash
            String[] parts = storedHash.split(SEPARATOR);
            if (parts.length != 2) {
                return false;
            }
            
            byte[] salt = Base64.getDecoder().decode(parts[0]);
            byte[] expectedHash = Base64.getDecoder().decode(parts[1]);
            
            // Hash provided password with same salt
            byte[] actualHash = hashWithSalt(password, salt);
            
            // Compare hashes
            return MessageDigest.isEqual(expectedHash, actualHash);
            
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * hashes password with given salt using sha-256.
     * 
     * @param password password to hash
     * @param salt salt bytes
     * @return hash bytes
     */
    private static byte[] hashWithSalt(String password, byte[] salt) 
            throws NoSuchAlgorithmException {
        
        MessageDigest md = MessageDigest.getInstance(HASH_ALGORITHM);
        md.update(salt);
        return md.digest(password.getBytes());
    }

    /**
     * checks if a stored password is hashed or plain text.
     * 
     * @param storedPassword stored password
     * @return true if hashed
     */
    public static boolean isHashed(String storedPassword) {
        return storedPassword != null && storedPassword.contains(SEPARATOR);
    }

    /**
     * simple hash for non-security purposes (e.g., checksums), do not use for password storage.
     * 
     * @param input input string
     * @return hex string hash
     */
    public static String simpleHash(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] hash = md.digest(input.getBytes());
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
            
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Hash algorithm not available", e);
        }
    }

    /**
     * validates password strength.
     * 
     * @param password password to validate
     * @return true if password meets minimum requirements
     */
    public static boolean isStrongPassword(String password) {
        if (password == null || password.length() < 6) {
            return false;
        }
        
        boolean hasLetter = false;
        boolean hasDigit = false;
        
        for (char c : password.toCharArray()) {
            if (Character.isLetter(c)) hasLetter = true;
            if (Character.isDigit(c)) hasDigit = true;
        }
        
        return hasLetter && hasDigit;
    }

    /**
     * gets password strength message.
     * 
     * @param password password to check
     * @return strength description
     */
    public static String getPasswordStrength(String password) {
        if (password == null || password.length() < 6) {
            return "Too short (minimum 6 characters)";
        }
        
        int score = 0;
        
        // Length score
        if (password.length() >= 8) score++;
        if (password.length() >= 12) score++;
        
        // Character variety
        if (password.matches(".*[a-z].*")) score++;
        if (password.matches(".*[A-Z].*")) score++;
        if (password.matches(".*\\d.*")) score++;
        if (password.matches(".*[!@#$%^&*(),.?\":{}|<>].*")) score++;
        
        if (score <= 2) return "Weak";
        if (score <= 4) return "Medium";
        return "Strong";
    }
}

