const AuthModalManager = {
    init() {
        this.loginModal = document.getElementById('login-modal');
        this.modalBackdrop = document.getElementById('modal-backdrop');
        this.loginCard = document.getElementById('login-card');

        // Close strictly when clicking the backdrop
        if (this.modalBackdrop) {
            this.modalBackdrop.addEventListener('click', (e) => {
                if (e.target === this.modalBackdrop) {
                    this.hideLoginModal();
                }
            });
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
});

document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape' && AuthModalManager.loginModal && !AuthModalManager.loginModal.classList.contains('hidden')) {
        AuthModalManager.hideLoginModal();
    }
});

document.addEventListener('mousemove', (e) => {
    const btn = document.querySelector('.google-signin-btn');
    if (btn) {
        const rect = btn.getBoundingClientRect();
        const x = ((e.clientX - rect.left) / rect.width) * 100;
        const y = ((e.clientY - rect.top) / rect.height) * 100;
        btn.style.setProperty('--x', `${x}%`);
        btn.style.setProperty('--y', `${y}%`);
    }
});

document.addEventListener('turbo:load', () => AuthModalManager.init());

if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', () => AuthModalManager.init());
} else {
    AuthModalManager.init();
}