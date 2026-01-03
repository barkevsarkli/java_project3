package com.greengrocer.dao;

import com.greengrocer.models.LoyaltySettings;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * data access object for loyalty settings.
 * 
 * @author Samira Çeçen
 * @since 24-12-2025
 */
public class LoyaltySettingsDAO
{
    private final DatabaseAdapter db;

    public LoyaltySettingsDAO()
    {
        this.db = DatabaseAdapter.getInstance();
    }

    /**
     * get the loyalty settings
     * @return LoyaltySettings object or null
     * @author Samira Çeçen
     * @version 1.0
     */
    public LoyaltySettings getSettings()
    {
        String sql = "SELECT * FROM loyalty_settings LIMIT 1";
        ResultSet rs = null;
        
        try
        {
            rs = db.executeQuery(sql);
            if (rs.next())
                return mapResultSetToSettings(rs);
        }
        catch (SQLException e)
        {
            System.err.println("Error getting loyalty settings: " + e.getMessage());
        }
        finally
        {
            DatabaseAdapter.closeResultSet(rs);
        }
        return null;
    }

    /**
     * update the loyalty settings
     * @param settings settings to update
     * @return true if successful
     * @author Samira Çeçen
     * @version 1.0
     */
    public boolean updateSettings(LoyaltySettings settings)
    {
        String sql = "UPDATE loyalty_settings SET " +
                     "tier1_threshold = ?, tier1_discount = ?, " +
                     "tier2_threshold = ?, tier2_discount = ?, " +
                     "tier3_threshold = ?, tier3_discount = ?, " +
                     "points_per_tl = ?, points_for_coupon = ?, coupon_value = ? " +
                     "WHERE id = ?";
        
        try
        {
            int affected = db.executeUpdate(sql,
                    settings.getTier1Threshold(),
                    settings.getTier1Discount(),
                    settings.getTier2Threshold(),
                    settings.getTier2Discount(),
                    settings.getTier3Threshold(),
                    settings.getTier3Discount(),
                    settings.getPointsPerTl(),
                    settings.getPointsForCoupon(),
                    settings.getCouponValue(),
                    settings.getId());
            
            return affected > 0;
        }
        catch (SQLException e)
        {
            System.err.println("Error updating loyalty settings: " + e.getMessage());
            return false;
        }
    }

    /**
     * create initial settings if none exist
     * @param settings default settings
     * @return created settings or null
     * @author Samira Çeçen
     * @version 1.0
     */
    public LoyaltySettings createIfNotExists(LoyaltySettings settings)
    {
        if (getSettings() != null)
            return getSettings();
        
        String sql = "INSERT INTO loyalty_settings " +
                     "(tier1_threshold, tier1_discount, tier2_threshold, tier2_discount, " +
                     "tier3_threshold, tier3_discount, points_per_tl, points_for_coupon, coupon_value) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try
        {
            int id = db.executeInsertAndGetKey(sql,
                    settings.getTier1Threshold(),
                    settings.getTier1Discount(),
                    settings.getTier2Threshold(),
                    settings.getTier2Discount(),
                    settings.getTier3Threshold(),
                    settings.getTier3Discount(),
                    settings.getPointsPerTl(),
                    settings.getPointsForCoupon(),
                    settings.getCouponValue());
            
            if (id > 0)
            {
                settings.setId(id);
                return settings;
            }
        }
        catch (SQLException e)
        {
            System.err.println("Error creating loyalty settings: " + e.getMessage());
        }
        return null;
    }

    private LoyaltySettings mapResultSetToSettings(ResultSet rs) throws SQLException
    {
        LoyaltySettings settings = new LoyaltySettings();
        settings.setId(rs.getInt("id"));
        settings.setTier1Threshold(rs.getInt("tier1_threshold"));
        settings.setTier1Discount(rs.getDouble("tier1_discount"));
        settings.setTier2Threshold(rs.getInt("tier2_threshold"));
        settings.setTier2Discount(rs.getDouble("tier2_discount"));
        settings.setTier3Threshold(rs.getInt("tier3_threshold"));
        settings.setTier3Discount(rs.getDouble("tier3_discount"));
        settings.setPointsPerTl(rs.getDouble("points_per_tl"));
        settings.setPointsForCoupon(rs.getInt("points_for_coupon"));
        settings.setCouponValue(rs.getDouble("coupon_value"));
        return settings;
    }
}
