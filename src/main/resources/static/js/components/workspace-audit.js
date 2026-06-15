// --- SEODrift Workspace SEO Auditor Module ---

if (window.Workspace) {
    Object.assign(window.Workspace, {
        initAudit() {
            const el = this.elements;
            if (el.canvasTitle) {
                el.canvasTitle.addEventListener('input', () => {
                    this.updateCharCounts();
                    this.runSeoAudit();
                });
            }
            if (el.canvasDescription) {
                el.canvasDescription.addEventListener('input', () => this.runSeoAudit());
            }
        },
        
        runSeoAudit() {
            const el = this.elements;
            if (!el.seoScoreText || !el.seoScoreProgress) return;
            
            let score = 0;
            const titleVal = el.canvasTitle ? el.canvasTitle.value.trim() : '';
            const descVal = el.canvasDescription ? el.canvasDescription.value.trim() : '';

            // 1. Title Length check (20-70 characters) -> 20 pts
            const titleLen = titleVal.length;
            if (titleLen >= 20 && titleLen <= 70) {
                score += 20;
                this.setCheckState(el.checkTitleLength, true);
            } else {
                this.setCheckState(el.checkTitleLength, false);
            }

            // 2. Keyword Synergy (title words found in tags list) -> 20 pts
            let keywordSynergy = false;
            if (titleVal && this.state.activeTags.length > 0) {
                const titleWords = titleVal.toLowerCase().split(/\s+/).filter(w => w.length > 3);
                keywordSynergy = titleWords.some(word => 
                    this.state.activeTags.some(tag => tag.toLowerCase().includes(word))
                );
            }
            if (keywordSynergy) {
                score += 20;
                this.setCheckState(el.checkTitleKeywords, true);
            } else {
                this.setCheckState(el.checkTitleKeywords, false);
            }

            // 3. Description links / CTAs (has http:// or https://) -> 20 pts
            const hasLinks = descVal.includes('http://') || descVal.includes('https://');
            if (hasLinks) {
                score += 20;
                this.setCheckState(el.checkDescCta, true);
            } else {
                this.setCheckState(el.checkDescCta, false);
            }

            // 4. Tag count (10-15 tags) -> 20 pts
            const tagCount = this.state.activeTags.length;
            if (tagCount >= 10 && tagCount <= 15) {
                score += 20;
                this.setCheckState(el.checkTagsCount, true);
            } else {
                this.setCheckState(el.checkTagsCount, false);
            }

            // 5. Hashtags count (3-5 hashtags) -> 20 pts
            const hashtagCount = this.state.activeHashtags.length;
            if (hashtagCount >= 3 && hashtagCount <= 5) {
                score += 20;
                this.setCheckState(el.checkHashtagsCount, true);
            } else {
                this.setCheckState(el.checkHashtagsCount, false);
            }

            // Update Score Radial Ring and Text
            el.seoScoreText.textContent = score;
            const offset = 264 - (264 * score) / 100;
            el.seoScoreProgress.style.strokeDashoffset = offset;

            // Color coordination
            if (score >= 80) {
                el.seoScoreProgress.className.baseVal = 'text-green-500 transition-all duration-500';
                el.seoScoreText.className = 'text-3xl font-black text-green-500';
            } else if (score >= 50) {
                el.seoScoreProgress.className.baseVal = 'text-orange-500 transition-all duration-500';
                el.seoScoreText.className = 'text-3xl font-black text-orange-500';
            } else {
                el.seoScoreProgress.className.baseVal = 'text-red-500 transition-all duration-500';
                el.seoScoreText.className = 'text-3xl font-black text-red-500';
            }
        },
        
        setCheckState(element, passed) {
            if (!element) return;
            if (passed) {
                element.classList.add('passed');
                const icon = element.querySelector('i');
                if (icon) icon.className = 'ph-bold ph-check-circle text-base shrink-0';
            } else {
                element.classList.remove('passed');
                const icon = element.querySelector('i');
                if (icon) icon.className = 'ph-bold ph-circle text-base shrink-0';
            }
        },
        
        updateCharCounts() {
            const el = this.elements;
            if (!el.canvasTitle || !el.charCountTitle) return;
            const titleLen = el.canvasTitle.value.length;
            el.charCountTitle.textContent = `${titleLen} / 100`;
            if (titleLen > 70 || titleLen < 20) {
                el.charCountTitle.className = 'text-[10px] font-bold text-red-500';
            } else {
                el.charCountTitle.className = 'text-[10px] font-bold text-green-500';
            }
        }
    });
}
