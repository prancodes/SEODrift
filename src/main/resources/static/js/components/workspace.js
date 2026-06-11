// --- SEODrift AI Content Workspace Javascript Canvas ---

function initWorkspaceCanvas() {
    // CSRF Configuration
    const csrfTokenMeta = document.querySelector('meta[name="_csrf"]');
    const csrfHeaderMeta = document.querySelector('meta[name="_csrf_header"]');
    const csrfToken = csrfTokenMeta ? csrfTokenMeta.getAttribute('content') : '';
    const csrfHeader = csrfHeaderMeta ? csrfHeaderMeta.getAttribute('content') : '';

    // Elements
    const draftIdInput = document.getElementById('draftId');
    if (!draftIdInput) return; // Exit if not on the workspace page
    
    const topicInput = document.getElementById('topic');
    const toneSelect = document.getElementById('tone');
    const btnGenerate = document.getElementById('btnGenerate');
    const spinnerGenerate = document.getElementById('spinnerGenerate');
    
    // Canvas Elements
    const canvasTitle = document.getElementById('canvasTitle');
    const charCountTitle = document.getElementById('charCountTitle');
    const canvasDescription = document.getElementById('canvasDescription');
    const canvasHook = document.getElementById('canvasHook');
    
    // Tags Elements
    const tagInput = document.getElementById('tagInput');
    const btnAddTag = document.getElementById('btnAddTag');
    const tagsContainer = document.getElementById('tagsContainer');
    
    // Hashtags Elements
    const hashtagInput = document.getElementById('hashtagInput');
    const btnAddHashtag = document.getElementById('btnAddHashtag');
    const hashtagsContainer = document.getElementById('hashtagsContainer');
    
    // Tag Suggestions Cloud Elements
    const tagSuggestionsBox = document.getElementById('tagSuggestionsBox');
    const tagCloudContainer = document.getElementById('tagCloudContainer');
    
    // Title suggestions Elements
    const titleSuggestionsCard = document.getElementById('titleSuggestionsCard');
    const titleSuggestionsContainer = document.getElementById('titleSuggestionsContainer');

    // Chapters Elements
    const btnAddChapter = document.getElementById('btnAddChapter');
    const chaptersTableBody = document.getElementById('chaptersTableBody');
    const btnInsertTimestamps = document.getElementById('btnInsertTimestamps');

    // Action Toolbar
    const btnSaveDraft = document.getElementById('btnSaveDraft');
    const btnCopyAll = document.getElementById('btnCopyAll');

    // SEO Auditor Elements
    const seoScoreProgress = document.getElementById('seoScoreProgress');
    const seoScoreText = document.getElementById('seoScoreText');
    const checkTitleLength = document.getElementById('checkTitleLength');
    const checkTitleKeywords = document.getElementById('checkTitleKeywords');
    const checkDescCta = document.getElementById('checkDescCta');
    const checkTagsCount = document.getElementById('checkTagsCount');
    const checkHashtagsCount = document.getElementById('checkHashtagsCount');

    // Local Workspace State
    let activeTags = [];
    let activeHashtags = [];
    let recommendedTags = [];

    // --- INITIALIZATION ---
    function initWorkspace() {
        // Load preloaded arrays if editing a draft
        try {
            const preloadedTagsVal = document.getElementById('preloadedTags').value;
            if (preloadedTagsVal && preloadedTagsVal !== 'null') {
                activeTags = JSON.parse(preloadedTagsVal);
            }
        } catch (e) { console.error('Failed to parse preloaded tags', e); }

        try {
            const preloadedHashtagsVal = document.getElementById('preloadedHashtags').value;
            if (preloadedHashtagsVal && preloadedHashtagsVal !== 'null') {
                activeHashtags = JSON.parse(preloadedHashtagsVal);
            }
        } catch (e) { console.error('Failed to parse preloaded hashtags', e); }

        try {
            const preloadedChaptersVal = document.getElementById('preloadedChapters').value;
            if (preloadedChaptersVal && preloadedChaptersVal !== 'null') {
                const chapters = JSON.parse(preloadedChaptersVal);
                chaptersTableBody.innerHTML = '';
                chapters.forEach(ch => addChapterRow(ch.timestamp, ch.title));
            } else {
                // Load default outline row
                addChapterRow('00:00', 'Introduction');
            }
        } catch (e) { 
            console.error('Failed to parse preloaded chapters', e);
            addChapterRow('00:00', 'Introduction');
        }

        renderTags();
        renderHashtags();
        updateCharCounts();
        runSeoAudit();
    }

    // --- RENDER FUNCTIONS ---
    function renderTags() {
        tagsContainer.innerHTML = '';
        activeTags.forEach((tag, idx) => {
            const tagPill = document.createElement('span');
            tagPill.className = 'tag-pill';
            tagPill.innerHTML = `
                <span class="cursor-pointer">${tag}</span>
                <button type="button" class="flex items-center text-xs text-gray-400 hover:text-red-500 focus:outline-none cursor-pointer" data-index="${idx}">
                    <i class="ph-bold ph-x"></i>
                </button>
            `;
            // Add click listener to the delete button
            tagPill.querySelector('button').addEventListener('click', function() {
                activeTags.splice(idx, 1);
                renderTags();
                renderCloudTags();
                runSeoAudit();
            });
            tagsContainer.appendChild(tagPill);
        });
    }

    function renderHashtags() {
        hashtagsContainer.innerHTML = '';
        activeHashtags.forEach((hashtag, idx) => {
            const hashPill = document.createElement('span');
            hashPill.className = 'hashtag-pill';
            hashPill.innerHTML = `
                <span class="cursor-pointer">#${hashtag.replace(/^#/, '')}</span>
                <button type="button" class="flex items-center text-xs text-gray-400 hover:text-red-500 focus:outline-none cursor-pointer" data-index="${idx}">
                    <i class="ph-bold ph-x"></i>
                </button>
            `;
            hashPill.querySelector('button').addEventListener('click', function() {
                activeHashtags.splice(idx, 1);
                renderHashtags();
                runSeoAudit();
            });
            hashtagsContainer.appendChild(hashPill);
        });
    }

    function renderCloudTags() {
        tagCloudContainer.innerHTML = '';
        // Filter out recommended tags that are already active
        const availableRecs = recommendedTags.filter(t => !activeTags.includes(t));
        
        if (availableRecs.length === 0) {
            tagSuggestionsBox.classList.add('hidden');
            return;
        }

        tagSuggestionsBox.classList.remove('hidden');
        availableRecs.forEach(tag => {
            const pill = document.createElement('span');
            pill.className = 'recommend-tag-pill cursor-pointer';
            pill.innerHTML = `<i class="ph-bold ph-plus text-[10px]"></i> ${tag}`;
            pill.addEventListener('click', function() {
                if (!activeTags.includes(tag)) {
                    activeTags.push(tag);
                    renderTags();
                    renderCloudTags();
                    runSeoAudit();
                }
            });
            tagCloudContainer.appendChild(pill);
        });
    }

    function addChapterRow(timestamp = '00:00', title = '') {
        const tr = document.createElement('tr');
        tr.className = 'chapter-row';
        tr.innerHTML = `
            <td class="p-3">
                <input type="text" class="chapter-time px-2 py-1 rounded border border-transparent hover:border-gray-200 focus:border-purple-500 dark:hover:border-slate-800 bg-transparent text-xs text-gray-800 dark:text-gray-200 outline-none w-20" value="${timestamp}">
            </td>
            <td class="p-3">
                <input type="text" class="chapter-title px-2 py-1 rounded border border-transparent hover:border-gray-200 focus:border-purple-500 dark:hover:border-slate-800 bg-transparent text-xs text-gray-850 dark:text-gray-100 outline-none w-full" value="${title}" placeholder="e.g. Setting up the workspace">
            </td>
            <td class="p-3 text-center">
                <button type="button" class="btn-delete-chapter text-gray-400 hover:text-red-500 transition-colors cursor-pointer">
                    <i class="ph-bold ph-trash text-base"></i>
                </button>
            </td>
        `;

        tr.querySelector('.btn-delete-chapter').addEventListener('click', function() {
            tr.remove();
            runSeoAudit();
        });

        // Add change listeners to trigger audits on editing
        tr.querySelector('.chapter-time').addEventListener('input', runSeoAudit);
        tr.querySelector('.chapter-title').addEventListener('input', runSeoAudit);

        chaptersTableBody.appendChild(tr);
    }

    // --- REAL-TIME SEO AUDITOR ---
    function runSeoAudit() {
        let score = 0;
        const titleVal = canvasTitle.value.trim();
        const descVal = canvasDescription.value.trim();

        // 1. Title Length check (20-70 characters) -> 20 pts
        const titleLen = titleVal.length;
        if (titleLen >= 20 && titleLen <= 70) {
            score += 20;
            setCheckState(checkTitleLength, true);
        } else {
            setCheckState(checkTitleLength, false);
        }

        // 2. Keyword Synergy (title words found in tags list) -> 20 pts
        let keywordSynergy = false;
        if (titleVal && activeTags.length > 0) {
            const titleWords = titleVal.toLowerCase().split(/\s+/).filter(w => w.length > 3);
            keywordSynergy = titleWords.some(word => 
                activeTags.some(tag => tag.toLowerCase().includes(word))
            );
        }
        if (keywordSynergy) {
            score += 20;
            setCheckState(checkTitleKeywords, true);
        } else {
            setCheckState(checkTitleKeywords, false);
        }

        // 3. Description links / CTAs (has http:// or https://) -> 20 pts
        const hasLinks = descVal.includes('http://') || descVal.includes('https://');
        if (hasLinks) {
            score += 20;
            setCheckState(checkDescCta, true);
        } else {
            setCheckState(checkDescCta, false);
        }

        // 4. Tag count (10-15 tags) -> 20 pts
        const tagCount = activeTags.length;
        if (tagCount >= 10 && tagCount <= 15) {
            score += 20;
            setCheckState(checkTagsCount, true);
        } else {
            setCheckState(checkTagsCount, false);
        }

        // 5. Hashtags count (3-5 hashtags) -> 20 pts
        const hashtagCount = activeHashtags.length;
        if (hashtagCount >= 3 && hashtagCount <= 5) {
            score += 20;
            setCheckState(checkHashtagsCount, true);
        } else {
            setCheckState(checkHashtagsCount, false);
        }

        // Update Score Radial Ring and Text
        seoScoreText.textContent = score;
        const offset = 264 - (264 * score) / 100;
        seoScoreProgress.style.strokeDashoffset = offset;

        // Color coordination
        if (score >= 80) {
            seoScoreProgress.className.baseVal = 'text-green-500 transition-all duration-500';
            seoScoreText.className = 'text-3xl font-black text-green-500';
        } else if (score >= 50) {
            seoScoreProgress.className.baseVal = 'text-orange-500 transition-all duration-500';
            seoScoreText.className = 'text-3xl font-black text-orange-500';
        } else {
            seoScoreProgress.className.baseVal = 'text-red-500 transition-all duration-500';
            seoScoreText.className = 'text-3xl font-black text-red-500';
        }
    }

    function setCheckState(element, passed) {
        if (passed) {
            element.classList.add('passed');
            const icon = element.querySelector('i');
            icon.className = 'ph-bold ph-check-circle text-base shrink-0';
        } else {
            element.classList.remove('passed');
            const icon = element.querySelector('i');
            icon.className = 'ph-bold ph-circle text-base shrink-0';
        }
    }

    function updateCharCounts() {
        const titleLen = canvasTitle.value.length;
        charCountTitle.textContent = `${titleLen} / 100`;
        if (titleLen > 70 || titleLen < 20) {
            charCountTitle.className = 'text-[10px] font-bold text-red-500';
        } else {
            charCountTitle.className = 'text-[10px] font-bold text-green-500';
        }
    }

    // --- ACTIONS AND LISTENERS ---
    canvasTitle.addEventListener('input', function() {
        updateCharCounts();
        runSeoAudit();
    });

    canvasDescription.addEventListener('input', runSeoAudit);

    // Add Tag
    function handleAddTag() {
        const val = tagInput.value.trim().toLowerCase();
        if (val && !activeTags.includes(val)) {
            activeTags.push(val);
            renderTags();
            renderCloudTags();
            runSeoAudit();
        }
        tagInput.value = '';
    }
    btnAddTag.addEventListener('click', handleAddTag);
    tagInput.addEventListener('keydown', function(e) {
        if (e.key === 'Enter') {
            e.preventDefault();
            handleAddTag();
        }
    });

    // Add Hashtag
    function handleAddHashtag() {
        const val = hashtagInput.value.trim().replace(/^#/, '');
        if (val && !activeHashtags.includes(val)) {
            activeHashtags.push(val);
            renderHashtags();
            runSeoAudit();
        }
        hashtagInput.value = '';
    }
    btnAddHashtag.addEventListener('click', handleAddHashtag);
    hashtagInput.addEventListener('keydown', function(e) {
        if (e.key === 'Enter') {
            e.preventDefault();
            handleAddHashtag();
        }
    });

    // Add Outline Row
    btnAddChapter.addEventListener('click', function() {
        addChapterRow('00:00', '');
        runSeoAudit();
    });

    // Append Chapters Outline to Description text
    btnInsertTimestamps.addEventListener('click', function() {
        const rows = chaptersTableBody.querySelectorAll('.chapter-row');
        if (rows.length === 0) return;

        let outlineText = '\n\nTIMESTAMPS:\n';
        rows.forEach(row => {
            const time = row.querySelector('.chapter-time').value.trim();
            const title = row.querySelector('.chapter-title').value.trim();
            if (time || title) {
                outlineText += `${time} ${title}\n`;
            }
        });

        // Append to description if not already present
        if (!canvasDescription.value.includes('TIMESTAMPS:')) {
            canvasDescription.value = canvasDescription.value.trim() + outlineText;
            runSeoAudit();
            
            // Visual feedback
            btnInsertTimestamps.innerHTML = '<i class="ph-bold ph-check-square text-sm"></i> Timestamps Appended';
            setTimeout(() => {
                btnInsertTimestamps.innerHTML = '<i class="ph-bold ph-plus-square text-sm"></i> Append Chapters to Description';
            }, 2000);
        } else {
            if (window.showToast) window.showToast('Notice', 'Timestamps header already exists in description.', 'info');
        }
    });

    // Copy All Metadata to Clipboard
    btnCopyAll.addEventListener('click', function() {
        const title = canvasTitle.value;
        const desc = canvasDescription.value;
        const tags = activeTags.join(', ');
        const hashtags = activeHashtags.map(h => '#' + h).join(' ');

        const formatted = `=== VIDEO TITLE ===\n${title}\n\n=== VIDEO DESCRIPTION ===\n${desc}\n\n=== TAGS ===\n${tags}\n\n=== HASHTAGS ===\n${hashtags}`;
        
        navigator.clipboard.writeText(formatted).then(() => {
            const originalHtml = btnCopyAll.innerHTML;
            btnCopyAll.innerHTML = '<i class="ph-bold ph-check text-sm"></i> Copied!';
            setTimeout(() => {
                btnCopyAll.innerHTML = originalHtml;
            }, 2000);
        }).catch(err => {
            console.error('Failed to copy text', err);
        });
    });

    // --- CALL GENERATE API ---
    btnGenerate.addEventListener('click', function() {
        const topic = topicInput.value.trim();
        const tone = toneSelect.value;

        if (!topic) {
            if (window.showToast) window.showToast('Missing Topic', 'Please specify a core topic.', 'error');
            return;
        }

        // Lock UI
        btnGenerate.disabled = true;
        btnGenerate.innerHTML = `
            <span class="flex items-center justify-center gap-2">
                <svg class="animate-spin h-5 w-5 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                    <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                    <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                </svg>
                Generating Ideas...
            </span>
        `;
        btnGenerate.classList.add("opacity-75", "cursor-not-allowed");

        fetch('/api/workspace/generate', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                [csrfHeader]: csrfToken
            },
            body: JSON.stringify({ topic, tone })
        })
        .then(response => {
            if (!response.ok) {
                throw new Error('API server returned error code ' + response.status);
            }
            return response.json();
        })
        .then(data => {
            // Unpack Data into Canvas
            if (data.titleSuggestions && data.titleSuggestions.length > 0) {
                // Populate Suggested Titles Card
                titleSuggestionsCard.classList.remove('hidden');
                titleSuggestionsContainer.innerHTML = '';
                
                data.titleSuggestions.forEach(title => {
                    const item = document.createElement('div');
                    item.className = 'title-suggestion-item cursor-pointer';
                    item.innerHTML = `<i class="ph-bold ph-sparkle text-purple-500"></i> <span>${title}</span>`;
                    item.addEventListener('click', function() {
                        canvasTitle.value = title;
                        updateCharCounts();
                        runSeoAudit();
                    });
                    titleSuggestionsContainer.appendChild(item);
                });

                // Auto-set the first title
                canvasTitle.value = data.titleSuggestions[0];
            }

            if (data.description) {
                canvasDescription.value = data.description;
            }

            if (data.hook) {
                canvasHook.value = data.hook;
            }

            if (data.recommendedTags) {
                recommendedTags = data.recommendedTags;
                // Auto-inject first 10 recommended tags into active tags
                activeTags = data.recommendedTags.slice(0, 10);
                renderTags();
                renderCloudTags();
            }

            if (data.recommendedHashtags) {
                activeHashtags = data.recommendedHashtags;
                renderHashtags();
            }

            if (data.chapters && data.chapters.length > 0) {
                chaptersTableBody.innerHTML = '';
                data.chapters.forEach(ch => {
                    addChapterRow(ch.timestamp, ch.title);
                });
            }

            // Run Audit & Counts
            updateCharCounts();
            runSeoAudit();
        })
        .catch(err => {
            console.error(err);
            if (window.showToast) window.showToast('Generation Failed', 'AI generation failed: ' + err.message, 'error');
        })
        .finally(() => {
            // Unlock UI
            btnGenerate.disabled = false;
            btnGenerate.innerHTML = `
                <i class="ph-bold ph-lightning text-base"></i>
                <span>Generate Ideas</span>
            `;
            btnGenerate.classList.remove("opacity-75", "cursor-not-allowed");
        });
    });

    // --- CALL SAVE DRAFT API ---
    btnSaveDraft.addEventListener('click', function() {
        const draftIdVal = draftIdInput.value ? parseInt(draftIdInput.value) : null;
        const topic = topicInput.value.trim();
        const tone = toneSelect.value;
        const title = canvasTitle.value.trim();
        const description = canvasDescription.value.trim();
        const hook = canvasHook.value.trim();
        const seoScore = parseInt(seoScoreText.textContent);

        if (!title) {
            if (window.showToast) window.showToast('Validation Error', 'A title is required to save your draft.', 'error');
            return;
        }

        // Collect chapters
        const chapters = [];
        const rows = chaptersTableBody.querySelectorAll('.chapter-row');
        rows.forEach(row => {
            const time = row.querySelector('.chapter-time').value.trim();
            const tit = row.querySelector('.chapter-title').value.trim();
            if (time || tit) {
                chapters.push({ timestamp: time, title: tit });
            }
        });

        const payload = {
            draftId: draftIdVal,
            topic,
            tone,
            title,
            description,
            hook,
            tags: activeTags,
            hashtags: activeHashtags,
            chapters: chapters,
            seoScore: seoScore
        };

        btnSaveDraft.disabled = true;
        btnSaveDraft.innerHTML = '<i class="ph-bold ph-spinner animate-spin"></i> Saving...';

        fetch('/api/workspace/save', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                [csrfHeader]: csrfToken
            },
            body: JSON.stringify(payload)
        })
        .then(response => {
            if (!response.ok) {
                throw new Error('API server returned error code ' + response.status);
            }
            return response.json();
        })
        .then(data => {
            if (data.draftId) {
                draftIdInput.value = data.draftId;
            }
            
            // Show saved toast style visual feedback on button
            btnSaveDraft.className = 'px-4 py-2 bg-green-600 hover:bg-green-700 text-white rounded-xl text-[10px] font-black uppercase tracking-widest shadow-md transition-all flex items-center gap-1.5 active:scale-95';
            btnSaveDraft.innerHTML = '<i class="ph-bold ph-check-circle text-sm"></i> Draft Saved';
            
            setTimeout(() => {
                btnSaveDraft.className = 'px-4 py-2 bg-purple-600 hover:bg-purple-700 text-white rounded-xl text-[10px] font-black uppercase tracking-widest shadow-md transition-all flex items-center gap-1.5 active:scale-95';
                btnSaveDraft.innerHTML = '<i class="ph-bold ph-floppy-disk text-sm"></i> Save Draft';
                btnSaveDraft.disabled = false;
            }, 2000);
        })
        .catch(err => {
            console.error(err);
            if (window.showToast) window.showToast('Save Failed', 'Failed to save draft: ' + err.message, 'error');
            btnSaveDraft.innerHTML = '<i class="ph-bold ph-floppy-disk text-sm"></i> Save Draft';
            btnSaveDraft.disabled = false;
        });
    });

    // Initialize Workspace Page
    initWorkspace();
}

// Hook into Turbo page load events as well as DOMContentLoaded
document.addEventListener('turbo:load', initWorkspaceCanvas);

if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initWorkspaceCanvas);
} else {
    initWorkspaceCanvas();
}
