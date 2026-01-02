package com.greengrocer.models;

import java.time.LocalDateTime;

/**
 * represents a message between customer and owner.
 * 
 * @author Samira Çeçen
 * @since 24-12-2025
 */
public class Message {

    /** Unique identifier */
    private int id;
    
    /** Sender user ID */
    private int senderId;
    
    /** Sender username (for display) */
    private String senderName;
    
    /** Receiver user ID */
    private int receiverId;
    
    /** Receiver username (for display) */
    private String receiverName;
    
    /** Message subject/title */
    private String subject;
    
    /** Message content */
    private String content;
    
    /** Timestamp when message was sent */
    private LocalDateTime sentAt;
    
    /** Whether message has been read */
    private boolean isRead;
    
    /** Parent message ID for replies (null if original message) */
    private Integer parentMessageId;

    /**
     * default constructor, sets sent time to now and isRead to false.
     */
    public Message() {
        // Set sent time to now
        // Set isRead to false
        this.sentAt = LocalDateTime.now();
        this.isRead = false;
    }

    /**
     * constructor with essential fields.
     * 
     * @param senderId sender id
     * @param receiverId receiver id
     * @param subject message subject
     * @param content message content
     */
    public Message(int senderId, int receiverId, String subject, String content) {
        // Call default constructor
        // Set provided fields
        this();
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.subject = subject;
        this.content = content;
    }

    // ==================== GETTERS ====================

    /**
     * @return The message ID
     */
    public int getId() {
        return id;
    }

    /**
     * @return The sender ID
     */
    public int getSenderId() {
        return senderId;
    }

    /**
     * @return The sender name
     */
    public String getSenderName() {
        return senderName;
    }

    /**
     * @return The receiver ID
     */
    public int getReceiverId() {
        return receiverId;
    }

    /**
     * @return The receiver name
     */
    public String getReceiverName() {
        return receiverName;
    }

    /**
     * @return The subject
     */
    public String getSubject() {
        return subject;
    }

    /**
     * @return The content
     */
    public String getContent() {
        return content;
    }

    /**
     * @return The sent timestamp
     */
    public LocalDateTime getSentAt() {
        return sentAt;
    }

    /**
     * @return Whether message is read
     */
    public boolean isRead() {
        return isRead;
    }

    /**
     * @return The parent message ID or null
     */
    public Integer getParentMessageId() {
        return parentMessageId;
    }

    // ==================== SETTERS ====================

    /**
     * @param id The message ID to set
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * @param senderId The sender ID to set
     */
    public void setSenderId(int senderId) {
        this.senderId = senderId;
    }

    /**
     * @param senderName The sender name to set
     */
    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    /**
     * @param receiverId The receiver ID to set
     */
    public void setReceiverId(int receiverId) {
        this.receiverId = receiverId;
    }

    /**
     * @param receiverName The receiver name to set
     */
    public void setReceiverName(String receiverName) {
        this.receiverName = receiverName;
    }

    /**
     * @param subject The subject to set
     */
    public void setSubject(String subject) {
        this.subject = subject;
    }

    /**
     * @param content The content to set
     */
    public void setContent(String content) {
        this.content = content;
    }

    /**
     * @param sentAt The sent timestamp to set
     */
    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }

    /**
     * @param read Whether message is read
     */
    public void setRead(boolean read) {
        isRead = read;
    }

    /**
     * @param parentMessageId The parent message ID to set
     */
    public void setParentMessageId(Integer parentMessageId) {
        this.parentMessageId = parentMessageId;
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Marks message as read.
     */
    public void markAsRead() {
        this.isRead = true;
    }

    /**
     * Checks if this is a reply to another message.
     * 
     * @return true if this is a reply
     */
    public boolean isReply() {
        return parentMessageId != null;
    }

    /**
     * Creates a reply to this message.
     * 
     * @param replyContent The reply content
     * @return New Message object as reply
     */
    public Message createReply(String replyContent) {
        // Create new message
        Message reply = new Message();
        
        // Set subject as "Re: " + original subject
        String replySubject = this.subject != null && !this.subject.isEmpty() 
            ? (this.subject.startsWith("Re: ") ? this.subject : "Re: " + this.subject)
            : "Re: (No Subject)";
        reply.setSubject(replySubject);
        
        // Set content
        reply.setContent(replyContent);
        
        // Set this message's ID as parent
        reply.setParentMessageId(this.id);
        
        // Return the reply message
        // Note: senderId and receiverId will be set by MessageService.replyToMessage()
        return reply;
    }

    /**
     * Gets a preview of the message content.
     * 
     * @param maxLength Maximum characters to show
     * @return Truncated content with ellipsis if needed
     */
    public String getContentPreview(int maxLength) {
        // If content length <= maxLength, return content
        // Otherwise, return truncated content + "..."
        return "";
    }

    @Override
    public String toString() {
        return String.format("Message{from='%s', to='%s', subject='%s', sentAt=%s}",
                senderName, receiverName, subject, sentAt);
    }
}

