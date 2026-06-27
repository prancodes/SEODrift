// settings-console.js - Uses event delegation for Turbo compatibility

// Global listeners (added once)
if (!window._settingsGlobalListenersAttached) {
    document.addEventListener("click", async function(e) {
        // 1. Email Notifications Toggle Logic
        const emailToggleBtn = e.target.closest("#email-toggle-btn");
        if (emailToggleBtn) {
            e.preventDefault();
            
            // Prevent double-clicks
            if (emailToggleBtn.dataset.isFetching === "true") return;
            emailToggleBtn.dataset.isFetching = "true";

            try {
                // Fetch CSRF token from meta tags
                const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
                const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');
                
                const headers = {};
                if (csrfHeader && csrfToken) {
                    headers[csrfHeader] = csrfToken;
                }
                
                // OPTIMISTIC UI: Visually toggle immediately
                const thumb = emailToggleBtn.querySelector('div');
                const isCurrentlyEnabled = emailToggleBtn.classList.contains('bg-brand-500');
                
                // Toggle visually
                if (isCurrentlyEnabled) {
                    emailToggleBtn.classList.remove('bg-brand-500');
                    emailToggleBtn.classList.add('bg-gray-300', 'dark:bg-gray-700');
                    thumb.classList.remove('translate-x-6');
                    thumb.classList.add('translate-x-1');
                } else {
                    emailToggleBtn.classList.remove('bg-gray-300', 'dark:bg-gray-700');
                    emailToggleBtn.classList.add('bg-brand-500');
                    thumb.classList.remove('translate-x-1');
                    thumb.classList.add('translate-x-6');
                }

                try {
                    const response = await fetch("/settings/toggle-email-notifications", {
                        method: "POST",
                        headers: headers
                    });
                    
                    if (response.ok) {
                        const data = await response.json();
                        if (data.enabled !== !isCurrentlyEnabled) {
                             if (data.enabled) {
                                emailToggleBtn.classList.remove('bg-gray-300', 'dark:bg-gray-700');
                                emailToggleBtn.classList.add('bg-brand-500');
                                thumb.classList.remove('translate-x-1');
                                thumb.classList.add('translate-x-6');
                             } else {
                                emailToggleBtn.classList.remove('bg-brand-500');
                                emailToggleBtn.classList.add('bg-gray-300', 'dark:bg-gray-700');
                                thumb.classList.remove('translate-x-6');
                                thumb.classList.add('translate-x-1');
                             }
                        }
                        
                        if (window.showToast) {
                            window.showToast("Settings Updated", "Email preferences saved successfully.", "success");
                        }
                    } else {
                        throw new Error("Server returned " + response.status);
                    }
                } catch (err) {
                    console.error("Error toggling email notifications:", err);
                    // REVERT OPTIMISTIC UPDATE ON FAILURE
                    if (isCurrentlyEnabled) {
                        emailToggleBtn.classList.remove('bg-gray-300', 'dark:bg-gray-700');
                        emailToggleBtn.classList.add('bg-brand-500');
                        thumb.classList.remove('translate-x-1');
                        thumb.classList.add('translate-x-6');
                    } else {
                        emailToggleBtn.classList.remove('bg-brand-500');
                        emailToggleBtn.classList.add('bg-gray-300', 'dark:bg-gray-700');
                        thumb.classList.remove('translate-x-6');
                        thumb.classList.add('translate-x-1');
                    }
                    if (window.showToast) {
                        window.showToast("Update Failed", "Could not save email preferences. Reverted.", "error");
                    }
                }
            } catch (e) {
                console.error("Critical error in toggle:", e);
            } finally {
                emailToggleBtn.dataset.isFetching = "false";
            }
        }
    });

    // 2. Delete Account Modal Logic
    document.addEventListener("input", function(e) {
        if (e.target.id === "confirmEmailInput") {
            const confirmDeleteBtn = document.getElementById("confirmDeleteBtn");
            const userEmail = window.userEmailForDelete || "";
            
            if (confirmDeleteBtn && userEmail) {
                if (e.target.value.trim().toLowerCase() === userEmail.toLowerCase()) {
                    confirmDeleteBtn.disabled = false;
                } else {
                    confirmDeleteBtn.disabled = true;
                }
            }
        }
    });

    window._settingsGlobalListenersAttached = true;
}
