package com.greengrocer.utils;

import java.util.regex.Pattern;

/**
 * utility class for validating turkish phone numbers.
 * 
 * @author Barkev Şarklı
 * @since 23-12-2025
 */
public class PhoneValidator {
    
    // Turkish mobile number pattern
    // Format: 05334589243 (11 digits starting with 0, no spaces)
    private static final Pattern TURKISH_MOBILE_PATTERN = 
        Pattern.compile("^0\\d{10}$");
    
    /**
     * validates if the phone number is a valid turkish mobile number.
     * format: 05334589243 (11 digits starting with 0, no spaces)
     * 
     * @param phoneNumber phone number to validate
     * @return true if valid turkish mobile format
     */
    public static boolean isValidTurkishMobile(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return false;
        }
        
        // Remove any spaces
        String cleaned = phoneNumber.replaceAll("\\s+", "");
        
        // Check format: 11 digits starting with 0
        return TURKISH_MOBILE_PATTERN.matcher(cleaned).matches();
    }
    
    /**
     * formats a turkish phone number to standard format: 05334589243, removes spaces and ensures it's in the correct format.
     * 
     * @param phoneNumber phone number to format
     * @return formatted phone number (11 digits starting with 0, no spaces) or original if invalid
     */
    public static String formatTurkishMobile(String phoneNumber) {
        if (phoneNumber == null) {
            return null;
        }
        
        // Extract only digits and remove spaces
        String digits = phoneNumber.replaceAll("[^0-9]", "");
        
        // If it starts with 90, remove it (international format)
        if (digits.startsWith("90") && digits.length() == 12) {
            digits = "0" + digits.substring(2);
        }
        
        // Should be 11 digits starting with 0
        if (digits.length() == 11 && digits.startsWith("0")) {
            return digits;
        }
        
        return phoneNumber; // Return original if can't format
    }
    
    /**
     * gets a user-friendly error message for invalid phone numbers.
     * 
     * @param phoneNumber the phone number that failed validation
     * @return error message explaining the correct format
     */
    public static String getValidationErrorMessage(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return "Phone number is required";
        }
        
        return "Phone number must be in format '05334589243' (11 digits starting with 0, no spaces allowed)";
    }
    
    /**
     * extracts clean digits from phone number (removes all non-digit characters).
     * 
     * @param phoneNumber phone number
     * @return only the digits
     */
    public static String getDigitsOnly(String phoneNumber) {
        if (phoneNumber == null) {
            return "";
        }
        return phoneNumber.replaceAll("[^0-9]", "");
    }
}

