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

        // --- TOAST NOTIFICATION: Styled for the New UI ---
        // Creating a "Glass-morphism" style toast that matches the site's cards
        const toast = document.createElement('div');
        
        // Tailwind classes for a sleek, centered mobile-responsive toast
        toast.className = `
            fixed bottom-8 left-1/2 -translate-x-1/2 md:left-auto md:right-8 md:translate-x-0
            bg-white/90 dark:bg-gray-800/90 backdrop-blur-md 
            text-gray-900 dark:text-white 
            px-6 py-4 rounded-2xl shadow-2xl border border-gray-100 dark:border-gray-700
            transform transition-all duration-500 z-50 
            flex items-center gap-3 opacity-0 translate-y-4
        `;
        
        // Unified Inter typography and Phospor icons
        toast.innerHTML = `
            <div class="p-1.5 bg-green-100 dark:bg-green-900/50 text-green-600 dark:text-green-400 rounded-lg">
                <i class="ph-fill ph-check-circle text-xl"></i> 
            </div>
            <div class="flex flex-col">
                <span class="font-black text-xs uppercase tracking-widest text-brand-600 dark:text-brand-400">Success</span>
                <span class="font-bold text-sm">Tags copied to clipboard!</span>
            </div>
        `;
        
        document.body.appendChild(toast);

        // Animate In: Uses requestAnimationFrame for smoother performance
        requestAnimationFrame(() => {
            toast.classList.remove('opacity-0', 'translate-y-4');
        });
        
        // Animate Out & Clean up DOM after 3 seconds
        setTimeout(() => {
            toast.classList.add('opacity-0', 'translate-y-2');
            setTimeout(() => toast.remove(), 500);
        }, 3000);

    }).catch(err => {
        console.error('Failed to copy: ', err);
        // Fallback for older browsers or permission denials
        alert('Could not copy tags. Please try selecting them manually.');
    });
}