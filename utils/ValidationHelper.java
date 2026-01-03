package com.greengrocer.utils;

import java.util.regex.Pattern;

/**
 * utility class for input validation.
 * 
 * @author Samira Çeçen
 * @since 24-12-2025
 */
public class ValidationHelper {

    /** Email pattern */
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );
    
    /** Phone pattern (international format) */
    private static final Pattern PHONE_PATTERN = 
        Pattern.compile("^\\+?\\d{1,4}[\\s-]?\\(?\\d{1,4}\\)?[\\s-]?\\d{1,4}[\\s-]?\\d{1,4}[\\s-]?\\d{0,4}$");

    /**
     * validates that a string is not null or empty.
     * 
     * @param value string to validate
     * @return true if not null/empty
     */
    public static boolean isNotEmpty(String value) {
        return value != null && !value.trim().isEmpty();
    }

    /**
     * validates that a string is null or empty.
     * 
     * @param value string to validate
     * @return true if null or empty
     */
    public static boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }

    /**
     * validates an email address format.
     * 
     * @param email email to validate
     * @return true if valid email format
     */
    public static boolean isValidEmail(String email) {
        // Check not empty
        // Match against email pattern
        if (isEmpty(email)) return false;
        return EMAIL_PATTERN.matcher(email).matches();
    }

    /**
     * validates a phone number format.
     * 
     * @param phone phone number to validate
     * @return true if valid phone format
     */
    public static boolean isValidPhone(String phone) {
        // Check not empty
        // Match against phone pattern (pattern already allows spaces, dashes, parentheses)
        if (isEmpty(phone)) return false;
        return PHONE_PATTERN.matcher(phone.trim()).matches();
    }

    /**
     * validates a positive number.
     * 
     * @param value number to validate
     * @return true if positive (> 0)
     */
    public static boolean isPositive(double value) {
        return value > 0;
    }

    /**
     * validates a non-negative number.
     * 
     * @param value number to validate
     * @return true if non-negative (>= 0)
     */
    public static boolean isNonNegative(double value) {
        return value >= 0;
    }

    /**
     * validates a positive integer.
     * 
     * @param value number to validate
     * @return true if positive (> 0)
     */
    public static boolean isPositive(int value) {
        return value > 0;
    }

    /**
     * parses a string to double, returning default on failure.
     * 
     * @param value string to parse
     * @param defaultValue default value if parsing fails
     * @return parsed double or default
     */
    public static double parseDouble(String value, double defaultValue) {
        // Try to parse string
        // Return default on exception
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * parses a string to int, returning default on failure.
     * 
     * @param value string to parse
     * @param defaultValue default value if parsing fails
     * @return parsed int or default
     */
    public static int parseInt(String value, int defaultValue) {
        // Try to parse string
        // Return default on exception
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * validates a double string can be parsed.
     * 
     * @param value string to validate
     * @return true if valid double format
     */
    public static boolean isValidDouble(String value) {
        // Try to parse
        // Return true if successful
        if (isEmpty(value)) return false;
        try {
            Double.parseDouble(value.trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * validates password meets minimum requirements.
     * 
     * @param password password to validate
     * @param minLength minimum length required
     * @return true if password meets requirements
     */
    public static boolean isValidPassword(String password, int minLength) {
        // Check not empty
        // Check meets minimum length
        if (isEmpty(password)) return false;
        return password.length() >= minLength;
    }

    /**
     * validates username format.
     * 
     * @param username username to validate
     * @return true if valid username format
     */
    public static boolean isValidUsername(String username) {
        // Check not empty
        // Check length (3-50 characters)
        // Check alphanumeric (letters, numbers, underscore)
        if (isEmpty(username)) return false;
        if (username.length() < 3 || username.length() > 50) return false;
        return username.matches("^[a-zA-Z0-9_]+$");
    }

    /**
     * validates that value is within range.
     * 
     * @param value value to validate
     * @param min minimum value (inclusive)
     * @param max maximum value (inclusive)
     * @return true if within range
     */
    public static boolean isInRange(double value, double min, double max) {
        return value >= min && value <= max;
    }

    /**
     * validates that value is within range.
     * 
     * @param value value to validate
     * @param min minimum value (inclusive)
     * @param max maximum value (inclusive)
     * @return true if within range
     */
    public static boolean isInRange(int value, int min, int max) {
        return value >= min && value <= max;
    }
}

