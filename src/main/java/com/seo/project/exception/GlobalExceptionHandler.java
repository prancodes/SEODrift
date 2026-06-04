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
     * Known browser/system probes (.well-known, devtools, favicon) are suppressed to DEBUG
     * to avoid polluting logs with noise on every page load in development.
     */
    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    public String handleNotFound(Exception ex, Model model) {
        String path = "";
        if (ex instanceof NoHandlerFoundException nhf) {
            path = nhf.getRequestURL();
        } else if (ex instanceof NoResourceFoundException nrf) {
            path = nrf.getResourcePath();
        }

        // Silently ignore known browser/tool probes — these are not real user errors
        if (isBrowserProbe(path)) {
            log.debug("Browser probe 404 (suppressed): {}", path);
        } else {
            log.warn("404 Error: Resource not found: {}", path);
        }

        model.addAttribute("errorMessage", "The page you are looking for doesn't exist.");
        model.addAttribute("status", 404);
        return "error";
    }

    /**
     * Returns true for paths that browsers/tools automatically probe
     * and that are not real user-navigated URLs.
     */
    private boolean isBrowserProbe(String path) {
        if (path == null) return false;
        return path.contains(".well-known")          // Chrome DevTools, ACME challenges
                || path.contains("devtools")          // Chrome DevTools JSON
                || path.endsWith("favicon.ico")       // Browser favicon fallback
                || path.endsWith("robots.txt")        // SEO crawlers
                || path.endsWith("sitemap.xml")       // SEO crawlers
                || path.endsWith("apple-touch-icon.png"); // iOS home screen
    }
}
