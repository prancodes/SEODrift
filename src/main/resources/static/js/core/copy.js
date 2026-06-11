/**
 * Copies tags to clipboard, shows a toast, and updates the button state.
 * Refined for SEODrift's professional UI/UX.
 * @param {string} tagsString - The tags to copy.
 * @param {HTMLElement} button - The button element that was clicked.
 */
function copyTags(tagsString, button) {
    // 1. Validation to prevent empty copies
    if (!tagsString) {
        return;
    }

    // 2. Use the modern Clipboard API
    navigator.clipboard.writeText(tagsString).then(() => {
        
        // --- UI UPDATE: Change Button State ---
        if (button) {
            // Save original state to revert after the animation
            const originalHTML = button.innerHTML;
            
            // Update to "Success" state with checkmark icon
            button.innerHTML = `<i class="ph-bold ph-check"></i> <span>Copied!</span>`;
            
            // Apply high-contrast success styling (Matches the Brand Blue)
            // Using '!' to ensure these styles override default hover states during the 2s window
            button.classList.add('!bg-brand-600', '!text-white', '!border-brand-600', 'scale-95');
            
            // Revert the button to its original state after 2 seconds
            setTimeout(() => {
                button.innerHTML = originalHTML;
                button.classList.remove('!bg-brand-600', '!text-white', '!border-brand-600', 'scale-95');
            }, 2000);
        }

        // --- TOAST NOTIFICATION ---
        if (window.showToast) {
            window.showToast('Success', 'Tags copied to clipboard!', 'success');
        }

    }).catch(err => {
        console.error('Failed to copy: ', err);
        if (window.showToast) {
            window.showToast('Error', 'Could not copy tags.', 'error');
        }
    });
}