package com.greengrocer.dao;

import com.greengrocer.models.Coupon;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * data access object for coupon entity.
 * 
 * @author Samira Çeçen
 * @since 24-12-2025
 */
public class CouponDAO {

    /** Database adapter reference */
    private final DatabaseAdapter dbAdapter;

    /**
     * Constructor.
     */
    public CouponDAO() {
        this.dbAdapter = DatabaseAdapter.getInstance();
    }

    /**
     * Finds a coupon by ID.
     * 
     * @param id Coupon ID
     * @return Optional containing Coupon if found
     * @throws SQLException if query fails
     */
    public Optional<Coupon> findById(int id) throws SQLException {
        String sql = "SELECT * FROM coupons WHERE id = ?";
        
        try (PreparedStatement ps = dbAdapter.getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                return Optional.of(mapResultSetToCoupon(rs));
            }
        }
        return Optional.empty();
    }

    /**
     * Finds a coupon by code.
     * 
     * @param code Coupon code
     * @return Optional containing Coupon if found
     * @throws SQLException if query fails
     */
    public Optional<Coupon> findByCode(String code) throws SQLException {
        String sql = "SELECT * FROM coupons WHERE code = ?";
        
        try (PreparedStatement ps = dbAdapter.getConnection().prepareStatement(sql)) {
            ps.setString(1, code);
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                return Optional.of(mapResultSetToCoupon(rs));
            }
        }
        return Optional.empty();
    }

    /**
     * Gets all coupons.
     * 
     * @return List of all coupons
     * @throws SQLException if query fails
     */
    public List<Coupon> findAll() throws SQLException {
        String sql = "SELECT * FROM coupons ORDER BY created_date DESC";
        List<Coupon> coupons = new ArrayList<>();
        
        try (PreparedStatement ps = dbAdapter.getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            
            while (rs.next()) {
                coupons.add(mapResultSetToCoupon(rs));
            }
        }
        return coupons;
    }

    /**
     * Gets available coupons for a user (including general coupons).
     * 
     * @param userId User ID
     * @return List of user's unused coupons
     * @throws SQLException if query fails
     */
    public List<Coupon> findAvailableForUser(int userId) throws SQLException {
        String sql = "SELECT * FROM coupons " +
                     "WHERE (user_id = ? OR user_id IS NULL) " +
                     "AND is_used = FALSE " +
                     "AND expiration_date >= CURRENT_DATE " +
                     "ORDER BY expiration_date ASC";
        
        List<Coupon> coupons = new ArrayList<>();
        
        try (PreparedStatement ps = dbAdapter.getConnection().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                coupons.add(mapResultSetToCoupon(rs));
            }
        }
        return coupons;
    }

    /**
     * Gets all active (non-expired, unused) coupons.
     * 
     * @return List of active coupons
     * @throws SQLException if query fails
     */
    public List<Coupon> findAllActive() throws SQLException {
        String sql = "SELECT * FROM coupons " +
                     "WHERE is_used = FALSE " +
                     "AND expiration_date >= CURRENT_DATE " +
                     "ORDER BY expiration_date ASC";
        
        List<Coupon> coupons = new ArrayList<>();
        
        try (PreparedStatement ps = dbAdapter.getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            
            while (rs.next()) {
                coupons.add(mapResultSetToCoupon(rs));
            }
        }
        return coupons;
    }

    /**
     * Inserts a new coupon.
     * 
     * @param coupon Coupon to insert
     * @return Generated coupon ID
     * @throws SQLException if insert fails
     */
    public int insert(Coupon coupon) throws SQLException {
        String sql = "INSERT INTO coupons (code, discount_percentage, user_id, is_used, " +
                     "created_date, expiration_date, minimum_order_value, maximum_discount) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement ps = dbAdapter.getConnection().prepareStatement(sql, 
                Statement.RETURN_GENERATED_KEYS)) {
            
            ps.setString(1, coupon.getCode());
            ps.setDouble(2, coupon.getDiscountPercentage());
            
            if (coupon.getUserId() != null) {
                ps.setInt(3, coupon.getUserId());
            } else {
                ps.setNull(3, Types.INTEGER);
            }
            
            ps.setBoolean(4, coupon.isUsed());
            ps.setDate(5, Date.valueOf(coupon.getCreatedDate() != null ? 
                    coupon.getCreatedDate() : LocalDate.now()));
            ps.setDate(6, Date.valueOf(coupon.getExpirationDate()));
            ps.setDouble(7, coupon.getMinimumOrderValue());
            
            if (coupon.getMaximumDiscount() > 0) {
                ps.setDouble(8, coupon.getMaximumDiscount());
            } else {
                ps.setNull(8, Types.DOUBLE);
            }
            
            ps.executeUpdate();
            
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) {
                return keys.getInt(1);
            }
        }
        return 0;
    }

    /**
     * Updates a coupon.
     * 
     * @param coupon Coupon with updated fields
     * @return true if update successful
     * @throws SQLException if update fails
     */
    public boolean update(Coupon coupon) throws SQLException {
        String sql = "UPDATE coupons SET code = ?, discount_percentage = ?, user_id = ?, " +
                     "is_used = ?, expiration_date = ?, minimum_order_value = ?, maximum_discount = ? " +
                     "WHERE id = ?";
        
        try (PreparedStatement ps = dbAdapter.getConnection().prepareStatement(sql)) {
            ps.setString(1, coupon.getCode());
            ps.setDouble(2, coupon.getDiscountPercentage());
            
            if (coupon.getUserId() != null) {
                ps.setInt(3, coupon.getUserId());
            } else {
                ps.setNull(3, Types.INTEGER);
            }
            
            ps.setBoolean(4, coupon.isUsed());
            ps.setDate(5, Date.valueOf(coupon.getExpirationDate()));
            ps.setDouble(6, coupon.getMinimumOrderValue());
            
            if (coupon.getMaximumDiscount() > 0) {
                ps.setDouble(7, coupon.getMaximumDiscount());
            } else {
                ps.setNull(7, Types.DOUBLE);
            }
            
            ps.setInt(8, coupon.getId());
            
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Marks a coupon as used.
     * 
     * @param couponId Coupon ID
     * @return true if update successful
     * @throws SQLException if update fails
     */
    public boolean markAsUsed(int couponId) throws SQLException {
        String sql = "UPDATE coupons SET is_used = TRUE, used_at = NOW() WHERE id = ?";
        
        try (PreparedStatement ps = dbAdapter.getConnection().prepareStatement(sql)) {
            ps.setInt(1, couponId);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Marks a coupon as used by code.
     * 
     * @param couponCode Coupon code
     * @return true if update successful
     * @throws SQLException if update fails
     */
    public boolean markAsUsedByCode(String couponCode) throws SQLException {
        String sql = "UPDATE coupons SET is_used = TRUE, used_at = NOW() WHERE code = ?";
        
        try (PreparedStatement ps = dbAdapter.getConnection().prepareStatement(sql)) {
            ps.setString(1, couponCode);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Marks a coupon as used and links to order.
     * 
     * @param couponCode Coupon code
     * @param orderId Order ID
     * @return true if update successful
     * @throws SQLException if update fails
     */
    public boolean markAsUsedWithOrder(String couponCode, int orderId) throws SQLException {
        String sql = "UPDATE coupons SET is_used = TRUE, used_at = NOW(), used_in_order_id = ? " +
                     "WHERE code = ?";
        
        try (PreparedStatement ps = dbAdapter.getConnection().prepareStatement(sql)) {
            ps.setInt(1, orderId);
            ps.setString(2, couponCode);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Restores a coupon (marks as unused).
     * Used when order is cancelled.
     * 
     * @param couponCode Coupon code
     * @return true if update successful
     * @throws SQLException if update fails
     */
    public boolean restoreCoupon(String couponCode) throws SQLException {
        String sql = "UPDATE coupons SET is_used = FALSE, used_at = NULL, used_in_order_id = NULL " +
                     "WHERE code = ?";
        
        try (PreparedStatement ps = dbAdapter.getConnection().prepareStatement(sql)) {
            ps.setString(1, couponCode);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Deletes a coupon.
     * 
     * @param couponId Coupon ID
     * @return true if deletion successful
     * @throws SQLException if delete fails
     */
    public boolean delete(int couponId) throws SQLException {
        String sql = "DELETE FROM coupons WHERE id = ?";
        
        try (PreparedStatement ps = dbAdapter.getConnection().prepareStatement(sql)) {
            ps.setInt(1, couponId);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Checks if coupon code already exists.
     * 
     * @param code Coupon code to check
     * @return true if code exists
     * @throws SQLException if query fails
     */
    public boolean codeExists(String code) throws SQLException {
        String sql = "SELECT COUNT(*) FROM coupons WHERE code = ?";
        
        try (PreparedStatement ps = dbAdapter.getConnection().prepareStatement(sql)) {
            ps.setString(1, code);
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        }
        return false;
    }

    /**
     * Validates a coupon for use.
     * Checks if coupon is valid, not expired, not used, and belongs to user (or is general).
     * 
     * @param code Coupon code
     * @param userId User trying to use it
     * @return Optional containing Coupon if valid for this user
     * @throws SQLException if query fails
     */
    public Optional<Coupon> validateCoupon(String code, int userId) throws SQLException {
        String sql = "SELECT * FROM coupons " +
                     "WHERE code = ? " +
                     "AND (user_id IS NULL OR user_id = ?) " +
                     "AND is_used = FALSE " +
                     "AND expiration_date >= CURRENT_DATE";
        
        try (PreparedStatement ps = dbAdapter.getConnection().prepareStatement(sql)) {
            ps.setString(1, code);
            ps.setInt(2, userId);
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                return Optional.of(mapResultSetToCoupon(rs));
            }
        }
        return Optional.empty();
    }

    /**
     * Validates a coupon is still valid (for re-validation at checkout).
     * 
     * @param code Coupon code
     * @return true if coupon is still valid
     * @throws SQLException if query fails
     */
    public boolean isCouponStillValid(String code) throws SQLException {
        String sql = "SELECT COUNT(*) FROM coupons " +
                     "WHERE code = ? " +
                     "AND is_used = FALSE " +
                     "AND expiration_date >= CURRENT_DATE";
        
        try (PreparedStatement ps = dbAdapter.getConnection().prepareStatement(sql)) {
            ps.setString(1, code);
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        }
        return false;
    }

    /**
     * Gets expired coupons for cleanup.
     * 
     * @return List of expired coupons
     * @throws SQLException if query fails
     */
    public List<Coupon> findExpired() throws SQLException {
        String sql = "SELECT * FROM coupons WHERE expiration_date < CURRENT_DATE";
        List<Coupon> coupons = new ArrayList<>();
        
        try (PreparedStatement ps = dbAdapter.getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            
            while (rs.next()) {
                coupons.add(mapResultSetToCoupon(rs));
            }
        }
        return coupons;
    }

    /**
     * Gets coupons expiring soon (within specified days).
     * 
     * @param days Number of days
     * @return List of expiring coupons
     * @throws SQLException if query fails
     */
    public List<Coupon> findExpiringSoon(int days) throws SQLException {
        String sql = "SELECT * FROM coupons " +
                     "WHERE is_used = FALSE " +
                     "AND expiration_date BETWEEN CURRENT_DATE AND DATE_ADD(CURRENT_DATE, INTERVAL ? DAY) " +
                     "ORDER BY expiration_date ASC";
        
        List<Coupon> coupons = new ArrayList<>();
        
        try (PreparedStatement ps = dbAdapter.getConnection().prepareStatement(sql)) {
            ps.setInt(1, days);
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                coupons.add(mapResultSetToCoupon(rs));
            }
        }
        return coupons;
    }

    /**
     * Gets coupon usage statistics.
     * 
     * @return Array: [total, used, unused, expired]
     * @throws SQLException if query fails
     */
    public int[] getCouponStatistics() throws SQLException {
        int[] stats = new int[4];
        
        // Total coupons
        String sqlTotal = "SELECT COUNT(*) FROM coupons";
        try (PreparedStatement ps = dbAdapter.getConnection().prepareStatement(sqlTotal);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) stats[0] = rs.getInt(1);
        }
        
        // Used coupons
        String sqlUsed = "SELECT COUNT(*) FROM coupons WHERE is_used = TRUE";
        try (PreparedStatement ps = dbAdapter.getConnection().prepareStatement(sqlUsed);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) stats[1] = rs.getInt(1);
        }
        
        // Unused and valid coupons
        String sqlUnused = "SELECT COUNT(*) FROM coupons WHERE is_used = FALSE AND expiration_date >= CURRENT_DATE";
        try (PreparedStatement ps = dbAdapter.getConnection().prepareStatement(sqlUnused);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) stats[2] = rs.getInt(1);
        }
        
        // Expired unused coupons
        String sqlExpired = "SELECT COUNT(*) FROM coupons WHERE is_used = FALSE AND expiration_date < CURRENT_DATE";
        try (PreparedStatement ps = dbAdapter.getConnection().prepareStatement(sqlExpired);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) stats[3] = rs.getInt(1);
        }
        
        return stats;
    }

    /**
     * Deletes all expired unused coupons (cleanup).
     * 
     * @return Number of deleted coupons
     * @throws SQLException if delete fails
     */
    public int deleteExpiredCoupons() throws SQLException {
        String sql = "DELETE FROM coupons WHERE is_used = FALSE AND expiration_date < CURRENT_DATE";
        
        try (PreparedStatement ps = dbAdapter.getConnection().prepareStatement(sql)) {
            return ps.executeUpdate();
        }
    }

    /**
     * Maps a ResultSet row to Coupon object.
     * 
     * @param rs ResultSet positioned at valid row
     * @return Coupon object
     * @throws SQLException if mapping fails
     */
    private Coupon mapResultSetToCoupon(ResultSet rs) throws SQLException {
        Coupon coupon = new Coupon();
        
        coupon.setId(rs.getInt("id"));
        coupon.setCode(rs.getString("code"));
        coupon.setDiscountPercentage(rs.getDouble("discount_percentage"));
        
        int userId = rs.getInt("user_id");
        if (!rs.wasNull()) {
            coupon.setUserId(userId);
        }
        
        coupon.setUsed(rs.getBoolean("is_used"));
        
        Date createdDate = rs.getDate("created_date");
        if (createdDate != null) {
            coupon.setCreatedDate(createdDate.toLocalDate());
        }
        
        Date expirationDate = rs.getDate("expiration_date");
        if (expirationDate != null) {
            coupon.setExpirationDate(expirationDate.toLocalDate());
        }
        
        coupon.setMinimumOrderValue(rs.getDouble("minimum_order_value"));
        
        double maxDiscount = rs.getDouble("maximum_discount");
        if (!rs.wasNull()) {
            coupon.setMaximumDiscount(maxDiscount);
        }
        
        return coupon;
    }
}
