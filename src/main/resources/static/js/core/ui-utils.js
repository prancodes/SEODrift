/**
 * Global UI Utilities for SEODrift
 * Includes Toast Notifications and Confirmation Modals
 */

// Global Toast System
window.showToast = function(title, message, type = 'success') {
    const toast = document.createElement('div');
    
    let icon = '<i class="ph-fill ph-check-circle text-xl"></i>';
    let colorClass = 'bg-green-100 dark:bg-green-900/50 text-green-600 dark:text-green-400';
    let titleClass = 'text-green-600 dark:text-green-400';

    if (type === 'error') {
        icon = '<i class="ph-fill ph-warning-circle text-xl"></i>';
        colorClass = 'bg-red-100 dark:bg-red-900/50 text-red-600 dark:text-red-400';
        titleClass = 'text-red-600 dark:text-red-400';
    } else if (type === 'info') {
        icon = '<i class="ph-fill ph-info text-xl"></i>';
        colorClass = 'bg-blue-100 dark:bg-blue-900/50 text-blue-600 dark:text-blue-400';
        titleClass = 'text-blue-600 dark:text-blue-400';
    }

    toast.className = `
        fixed bottom-8 left-4 right-4 md:left-auto md:right-8 md:translate-x-0
        bg-white/90 dark:bg-gray-800/90 backdrop-blur-md 
        text-gray-900 dark:text-white 
        px-6 py-4 rounded-2xl shadow-2xl border border-gray-100 dark:border-gray-700
        transform transition-all duration-500 z-[100] 
        flex items-center gap-3 opacity-0 translate-y-4
        md:max-w-sm cursor-pointer
    `;
    
    toast.innerHTML = `
        <div class="p-1.5 rounded-lg ${colorClass}">
            ${icon}
        </div>
        <div class="flex flex-col">
            <span class="font-black text-xs uppercase tracking-widest ${titleClass}">${title}</span>
            <span class="font-bold text-sm">${message}</span>
        </div>
    `;
    
    toast.onclick = () => {
        toast.classList.add('opacity-0', 'translate-y-2');
        setTimeout(() => toast.remove(), 500);
    };

    document.body.appendChild(toast);

    requestAnimationFrame(() => {
        toast.classList.remove('opacity-0', 'translate-y-4');
    });
    
    setTimeout(() => {
        if (document.body.contains(toast)) {
            toast.classList.add('opacity-0', 'translate-y-2');
            setTimeout(() => toast.remove(), 500);
        }
    }, 4000);
};

// Global Premium Confirmation Modal
window.showConfirm = function(title, message, onConfirm) {
    const overlay = document.createElement('div');
    overlay.className = 'fixed inset-0 bg-gray-900/40 dark:bg-gray-900/60 backdrop-blur-sm z-[200] flex items-center justify-center opacity-0 transition-opacity duration-300';
    
    const modal = document.createElement('div');
    modal.className = 'bg-white dark:bg-gray-800 rounded-3xl p-6 md:p-8 max-w-sm w-[calc(100%-2rem)] mx-4 shadow-2xl border border-gray-100 dark:border-gray-700 transform scale-95 transition-transform duration-300';
    
    modal.innerHTML = `
        <div class="w-12 h-12 rounded-full bg-red-100 dark:bg-red-900/30 flex items-center justify-center text-red-500 mb-4">
            <i class="ph-bold ph-warning text-2xl"></i>
        </div>
        <h3 class="text-xl font-black text-gray-900 dark:text-white mb-2 tracking-tight">${title}</h3>
        <p class="text-sm font-bold text-gray-500 dark:text-gray-400 mb-8">${message}</p>
        <div class="flex gap-3 w-full">
            <button id="btnCancel" class="flex-1 px-4 py-3 bg-gray-100 hover:bg-gray-200 dark:bg-gray-700 dark:hover:bg-gray-600 text-gray-700 dark:text-gray-300 rounded-xl text-xs font-black uppercase tracking-widest transition-all cursor-pointer">Cancel</button>
            <button id="btnConfirm" class="flex-1 px-4 py-3 bg-red-500 hover:bg-red-600 text-white rounded-xl text-xs font-black uppercase tracking-widest shadow-lg shadow-red-500/20 transition-all cursor-pointer">Delete</button>
        </div>
    `;
    
    overlay.appendChild(modal);
    document.body.appendChild(overlay);
    
    requestAnimationFrame(() => {
        overlay.classList.remove('opacity-0');
        modal.classList.remove('scale-95');
    });
    
    const closeModal = () => {
        overlay.classList.add('opacity-0');
        modal.classList.add('scale-95');
        setTimeout(() => overlay.remove(), 300);
    };
    
    modal.querySelector('#btnCancel').onclick = closeModal;
    modal.querySelector('#btnConfirm').onclick = () => {
        closeModal();
        if (onConfirm) onConfirm();
    };
};

// Global History Delete Logic
window.deleteHistoryItem = function(id, btnElement) {
    if (!window.showConfirm) {
        if (!confirm('Are you sure you want to delete this item?')) return;
        window.executeDelete(id, btnElement);
    } else {
        window.showConfirm(
            'Delete Item', 
            'Are you sure you want to permanently delete this item from your history? This action cannot be undone.', 
            () => window.executeDelete(id, btnElement)
        );
    }
};

window.executeDelete = function(id, btnElement) {
    const csrfTokenMeta = document.querySelector('meta[name="_csrf"]');
    const csrfHeaderMeta = document.querySelector('meta[name="_csrf_header"]');
    const csrfToken = csrfTokenMeta ? csrfTokenMeta.getAttribute('content') : '';
    const csrfHeader = csrfHeaderMeta ? csrfHeaderMeta.getAttribute('content') : '';

    const row = btnElement.closest('tr');
    if (row) row.style.opacity = '0.5';

    fetch(`/api/history/${id}`, {
        method: 'DELETE',
        headers: {
            [csrfHeader]: csrfToken
        }
    })
    .then(response => {
        if (!response.ok) throw new Error('Delete failed');
        return response.json();
    })
    .then(data => {
        if (row) row.remove();
        if (window.showToast) window.showToast('Deleted', 'Item permanently removed from history.', 'success');
        
        // Smoothly reload the page after a brief delay so server-rendered metrics update
        // The delay ensures the user has time to see the success toast before the DOM replaces
        setTimeout(() => {
            if (typeof Turbo !== 'undefined') {
                Turbo.visit(window.location.href, { action: "replace" });
            } else {
                location.reload();
            }
        }, 1200);
    })
    .catch(err => {
        console.error(err);
        if (row) row.style.opacity = '1';
        if (window.showToast) window.showToast('Error', 'Failed to delete item.', 'error');
    });
};
