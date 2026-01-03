package com.greengrocer.services;

import com.greengrocer.dao.CouponDAO;
import com.greengrocer.models.Coupon;
import com.greengrocer.app.SessionManager;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * service class for coupon operations.
 * 
 * @author Samira Çeçen
 * @since 24-12-2025
 */
public class CouponService {

    /** Coupon data access object */
    private final CouponDAO couponDAO;

    /**
     * Constructor.
     */
    public CouponService() {
        // Initialize CouponDAO
        this.couponDAO = new CouponDAO();
    }

    /**
     * Validates and retrieves a coupon by code.
     * 
     * @param couponCode Coupon code to validate
     * @return Coupon if valid, null otherwise
     */
    public Coupon validateCoupon(String couponCode) {
        // Get current user ID
        // Call couponDAO.validateCoupon()
        // Return coupon or null
        int userId = SessionManager.getInstance().getCurrentUser().getId();
        try {
            Optional<Coupon> couponOpt = couponDAO.validateCoupon(couponCode, userId);
            return couponOpt.orElse(null);
        } catch (SQLException e) {
            return null;
        }
    }

    /**
     * Gets available coupons for current user.
     * 
     * @return List of user's unused coupons
     */
    public List<Coupon> getAvailableCoupons() {
        // Get current user
        // Call couponDAO.findAvailableForUser()
        // Return list
        int userId = SessionManager.getInstance().getCurrentUser().getId();
        try {
            return couponDAO.findAvailableForUser(userId);
        } catch (SQLException e) {
            return new ArrayList<>();
        }
    }

    /**
     * Marks a coupon as used after checkout.
     * 
     * @param couponCode Coupon code
     * @return true if marked successfully
     */
    public boolean markCouponUsed(String couponCode) {
        // Call couponDAO.markAsUsedByCode()
        // Return result
        try {
            return couponDAO.markAsUsedByCode(couponCode);
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Creates a new coupon (owner function).
     * 
     * @param code Coupon code
     * @param discountPercentage Discount percentage
     * @param expirationDate Expiration date
     * @param minimumOrderValue Minimum order value required
     * @param maximumDiscount Maximum discount amount (cap)
     * @param userId User ID (null for general coupon)
     * @return Created Coupon or null on failure
     * @throws IllegalArgumentException if validation fails
     */
    public Coupon createCoupon(String code, double discountPercentage, LocalDate expirationDate,
                               double minimumOrderValue, double maximumDiscount, Integer userId)
            throws IllegalArgumentException {
        // Validate code is not empty and unique
        // Validate discount percentage is 0-100
        // Validate expiration date is in future
        // Create Coupon object
        // Call couponDAO.insert()
        // Return created coupon
        
        if (code == null || code.trim().isEmpty()) {
            throw new IllegalArgumentException("Coupon code cannot be empty");
        }
        
        try {
            if (couponDAO.codeExists(code)) {
                throw new IllegalArgumentException("Coupon code already exists");
            }
        } catch (SQLException e) {
            throw new IllegalArgumentException("Error checking coupon code: " + e.getMessage());
        }
        
        if (discountPercentage <= 0 || discountPercentage > 100) {
            throw new IllegalArgumentException("Discount percentage must be between 0 and 100");
        }
        
        if (expirationDate == null || expirationDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Expiration date must be in the future");
        }
        
        if (minimumOrderValue < 0) {
            throw new IllegalArgumentException("Minimum order value cannot be negative");
        }
        
        Coupon coupon = new Coupon(code, discountPercentage, expirationDate);
        coupon.setMinimumOrderValue(minimumOrderValue);
        coupon.setMaximumDiscount(maximumDiscount);
        coupon.setUserId(userId);
        
        try {
            int id = couponDAO.insert(coupon);
            if (id > 0) {
                coupon.setId(id);
                return coupon;
            }
        } catch (SQLException e) {
            throw new IllegalArgumentException("Error creating coupon: " + e.getMessage());
        }
        
        return null;
    }

    /**
     * Updates a coupon (owner function).
     * 
     * @param coupon Coupon with updated fields
     * @return true if update successful
     */
    public boolean updateCoupon(Coupon coupon) {
        // Call couponDAO.update()
        // Return result
        try {
            return couponDAO.update(coupon);
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Deletes a coupon (owner function).
     * 
     * @param couponId Coupon ID
     * @return true if deletion successful
     */
    public boolean deleteCoupon(int couponId) {
        // Call couponDAO.delete()
        // Return result
        try {
            return couponDAO.delete(couponId);
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Gets all coupons (owner function).
     * 
     * @return List of all coupons
     */
    public List<Coupon> getAllCoupons() {
        // Call couponDAO.findAll()
        // Return list
        try {
            return couponDAO.findAll();
        } catch (SQLException e) {
            return new ArrayList<>();
        }
    }

    /**
     * Gets all active (non-expired) coupons.
     * 
     * @return List of active coupons
     */
    public List<Coupon> getActiveCoupons() {
        // Call couponDAO.findAllActive()
        // Return list
        try {
            return couponDAO.findAllActive();
        } catch (SQLException e) {
            return new ArrayList<>();
        }
    }

    /**
     * Checks if coupon code exists.
     * 
     * @param code Coupon code
     * @return true if code exists
     */
    public boolean codeExists(String code) {
        // Call couponDAO.codeExists()
        // Return result
        try {
            return couponDAO.codeExists(code);
        } catch (SQLException e) {
            return true; // Assume exists on error for safety
        }
    }

    /**
     * Generates a unique coupon code.
     * 
     * @return Generated unique code
     */
    public String generateUniqueCode() {
        // Generate random alphanumeric code
        // Check if exists, regenerate if needed
        // Return unique code
        String code;
        do {
            code = generateRandomCode();
        } while (codeExists(code));
        return code;
    }

    /**
     * Generates a random coupon code.
     * 
     * @return Random alphanumeric code
     */
    private String generateRandomCode() {
        // Generate 8-character alphanumeric code
        // Use combination of letters and numbers
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            int index = (int) (Math.random() * chars.length());
            code.append(chars.charAt(index));
        }
        return code.toString();
    }

    /**
     * Awards a coupon to user based on purchase amount.
     * 
     * @param userId User ID
     * @param purchaseAmount Purchase amount
     * @return Awarded Coupon or null if criteria not met
     */
    public Coupon awardCouponForPurchase(int userId, double purchaseAmount) {
        // Determine if user qualifies for coupon
        // Create coupon for user
        // Return coupon or null
        
        // Award 10% coupon for purchases over 500 TL
        if (purchaseAmount >= 500) {
            String code = generateUniqueCode();
            LocalDate expiration = LocalDate.now().plusMonths(3); // Valid for 3 months
            
            try {
                Coupon coupon = new Coupon(code, 10.0, expiration);
                coupon.setUserId(userId);
                coupon.setMinimumOrderValue(100.0);
                coupon.setMaximumDiscount(50.0);
                
                int id = couponDAO.insert(coupon);
                if (id > 0) {
                    coupon.setId(id);
                    return coupon;
                }
            } catch (SQLException e) {
                return null;
            }
        }
        
        return null;
    }

    /**
     * Calculates discount amount for order.
     * 
     * @param coupon Coupon to apply
     * @param orderValue Order subtotal
     * @return Discount amount
     */
    public double calculateDiscount(Coupon coupon, double orderValue) {
        // Check if coupon can be applied to order value
        // Calculate discount
        // Apply maximum cap if set
        // Return discount amount
        if (coupon == null || !coupon.canApplyTo(orderValue)) {
            return 0;
        }
        return coupon.calculateDiscount(orderValue);
    }
}

