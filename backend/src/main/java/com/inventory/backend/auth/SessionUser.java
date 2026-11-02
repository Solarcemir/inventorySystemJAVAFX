package com.inventory.backend.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.io.Serializable;

/** The logged-in user, kept in the server-side HTTP session. */
public record SessionUser(String username, String role) implements Serializable {

    public static final String SESSION_KEY = "sessionUser";

    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }

    /** Returns the user of this request, or null when nobody is logged in. */
    public static SessionUser from(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session != null ? (SessionUser) session.getAttribute(SESSION_KEY) : null;
    }
}
