package com.greengrocer.models;

/**
 * represents loyalty program settings with discount tiers.
 * 
 * @author Samira Çeçen
 * @since 24-12-2025
 */
public class LoyaltySettings
{
    private int id;
    private int tier1Threshold;
    private double tier1Discount;
    private int tier2Threshold;
    private double tier2Discount;
    private int tier3Threshold;
    private double tier3Discount;
    private double pointsPerTl;
    private int pointsForCoupon;
    private double couponValue;

    public LoyaltySettings()
    {
        this.tier1Threshold = 5;
        this.tier1Discount = 5.0;
        this.tier2Threshold = 10;
        this.tier2Discount = 10.0;
        this.tier3Threshold = 20;
        this.tier3Discount = 15.0;
        this.pointsPerTl = 1.0;
        this.pointsForCoupon = 100;
        this.couponValue = 10.0;
    }

    public LoyaltySettings(int id, int tier1Threshold, double tier1Discount,
                           int tier2Threshold, double tier2Discount,
                           int tier3Threshold, double tier3Discount)
    {
        this.id = id;
        this.tier1Threshold = tier1Threshold;
        this.tier1Discount = tier1Discount;
        this.tier2Threshold = tier2Threshold;
        this.tier2Discount = tier2Discount;
        this.tier3Threshold = tier3Threshold;
        this.tier3Discount = tier3Discount;
    }

    // ==================== GETTERS ====================

    public int getId()
    {
        return id;
    }

    public int getTier1Threshold()
    {
        return tier1Threshold;
    }

    public double getTier1Discount()
    {
        return tier1Discount;
    }

    public int getTier2Threshold()
    {
        return tier2Threshold;
    }

    public double getTier2Discount()
    {
        return tier2Discount;
    }

    public int getTier3Threshold()
    {
        return tier3Threshold;
    }

    public double getTier3Discount()
    {
        return tier3Discount;
    }

    public double getPointsPerTl()
    {
        return pointsPerTl;
    }

    public int getPointsForCoupon()
    {
        return pointsForCoupon;
    }

    public double getCouponValue()
    {
        return couponValue;
    }

    // ==================== SETTERS ====================

    public void setId(int id)
    {
        this.id = id;
    }

    public void setTier1Threshold(int tier1Threshold)
    {
        this.tier1Threshold = tier1Threshold;
    }

    public void setTier1Discount(double tier1Discount)
    {
        this.tier1Discount = tier1Discount;
    }

    public void setTier2Threshold(int tier2Threshold)
    {
        this.tier2Threshold = tier2Threshold;
    }

    public void setTier2Discount(double tier2Discount)
    {
        this.tier2Discount = tier2Discount;
    }

    public void setTier3Threshold(int tier3Threshold)
    {
        this.tier3Threshold = tier3Threshold;
    }

    public void setTier3Discount(double tier3Discount)
    {
        this.tier3Discount = tier3Discount;
    }

    public void setPointsPerTl(double pointsPerTl)
    {
        this.pointsPerTl = pointsPerTl;
    }

    public void setPointsForCoupon(int pointsForCoupon)
    {
        this.pointsForCoupon = pointsForCoupon;
    }

    public void setCouponValue(double couponValue)
    {
        this.couponValue = couponValue;
    }

    // ==================== BUSINESS LOGIC ====================

    /**
     * gets the discount for a given number of completed orders.
     * 
     * @param completedOrders number of completed orders
     * @return discount percentage
     */
    public double getDiscountForOrders(int completedOrders)
    {
        if (completedOrders >= tier3Threshold)
            return tier3Discount;
        else if (completedOrders >= tier2Threshold)
            return tier2Discount;
        else if (completedOrders >= tier1Threshold)
            return tier1Discount;

        return 0;
    }

    /**
     * gets the tier name for a given number of completed orders.
     * 
     * @param completedOrders number of completed orders
     * @return tier name
     */
    public String getTierName(int completedOrders)
    {
        if (completedOrders >= tier3Threshold)
            return "Gold";
        else if (completedOrders >= tier2Threshold)
            return "Silver";
        else if (completedOrders >= tier1Threshold)
            return "Bronze";

        return "Standard";
    }

    /**
     * calculates points earned for a purchase.
     * 
     * @param purchaseAmount purchase amount in tl
     * @return points earned
     */
    public int calculatePointsEarned(double purchaseAmount)
    {
        return (int) (purchaseAmount * pointsPerTl);
    }

    /**
     * checks if points are sufficient for a coupon.
     * 
     * @param points current points
     * @return true if enough for a coupon
     */
    public boolean canGenerateCoupon(int points)
    {
        return points >= pointsForCoupon;
    }

    @Override
    public String toString()
    {
        return "LoyaltySettings{" +
               "tier1=" + tier1Threshold + "/" + tier1Discount + "%, " +
               "tier2=" + tier2Threshold + "/" + tier2Discount + "%, " +
               "tier3=" + tier3Threshold + "/" + tier3Discount + "%}";
    }
}
