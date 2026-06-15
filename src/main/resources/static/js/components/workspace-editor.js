// --- SEODrift Workspace Editor Module ---

if (window.Workspace) {
    Object.assign(window.Workspace, {
        initEditor() {
            const el = this.elements;
            
            if (el.btnAddTag) {
                el.btnAddTag.addEventListener('click', () => this.handleAddTag());
            }
            if (el.tagInput) {
                el.tagInput.addEventListener('keydown', (e) => {
                    if (e.key === 'Enter') {
                        e.preventDefault();
                        this.handleAddTag();
                    }
                });
            }
            
            if (el.btnAddHashtag) {
                el.btnAddHashtag.addEventListener('click', () => this.handleAddHashtag());
            }
            if (el.hashtagInput) {
                el.hashtagInput.addEventListener('keydown', (e) => {
                    if (e.key === 'Enter') {
                        e.preventDefault();
                        this.handleAddHashtag();
                    }
                });
            }
            
            if (el.btnAddChapter) {
                el.btnAddChapter.addEventListener('click', () => {
                    this.addChapterRow('00:00', '');
                    if (typeof this.runSeoAudit === 'function') this.runSeoAudit();
                });
            }
            
            if (el.btnInsertTimestamps) {
                el.btnInsertTimestamps.addEventListener('click', () => this.handleInsertTimestamps());
            }
        },
        
        renderTags() {
            const container = this.elements.tagsContainer;
            if (!container) return;
            container.innerHTML = '';
            this.state.activeTags.forEach((tag, idx) => {
                const tagPill = document.createElement('span');
                tagPill.className = 'tag-pill';
                tagPill.innerHTML = `
                    <span class="cursor-pointer">${tag}</span>
                    <button type="button" aria-label="Remove Tag ${tag}" class="flex items-center text-xs text-gray-400 hover:text-red-500 focus:outline-none cursor-pointer" data-index="${idx}">
                        <i class="ph-bold ph-x"></i>
                    </button>
                `;
                tagPill.querySelector('button').addEventListener('click', () => {
                    this.state.activeTags.splice(idx, 1);
                    this.renderTags();
                    this.renderCloudTags();
                    if (typeof this.runSeoAudit === 'function') this.runSeoAudit();
                });
                container.appendChild(tagPill);
            });
        },
        
        renderHashtags() {
            const container = this.elements.hashtagsContainer;
            if (!container) return;
            container.innerHTML = '';
            this.state.activeHashtags.forEach((hashtag, idx) => {
                const hashPill = document.createElement('span');
                hashPill.className = 'hashtag-pill';
                hashPill.innerHTML = `
                    <span class="cursor-pointer">#${hashtag.replace(/^#/, '')}</span>
                    <button type="button" aria-label="Remove Hashtag ${hashtag}" class="flex items-center text-xs text-gray-400 hover:text-red-500 focus:outline-none cursor-pointer" data-index="${idx}">
                        <i class="ph-bold ph-x"></i>
                    </button>
                `;
                hashPill.querySelector('button').addEventListener('click', () => {
                    this.state.activeHashtags.splice(idx, 1);
                    this.renderHashtags();
                    if (typeof this.runSeoAudit === 'function') this.runSeoAudit();
                });
                container.appendChild(hashPill);
            });
        },
        
        renderCloudTags() {
            const container = this.elements.tagCloudContainer;
            const box = this.elements.tagSuggestionsBox;
            if (!container || !box) return;
            container.innerHTML = '';
            const availableRecs = this.state.recommendedTags.filter(t => !this.state.activeTags.includes(t));
            
            if (availableRecs.length === 0) {
                box.classList.add('hidden');
                return;
            }

            box.classList.remove('hidden');
            availableRecs.forEach(tag => {
                const pill = document.createElement('span');
                pill.className = 'recommend-tag-pill cursor-pointer';
                pill.innerHTML = `<i class="ph-bold ph-plus text-[10px]"></i> ${tag}`;
                pill.addEventListener('click', () => {
                    if (!this.state.activeTags.includes(tag)) {
                        this.state.activeTags.push(tag);
                        this.renderTags();
                        this.renderCloudTags();
                        if (typeof this.runSeoAudit === 'function') this.runSeoAudit();
                    }
                });
                container.appendChild(pill);
            });
        },
        
        addChapterRow(timestamp = '00:00', title = '') {
            const container = this.elements.chaptersTableBody;
            if (!container) return;
            const tr = document.createElement('tr');
            tr.className = 'chapter-row';
            tr.innerHTML = `
                <td class="p-3">
                    <input type="text" aria-label="Chapter Timestamp" class="chapter-time px-2 py-1 rounded border border-transparent hover:border-gray-200 focus:border-purple-500 dark:hover:border-slate-800 bg-transparent text-xs text-gray-800 dark:text-gray-200 outline-none w-20" value="${timestamp}">
                </td>
                <td class="p-3">
                    <input type="text" aria-label="Chapter Title" class="chapter-title px-2 py-1 rounded border border-transparent hover:border-gray-200 focus:border-purple-500 dark:hover:border-slate-800 bg-transparent text-xs text-gray-855 dark:text-gray-100 outline-none w-full" value="${title}" placeholder="e.g. Setting up the workspace">
                </td>
                <td class="p-3 text-center">
                    <button type="button" aria-label="Delete Chapter Segment" class="btn-delete-chapter text-gray-400 hover:text-red-500 transition-colors cursor-pointer">
                        <i class="ph-bold ph-trash text-base"></i>
                    </button>
                </td>
            `;

            tr.querySelector('.btn-delete-chapter').addEventListener('click', () => {
                tr.remove();
                if (typeof this.runSeoAudit === 'function') this.runSeoAudit();
            });

            tr.querySelector('.chapter-time').addEventListener('input', () => {
                if (typeof this.runSeoAudit === 'function') this.runSeoAudit();
            });
            tr.querySelector('.chapter-title').addEventListener('input', () => {
                if (typeof this.runSeoAudit === 'function') this.runSeoAudit();
            });

            container.appendChild(tr);
        },
        
        handleAddTag() {
            const input = this.elements.tagInput;
            if (!input) return;
            const val = input.value.trim().toLowerCase();
            if (val && !this.state.activeTags.includes(val)) {
                this.state.activeTags.push(val);
                this.renderTags();
                this.renderCloudTags();
                if (typeof this.runSeoAudit === 'function') this.runSeoAudit();
            }
            input.value = '';
        },
        
        handleAddHashtag() {
            const input = this.elements.hashtagInput;
            if (!input) return;
            const val = input.value.trim().replace(/^#/, '');
            if (val && !this.state.activeHashtags.includes(val)) {
                this.state.activeHashtags.push(val);
                this.renderHashtags();
                if (typeof this.runSeoAudit === 'function') this.runSeoAudit();
            }
            input.value = '';
        },
        
        handleInsertTimestamps() {
            const container = this.elements.chaptersTableBody;
            const desc = this.elements.canvasDescription;
            const btn = this.elements.btnInsertTimestamps;
            if (!container || !desc || !btn) return;
            
            const rows = container.querySelectorAll('.chapter-row');
            if (rows.length === 0) return;

            let outlineText = '\n\nTIMESTAMPS:\n';
            rows.forEach(row => {
                const time = row.querySelector('.chapter-time').value.trim();
                const title = row.querySelector('.chapter-title').value.trim();
                if (time || title) {
                    outlineText += `${time} ${title}\n`;
                }
            });

            if (!desc.value.includes('TIMESTAMPS:')) {
                desc.value = desc.value.trim() + outlineText;
                if (typeof this.runSeoAudit === 'function') this.runSeoAudit();
                
                const originalHtml = btn.innerHTML;
                btn.innerHTML = '<i class="ph-bold ph-check-square text-sm"></i> Timestamps Appended';
                setTimeout(() => {
                    btn.innerHTML = originalHtml;
                }, 2000);
            } else {
                if (window.showToast) window.showToast('Notice', 'Timestamps header already exists in description.', 'info');
            }
        }
    });
}
