const NavbarManager = {
    init() {
        this.menuBtn = document.getElementById('mobile-menu-btn');
        this.menu = document.getElementById('mobile-menu');
        this.profileBtn = document.getElementById('profile-dropdown-btn');
        this.profileMenu = document.getElementById('profile-dropdown-menu');
        this.menuIcon = this.menuBtn ? this.menuBtn.querySelector('i') : null;

        if (this.menuBtn && this.menu && !this.menuBtn._hasListener) {
            this.menuBtn.addEventListener('click', (e) => {
                e.stopPropagation();
                this.toggleMobileMenu();
            });
            this.menuBtn._hasListener = true;
        }

        if (this.profileBtn && this.profileMenu && !this.profileBtn._hasListener) {
            this.profileBtn.addEventListener('click', (e) => {
                e.stopPropagation();
                this.toggleProfileMenu();
            });
            this.profileBtn._hasListener = true;
        }

        // Close on link click
        document.querySelectorAll('#mobile-menu a, #profile-dropdown-menu a').forEach(link => {
            if (!link._hasListener) {
                link.addEventListener('click', () => this.closeAll());
                link._hasListener = true;
            }
        });
    },

    toggleMobileMenu() {
        const isHidden = this.menu.classList.contains('hidden');
        if (isHidden) {
            this.menu.classList.remove('hidden');
            if (this.menuIcon) this.menuIcon.classList.replace('ph-list', 'ph-x');
            if (this.profileMenu) this.profileMenu.classList.add('hidden');
        } else {
            this.menu.classList.add('hidden');
            if (this.menuIcon) this.menuIcon.classList.replace('ph-x', 'ph-list');
        }
    },

    toggleProfileMenu() {
        const isHidden = this.profileMenu.classList.contains('hidden');
        if (isHidden) {
            this.profileMenu.classList.remove('hidden');
            if (this.menu) this.menu.classList.add('hidden');
            if (this.menuIcon) this.menuIcon.classList.replace('ph-x', 'ph-list');
        } else {
            this.profileMenu.classList.add('hidden');
        }
    },

    closeAll() {
        if (this.menu) this.menu.classList.add('hidden');
        if (this.menuIcon) this.menuIcon.classList.replace('ph-x', 'ph-list');
        if (this.profileMenu) this.profileMenu.classList.add('hidden');
    }
};

// Global listeners (added once)
if (!window._navbarGlobalListenersAttached) {
    document.addEventListener('click', (e) => {
        if (NavbarManager.menu && !NavbarManager.menu.contains(e.target) && 
            NavbarManager.menuBtn && !NavbarManager.menuBtn.contains(e.target)) {
            NavbarManager.closeAll();
        }
        
        if (NavbarManager.profileMenu && !NavbarManager.profileMenu.contains(e.target) && 
            NavbarManager.profileBtn && !NavbarManager.profileBtn.contains(e.target)) {
            NavbarManager.profileMenu.classList.add('hidden');
        }
    });
    window._navbarGlobalListenersAttached = true;
}

document.addEventListener('turbo:load', () => {
    NavbarManager.init();
});
