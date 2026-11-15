package com.university.management.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.university.management.dto.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/**
 * Called when a user who is NOT logged in asks for a protected URL.
 * REST call (/api/...)  -> 401 with JSON body.
 * Browser page          -> redirect to the login page.
 */
@Component
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException exception) throws IOException {
        if (!request.getRequestURI().startsWith(request.getContextPath() + "/api/")) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        String message = "Authentication is required";
        if (exception instanceof DisabledException) {
            message = "Your account is pending admin approval.";
        } else if (exception instanceof BadCredentialsException) {
            message = "Invalid email or password";
        }

        ApiError body = new ApiError(HttpStatus.UNAUTHORIZED.value(), "Unauthorized", message);
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
