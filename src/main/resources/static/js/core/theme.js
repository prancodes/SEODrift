/**
 * Theme Management Script
 * Handles Dark/Light mode toggling and persistence.
*/

const ThemeManager = {
    init() {
        this.toggleBtn = document.getElementById('theme-toggle');
        this.html = document.documentElement;

        // Apply theme immediately based on local storage or system preference
        // Pass false to avoid dispatching theme-changed event on initial load/navigation
        const preferredTheme = this.getPreferredTheme();
        this.applyTheme(preferredTheme, false);

        // Event Listener (ensure attached only once per element instance)
        if (this.toggleBtn && !this.toggleBtn._themeListenerAttached) {
            this.toggleBtn.addEventListener('click', () => this.toggleTheme());
            this.toggleBtn._themeListenerAttached = true;
        }
    },

    getPreferredTheme() {
        if ('theme' in localStorage) {
            return localStorage.theme;
        }
        return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
    },

    applyTheme(theme, dispatchEvent = true) {
        if (theme === 'dark') {
            this.html.classList.add('dark');
            localStorage.theme = 'dark';
        } else {
            this.html.classList.remove('dark');
            localStorage.theme = 'light';
        }
        if (dispatchEvent) {
            document.dispatchEvent(new CustomEvent('theme-changed', { detail: { theme } }));
        }
    },

    toggleTheme() {
        const isDark = this.html.classList.contains('dark');
        this.applyTheme(isDark ? 'light' : 'dark', true);
    }
};

// Run immediately to prevent FOUC
ThemeManager.init();

// Re-run on turbo:load to ensure elements are attached after navigation
document.addEventListener('turbo:load', () => {
    ThemeManager.init();
});