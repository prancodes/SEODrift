// --- SEODrift Workspace Core (Namespace & State Coordinator) ---

window.Workspace = {
    state: {
        activeTags: [],
        activeHashtags: [],
        recommendedTags: []
    },
    elements: {},
    csrf: {
        token: '',
        header: ''
    },
    
    init() {
        // CSRF Configuration
        const csrfTokenMeta = document.querySelector('meta[name="_csrf"]');
        const csrfHeaderMeta = document.querySelector('meta[name="_csrf_header"]');
        this.csrf.token = csrfTokenMeta ? csrfTokenMeta.getAttribute('content') : '';
        this.csrf.header = csrfHeaderMeta ? csrfHeaderMeta.getAttribute('content') : '';

        // Query all DOM Elements
        const draftIdInput = document.getElementById('draftId');
        if (!draftIdInput) return; // Exit if not on the workspace page
        if (draftIdInput._initialized) return;
        draftIdInput._initialized = true;
        
        this.elements.draftIdInput = draftIdInput;
        
        this.elements.topicInput = document.getElementById('topic');
        this.elements.toneSelect = document.getElementById('tone');
        this.elements.btnGenerate = document.getElementById('btnGenerate');
        this.elements.spinnerGenerate = document.getElementById('spinnerGenerate');
        
        this.elements.canvasTitle = document.getElementById('canvasTitle');
        this.elements.charCountTitle = document.getElementById('charCountTitle');
        this.elements.canvasDescription = document.getElementById('canvasDescription');
        this.elements.canvasHook = document.getElementById('canvasHook');
        
        this.elements.tagInput = document.getElementById('tagInput');
        this.elements.btnAddTag = document.getElementById('btnAddTag');
        this.elements.tagsContainer = document.getElementById('tagsContainer');
        
        this.elements.hashtagInput = document.getElementById('hashtagInput');
        this.elements.btnAddHashtag = document.getElementById('btnAddHashtag');
        this.elements.hashtagsContainer = document.getElementById('hashtagsContainer');
        
        this.elements.tagSuggestionsBox = document.getElementById('tagSuggestionsBox');
        this.elements.tagCloudContainer = document.getElementById('tagCloudContainer');
        
        this.elements.titleSuggestionsCard = document.getElementById('titleSuggestionsCard');
        this.elements.titleSuggestionsContainer = document.getElementById('titleSuggestionsContainer');

        this.elements.btnAddChapter = document.getElementById('btnAddChapter');
        this.elements.chaptersTableBody = document.getElementById('chaptersTableBody');
        this.elements.btnInsertTimestamps = document.getElementById('btnInsertTimestamps');

        this.elements.btnSaveDraft = document.getElementById('btnSaveDraft');
        this.elements.btnCopyAll = document.getElementById('btnCopyAll');
        this.elements.btnOpenPublishModal = document.getElementById('btnOpenPublishModal');

        this.elements.publishModal = document.getElementById('publishModal');
        this.elements.publishModalContent = this.elements.publishModal?.querySelector('.publish-modal-content');
        this.elements.btnClosePublishModal = document.getElementById('btnClosePublishModal');
        this.elements.publishForm = document.getElementById('publishForm');
        this.elements.publishWarnings = document.getElementById('publishWarnings');
        this.elements.publishProgressContainer = document.getElementById('publishProgressContainer');
        this.elements.publishProgressBar = document.getElementById('publishProgressBar');
        this.elements.publishProgressText = document.getElementById('publishProgressText');
        this.elements.btnConfirmPublish = document.getElementById('btnConfirmPublish');
        this.elements.publishIcon = document.getElementById('publishIcon');
        this.elements.publishBtnText = document.getElementById('publishBtnText');

        this.elements.seoScoreProgress = document.getElementById('seoScoreProgress');
        this.elements.seoScoreText = document.getElementById('seoScoreText');
        this.elements.checkTitleLength = document.getElementById('checkTitleLength');
        this.elements.checkTitleKeywords = document.getElementById('checkTitleKeywords');
        this.elements.checkDescCta = document.getElementById('checkDescCta');
        this.elements.checkTagsCount = document.getElementById('checkTagsCount');
        this.elements.checkHashtagsCount = document.getElementById('checkHashtagsCount');

        // Now trigger the specific initializations in other files if they exist
        if (typeof this.initEditor === 'function') this.initEditor();
        if (typeof this.initAudit === 'function') this.initAudit();
        if (typeof this.initApi === 'function') this.initApi();
        if (typeof this.initPublish === 'function') this.initPublish();
        
        // Restore session backup if it exists
        this.restoreSessionBackup();
    },

    handleSessionExpiration() {
        const chapters = [];
        if (this.elements.chaptersTableBody) {
            const rows = this.elements.chaptersTableBody.querySelectorAll('.chapter-row');
            rows.forEach(row => {
                const time = row.querySelector('.chapter-time').value.trim();
                const tit = row.querySelector('.chapter-title').value.trim();
                if (time || tit) {
                    chapters.push({ timestamp: time, title: tit });
                }
            });
        }

        localStorage.setItem('seodrift_draft_backup', JSON.stringify({
            title: this.elements.canvasTitle.value,
            description: this.elements.canvasDescription.value,
            hook: this.elements.canvasHook.value,
            tags: this.state.activeTags,
            hashtags: this.state.activeHashtags,
            topic: this.elements.topicInput.value,
            tone: this.elements.toneSelect.value,
            chapters: chapters
        }));
        
        if (window.showToast) {
            window.showToast('Session Expired', 'Please log in again to save your progress.', 'warning');
        }

        localStorage.setItem('seodrift_redirect_after_login', window.location.href);
        
        setTimeout(() => {
            if (window.showLoginModal) {
                window.showLoginModal();
            } else {
                window.location.href = '/oauth2/authorization/google';
            }
        }, 1000);
    },

    restoreSessionBackup() {
        try {
            const backupStr = localStorage.getItem('seodrift_draft_backup');
            if (backupStr) {
                const backup = JSON.parse(backupStr);
                
                if (!this.elements.canvasTitle.value && !this.elements.canvasDescription.value) {
                    if (backup.title) this.elements.canvasTitle.value = backup.title;
                    if (backup.description) this.elements.canvasDescription.value = backup.description;
                    if (backup.hook) this.elements.canvasHook.value = backup.hook;
                    if (backup.topic) this.elements.topicInput.value = backup.topic;
                    if (backup.tone) this.elements.toneSelect.value = backup.tone;
                    if (backup.tags) this.state.activeTags = backup.tags;
                    if (backup.hashtags) this.state.activeHashtags = backup.hashtags;
                    
                    if (backup.chapters && backup.chapters.length > 0) {
                        this.elements.chaptersTableBody.innerHTML = '';
                        backup.chapters.forEach(ch => this.addChapterRow(ch.timestamp, ch.title));
                    }
                    
                    if (window.showToast) window.showToast('Draft Restored', 'We restored your unsaved progress from your previous session.', 'info');
                }
                localStorage.removeItem('seodrift_draft_backup');
            }
        } catch(e) { console.error('Failed to restore draft backup', e); }

        // Load preloaded arrays
        try {
            const preloadedTagsVal = document.getElementById('preloadedTags').value;
            if (preloadedTagsVal && preloadedTagsVal !== 'null') {
                this.state.activeTags = JSON.parse(preloadedTagsVal);
            }
        } catch (e) { console.error('Failed to parse preloaded tags', e); }

        try {
            const preloadedHashtagsVal = document.getElementById('preloadedHashtags').value;
            if (preloadedHashtagsVal && preloadedHashtagsVal !== 'null') {
                this.state.activeHashtags = JSON.parse(preloadedHashtagsVal);
            }
        } catch (e) { console.error('Failed to parse preloaded hashtags', e); }

        try {
            const preloadedChaptersVal = document.getElementById('preloadedChapters').value;
            if (preloadedChaptersVal && preloadedChaptersVal !== 'null') {
                const chapters = JSON.parse(preloadedChaptersVal);
                this.elements.chaptersTableBody.innerHTML = '';
                chapters.forEach(ch => this.addChapterRow(ch.timestamp, ch.title));
            } else {
                this.addChapterRow('00:00', 'Introduction');
            }
        } catch (e) { 
            console.error('Failed to parse preloaded chapters', e);
            this.addChapterRow('00:00', 'Introduction');
        }

        if (typeof this.renderTags === 'function') this.renderTags();
        if (typeof this.renderHashtags === 'function') this.renderHashtags();
        if (typeof this.updateCharCounts === 'function') this.updateCharCounts();
        if (typeof this.runSeoAudit === 'function') this.runSeoAudit();
    }
};
