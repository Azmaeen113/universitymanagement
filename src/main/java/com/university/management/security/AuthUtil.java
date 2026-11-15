package com.university.management.security;

import org.springframework.security.core.Authentication;

/** Small helper to read the role of the logged-in user. */
public final class AuthUtil {

    private AuthUtil() {
    }

    public static boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
    }
}
