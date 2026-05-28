package com.seo.project.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Global Exception Handler to prevent "Whitelabel Error Pages".
 * It catches various exceptions and redirects users to a friendly error view.
 */
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles general internal server errors (500).
     */
    @ExceptionHandler(Exception.class)
    public String handleGeneralException(Exception ex, Model model) {
        log.error("CRITICAL ERROR: Uncaught exception in request processing.", ex);
        model.addAttribute("errorMessage", "Something went wrong on our end. Please try again later.");
        model.addAttribute("details", ex.getMessage());
        model.addAttribute("status", 500);
        return "error"; 
    }

    /**
     * Handles 404 Not Found errors.
     */
    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    public String handleNotFound(Exception ex, Model model) {
        if (ex instanceof NoHandlerFoundException nhf) {
            log.warn("404 Error: User attempted to access non-existent path: {}", nhf.getRequestURL());
        } else if (ex instanceof NoResourceFoundException nrf) {
            log.warn("404 Error: Resource not found: {}", nrf.getResourcePath());
        }
        model.addAttribute("errorMessage", "The page you are looking for doesn't exist.");
        model.addAttribute("status", 404);
        return "error";
    }
}
