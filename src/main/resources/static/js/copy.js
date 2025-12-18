/**
 * Copies tags to clipboard, shows a toast, and updates the button state.
 * @param {string} tagsString - The tags to copy.
 * @param {HTMLElement} button - The button element that was clicked.
 */
function copyTags(tagsString, button) {
    // 1. Validation
    if (!tagsString) {
        alert("No tags to copy!");
        return;
    }

    // 2. Copy to Clipboard
    navigator.clipboard.writeText(tagsString).then(() => {
        
        // --- UI UPDATE: Change Button State ---
        if (button) {
            // Save original state to revert later
            const originalHTML = button.innerHTML;
            
            // 1. Change Text and Icon to Checkmark
            button.innerHTML = `<i class="ph-bold ph-check"></i> <span>Copied!</span>`;
            
            // 2. Force "Success" Styling (Blue Theme)
            // We use '!' (important) to override any existing dark/light mode colors immediately
            button.classList.add('!bg-brand-600', '!text-white', '!border-brand-600');
            
            // 3. Revert after 2 seconds
            setTimeout(() => {
                button.innerHTML = originalHTML;
                button.classList.remove('!bg-brand-600', '!text-white', '!border-brand-600');
            }, 2000);
        }

        // --- TOAST NOTIFICATION ---
        const toast = document.createElement('div');
        toast.className = 'fixed bottom-5 right-5 bg-gray-900 dark:bg-white text-white dark:text-gray-900 px-6 py-3 rounded-xl shadow-lg transform transition-all duration-300 z-50 flex items-center gap-3 opacity-0 translate-y-2';
        toast.innerHTML = `
            <i class="ph-fill ph-check-circle text-brand-500 text-xl"></i> 
            <span class="font-medium">Tags copied to clipboard!</span>
        `;
        
        document.body.appendChild(toast);

        // Animate In
        requestAnimationFrame(() => {
            toast.classList.remove('opacity-0', 'translate-y-2');
        });
        
        // Animate Out & Remove after 3 seconds
        setTimeout(() => {
            toast.classList.add('opacity-0', 'translate-y-2');
            setTimeout(() => toast.remove(), 300);
        }, 3000);

    }).catch(err => {
        console.error('Failed to copy: ', err);
        alert('Failed to copy tags.');
    });
}