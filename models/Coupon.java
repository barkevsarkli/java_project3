package com.greengrocer.models;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * represents a discount coupon in the system.
 * 
 * @author Samira Çeçen
 * @since 24-12-2025
 */
public class Coupon {

    /** Unique identifier */
    private int id;
    
    /** Coupon code string */
    private String code;
    
    /** Discount percentage (0-100) */
    private double discountPercentage;
    
    /** User ID who owns this coupon (null for general coupons) */
    private Integer userId;
    
    /** Whether coupon has been used */
    private boolean isUsed;
    
    /** Date when coupon was created */
    private LocalDate createdDate;
    
    /** Expiration date */
    private LocalDate expirationDate;
    
    /** Minimum order value required to use coupon */
    private double minimumOrderValue;
    
    /** Maximum discount amount (cap) */
    private double maximumDiscount;

    /**
     * default constructor, initializes with default values.
     */
    public Coupon() {
        this.isUsed = false;
        this.createdDate = LocalDate.now();
        this.minimumOrderValue = 0;
        this.maximumDiscount = 0;
    }

    /**
     * constructor for creating new coupon.
     * 
     * @param code coupon code
     * @param discountPercentage discount percentage
     * @param expirationDate expiration date
     */
    public Coupon(String code, double discountPercentage, LocalDate expirationDate) {
        this();
        this.code = code;
        this.discountPercentage = discountPercentage;
        this.expirationDate = expirationDate;
    }

    /**
     * full constructor with all fields.
     * 
     * @param code coupon code
     * @param discountPercentage discount percentage
     * @param expirationDate expiration date
     * @param minimumOrderValue minimum order value
     * @param maximumDiscount maximum discount cap
     */
    public Coupon(String code, double discountPercentage, LocalDate expirationDate,
                  double minimumOrderValue, double maximumDiscount) {
        this(code, discountPercentage, expirationDate);
        this.minimumOrderValue = minimumOrderValue;
        this.maximumDiscount = maximumDiscount;
    }

    // ==================== GETTERS ====================

    /**
     * @return The coupon ID
     */
    public int getId() {
        return id;
    }

    /**
     * @return The coupon code
     */
    public String getCode() {
        return code;
    }

    /**
     * @return The discount percentage
     */
    public double getDiscountPercentage() {
        return discountPercentage;
    }

    /**
     * @return The user ID or null
     */
    public Integer getUserId() {
        return userId;
    }

    /**
     * @return Whether coupon is used
     */
    public boolean isUsed() {
        return isUsed;
    }

    /**
     * @return The created date
     */
    public LocalDate getCreatedDate() {
        return createdDate;
    }

    /**
     * @return The expiration date
     */
    public LocalDate getExpirationDate() {
        return expirationDate;
    }

    /**
     * @return The minimum order value
     */
    public double getMinimumOrderValue() {
        return minimumOrderValue;
    }

    /**
     * @return The maximum discount
     */
    public double getMaximumDiscount() {
        return maximumDiscount;
    }

    // ==================== SETTERS ====================

    /**
     * @param id The coupon ID to set
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * @param code The coupon code to set
     */
    public void setCode(String code) {
        this.code = code;
    }

    /**
     * @param discountPercentage The discount percentage to set
     */
    public void setDiscountPercentage(double discountPercentage) {
        this.discountPercentage = discountPercentage;
    }

    /**
     * @param userId The user ID to set
     */
    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    /**
     * @param used Whether coupon is used
     */
    public void setUsed(boolean used) {
        isUsed = used;
    }

    /**
     * @param createdDate The created date to set
     */
    public void setCreatedDate(LocalDate createdDate) {
        this.createdDate = createdDate;
    }

    /**
     * @param expirationDate The expiration date to set
     */
    public void setExpirationDate(LocalDate expirationDate) {
        this.expirationDate = expirationDate;
    }

    /**
     * @param minimumOrderValue The minimum order value to set
     */
    public void setMinimumOrderValue(double minimumOrderValue) {
        this.minimumOrderValue = minimumOrderValue;
    }

    /**
     * @param maximumDiscount The maximum discount to set
     */
    public void setMaximumDiscount(double maximumDiscount) {
        this.maximumDiscount = maximumDiscount;
    }

    // ==================== BUSINESS METHODS ====================

    /**
     * checks if coupon is valid for use.
     * 
     * @return true if coupon is valid
     */
    public boolean isValid() {
        return !isUsed && !isExpired();
    }

    /**
     * checks if coupon is expired.
     * 
     * @return true if expired
     */
    public boolean isExpired() {
        if (expirationDate == null) {
            return false;
        }
        return LocalDate.now().isAfter(expirationDate);
    }

    /**
     * checks if coupon expires today.
     * 
     * @return true if expiring today
     */
    public boolean isExpiringToday() {
        if (expirationDate == null) {
            return false;
        }
        return LocalDate.now().equals(expirationDate);
    }

    /**
     * checks if coupon can be applied to given order value.
     * 
     * @param orderValue the order subtotal
     * @return true if coupon can be applied
     */
    public boolean canApplyTo(double orderValue) {
        if (!isValid()) {
            return false;
        }
        return orderValue >= minimumOrderValue;
    }

    /**
     * calculates discount amount for given order value, respects maximum discount cap.
     * 
     * @param orderValue the order subtotal
     * @return discount amount
     */
    public double calculateDiscount(double orderValue) {
        // Check if coupon can be applied
        if (!canApplyTo(orderValue)) {
            return 0.0;
        }
        
        // Calculate percentage discount
        double discount = orderValue * (discountPercentage / 100.0);
        
        // Apply maximum discount cap if set
        if (maximumDiscount > 0 && discount > maximumDiscount) {
            discount = maximumDiscount;
        }
        
        // Round to 2 decimal places
        return Math.round(discount * 100.0) / 100.0;
    }

    /**
     * marks coupon as used.
     */
    public void markAsUsed() {
        this.isUsed = true;
    }

    /**
     * marks coupon as unused (for order cancellation).
     */
    public void markAsUnused() {
        this.isUsed = false;
    }

    /**
     * gets days until expiration.
     * 
     * @return days remaining, negative if expired, 0 if expires today
     */
    public long getDaysUntilExpiration() {
        if (expirationDate == null) {
            return Long.MAX_VALUE; // No expiration
        }
        return ChronoUnit.DAYS.between(LocalDate.now(), expirationDate);
    }

    /**
     * checks if coupon is expiring soon (within specified days).
     * 
     * @param days number of days to check
     * @return true if expiring within specified days
     */
    public boolean isExpiringSoon(int days) {
        long daysUntil = getDaysUntilExpiration();
        return daysUntil >= 0 && daysUntil <= days;
    }

    /**
     * checks if this is a personal coupon (assigned to a specific user).
     * 
     * @return true if personal coupon
     */
    public boolean isPersonalCoupon() {
        return userId != null;
    }

    /**
     * checks if this is a general coupon (available to all users).
     * 
     * @return true if general coupon
     */
    public boolean isGeneralCoupon() {
        return userId == null;
    }

    /**
     * gets a formatted string describing the discount.
     * 
     * @return formatted discount description
     */
    public String getDiscountDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%.0f%% off", discountPercentage));
        
        if (maximumDiscount > 0) {
            sb.append(String.format(" (max %.2f TL)", maximumDiscount));
        }
        
        if (minimumOrderValue > 0) {
            sb.append(String.format(" on orders over %.2f TL", minimumOrderValue));
        }
        
        return sb.toString();
    }

    /**
     * gets the status of the coupon as a string.
     * 
     * @return status string
     */
    public String getStatus() {
        if (isUsed) {
            return "Used";
        } else if (isExpired()) {
            return "Expired";
        } else if (isExpiringToday()) {
            return "Expires Today!";
        } else if (isExpiringSoon(3)) {
            return "Expiring Soon";
        } else {
            return "Active";
        }
    }

    /**
     * gets formatted expiration info.
     * 
     * @return expiration info string
     */
    public String getExpirationInfo() {
        if (expirationDate == null) {
            return "No expiration";
        }
        
        long days = getDaysUntilExpiration();
        
        if (days < 0) {
            return "Expired " + Math.abs(days) + " days ago";
        } else if (days == 0) {
            return "Expires today!";
        } else if (days == 1) {
            return "Expires tomorrow";
        } else {
            return "Expires in " + days + " days";
        }
    }

    @Override
    public String toString() {
        return String.format("Coupon{code='%s', discount=%.0f%%, status=%s, expires=%s}",
                code, discountPercentage, getStatus(), expirationDate);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Coupon coupon = (Coupon) obj;
        return id == coupon.id || (code != null && code.equals(coupon.code));
    }

    @Override
    public int hashCode() {
        return code != null ? code.hashCode() : id;
    }
}
