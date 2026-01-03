package com.greengrocer.utils;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * email configuration manager.
 * 
 * @author Barkev Şarklı
 * @since 23-12-2025
 */
public class EmailConfig {

    private static final String CONFIG_FILE = "email.properties";
    private static EmailConfig instance;
    
    // Default Gmail SMTP settings
    private String smtpHost = "smtp.gmail.com";
    private String smtpPort = "587";
    private String senderEmail = "greengrocergreengrocer@gmail.com";
    private String senderPassword = "own12345";
    private String senderName = "GreenGrocer";
    private String baseUrl = "http://localhost:8080"; // Base URL for verification links
    private boolean enableAuth = true;
    private boolean enableTLS = true;
    private boolean enableEmailSending = false; // Disabled by default for security

    private EmailConfig() {
        loadConfiguration();
    }

    public static synchronized EmailConfig getInstance() {
        if (instance == null) {
            instance = new EmailConfig();
        }
        return instance;
    }

    /**
     * Loads email configuration from properties file.
     * If file doesn't exist, creates one with default values.
     */
    private void loadConfiguration() {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(CONFIG_FILE)) {
            props.load(fis);
            
            smtpHost = props.getProperty("smtp.host", smtpHost);
            smtpPort = props.getProperty("smtp.port", smtpPort);
            senderEmail = props.getProperty("sender.email", senderEmail);
            senderPassword = props.getProperty("sender.password", senderPassword);
            senderName = props.getProperty("sender.name", senderName);
            baseUrl = props.getProperty("base.url", baseUrl);
            enableAuth = Boolean.parseBoolean(props.getProperty("smtp.auth", "true"));
            enableTLS = Boolean.parseBoolean(props.getProperty("smtp.starttls", "true"));
            enableEmailSending = Boolean.parseBoolean(props.getProperty("email.enabled", "false"));
            
            System.out.println("✅ Email configuration loaded from " + CONFIG_FILE);
            
        } catch (IOException e) {
            System.out.println("⚠️  Email config file not found, creating default configuration...");
            saveDefaultConfiguration();
        }
    }

    /**
     * Saves default configuration to file.
     */
    private void saveDefaultConfiguration() {
        Properties props = new Properties();
        props.setProperty("smtp.host", smtpHost);
        props.setProperty("smtp.port", smtpPort);
        props.setProperty("sender.email", senderEmail);
        props.setProperty("sender.password", senderPassword);
        props.setProperty("sender.name", senderName);
        props.setProperty("base.url", baseUrl);
        props.setProperty("smtp.auth", String.valueOf(enableAuth));
        props.setProperty("smtp.starttls", String.valueOf(enableTLS));
        props.setProperty("email.enabled", String.valueOf(enableEmailSending));
        
        try (FileOutputStream fos = new FileOutputStream(CONFIG_FILE)) {
            props.store(fos, "GreenGrocer Email Configuration\n" +
                    "# To enable email sending:\n" +
                    "# 1. Set email.enabled=true\n" +
                    "# 2. Update sender.email with your Gmail address\n" +
                    "# 3. Generate an App Password: https://myaccount.google.com/apppasswords\n" +
                    "# 4. Set sender.password to your App Password\n" +
                    "# For other SMTP providers, update smtp.host and smtp.port accordingly.");
            System.out.println("✅ Default email configuration saved to " + CONFIG_FILE);
        } catch (IOException e) {
            System.err.println("❌ Failed to save email configuration: " + e.getMessage());
        }
    }

    /**
     * Gets Properties object configured for JavaMail Session.
     */
    public Properties getMailProperties() {
        Properties props = new Properties();
        props.put("mail.smtp.host", smtpHost);
        props.put("mail.smtp.port", smtpPort);
        props.put("mail.smtp.auth", String.valueOf(enableAuth));
        props.put("mail.smtp.starttls.enable", String.valueOf(enableTLS));
        props.put("mail.smtp.ssl.protocols", "TLSv1.2");
        props.put("mail.smtp.connectiontimeout", "5000");
        props.put("mail.smtp.timeout", "5000");
        return props;
    }

    // Getters
    public String getSmtpHost() { return smtpHost; }
    public String getSmtpPort() { return smtpPort; }
    public String getSenderEmail() { return senderEmail; }
    public String getSenderPassword() { return senderPassword; }
    public String getSenderName() { return senderName; }
    public String getBaseUrl() { return baseUrl; }
    public boolean isAuthEnabled() { return enableAuth; }
    public boolean isTLSEnabled() { return enableTLS; }
    public boolean isEmailSendingEnabled() { return enableEmailSending; }

    // Setters (for runtime configuration)
    public void setSmtpHost(String smtpHost) { this.smtpHost = smtpHost; }
    public void setSmtpPort(String smtpPort) { this.smtpPort = smtpPort; }
    public void setSenderEmail(String senderEmail) { this.senderEmail = senderEmail; }
    public void setSenderPassword(String senderPassword) { this.senderPassword = senderPassword; }
    public void setSenderName(String senderName) { this.senderName = senderName; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public void setEnableAuth(boolean enableAuth) { this.enableAuth = enableAuth; }
    public void setEnableTLS(boolean enableTLS) { this.enableTLS = enableTLS; }
    public void setEnableEmailSending(boolean enableEmailSending) { this.enableEmailSending = enableEmailSending; }

    /**
     * Validates if email configuration is properly set up.
     */
    public boolean isConfigured() {
        return !senderPassword.equals("your-app-password-here") 
                && !senderEmail.contains("example.com")
                && enableEmailSending;
    }

    /**
     * Gets a user-friendly status message about email configuration.
     */
    public String getStatusMessage() {
        if (!enableEmailSending) {
            return "❌ Email sending is disabled. Enable in email.properties";
        }
        if (!isConfigured()) {
            return "⚠️  Email not configured. Update email.properties with your SMTP credentials.";
        }
        return "✅ Email sending is enabled and configured.";
    }
}

