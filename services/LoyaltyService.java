package com.greengrocer.services;

import com.greengrocer.app.SessionManager;
import com.greengrocer.dao.LoyaltySettingsDAO;
import com.greengrocer.dao.UserDAO;
import com.greengrocer.models.LoyaltySettings;
import com.greengrocer.models.User;

/**
 * service for loyalty program operations.
 * 
 * @author Samira Çeçen
 * @since 24-12-2025
 */
public class LoyaltyService {

    /** Loyalty settings DAO */
    private final LoyaltySettingsDAO settingsDAO;
    
    /** User DAO */
    private final UserDAO userDAO;
    
    /** Cached settings */
    private LoyaltySettings cachedSettings;

    /**
     * Constructor initializes DAOs.
     */
    public LoyaltyService() {
        this.settingsDAO = new LoyaltySettingsDAO();
        this.userDAO = new UserDAO();
    }

    /**
     * Gets the loyalty settings.
     * Uses caching to avoid frequent DB calls.
     * 
     * @return Loyalty settings
     */
    public LoyaltySettings getSettings() {
        if (cachedSettings == null) {
            cachedSettings = settingsDAO.getSettings();
            if (cachedSettings == null) {
                // Return default settings if none in DB
                cachedSettings = new LoyaltySettings();
            }
        }
        return cachedSettings;
    }

    /**
     * Updates loyalty settings.
     * 
     * @param settings New settings
     * @return true if successful
     */
    public boolean updateSettings(LoyaltySettings settings) {
        boolean success = settingsDAO.updateSettings(settings);
        if (success) {
            cachedSettings = settings;
        }
        return success;
    }

    /**
     * Clears cached settings (forces reload).
     */
    public void refreshSettings() {
        cachedSettings = null;
    }

    /**
     * Gets the current user's loyalty tier name.
     * 
     * @return Tier name
     */
    public String getCurrentUserTier() {
        User user = SessionManager.getInstance().getCurrentUser();
        if (user != null && user.isCustomer()) {
            return getSettings().getTierName(user.getCompletedTransactions());
        }
        return "Standard";
    }

    /**
     * Gets the current user's discount percentage.
     * 
     * @return Discount percentage
     */
    public double calculateCurrentUserDiscount() {
        User user = SessionManager.getInstance().getCurrentUser();
        if (user != null && user.isCustomer()) {
            return getSettings().getDiscountForOrders(user.getCompletedTransactions());
        }
        return 0;
    }

    /**
     * Gets discount for a specific user.
     * 
     * @param userId User ID
     * @return Discount percentage
     */
    public double getDiscountForUser(int userId) {
        User user = userDAO.findById(userId);
        if (user != null && user.isCustomer()) {
            return getSettings().getDiscountForOrders(user.getCompletedTransactions());
        }
        return 0;
    }

    /**
     * Gets tier name for a specific user.
     * 
     * @param userId User ID
     * @return Tier name
     */
    public String getTierForUser(int userId) {
        User user = userDAO.findById(userId);
        if (user != null && user.isCustomer()) {
            return getSettings().getTierName(user.getCompletedTransactions());
        }
        return "Standard";
    }

    /**
     * Calculates points earned for a purchase.
     * 
     * @param purchaseAmount Amount in TL
     * @return Points earned
     */
    public int calculatePointsEarned(double purchaseAmount) {
        return getSettings().calculatePointsEarned(purchaseAmount);
    }

    /**
     * Adds loyalty points to a user.
     * 
     * @param userId User ID
     * @param points Points to add
     * @return true if successful
     */
    public boolean addPointsToUser(int userId, int points) {
        return userDAO.addLoyaltyPoints(userId, points);
    }

    /**
     * Increments user's completed transactions.
     * 
     * @param userId User ID
     * @return true if successful
     */
    public boolean incrementUserTransactions(int userId) {
        return userDAO.incrementTransactions(userId);
    }

    /**
     * Gets orders needed to reach next tier.
     * 
     * @param completedOrders Current completed orders
     * @return Orders needed for next tier, or 0 if at max
     */
    public int getOrdersToNextTier(int completedOrders) {
        LoyaltySettings settings = getSettings();
        
        if (completedOrders < settings.getTier1Threshold()) {
            return settings.getTier1Threshold() - completedOrders;
        } else if (completedOrders < settings.getTier2Threshold()) {
            return settings.getTier2Threshold() - completedOrders;
        } else if (completedOrders < settings.getTier3Threshold()) {
            return settings.getTier3Threshold() - completedOrders;
        }
        return 0; // Already at max tier
    }

    /**
     * Gets progress info for current user.
     * 
     * @return Progress description
     */
    public String getCurrentUserProgress() {
        User user = SessionManager.getInstance().getCurrentUser();
        if (user == null || !user.isCustomer()) {
            return "";
        }
        
        String currentTier = getCurrentUserTier();
        int ordersToNext = getOrdersToNextTier(user.getCompletedTransactions());
        
        if (ordersToNext > 0) {
            LoyaltySettings settings = getSettings();
            String nextTier;
            if (user.getCompletedTransactions() < settings.getTier1Threshold()) {
                nextTier = "Bronze";
            } else if (user.getCompletedTransactions() < settings.getTier2Threshold()) {
                nextTier = "Silver";
            } else {
                nextTier = "Gold";
            }
            return String.format("%s tier • %d more order(s) to %s", 
                    currentTier, ordersToNext, nextTier);
        }
        return currentTier + " tier (Max)";
    }
}
