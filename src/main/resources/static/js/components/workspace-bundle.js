import './workspace-core.js';
import './workspace-editor.js';
import './workspace-audit.js';
import './workspace-api.js';
import './workspace-publish.js';

function initWorkspaceCanvasCoordinator() {
    if (typeof window.Workspace !== 'undefined' && window.Workspace.init) {
        window.Workspace.init();
    }
}

document.addEventListener('turbo:load', initWorkspaceCanvasCoordinator);
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initWorkspaceCanvasCoordinator);
} else {
    initWorkspaceCanvasCoordinator();
}
