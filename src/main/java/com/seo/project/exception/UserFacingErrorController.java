package com.seo.project.exception;

import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import jakarta.servlet.http.HttpServletRequest;

/**
 * UserFacingErrorController handles user-visible error states that originate
 * from application-level redirects or general container errors.
 *
 * All errors point to the same URL (/error) and same view (error.html)
 * for a unified and clean error handling experience.
 */
@Controller
public class UserFacingErrorController implements ErrorController {

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        String reason = request.getParameter("reason");

        if ("invalid_redirect".equals(reason)) {
            model.addAttribute("status", 400);
            model.addAttribute("errorMessage",
                    "Your session contained an unusual redirect that was blocked for your security. " +
                    "Please log in again and navigate normally.");
            return "error";
        }

        if ("payment_failed".equals(reason)) {
            model.addAttribute("status", 400);
            model.addAttribute("errorMessage",
                    "We could not verify your payment with Dodo Payments. " +
                    "If you were charged, please contact support. Otherwise, please try the upgrade again.");
            return "error";
        }

        // Standard servlet error attributes for generic 404 / 500 container-level errors
        Object status = request.getAttribute("jakarta.servlet.error.status_code");
        Object exception = request.getAttribute("jakarta.servlet.error.exception");
        Object message = request.getAttribute("jakarta.servlet.error.message");

        int statusCode = status != null ? Integer.parseInt(status.toString()) : 500;
        model.addAttribute("status", statusCode);

        if (statusCode == 404) {
            model.addAttribute("errorMessage", "The page you are looking for doesn't exist.");
        } else {
            model.addAttribute("errorMessage", message != null && !message.toString().isEmpty() 
                    ? message.toString() 
                    : "Something went wrong on our end. Please try again later.");
        }

        if (exception != null) {
            model.addAttribute("details", ((Throwable) exception).getMessage());
        }

        return "error";
    }
}
