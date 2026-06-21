const AuthModalManager = {
    init() {
        this.loginModal = document.getElementById('login-modal');
        this.modalBackdrop = document.getElementById('modal-backdrop');
        this.loginCard = document.getElementById('login-card');

        // Close strictly when clicking the backdrop
        if (this.modalBackdrop && !this.modalBackdrop._hasListener) {
            this.modalBackdrop.addEventListener('click', (e) => {
                if (e.target === this.modalBackdrop) {
                    this.hideLoginModal();
                }
            });
            this.modalBackdrop._hasListener = true;
        }

        // Localized hover gradient for Google sign-in button to avoid global mousemove layout thrashing
        const btn = document.querySelector('.google-signin-btn');
        if (btn && !btn._hasMouseListener) {
            btn.addEventListener('mousemove', (e) => {
                const rect = btn.getBoundingClientRect();
                const x = ((e.clientX - rect.left) / rect.width) * 100;
                const y = ((e.clientY - rect.top) / rect.height) * 100;
                btn.style.setProperty('--x', `${x}%`);
                btn.style.setProperty('--y', `${y}%`);
            });
            btn._hasMouseListener = true;
        }
    },

    showLoginModal() {
        if (!this.loginModal) return;
        this.loginModal.classList.remove('hidden');
        document.body.classList.add('overflow-hidden', 'modal-blur-active');

        requestAnimationFrame(() => {
            this.modalBackdrop.classList.remove('opacity-0');
            this.modalBackdrop.classList.add('opacity-100');
            this.loginCard.classList.remove('opacity-0', 'translate-y-12', 'scale-95');
            this.loginCard.classList.add('modal-celestial-enter');
        });
    },

    hideLoginModal() {
        if (!this.loginModal) return;
        this.modalBackdrop.classList.remove('opacity-100');
        this.modalBackdrop.classList.add('opacity-0');
        
        this.loginCard.classList.remove('modal-celestial-enter');
        this.loginCard.classList.add('opacity-0', 'translate-y-12', 'scale-95');
        
        document.body.classList.remove('modal-blur-active');

        setTimeout(() => {
            this.loginModal.classList.add('hidden');
            document.body.classList.remove('overflow-hidden');
        }, 800);
    }
};

// Global exports for inline onclicks if any
window.showLoginModal = () => AuthModalManager.showLoginModal();
window.hideLoginModal = () => AuthModalManager.hideLoginModal();

let isLoggingIn = false;

// Global listeners (added once)
document.addEventListener('click', (e) => {
    const authLink = e.target.closest('[data-auth="true"]');
    if (authLink) {
        const isAuth = document.body.getAttribute('data-authenticated') === 'true';
        if (!isAuth) {
            e.preventDefault();
            AuthModalManager.showLoginModal();
        }
    }

    const oauthLink = e.target.closest('a[href^="/oauth2/authorization/"]');
    if (oauthLink) {
        e.preventDefault();
        if (isLoggingIn) return;
        isLoggingIn = true;

        // Show spinner / loading feedback inside the clicked link/button
        const isGoogleBtn = oauthLink.classList.contains('google-signin-btn');
        if (isGoogleBtn) {
            const btnText = oauthLink.querySelector('span');
            if (btnText) {
                btnText.textContent = "Connecting to Google...";
            }
        } else {
            oauthLink.innerHTML = `<i class="ph-bold ph-spinner animate-spin mr-1.5"></i> Connecting...`;
        }

        oauthLink.classList.add('pointer-events-none', 'opacity-80');

        // Set the redirect cookie to the current location URL
        document.cookie = "seodrift_login_redirect=" + encodeURIComponent(window.location.href) + "; path=/; max-age=300; SameSite=Lax";
        window.location.href = oauthLink.getAttribute('href');
    }
});

document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape' && AuthModalManager.loginModal && !AuthModalManager.loginModal.classList.contains('hidden')) {
        AuthModalManager.hideLoginModal();
    }
});

document.addEventListener('turbo:load', () => {
    AuthModalManager.init();
    
    // Auto-bounce back to the previous page if returning from an expired session login
    const redirectUrl = localStorage.getItem('seodrift_redirect_after_login');
    const isAuth = document.body.getAttribute('data-authenticated') === 'true';
    if (redirectUrl && isAuth) {
        localStorage.removeItem('seodrift_redirect_after_login');
        if (window.location.href !== redirectUrl) {
            window.location.href = redirectUrl;
        }
    }
});
