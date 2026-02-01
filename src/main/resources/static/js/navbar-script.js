document.addEventListener('DOMContentLoaded', () => {
    const menuBtn = document.getElementById('mobile-menu-btn');
    const menu = document.getElementById('mobile-menu');
    const menuIcon = menuBtn.querySelector('i');

    menuBtn.addEventListener('click', (e) => {
        e.stopPropagation();
        const isHidden = menu.classList.contains('hidden');
        
        if (isHidden) {
            menu.classList.remove('hidden');
            menuIcon.classList.replace('ph-list', 'ph-x');
        } else {
            menu.classList.add('hidden');
            menuIcon.classList.replace('ph-x', 'ph-list');
        }
    });

    document.addEventListener('click', (e) => {
        if (!menu.contains(e.target) && !menuBtn.contains(e.target)) {
            menu.classList.add('hidden');
            menuIcon.classList.replace('ph-x', 'ph-list');
        }
    });

    menu.querySelectorAll('a').forEach(link => {
        link.addEventListener('click', () => {
            menu.classList.add('hidden');
            menuIcon.classList.replace('ph-x', 'ph-list');
        });
    });
});
