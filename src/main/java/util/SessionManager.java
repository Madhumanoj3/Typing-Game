package util;

import model.User;

/**
 * Simple in-memory session holder.
 * Call {@link #login(User)} after authentication succeeds and
 * {@link #logout()} when the user signs out.
 */
public class SessionManager {

    private static SessionManager instance;
    private User currentUser;

    private SessionManager() { }

    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    public void login(User user) {
        this.currentUser = user;
    }

    public void logout() {
        this.currentUser = null;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }

    /** Convenience: returns username or "Guest" if not logged in. */
    public String getUsername() {
        return isLoggedIn() ? currentUser.getUsername() : "Guest";
    }
}
