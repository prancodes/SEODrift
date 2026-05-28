/**
 * Theme Management Script
 * Handles Dark/Light mode toggling and persistence.
*/

const ThemeManager = {
    init() {
        this.toggleBtn = document.getElementById('theme-toggle');
        this.html = document.documentElement;

        // Apply theme immediately based on local storage or system preference
        const preferredTheme = this.getPreferredTheme();
        this.applyTheme(preferredTheme);

        // Event Listener
        if (this.toggleBtn) {
            this.toggleBtn.addEventListener('click', () => this.toggleTheme());
        }
    },

    getPreferredTheme() {
        if ('theme' in localStorage) {
            return localStorage.theme;
        }
        return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
    },

    applyTheme(theme) {
        if (theme === 'dark') {
            this.html.classList.add('dark');
            localStorage.theme = 'dark';
        } else {
            this.html.classList.remove('dark');
            localStorage.theme = 'light';
        }
    },

    toggleTheme() {
        const isDark = this.html.classList.contains('dark');
        this.applyTheme(isDark ? 'light' : 'dark');
    }
};

// Run immediately to prevent FOUC
ThemeManager.init();

// Re-run on turbo:load to ensure elements are attached after navigation
document.addEventListener('turbo:load', () => {
    ThemeManager.init();
});