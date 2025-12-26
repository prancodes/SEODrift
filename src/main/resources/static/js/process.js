function startDownload(btn, videoUrl, title, formatId, codec, isSimple) {
    if (btn.classList.contains('pointer-events-none')) return;

    const originalContent = btn.innerHTML;
    const token = new Date().getTime(); // Unique ID

    // 1. UI Loading State
    btn.classList.add('opacity-75', 'cursor-not-allowed', 'pointer-events-none');
    updateBtn(btn, "Connecting...");

    // 2. Simple Download (Deprecated by your request, but logic kept for safety)
    if (isSimple) {
        window.location.href = `/video/download?videoUrl=${encodeURIComponent(videoUrl)}&title=${encodeURIComponent(title)}&formatId=${encodeURIComponent(formatId)}`;
        setTimeout(() => resetBtn(btn, originalContent), 4000);
        return;
    }

    // 3. Async Background Process (Used for ALL downloads now)
    fetch(`/video/process?videoUrl=${encodeURIComponent(videoUrl)}&formatId=${encodeURIComponent(formatId)}&downloadToken=${token}&codec=${codec || ''}`)
        .then(res => {
            if (!res.ok) throw new Error("Failed to start");
            
            // Step B: Poll Status (FASTER POLLING: 500ms)
            const poller = setInterval(() => {
                fetch(`/video/status?downloadToken=${token}`)
                    .then(r => r.text())
                    .then(status => {
                        // Update Button Text
                        updateBtn(btn, status);

                        if (status === "COMPLETED") {
                            clearInterval(poller);
                            updateBtn(btn, "Download Started!");
                            
                            // Step C: Get the File
                            window.location.href = `/video/serve?downloadToken=${token}&title=${encodeURIComponent(title)}`;
                            
                            setTimeout(() => resetBtn(btn, originalContent), 4000);
                        } 
                        else if (status.startsWith("ERROR")) {
                            clearInterval(poller);
                            updateBtn(btn, "Error!");
                            alert(status);
                            resetBtn(btn, originalContent);
                        }
                    });
            }, 500); // ✅ Check every 0.5 seconds for snappy feedback
        })
        .catch(err => {
            alert("Error starting download");
            resetBtn(btn, originalContent);
        });
}

function updateBtn(btn, text) {
    btn.innerHTML = `<i class="ph-bold ph-spinner animate-spin text-lg"></i> ${text}`;
}

function resetBtn(btn, html) {
    btn.innerHTML = html;
    btn.classList.remove('opacity-75', 'cursor-not-allowed', 'pointer-events-none');
}