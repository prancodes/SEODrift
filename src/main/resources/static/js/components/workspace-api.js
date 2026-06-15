// --- SEODrift Workspace API Calls Module ---

if (window.Workspace) {
    Object.assign(window.Workspace, {
        initApi() {
            const el = this.elements;
            if (el.btnGenerate) {
                el.btnGenerate.addEventListener('click', () => this.generateIdeas());
            }
            if (el.btnSaveDraft) {
                el.btnSaveDraft.addEventListener('click', () => this.saveDraft());
            }
            if (el.btnCopyAll) {
                el.btnCopyAll.addEventListener('click', () => this.copyAllMetadata());
            }
        },
        
        generateIdeas() {
            const el = this.elements;
            if (!el.topicInput || !el.toneSelect || !el.btnGenerate) return;
            
            const topic = el.topicInput.value.trim();
            const tone = el.toneSelect.value;

            if (!topic) {
                if (window.showToast) window.showToast('Missing Topic', 'Please specify a core topic.', 'error');
                return;
            }

            // Lock UI
            el.btnGenerate.disabled = true;
            const originalHtml = el.btnGenerate.innerHTML;
            el.btnGenerate.innerHTML = `
                <span class="flex items-center justify-center gap-2">
                    <svg class="animate-spin h-5 w-5 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                        <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                        <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                    </svg>
                    Generating Ideas...
                </span>
            `;
            el.btnGenerate.classList.add("opacity-75", "cursor-not-allowed");

            fetch('/api/workspace/generate', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    [this.csrf.header]: this.csrf.token
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
                if (data.titleSuggestions && data.titleSuggestions.length > 0) {
                    if (el.titleSuggestionsCard && el.titleSuggestionsContainer) {
                        el.titleSuggestionsCard.classList.remove('hidden');
                        el.titleSuggestionsContainer.innerHTML = '';
                        
                        data.titleSuggestions.forEach(title => {
                            const item = document.createElement('div');
                            item.className = 'title-suggestion-item cursor-pointer';
                            item.innerHTML = `<i class="ph-bold ph-sparkle text-purple-500"></i> <span>${title}</span>`;
                            item.addEventListener('click', () => {
                                el.canvasTitle.value = title;
                                this.updateCharCounts();
                                this.runSeoAudit();
                            });
                            el.titleSuggestionsContainer.appendChild(item);
                        });
                    }

                    if (el.canvasTitle) el.canvasTitle.value = data.titleSuggestions[0];
                }

                if (data.description && el.canvasDescription) {
                    el.canvasDescription.value = data.description;
                }

                if (data.hook && el.canvasHook) {
                    el.canvasHook.value = data.hook;
                }

                if (data.recommendedTags) {
                    this.state.recommendedTags = data.recommendedTags;
                    this.state.activeTags = data.recommendedTags.slice(0, 10);
                    this.renderTags();
                    this.renderCloudTags();
                }

                if (data.recommendedHashtags) {
                    this.state.activeHashtags = data.recommendedHashtags;
                    this.renderHashtags();
                }

                if (data.chapters && data.chapters.length > 0 && el.chaptersTableBody) {
                    el.chaptersTableBody.innerHTML = '';
                    data.chapters.forEach(ch => {
                        this.addChapterRow(ch.timestamp, ch.title);
                    });
                }

                this.updateCharCounts();
                this.runSeoAudit();
            })
            .catch(err => {
                console.error(err);
                if (window.showToast) window.showToast('Generation Failed', 'AI generation failed: ' + err.message, 'error');
            })
            .finally(() => {
                el.btnGenerate.disabled = false;
                el.btnGenerate.innerHTML = originalHtml;
                el.btnGenerate.classList.remove("opacity-75", "cursor-not-allowed");
            });
        },
        
        saveDraft() {
            const el = this.elements;
            if (!el.btnSaveDraft || !el.canvasTitle || !el.seoScoreText) return;
            
            const draftIdVal = el.draftIdInput?.value ? parseInt(el.draftIdInput.value) : null;
            const topic = el.topicInput ? el.topicInput.value.trim() : '';
            const tone = el.toneSelect ? el.toneSelect.value : '';
            const title = el.canvasTitle.value.trim();
            const description = el.canvasDescription ? el.canvasDescription.value.trim() : '';
            const hook = el.canvasHook ? el.canvasHook.value.trim() : '';
            const seoScore = parseInt(el.seoScoreText.textContent);

            if (!title || !description) {
                if (window.showToast) window.showToast('Validation Error', 'Please fill in both Video Title and Description before saving your draft.', 'warning');
                return;
            }

            const chapters = [];
            if (el.chaptersTableBody) {
                const rows = el.chaptersTableBody.querySelectorAll('.chapter-row');
                rows.forEach(row => {
                    const time = row.querySelector('.chapter-time').value.trim();
                    const tit = row.querySelector('.chapter-title').value.trim();
                    if (time || tit) {
                        chapters.push({ timestamp: time, title: tit });
                    }
                });
            }

            const payload = {
                draftId: draftIdVal,
                topic,
                tone,
                title,
                description,
                hook,
                tags: this.state.activeTags,
                hashtags: this.state.activeHashtags,
                chapters: chapters,
                seoScore: seoScore
            };

            el.btnSaveDraft.disabled = true;
            const originalHtml = el.btnSaveDraft.innerHTML;
            el.btnSaveDraft.innerHTML = '<i class="ph-bold ph-spinner animate-spin"></i> Saving...';

            fetch('/api/workspace/save', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    [this.csrf.header]: this.csrf.token
                },
                body: JSON.stringify(payload)
            })
            .then(response => {
                if (response.status === 401) {
                    this.handleSessionExpiration();
                    throw new Error('Session Expired');
                }
                if (!response.ok) {
                    throw new Error('API server returned error code ' + response.status);
                }
                return response.json();
            })
            .then(data => {
                if (data.draftId && el.draftIdInput) {
                    el.draftIdInput.value = data.draftId;
                }
                
                el.btnSaveDraft.className = 'px-4 py-2 bg-green-600 hover:bg-green-700 text-white rounded-xl text-[10px] font-black uppercase tracking-widest shadow-md transition-all flex items-center gap-1.5 active:scale-95';
                el.btnSaveDraft.innerHTML = '<i class="ph-bold ph-check-circle text-sm"></i> Draft Saved';
                
                setTimeout(() => {
                    el.btnSaveDraft.className = 'px-4 py-2 bg-purple-600 hover:bg-purple-700 text-white rounded-xl text-[10px] font-black uppercase tracking-widest shadow-md transition-all flex items-center gap-1.5 active:scale-95';
                    el.btnSaveDraft.innerHTML = originalHtml;
                    el.btnSaveDraft.disabled = false;
                }, 2000);
            })
            .catch(err => {
                console.error(err);
                if (err.message !== 'Session Expired') {
                    if (window.showToast) window.showToast('Save Failed', 'Failed to save draft: ' + err.message, 'error');
                }
                el.btnSaveDraft.innerHTML = originalHtml;
                el.btnSaveDraft.disabled = false;
            });
        },
        
        copyAllMetadata() {
            const el = this.elements;
            if (!el.btnCopyAll || !el.canvasTitle || !el.canvasDescription) return;
            
            const title = el.canvasTitle.value.trim();
            const desc = el.canvasDescription.value.trim();

            if (!title || !desc) {
                if (window.showToast) window.showToast('Validation Error', 'Please fill in both Video Title and Description before copying metadata.', 'warning');
                return;
            }

            const tags = this.state.activeTags.join(', ');
            const hashtags = this.state.activeHashtags.map(h => '#' + h).join(' ');

            const formatted = `=== VIDEO TITLE ===\n${title}\n\n=== VIDEO DESCRIPTION ===\n${desc}\n\n=== TAGS ===\n${tags}\n\n=== HASHTAGS ===\n${hashtags}`;
            
            navigator.clipboard.writeText(formatted).then(() => {
                const originalHtml = el.btnCopyAll.innerHTML;
                el.btnCopyAll.innerHTML = '<i class="ph-bold ph-check text-sm"></i> Copied!';
                setTimeout(() => {
                    el.btnCopyAll.innerHTML = originalHtml;
                }, 2000);
            }).catch(err => {
                console.error('Failed to copy text', err);
            });
        }
    });
}
