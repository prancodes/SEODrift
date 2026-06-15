// --- SEODrift Workspace Publishing Module ---

if (window.Workspace) {
    Object.assign(window.Workspace, {
        initPublish() {
            const el = this.elements;
            const dropzone = document.getElementById('dropzone');
            const fileInput = document.getElementById('videoFile');
            const fileInfo = document.getElementById('selectedFileInfo');
            const fileNameSpan = document.getElementById('selectedFileName');
            const btnRemove = document.getElementById('btnRemoveFile');
            
            if (el.btnOpenPublishModal) {
                el.btnOpenPublishModal.addEventListener('click', () => {
                    const title = el.canvasTitle?.value.trim();
                    const desc = el.canvasDescription?.value.trim();

                    if (!title || !desc) {
                        if (window.showToast) {
                            window.showToast('Validation Error', 'Please fill in both Video Title and Description before publishing to YouTube.', 'warning');
                        }
                        return;
                    }

                    // Enforce SEO score threshold (must be at least 80/100)
                    const seoScore = parseInt(el.seoScoreText?.textContent || '0');
                    if (seoScore < 80) {
                        if (window.showToast) {
                            window.showToast('SEO Warning', `Your SEO score is only ${seoScore}/100. Please optimize your metadata to at least 80/100 before publishing to YouTube.`, 'warning');
                        }
                        return;
                    }

                    el.publishModal?.classList.remove('opacity-0', 'pointer-events-none');
                    setTimeout(() => el.publishModalContent?.classList.remove('scale-95'), 10);
                });
            }

            if (el.btnClosePublishModal) {
                el.btnClosePublishModal.addEventListener('click', () => {
                    el.publishModalContent?.classList.add('scale-95');
                    setTimeout(() => {
                        el.publishModal?.classList.add('opacity-0', 'pointer-events-none');
                        // Reset file input and warnings on close
                        if (fileInput) fileInput.value = '';
                        if (fileInfo) fileInfo.classList.add('hidden');
                        if (el.publishWarnings) el.publishWarnings.classList.add('hidden');
                    }, 300);
                });
            }

            if (dropzone && fileInput) {
                dropzone.addEventListener('click', (e) => {
                    if (e.target !== fileInput && !btnRemove?.contains(e.target)) {
                        fileInput.click();
                    }
                });

                ['dragenter', 'dragover'].forEach(eventName => {
                    dropzone.addEventListener(eventName, (e) => {
                        e.preventDefault();
                        e.stopPropagation();
                        dropzone.classList.add('border-red-500', 'bg-red-50/10', 'dark:bg-red-950/10');
                    }, false);
                });

                ['dragleave', 'drop'].forEach(eventName => {
                    dropzone.addEventListener(eventName, (e) => {
                        e.preventDefault();
                        e.stopPropagation();
                        dropzone.classList.remove('border-red-500', 'bg-red-50/10', 'dark:bg-red-950/10');
                    }, false);
                });

                dropzone.addEventListener('drop', (e) => {
                    const dt = e.dataTransfer;
                    const files = dt.files;
                    if (files && files.length > 0) {
                        fileInput.files = files;
                        fileInput.dispatchEvent(new Event('change'));
                    }
                });

                fileInput.addEventListener('change', () => {
                    if (fileInput.files && fileInput.files.length > 0) {
                        const file = fileInput.files[0];
                        if (fileNameSpan) fileNameSpan.textContent = `${file.name} (${(file.size / (1024 * 1024)).toFixed(1)} MB)`;
                        fileInfo?.classList.remove('hidden');
                    } else {
                        fileInfo?.classList.add('hidden');
                    }
                });
            }

            if (btnRemove && fileInput) {
                btnRemove.addEventListener('click', (e) => {
                    e.preventDefault();
                    e.stopPropagation();
                    fileInput.value = '';
                    fileInfo?.classList.add('hidden');
                });
            }

            if (el.publishForm) {
                el.publishForm.addEventListener('submit', (e) => {
                    e.preventDefault();
                    this.publishVideo();
                });
            }
        },
        
        publishVideo() {
            const el = this.elements;
            if (!el.canvasTitle || !el.canvasDescription || !el.publishForm) return;

            const title = el.canvasTitle.value.trim();
            const description = el.canvasDescription.value.trim();
            const tags = this.state.activeTags.join(',');
            const videoFile = document.getElementById('videoFile')?.files[0];
            const privacyStatus = document.getElementById('privacyStatus')?.value || 'private';
            const categoryId = document.getElementById('categoryId')?.value || '22';

            if (!videoFile) return;

            const formData = new FormData();
            formData.append('title', title);
            formData.append('description', description);
            formData.append('tags', tags);
            formData.append('privacyStatus', privacyStatus);
            formData.append('categoryId', categoryId);
            formData.append('file', videoFile);

            // UI Reset & Loading State
            el.publishWarnings.classList.add('hidden');
            el.publishProgressContainer.classList.remove('hidden');
            el.publishProgressBar.style.width = '0%';
            el.publishProgressText.textContent = '0%';
            
            el.btnConfirmPublish.disabled = true;
            el.publishIcon.className = 'ph-bold ph-spinner animate-spin text-base';
            el.publishBtnText.textContent = 'Uploading...';

            const xhr = new XMLHttpRequest();
            xhr.open('POST', '/api/publish/video', true);
            if (this.csrf.header && this.csrf.token) {
                xhr.setRequestHeader(this.csrf.header, this.csrf.token);
            }

            xhr.upload.onprogress = (event) => {
                if (event.lengthComputable) {
                    const percentComplete = Math.round((event.loaded / event.total) * 100);
                    el.publishProgressBar.style.width = percentComplete + '%';
                    el.publishProgressText.textContent = percentComplete + '%';
                }
            };

            xhr.onload = () => {
                el.btnConfirmPublish.disabled = false;
                el.publishIcon.className = 'ph-bold ph-upload-simple text-base';
                el.publishBtnText.textContent = 'Upload & Publish';
                el.publishProgressContainer.classList.add('hidden');

                if (xhr.status >= 200 && xhr.status < 300) {
                    const response = JSON.parse(xhr.responseText);
                    if (window.showToast) window.showToast('Success', 'Video published successfully!', 'success');
                    
                    // Close modal
                    el.btnClosePublishModal.click();
                    
                    if (response.videoId) {
                        window.open(`https://www.youtube.com/watch?v=${response.videoId}`, '_blank');
                    }
                } else if (xhr.status === 401) {
                    this.handleSessionExpiration();
                } else {
                    let errorMessage = 'Upload failed.';
                    try {
                        const errData = JSON.parse(xhr.responseText);
                        if (errData.warnings && errData.warnings.length > 0) {
                            errorMessage = errData.warnings.join('<br>');
                        } else if (errData.message) {
                            errorMessage = errData.message;
                        }
                    } catch (e) {
                        errorMessage = 'An unexpected error occurred during upload.';
                    }
                    
                    el.publishWarnings.innerHTML = `<strong>Gatekeeper Validation Failed:</strong><br>${errorMessage}`;
                    el.publishWarnings.classList.remove('hidden');
                }
            };

            xhr.onerror = () => {
                el.btnConfirmPublish.disabled = false;
                el.publishIcon.className = 'ph-bold ph-upload-simple text-base';
                el.publishBtnText.textContent = 'Upload & Publish';
                el.publishProgressContainer.classList.add('hidden');
                
                el.publishWarnings.innerHTML = 'Network error occurred during upload.';
                el.publishWarnings.classList.remove('hidden');
            };

            xhr.send(formData);
        }
    });
}
