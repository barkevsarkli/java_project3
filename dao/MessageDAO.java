package com.greengrocer.dao;

import com.greengrocer.models.Message;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * data access object for message entity.
 * 
 * @author Samira Çeçen
 * @since 24-12-2025
 */
public class MessageDAO {

    /** Database adapter reference */
    private final DatabaseAdapter dbAdapter;

    /**
     * Constructor.
     */
    public MessageDAO() {
        this.dbAdapter = DatabaseAdapter.getInstance();
    }

    /**
     * Finds a message by ID.
     * 
     * @param id Message ID
     * @return Optional containing Message if found
     * @throws SQLException if query fails
     */
    public Optional<Message> findById(int id) throws SQLException {
        String sql = "SELECT m.*, s.username AS sender_name, r.username AS receiver_name " +
                    "FROM messages m " +
                    "JOIN users s ON m.sender_id = s.id " +
                    "JOIN users r ON m.receiver_id = r.id " +
                    "WHERE m.id = ?";
        
        try (PreparedStatement ps = dbAdapter.getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                return Optional.of(mapResultSetToMessage(rs));
            }
        }
        return Optional.empty();
    }

    /**
     * Gets all messages received by a user.
     * 
     * @param userId User ID
     * @return List of received messages
     * @throws SQLException if query fails
     */
    public List<Message> findByReceiverId(int userId) throws SQLException {
        String sql = "SELECT m.*, s.username AS sender_name, r.username AS receiver_name " +
                    "FROM messages m " +
                    "JOIN users s ON m.sender_id = s.id " +
                    "JOIN users r ON m.receiver_id = r.id " +
                    "WHERE m.receiver_id = ? " +
                    "ORDER BY m.sent_at DESC";
        
        List<Message> messages = new ArrayList<>();
        try (PreparedStatement ps = dbAdapter.getConnection().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                messages.add(mapResultSetToMessage(rs));
            }
        }
        return messages;
    }

    /**
     * Gets all messages sent by a user.
     * 
     * @param userId User ID
     * @return List of sent messages
     * @throws SQLException if query fails
     */
    public List<Message> findBySenderId(int userId) throws SQLException {
        String sql = "SELECT m.*, s.username AS sender_name, r.username AS receiver_name " +
                    "FROM messages m " +
                    "JOIN users s ON m.sender_id = s.id " +
                    "JOIN users r ON m.receiver_id = r.id " +
                    "WHERE m.sender_id = ? " +
                    "ORDER BY m.sent_at DESC";
        
        List<Message> messages = new ArrayList<>();
        try (PreparedStatement ps = dbAdapter.getConnection().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                messages.add(mapResultSetToMessage(rs));
            }
        }
        return messages;
    }

    /**
     * Gets unread messages for a user.
     * 
     * @param userId User ID
     * @return List of unread messages
     * @throws SQLException if query fails
     */
    public List<Message> findUnreadByReceiverId(int userId) throws SQLException {
        String sql = "SELECT m.*, s.username AS sender_name, r.username AS receiver_name " +
                    "FROM messages m " +
                    "JOIN users s ON m.sender_id = s.id " +
                    "JOIN users r ON m.receiver_id = r.id " +
                    "WHERE m.receiver_id = ? AND m.is_read = false " +
                    "ORDER BY m.sent_at DESC";
        
        List<Message> messages = new ArrayList<>();
        try (PreparedStatement ps = dbAdapter.getConnection().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                messages.add(mapResultSetToMessage(rs));
            }
        }
        return messages;
    }

    /**
     * Gets conversation thread (original message and all replies, including nested replies).
     * Uses recursive approach to get all messages in the thread.
     * 
     * @param originalMessageId ID of the original message
     * @return List of messages in thread
     * @throws SQLException if query fails
     */
    public List<Message> findConversationThread(int originalMessageId) throws SQLException {
        // First get the original message to find its root
        Optional<Message> original = findById(originalMessageId);
        if (!original.isPresent()) {
            return new ArrayList<>();
        }
        
        // Find the root message (the one without a parent, or trace back to root)
        int rootId = findRootMessageId(originalMessageId);
        
        // Get all messages in the thread using recursive approach
        List<Message> allMessages = new ArrayList<>();
        collectThreadMessages(rootId, allMessages);
        
        // Sort by sent_at ascending (oldest first)
        allMessages.sort((m1, m2) -> m1.getSentAt().compareTo(m2.getSentAt()));
        
        return allMessages;
    }
    
    /**
     * Finds the root message ID by traversing up the parent chain.
     * 
     * @param messageId Starting message ID
     * @return Root message ID
     * @throws SQLException if query fails
     */
    private int findRootMessageId(int messageId) throws SQLException {
        int currentId = messageId;
        int iterations = 0;
        final int MAX_ITERATIONS = 100; // Prevent infinite loops
        
        while (iterations < MAX_ITERATIONS) {
            Optional<Message> msg = findById(currentId);
            if (!msg.isPresent()) {
                break;
            }
            
            Integer parentId = msg.get().getParentMessageId();
            if (parentId == null) {
                // This is the root
                return currentId;
            }
            
            currentId = parentId;
            iterations++;
        }
        
        // If we couldn't find root, return the original message ID
        return messageId;
    }
    
    /**
     * Recursively collects all messages in a thread starting from root.
     * 
     * @param rootId Root message ID
     * @param collectedMessages List to collect messages into
     * @throws SQLException if query fails
     */
    private void collectThreadMessages(int rootId, List<Message> collectedMessages) throws SQLException {
        // Get the root message
        Optional<Message> root = findById(rootId);
        if (!root.isPresent()) {
            return;
        }
        
        // Add root if not already added
        if (collectedMessages.stream().noneMatch(m -> m.getId() == rootId)) {
            collectedMessages.add(root.get());
        }
        
        // Find all direct children (messages with parent_message_id = rootId)
        String sql = "SELECT m.*, s.username AS sender_name, r.username AS receiver_name " +
                    "FROM messages m " +
                    "JOIN users s ON m.sender_id = s.id " +
                    "JOIN users r ON m.receiver_id = r.id " +
                    "WHERE m.parent_message_id = ?";
        
        try (PreparedStatement ps = dbAdapter.getConnection().prepareStatement(sql)) {
            ps.setInt(1, rootId);
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                Message child = mapResultSetToMessage(rs);
                
                // Add child if not already added
                if (collectedMessages.stream().noneMatch(m -> m.getId() == child.getId())) {
                    collectedMessages.add(child);
                    
                    // Recursively collect children of this child
                    collectThreadMessages(child.getId(), collectedMessages);
                }
            }
        }
    }

    /**
     * Gets messages between two users.
     * 
     * @param userId1 First user ID
     * @param userId2 Second user ID
     * @return List of messages between users
     * @throws SQLException if query fails
     */
    public List<Message> findConversationBetween(int userId1, int userId2) throws SQLException {
        String sql = "SELECT m.*, s.username AS sender_name, r.username AS receiver_name " +
                    "FROM messages m " +
                    "JOIN users s ON m.sender_id = s.id " +
                    "JOIN users r ON m.receiver_id = r.id " +
                    "WHERE (m.sender_id = ? AND m.receiver_id = ?) OR (m.sender_id = ? AND m.receiver_id = ?) " +
                    "ORDER BY m.sent_at DESC";
        
        List<Message> messages = new ArrayList<>();
        try (PreparedStatement ps = dbAdapter.getConnection().prepareStatement(sql)) {
            ps.setInt(1, userId1);
            ps.setInt(2, userId2);
            ps.setInt(3, userId2);
            ps.setInt(4, userId1);
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                messages.add(mapResultSetToMessage(rs));
            }
        }
        return messages;
    }

    /**
     * Inserts a new message.
     * 
     * @param message Message to insert
     * @return Generated message ID
     * @throws SQLException if insert fails
     */
    public int insert(Message message) throws SQLException {
        String sql = "INSERT INTO messages (sender_id, receiver_id, subject, content, sent_at, is_read, parent_message_id) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement ps = dbAdapter.getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, message.getSenderId());
            ps.setInt(2, message.getReceiverId());
            ps.setString(3, message.getSubject());
            ps.setString(4, message.getContent());
            ps.setTimestamp(5, Timestamp.valueOf(message.getSentAt() != null ? message.getSentAt() : LocalDateTime.now()));
            ps.setBoolean(6, message.isRead());
            ps.setObject(7, message.getParentMessageId(), Types.INTEGER);
            
            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating message failed, no rows affected.");
            }
            
            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                } else {
                    throw new SQLException("Creating message failed, no ID obtained.");
                }
            }
        }
    }

    /**
     * Marks a message as read.
     * 
     * @param messageId Message ID
     * @return true if update successful
     * @throws SQLException if update fails
     */
    public boolean markAsRead(int messageId) throws SQLException {
        String sql = "UPDATE messages SET is_read = true WHERE id = ?";
        
        try (PreparedStatement ps = dbAdapter.getConnection().prepareStatement(sql)) {
            ps.setInt(1, messageId);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Marks all messages for a receiver as read.
     * 
     * @param receiverId Receiver's user ID
     * @return Number of messages marked as read
     * @throws SQLException if update fails
     */
    public int markAllAsRead(int receiverId) throws SQLException {
        String sql = "UPDATE messages SET is_read = true WHERE receiver_id = ? AND is_read = false";
        
        try (PreparedStatement ps = dbAdapter.getConnection().prepareStatement(sql)) {
            ps.setInt(1, receiverId);
            return ps.executeUpdate();
        }
    }

    /**
     * Deletes a message.
     * 
     * @param messageId Message ID
     * @return true if deletion successful
     * @throws SQLException if delete fails
     */
    public boolean delete(int messageId) throws SQLException {
        String sql = "DELETE FROM messages WHERE id = ?";
        
        try (PreparedStatement ps = dbAdapter.getConnection().prepareStatement(sql)) {
            ps.setInt(1, messageId);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Gets count of unread messages for a user.
     * 
     * @param userId User ID
     * @return Number of unread messages
     * @throws SQLException if query fails
     */
    public int getUnreadCount(int userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM messages WHERE receiver_id = ? AND is_read = false";
        
        try (PreparedStatement ps = dbAdapter.getConnection().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    /**
     * Gets all messages for owner (from all customers).
     * 
     * @param ownerId Owner's user ID
     * @return List of all messages to owner
     * @throws SQLException if query fails
     */
    public List<Message> findAllForOwner(int ownerId) throws SQLException {
        String sql = "SELECT m.*, s.username AS sender_name, r.username AS receiver_name " +
                    "FROM messages m " +
                    "JOIN users s ON m.sender_id = s.id " +
                    "JOIN users r ON m.receiver_id = r.id " +
                    "WHERE m.receiver_id = ? " +
                    "ORDER BY m.sent_at DESC";
        
        List<Message> messages = new ArrayList<>();
        try (PreparedStatement ps = dbAdapter.getConnection().prepareStatement(sql)) {
            ps.setInt(1, ownerId);
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                messages.add(mapResultSetToMessage(rs));
            }
        }
        return messages;
    }

    /**
     * Maps a ResultSet row to Message object.
     * 
     * @param rs ResultSet positioned at valid row
     * @return Message object
     * @throws SQLException if mapping fails
     */
    private Message mapResultSetToMessage(ResultSet rs) throws SQLException {
        Message message = new Message();
        message.setId(rs.getInt("id"));
        message.setSenderId(rs.getInt("sender_id"));
        message.setReceiverId(rs.getInt("receiver_id"));
        message.setSubject(rs.getString("subject"));
        message.setContent(rs.getString("content"));
        
        Timestamp sentAt = rs.getTimestamp("sent_at");
        if (sentAt != null) {
            message.setSentAt(sentAt.toLocalDateTime());
        }
        
        message.setRead(rs.getBoolean("is_read"));
        
        Integer parentId = (Integer) rs.getObject("parent_message_id");
        message.setParentMessageId(parentId);
        
        // Set names from joins
        try {
            message.setSenderName(rs.getString("sender_name"));
            message.setReceiverName(rs.getString("receiver_name"));
        } catch (SQLException e) {
            // Names might not be in ResultSet if joins weren't used
            // That's okay, they'll just be null
        }
        
        return message;
    }
}
