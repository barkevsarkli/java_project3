package com.greengrocer.services;

import com.greengrocer.dao.MessageDAO;
import com.greengrocer.dao.UserDAO;
import com.greengrocer.models.Message;
import com.greengrocer.models.User;
import com.greengrocer.app.SessionManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * service class for messaging operations.
 * 
 * @author Samira Çeçen
 * @since 24-12-2025
 */
public class MessageService {

    /** Message data access object */
    private final MessageDAO messageDAO;
    
    /** User data access object */
    private final UserDAO userDAO;

    /**
     * Constructor.
     */
    public MessageService() {
        // Initialize DAOs
        this.messageDAO = new MessageDAO();
        this.userDAO = new UserDAO();
    }

    /**
     * Sends a message from current user.
     * 
     * @param receiverId Receiver's user ID
     * @param subject Message subject
     * @param content Message content
     * @return Created Message or null on failure
     */
    public Message sendMessage(int receiverId, String subject, String content) {
        // Get current user from SessionManager
        // Create Message object
        // Call messageDAO.insert()
        // Return created message
        User currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser == null) return null;
        
        try {
            Message message = new Message(currentUser.getId(), receiverId, subject, content);
            int id = messageDAO.insert(message);
            message.setId(id);
            return message;
        } catch (SQLException e) {
            return null;
        }
    }

    /**
     * Sends a message to the owner.
     * Convenience method for customers.
     * 
     * @param subject Message subject
     * @param content Message content
     * @return Created Message or null on failure
     */
    public Message sendMessageToOwner(String subject, String content) {
        // Get owner user ID
        // Call sendMessage()
        User owner = userDAO.findOwner();
        if (owner == null) return null;
        return sendMessage(owner.getId(), subject, content);
    }
    
    /**
     * Sends a message to the assigned carrier for customer's most recent active order.
     * Automatically includes order ID reference in the message.
     * 
     * @param subject Message subject
     * @param content Message content
     * @return Created Message or null on failure
     */
    public Message sendMessageToMyCarrier(String subject, String content) {
        User currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser == null || !currentUser.isCustomer()) return null;
        
        try {
            // Find customer's most recent assigned or in-progress order with order ID
            String sql = "SELECT id, carrier_id FROM orders " +
                        "WHERE customer_id = ? AND carrier_id IS NOT NULL " +
                        "AND status IN ('assigned', 'confirmed') " +
                        "ORDER BY order_time DESC LIMIT 1";
            
            com.greengrocer.dao.DatabaseAdapter dbAdapter = 
                com.greengrocer.dao.DatabaseAdapter.getInstance();
            java.sql.PreparedStatement ps = dbAdapter.getConnection().prepareStatement(sql);
            ps.setInt(1, currentUser.getId());
            java.sql.ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                int orderId = rs.getInt("id");
                int carrierId = rs.getInt("carrier_id");
                
                // Prepend order reference to subject and content
                String enhancedSubject = "[Order #" + orderId + "] " + subject;
                String enhancedContent = "📦 Regarding Order #" + orderId + "\n\n" + content;
                
                return sendMessage(carrierId, enhancedSubject, enhancedContent);
            }
            return null;
        } catch (java.sql.SQLException e) {
            return null;
        }
    }
    
    /**
     * Gets the assigned carrier for customer's most recent active order.
     * 
     * @return User (carrier) or null if no carrier assigned
     */
    public User getMyAssignedCarrier() {
        User currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser == null || !currentUser.isCustomer()) return null;
        
        try {
            String sql = "SELECT carrier_id FROM orders " +
                        "WHERE customer_id = ? AND carrier_id IS NOT NULL " +
                        "AND status IN ('assigned', 'confirmed') " +
                        "ORDER BY order_time DESC LIMIT 1";
            
            com.greengrocer.dao.DatabaseAdapter dbAdapter = 
                com.greengrocer.dao.DatabaseAdapter.getInstance();
            java.sql.PreparedStatement ps = dbAdapter.getConnection().prepareStatement(sql);
            ps.setInt(1, currentUser.getId());
            java.sql.ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                int carrierId = rs.getInt("carrier_id");
                return userDAO.findById(carrierId);
            }
            return null;
        } catch (java.sql.SQLException e) {
            return null;
        }
    }
    
    /**
     * Checks if customer has an assigned carrier.
     * 
     * @return true if customer has an active order with assigned carrier
     */
    public boolean hasAssignedCarrier() {
        return getMyAssignedCarrier() != null;
    }
    
    /**
     * Gets all active orders with assigned carriers for the current customer.
     * Returns a list of Object arrays: [orderId, carrierId, carrierName, orderStatus]
     * 
     * @return List of order info arrays
     */
    public java.util.List<Object[]> getMyOrdersWithCarriers() {
        User currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser == null || !currentUser.isCustomer()) return new java.util.ArrayList<>();
        
        java.util.List<Object[]> orders = new java.util.ArrayList<>();
        try {
            String sql = "SELECT o.id, o.carrier_id, u.username AS carrier_name, o.status " +
                        "FROM orders o " +
                        "JOIN users u ON o.carrier_id = u.id " +
                        "WHERE o.customer_id = ? AND o.carrier_id IS NOT NULL " +
                        "AND o.status IN ('assigned', 'confirmed') " +
                        "ORDER BY o.order_time DESC";
            
            com.greengrocer.dao.DatabaseAdapter dbAdapter = 
                com.greengrocer.dao.DatabaseAdapter.getInstance();
            java.sql.PreparedStatement ps = dbAdapter.getConnection().prepareStatement(sql);
            ps.setInt(1, currentUser.getId());
            java.sql.ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                Object[] order = new Object[]{
                    rs.getInt("id"),
                    rs.getInt("carrier_id"),
                    rs.getString("carrier_name"),
                    rs.getString("status")
                };
                orders.add(order);
            }
        } catch (java.sql.SQLException e) {
            e.printStackTrace();
        }
        return orders;
    }
    
    /**
     * Sends a message to the carrier of a specific order.
     * 
     * @param orderId Order ID
     * @param subject Message subject
     * @param content Message content
     * @return Created Message or null on failure
     */
    public Message sendMessageToCarrierForOrder(int orderId, String subject, String content) {
        User currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser == null || !currentUser.isCustomer()) return null;
        
        try {
            // Get carrier for the specific order
            String sql = "SELECT carrier_id FROM orders " +
                        "WHERE id = ? AND customer_id = ? AND carrier_id IS NOT NULL";
            
            com.greengrocer.dao.DatabaseAdapter dbAdapter = 
                com.greengrocer.dao.DatabaseAdapter.getInstance();
            java.sql.PreparedStatement ps = dbAdapter.getConnection().prepareStatement(sql);
            ps.setInt(1, orderId);
            ps.setInt(2, currentUser.getId());
            java.sql.ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                int carrierId = rs.getInt("carrier_id");
                
                // Prepend order reference to subject and content
                String enhancedSubject = "[Order #" + orderId + "] " + subject;
                String enhancedContent = "📦 Regarding Order #" + orderId + "\n\n" + content;
                
                return sendMessage(carrierId, enhancedSubject, enhancedContent);
            }
            return null;
        } catch (java.sql.SQLException e) {
            return null;
        }
    }

    /**
     * Replies to a message.
     * 
     * @param originalMessageId ID of message being replied to
     * @param content Reply content
     * @return Created reply Message or null on failure
     */
    public Message replyToMessage(int originalMessageId, String content) {
        try {
            Message original = messageDAO.findById(originalMessageId).orElse(null);
            if (original == null) {
                System.err.println("MessageService.replyToMessage: Original message not found with ID: " + originalMessageId);
                return null;
            }
            
            Message reply = original.createReply(content);
            if (reply == null) {
                System.err.println("MessageService.replyToMessage: createReply returned null");
                return null;
            }
            
            User currentUser = SessionManager.getInstance().getCurrentUser();
            if (currentUser == null) {
                System.err.println("MessageService.replyToMessage: No current user in session");
                return null;
            }
            
            reply.setSenderId(currentUser.getId());
            
            // Determine receiver: if current user sent the original, receiver is the original receiver
            // Otherwise, receiver is the original sender
            if (original.getSenderId() == currentUser.getId()) {
                // Replying to my own message - send to original receiver
                reply.setReceiverId(original.getReceiverId());
            } else {
                // Replying to someone else's message - send to original sender
                reply.setReceiverId(original.getSenderId());
            }
            
            int id = messageDAO.insert(reply);
            reply.setId(id);
            return reply;
        } catch (SQLException e) {
            System.err.println("MessageService.replyToMessage: SQLException - " + e.getMessage());
            e.printStackTrace();
            return null;
        } catch (Exception e) {
            System.err.println("MessageService.replyToMessage: Unexpected exception - " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Gets messages received by current user.
     * 
     * @return List of received messages
     */
    public List<Message> getReceivedMessages() {
        // Get current user
        // Call messageDAO.findByReceiverId()
        // Return list
        User currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser == null) return new ArrayList<>();
        
        try {
            return messageDAO.findByReceiverId(currentUser.getId());
        } catch (SQLException e) {
            return new ArrayList<>();
        }
    }

    /**
     * Gets messages sent by current user.
     * 
     * @return List of sent messages
     */
    public List<Message> getSentMessages() {
        // Get current user
        // Call messageDAO.findBySenderId()
        // Return list
        User currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser == null) return new ArrayList<>();
        
        try {
            return messageDAO.findBySenderId(currentUser.getId());
        } catch (SQLException e) {
            return new ArrayList<>();
        }
    }

    /**
     * Gets unread messages for current user.
     * 
     * @return List of unread messages
     */
    public List<Message> getUnreadMessages() {
        // Get current user
        // Call messageDAO.findUnreadByReceiverId()
        // Return list
        User currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser == null) return new ArrayList<>();
        
        try {
            return messageDAO.findUnreadByReceiverId(currentUser.getId());
        } catch (SQLException e) {
            return new ArrayList<>();
        }
    }

    /**
     * Gets unread message count for current user.
     * 
     * @return Number of unread messages
     */
    public int getUnreadCount() {
        // Get current user
        // Call messageDAO.getUnreadCount()
        // Return count
        User currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser == null) return 0;
        
        try {
            return messageDAO.getUnreadCount(currentUser.getId());
        } catch (SQLException e) {
            return 0;
        }
    }

    /**
     * Marks a message as read.
     * 
     * @param messageId Message ID
     * @return true if update successful
     */
    public boolean markAsRead(int messageId) {
        // Call messageDAO.markAsRead()
        // Return result
        try {
            return messageDAO.markAsRead(messageId);
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Marks all messages as read for current user.
     * 
     * @return Number of messages marked as read
     */
    public int markAllAsRead() {
        // Get current user
        // Call messageDAO.markAllAsRead()
        // Return count
        User currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser == null) return 0;
        
        try {
            return messageDAO.markAllAsRead(currentUser.getId());
        } catch (SQLException e) {
            return 0;
        }
    }

    /**
     * Gets conversation thread.
     * 
     * @param messageId Message ID (original or reply)
     * @return List of messages in thread
     */
    public List<Message> getConversationThread(int messageId) {
        // Call messageDAO.findConversationThread()
        // Return list
        try {
            return messageDAO.findConversationThread(messageId);
        } catch (SQLException e) {
            return new ArrayList<>();
        }
    }

    /**
     * Gets all messages for owner.
     * 
     * @return List of all messages to owner
     */
    public List<Message> getAllMessagesForOwner() {
        // Get owner user
        // Call messageDAO.findAllForOwner()
        // Return list
        try {
            User owner = userDAO.findOwner();
            if (owner == null) return new ArrayList<>();
            return messageDAO.findAllForOwner(owner.getId());
        } catch (SQLException e) {
            return new ArrayList<>();
        }
    }

    /**
     * Deletes a message.
     * 
     * @param messageId Message ID
     * @return true if deletion successful
     */
    public boolean deleteMessage(int messageId) {
        // Call messageDAO.delete()
        // Return result
        try {
            return messageDAO.delete(messageId);
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Gets message by ID.
     * 
     * @param messageId Message ID
     * @return Message or null if not found
     */
    public Message getMessageById(int messageId) {
        // Call messageDAO.findById()
        // Return message or null
        try {
            return messageDAO.findById(messageId).orElse(null);
        } catch (SQLException e) {
            return null;
        }
    }
}

