package com.greengrocer.app;

import com.greengrocer.models.User;
import com.greengrocer.models.ShoppingCart;

/**
 * manages the current user session, its singleton because there can be only one session at a time
 * 
 * @author Barkev Şarklı
 * @version 1.0
 * @since 24.12.2025
 */
public class SessionManager
{
    private User currentUser;

    private static SessionManager instance; // Singleton instance
    
    private ShoppingCart shoppingCart;

    private SessionManager()
    {
        // ne zaman şunu yazmayı unutsam 1 saat debug yapıyorum
    }
    
    /** we use a singleton class, and before we create we check if there is already an instance
     * @return SessionManager instance
     * @author Barkev Şarklı
     * @version 1.0
     */
    public static synchronized SessionManager getInstance()
    {
        if (instance == null)
            instance = new SessionManager();

        return instance;
    }

    /**
     * login a user and create a new session.
     * 
     * @param user user to login
     * @author Barkev Şarklı
     * @version 1.0
     */
    public void login(User user)
    {
        this.currentUser = user;
        this.shoppingCart = new ShoppingCart();
    }

    /**
     * Logs out the current user and clears session.
     * @author Barkev Şarklı    
     * @version 1.0
     */
    public void logout()
    {
        this.currentUser = null;
        this.shoppingCart = null;
    }

    /**
     * Checks if a user is currently logged in.
     * @author Barkev Şarklı
     * @version 1.0
     * @return true if logged in, false otherwise
     */
    public boolean isLoggedIn()
    {
        return currentUser != null;
    }

    /**
     * get the currently logged in user.
     * 
     * @return current user or null if not logged in
     * @author Barkev Şarklı
     * @version 1.0
     */
    public User getCurrentUser()
    {
        return currentUser;
    }

    /**
     * Gets the current shopping cart.
     * Creates a new one if none exists.
     * 
     * @return Shopping cart
     */
    public ShoppingCart getShoppingCart()
    {
        if (shoppingCart == null)
        {
            shoppingCart = new ShoppingCart();
        }
        return shoppingCart;
    }

    /**
     * Clears the shopping cart.
     */
    public void clearCart()
    {
        shoppingCart = new ShoppingCart();
    }

    /**
     * Updates the current user's data.
     * 
     * @param user Updated user
     */
    public void updateUser(User user)
    {
        this.currentUser = user;
    }

    /**
     * Checks if current user has specified role.
     * 
     * @param role Role to check
     * @return true if user has role
     */
    public boolean hasRole(String role)
    {
        return currentUser != null && currentUser.getRole().equalsIgnoreCase(role);
    }

    /**
     * Gets the current user ID.
     * 
     * @return User ID or -1 if not logged in
     */
    public int getCurrentUserId()
    {
        return currentUser != null ? currentUser.getId() : -1;
    }
}
