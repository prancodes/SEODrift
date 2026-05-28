import * as Turbo from "@hotwired/turbo"

// Configure Turbo
Turbo.start()

// CSRF Handling for Turbo
// Turbo needs the CSRF token in the header for non-GET requests
document.addEventListener("turbo:before-fetch-request", (event) => {
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.content
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content

    if (csrfToken && csrfHeader) {
        event.detail.fetchOptions.headers[csrfHeader] = csrfToken
    }
})

// Global Form Submit Spinner Handler
document.addEventListener("turbo:submit-start", (event) => {
    const form = event.target;
    const submitBtn = form.querySelector("button[type='submit']");
    if (submitBtn) {
        // Disable the button to prevent multiple clicks
        submitBtn.disabled = true;
        submitBtn.setAttribute("disabled", "disabled");
        
        // Save the original content
        submitBtn.dataset.originalContent = submitBtn.innerHTML;
        
        // Check for specific buttons to style nicely
        let loadingText = "Processing...";
        if (submitBtn.innerText.includes("Fetch")) {
            loadingText = "Fetching Assets...";
        } else if (submitBtn.innerText.includes("Generate")) {
            loadingText = "Generating Tags...";
        } else if (submitBtn.innerText.includes("Audit") || submitBtn.innerText.includes("Analyze") || submitBtn.innerText.includes("Insights")) {
            loadingText = "Analyzing Video...";
        }
        
        // Replace inner HTML with spinner
        submitBtn.innerHTML = `
            <span class="flex items-center justify-center gap-2">
                <svg class="animate-spin h-5 w-5 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                    <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                    <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                </svg>
                ${loadingText}
            </span>
        `;
        submitBtn.classList.add("opacity-75", "cursor-not-allowed");
    }
});

document.addEventListener("turbo:submit-end", (event) => {
    const form = event.target;
    const submitBtn = form.querySelector("button[type='submit']");
    if (submitBtn && submitBtn.dataset.originalContent) {
        submitBtn.disabled = false;
        submitBtn.removeAttribute("disabled");
        submitBtn.innerHTML = submitBtn.dataset.originalContent;
        submitBtn.classList.remove("opacity-75", "cursor-not-allowed");
    }
});

// Download Button Feedback Handler
document.addEventListener("click", (event) => {
    const downloadBtn = event.target.closest("a[href*='/download']");
    if (downloadBtn) {
        // Prevent double click during processing
        if (downloadBtn.classList.contains("pointer-events-none")) {
            event.preventDefault();
            return;
        }

        // Save original styling and text
        const originalContent = downloadBtn.innerHTML;
        downloadBtn.dataset.originalContent = originalContent;
        
        // Disable click events temporarily
        downloadBtn.classList.add("pointer-events-none", "opacity-70", "cursor-not-allowed");
        
        // Show spinner inside button
        downloadBtn.innerHTML = `
            <span class="flex items-center gap-1.5 justify-center">
                <svg class="animate-spin h-4 w-4 text-current" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                    <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                    <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                </svg>
                Downloading...
            </span>
        `;

        // Restore after 3 seconds (when download should have started/finished)
        setTimeout(() => {
            downloadBtn.innerHTML = downloadBtn.dataset.originalContent;
            downloadBtn.classList.remove("pointer-events-none", "opacity-70", "cursor-not-allowed");
        }, 3000);
    }
});

console.log("🚀 Turbo initialized successfully!")
