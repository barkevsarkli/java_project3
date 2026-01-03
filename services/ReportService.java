package com.greengrocer.services;

import com.greengrocer.dao.OrderDAO;
import com.greengrocer.dao.ProductDAO;
import com.greengrocer.models.Order;
import com.greengrocer.models.OrderItem;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * service class for generating reports and analytics.
 * 
 * @author Samira Çeçen
 * @since 24-12-2025
 */
public class ReportService {

    /** Order data access object */
    private final OrderDAO orderDAO;
    
    /** Product data access object */
    private final ProductDAO productDAO;

    /**
     * Constructor.
     */
    public ReportService() {
        // Initialize DAOs
        this.orderDAO = new OrderDAO();
        this.productDAO = new ProductDAO();
    }

    /**
     * Gets revenue data for a date range.
     * 
     * @param startDate Start date
     * @param endDate End date
     * @return Map of date to revenue amount
     */
    public Map<LocalDate, Double> getRevenueByDate(LocalDate startDate, LocalDate endDate) {
        // Get orders in date range
        // Group by date and sum totals
        // Return map
        Map<LocalDate, Double> revenueMap = new HashMap<>();
        try {
            LocalDateTime start = startDate.atStartOfDay();
            LocalDateTime end = endDate.atTime(LocalTime.MAX);
            List<Order> orders = orderDAO.findByDateRange(start, end);
            
            for (Order order : orders) {
                if (order.isDelivered()) {
                    LocalDate date = order.getOrderTime().toLocalDate();
                    revenueMap.merge(date, order.getTotalCost(), Double::sum);
                }
            }
        } catch (SQLException e) {
            // Log error
        }
        return revenueMap;
    }

    /**
     * Gets total revenue for a date range.
     * 
     * @param startDate Start date
     * @param endDate End date
     * @return Total revenue
     */
    public double getTotalRevenue(LocalDate startDate, LocalDate endDate) {
        // Call orderDAO.getTotalRevenue()
        // Return total
        try {
            LocalDateTime start = startDate.atStartOfDay();
            LocalDateTime end = endDate.atTime(LocalTime.MAX);
            return orderDAO.getTotalRevenue(start, end);
        } catch (SQLException e) {
            return 0.0;
        }
    }

    /**
     * Gets order count by status.
     * 
     * @return Map of status to count
     */
    public Map<String, Integer> getOrderCountByStatus() {
        // Query counts for each status
        // Return map
        Map<String, Integer> statusCounts = new HashMap<>();
        try {
            statusCounts.put("pending", orderDAO.getOrderCountByStatus("pending"));
            statusCounts.put("confirmed", orderDAO.getOrderCountByStatus("confirmed"));
            statusCounts.put("assigned", orderDAO.getOrderCountByStatus("assigned"));
            statusCounts.put("delivered", orderDAO.getOrderCountByStatus("delivered"));
            statusCounts.put("cancelled", orderDAO.getOrderCountByStatus("cancelled"));
        } catch (SQLException e) {
            // Log error
        }
        return statusCounts;
    }

    /**
     * Gets top selling products.
     * 
     * @param limit Number of products to return
     * @return List of arrays [productName, totalQuantitySold, totalRevenue]
     */
    public List<Object[]> getTopSellingProducts(int limit) {
        // Query order items grouped by product
        // Sum quantities and revenues
        // Order by quantity descending
        // Return top N
        List<Object[]> topProducts = new ArrayList<>();
        
        try {
            List<Order> orders = orderDAO.findByStatus("delivered");
            Map<String, Double> productQuantities = new HashMap<>();
            Map<String, Double> productRevenues = new HashMap<>();
            
            for (Order order : orders) {
                for (OrderItem item : orderDAO.getOrderItems(order.getId())) {
                    String productName = item.getProductName();
                    productQuantities.merge(productName, item.getQuantity(), Double::sum);
                    productRevenues.merge(productName, item.getTotalPrice(), Double::sum);
                }
            }
            
            // Sort by quantity descending
            productQuantities.entrySet().stream()
                    .sorted((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()))
                    .limit(limit)
                    .forEach(entry -> {
                        String productName = entry.getKey();
                        Double quantity = entry.getValue();
                        Double revenue = productRevenues.getOrDefault(productName, 0.0);
                        topProducts.add(new Object[]{productName, quantity, revenue});
                    });
            
        } catch (SQLException e) {
            System.err.println("Error getting top selling products: " + e.getMessage());
        }
        
        return topProducts;
    }

    /**
     * Gets sales data by product.
     * 
     * @param startDate Start date
     * @param endDate End date
     * @return Map of product name to total quantity sold
     */
    public Map<String, Double> getSalesByProduct(LocalDate startDate, LocalDate endDate) {
        // Get orders in date range
        // Extract order items
        // Group by product and sum quantities
        // Return map
        Map<String, Double> salesMap = new HashMap<>();
        try {
            LocalDateTime start = startDate.atStartOfDay();
            LocalDateTime end = endDate.atTime(LocalTime.MAX);
            List<Order> orders = orderDAO.findByDateRange(start, end);
            
            for (Order order : orders) {
                if (order.isDelivered()) {
                    for (OrderItem item : orderDAO.getOrderItems(order.getId())) {
                        salesMap.merge(item.getProductName(), item.getQuantity(), Double::sum);
                    }
                }
            }
        } catch (SQLException e) {
            // Log error
        }
        return salesMap;
    }

    /**
     * Gets revenue by product type.
     * 
     * @param startDate Start date
     * @param endDate End date
     * @return Map of type (vegetable/fruit) to revenue
     */
    public Map<String, Double> getRevenueByProductType(LocalDate startDate, LocalDate endDate) {
        // Get sales data by product
        // Categorize by type and sum revenues
        // Return map
        Map<String, Double> typeRevenue = new HashMap<>();
        typeRevenue.put("vegetable", 0.0);
        typeRevenue.put("fruit", 0.0);
        
        try {
            LocalDateTime start = startDate.atStartOfDay();
            LocalDateTime end = endDate.atTime(LocalTime.MAX);
            List<Order> orders = orderDAO.findByDateRange(start, end);
            
            for (Order order : orders) {
                if (order.isDelivered()) {
                    for (OrderItem item : orderDAO.getOrderItems(order.getId())) {
                        // Get product to determine type
                        var product = productDAO.findById(item.getProductId());
                        if (product != null) {
                            String type = product.getType();
                            typeRevenue.merge(type, item.getTotalPrice(), Double::sum);
                        }
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting revenue by product type: " + e.getMessage());
        }
        
        return typeRevenue;
    }

    /**
     * Gets monthly revenue for the past year.
     * 
     * @return Map of month (YYYY-MM) to revenue
     */
    public Map<String, Double> getMonthlyRevenue() {
        // Calculate date range (past 12 months)
        // Query revenue grouped by month
        // Return map
        Map<String, Double> monthlyRevenue = new HashMap<>();
        // Implementation would query and aggregate by month
        return monthlyRevenue;
    }

    /**
     * Gets daily order counts for a date range.
     * 
     * @param startDate Start date
     * @param endDate End date
     * @return Map of date to order count
     */
    public Map<LocalDate, Integer> getDailyOrderCounts(LocalDate startDate, LocalDate endDate) {
        // Get orders in date range
        // Group by date and count
        // Return map
        Map<LocalDate, Integer> dailyCounts = new HashMap<>();
        try {
            LocalDateTime start = startDate.atStartOfDay();
            LocalDateTime end = endDate.atTime(LocalTime.MAX);
            List<Order> orders = orderDAO.findByDateRange(start, end);
            
            for (Order order : orders) {
                LocalDate date = order.getOrderTime().toLocalDate();
                dailyCounts.merge(date, 1, Integer::sum);
            }
        } catch (SQLException e) {
            // Log error
        }
        return dailyCounts;
    }

    /**
     * Gets average order value.
     * 
     * @param startDate Start date
     * @param endDate End date
     * @return Average order total
     */
    public double getAverageOrderValue(LocalDate startDate, LocalDate endDate) {
        // Get orders in date range
        // Calculate average of totals
        // Return average
        try {
            LocalDateTime start = startDate.atStartOfDay();
            LocalDateTime end = endDate.atTime(LocalTime.MAX);
            List<Order> orders = orderDAO.findByDateRange(start, end);
            
            if (orders.isEmpty()) return 0.0;
            
            double sum = orders.stream()
                    .filter(Order::isDelivered)
                    .mapToDouble(Order::getTotalCost)
                    .sum();
            long count = orders.stream().filter(Order::isDelivered).count();
            
            return count > 0 ? sum / count : 0.0;
        } catch (SQLException e) {
            return 0.0;
        }
    }

    /**
     * Gets statistics summary for dashboard.
     * 
     * @return Map of statistic name to value
     */
    public Map<String, Object> getDashboardStatistics() {
        // Compile various statistics
        // Total orders, revenue, customers, etc.
        // Return map
        Map<String, Object> stats = new HashMap<>();
        
        LocalDate today = LocalDate.now();
        LocalDate startOfMonth = today.withDayOfMonth(1);
        
        stats.put("todayRevenue", getTotalRevenue(today, today));
        stats.put("monthRevenue", getTotalRevenue(startOfMonth, today));
        stats.put("orderCounts", getOrderCountByStatus());
        stats.put("averageOrderValue", getAverageOrderValue(startOfMonth, today));
        
        return stats;
    }

    /**
     * Generates product inventory report.
     * 
     * @return List of product stock status
     */
    public List<Map<String, Object>> getInventoryReport() {
        // Get all products with stock info
        // Flag low stock items
        // Return report data
        List<Map<String, Object>> inventoryReport = new ArrayList<>();
        
        try {
            var products = productDAO.findAll();
            
            for (var product : products) {
                Map<String, Object> productInfo = new HashMap<>();
                productInfo.put("id", product.getId());
                productInfo.put("name", product.getName());
                productInfo.put("type", product.getType());
                productInfo.put("stock", product.getStock());
                productInfo.put("threshold", product.getThreshold());
                productInfo.put("price", product.getPrice());
                
                // Flag low stock items
                boolean isLowStock = product.getStock() <= product.getThreshold();
                productInfo.put("isLowStock", isLowStock);
                productInfo.put("status", isLowStock ? "LOW STOCK" : "OK");
                
                inventoryReport.add(productInfo);
            }
        } catch (Exception e) {
            System.err.println("Error generating inventory report: " + e.getMessage());
        }
        
        return inventoryReport;
    }

    /**
     * Gets hourly order distribution.
     * Shows which hours have most orders.
     * 
     * @param startDate Start date
     * @param endDate End date
     * @return Map of hour (0-23) to order count
     */
    public Map<Integer, Integer> getHourlyOrderDistribution(LocalDate startDate, LocalDate endDate) {
        // Get orders in date range
        // Group by hour of day
        // Return distribution
        Map<Integer, Integer> hourlyDistribution = new HashMap<>();
        for (int i = 0; i < 24; i++) {
            hourlyDistribution.put(i, 0);
        }
        // Implementation would count orders by hour
        return hourlyDistribution;
    }
}

