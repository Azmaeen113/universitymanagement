package com.university.management.exception;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/** Same idea as GlobalExceptionHandler, but for the Thymeleaf pages: shows an HTML error page. */
@ControllerAdvice(basePackages = "com.university.management.controller.web")
public class WebExceptionHandler {

    @ExceptionHandler(AccessDeniedException.class)
    public String handleAccessDenied(HttpServletResponse response) {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        return "error/403";
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public String handleNotFound(HttpServletResponse response) {
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        return "error/404";
    }
}
