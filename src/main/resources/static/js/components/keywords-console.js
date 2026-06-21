let ChartModule = null;
let MarkedModule = null;

async function getChart() {
    if (!ChartModule) {
        const module = await import('chart.js/auto');
        ChartModule = module.default;
    }
    return ChartModule;
}

async function getMarked() {
    if (!MarkedModule) {
        const { marked } = await import('marked');
        MarkedModule = marked;
    }
    return MarkedModule;
}

/**
 * Keyword Search Velocity Console Script
 * Manages AJAX add/delete actions, custom toasts, and renders the historical trajectory line charts.
 */

let keywordsInitialized = false;
let historyChartInstance = null;
let allKeywordsHistory = null;
let currentSelectedKeyword = "";

function destroyKeywords() {
    keywordsInitialized = false;
    allKeywordsHistory = null;
    currentSelectedKeyword = "";
    if (historyChartInstance) {
        historyChartInstance.destroy();
        historyChartInstance = null;
    }
}

function initializeKeywords() {
    if (!document.getElementById("keywordHistoryChart") && 
        !document.getElementById("addKeywordForm")) {
        return;
    }

    if (keywordsInitialized) return;
    keywordsInitialized = true;

    // Run non-blockingly to reduce main thread load on initial visit
    requestAnimationFrame(() => {
        initKeywordForm();
        
        setTimeout(() => {
            initNicheSuggestions();
            // Pre-load marked library in background
            getMarked().catch(console.error);
        }, 30);

        setTimeout(() => {
            preloadAllHistoryAndInit();
        }, 60);
    });
}

async function preloadAllHistoryAndInit() {
    const canvas = document.getElementById("keywordHistoryChart");
    if (!canvas) return;

    try {
        const response = await fetch('/api/keywords/all-history');
        if (!response.ok) throw new Error("Failed to load all trend history");
        
        allKeywordsHistory = await response.json();
        initKeywordSelection();
    } catch (error) {
        console.error("Error preloading all keywords history:", error);
    }
}

function initNicheSuggestions() {
    const pills = document.querySelectorAll(".suggestion-pill");
    pills.forEach(pill => {
        if (pill.dataset.listenerAttached) return;
        pill.dataset.listenerAttached = "true";
        
        pill.addEventListener("click", () => {
            const input = document.getElementById("keywordInput");
            if (input) {
                input.value = pill.textContent.trim();
                input.focus();
            }
        });
    });
}

document.addEventListener("turbo:load", () => {
    destroyKeywords();
    initializeKeywords();
});

if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", () => {
        initializeKeywords();
    });
} else {
    initializeKeywords();
}

function initKeywordForm() {
    const form = document.getElementById("addKeywordForm");
    if (!form || form.dataset.listenerAttached) return;
    form.dataset.listenerAttached = "true";

    form.addEventListener("submit", async (e) => {
        e.preventDefault();
        const input = document.getElementById("keywordInput");
        const btn = form.querySelector("button[type='submit']");
        const keyword = input.value.trim();
        if (!keyword) return;

        input.disabled = true;
        btn.disabled = true;
        const originalBtn = btn.innerHTML;
        btn.innerHTML = `<i class="ph-bold ph-spinner animate-spin"></i>`;

        try {
            const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
            const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;

            const response = await fetch('/api/keywords/add', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    [csrfHeader]: csrfToken
                },
                body: JSON.stringify({ keyword: keyword })
            });

            if (response.ok) {
                if (window.showToast) {
                    window.showToast("Keyword Added", `"${keyword}" is now being tracked.`, "success");
                }
                setTimeout(() => window.location.reload(), 1000);
            } else {
                const data = await response.json();
                if (window.showToast) {
                    window.showToast("Failed to Add", data.error || "Could not track keyword.", "error");
                }
            }
        } catch (error) {
            console.error(error);
            if (window.showToast) {
                window.showToast("Network Error", "Could not reach the server.", "error");
            }
        } finally {
            input.disabled = false;
            btn.disabled = false;
            btn.innerHTML = originalBtn;
            input.value = "";
        }
    });
}

function initKeywordSelection() {
    const rows = document.querySelectorAll(".keyword-row");
    if (rows.length === 0) return;

    // Handle clicking keyword list rows
    rows.forEach(row => {
        if (row.dataset.listenerAttached) return;
        row.dataset.listenerAttached = "true";

        row.addEventListener("click", (e) => {
            // Skip deletion click propagation
            if (e.target.closest(".delete-keyword-btn") || e.target.closest("i")) {
                return;
            }
            selectKeyword(row);
        });

        // Set up individual delete button click handlers
        const deleteBtn = row.querySelector(".delete-keyword-btn");
        if (deleteBtn) {
            deleteBtn.addEventListener("click", (e) => {
                e.stopPropagation();
                const keyword = deleteBtn.getAttribute("data-keyword");
                if (!keyword) return;

                if (window.showConfirm) {
                    window.showConfirm(
                        "Stop Tracking Keyword",
                        `Are you sure you want to stop tracking "${keyword}"? Historical trajectory data will be archived.`,
                        () => executeDeleteKeyword(keyword, deleteBtn)
                    );
                } else {
                    if (confirm(`Stop tracking "${keyword}"?`)) {
                        executeDeleteKeyword(keyword, deleteBtn);
                    }
                }
            });
        }
    });

    // Auto-select the first active keyword row on load
    const activeRow = document.querySelector(".keyword-row.active-keyword");
    if (activeRow) {
        selectKeyword(activeRow);
    } else if (rows.length > 0) {
        selectKeyword(rows[0]);
    }
}

async function executeDeleteKeyword(keyword, btn) {
    btn.disabled = true;
    const originalIcon = btn.innerHTML;
    btn.innerHTML = `<i class="ph-bold ph-spinner animate-spin"></i>`;

    try {
        const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
        const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;

        const response = await fetch('/api/keywords/delete', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                [csrfHeader]: csrfToken
            },
            body: JSON.stringify({ keyword: keyword })
        });

        if (response.ok) {
            if (window.showToast) {
                window.showToast("Keyword Removed", `Stopped tracking "${keyword}".`, "success");
            }
            setTimeout(() => window.location.reload(), 1000);
        } else {
            const data = await response.json();
            if (window.showToast) {
                window.showToast("Delete Failed", data.error || "Could not delete keyword.", "error");
            }
        }
    } catch (error) {
        console.error(error);
        if (window.showToast) {
            window.showToast("Network Error", "Could not reach the server.", "error");
        }
    } finally {
        btn.disabled = false;
        btn.innerHTML = originalIcon;
    }
}

async function selectKeyword(selectedRow) {
    // 1. Update UI highlight styling
    document.querySelectorAll(".keyword-row").forEach(r => {
        r.classList.remove("border-orange-500", "dark:border-orange-500", "bg-orange-50/10", "dark:bg-orange-950/5", "active-keyword");
    });
    selectedRow.classList.add("border-orange-500", "dark:border-orange-500", "bg-orange-50/10", "dark:bg-orange-950/5", "active-keyword");

    const keyword = selectedRow.getAttribute("data-keyword");
    if (!keyword) return;

    currentSelectedKeyword = keyword;

    // Show active badge
    const activeBadge = document.getElementById("activeKeywordBadge");
    if (activeBadge) {
        activeBadge.textContent = keyword.toUpperCase();
        activeBadge.classList.remove("hidden");
    }

    // Hide placeholder, show chart
    const placeholder = document.getElementById("chartPlaceholder");
    const canvas = document.getElementById("keywordHistoryChart");
    if (placeholder) placeholder.classList.add("hidden");
    if (canvas) canvas.classList.remove("hidden");

    if (allKeywordsHistory) {
        // 2. Plot Comparative Chart.js
        renderComparativeChart(canvas, keyword);

        // 3. Update Insights and recommendation cards dynamically
        const selectedHistory = allKeywordsHistory[keyword] || [];
        updateInsights(keyword, selectedHistory);
    }
}

async function renderComparativeChart(canvas, selectedKeyword) {
    const isDark = document.documentElement.classList.contains('dark') || document.body.classList.contains('dark');
    const labelColor = isDark ? '#94a3b8' : '#64748b';
    const gridColor = isDark ? 'rgba(255, 255, 255, 0.06)' : 'rgba(15, 23, 42, 0.05)';
    const mutedLineColor = isDark ? 'rgba(148, 163, 184, 0.15)' : 'rgba(100, 116, 139, 0.15)';
 
    const Chart = await getChart();
    if (!Chart) return;

    // Gather all distinct dates across all keywords
    const allDatesSet = new Set();
    Object.values(allKeywordsHistory).forEach(points => {
        points.forEach(pt => {
            if (pt.recordedDate) allDatesSet.add(pt.recordedDate);
        });
    });
    const sortedDates = Array.from(allDatesSet).sort();

    // Map each keyword's data to the sortedDates
    const datasets = Object.keys(allKeywordsHistory).map(kw => {
        const points = allKeywordsHistory[kw];
        const pointMap = {};
        points.forEach(pt => {
            pointMap[pt.recordedDate] = pt.videoCountThisMonth;
        });
        const alignedData = sortedDates.map(d => pointMap[d] !== undefined ? pointMap[d] : null);

        const isSelected = (kw.trim().toLowerCase() === selectedKeyword.trim().toLowerCase());
        const activeLineColor = '#f97316'; // Orange-500
        const inactiveLineColor = isDark ? 'rgba(129, 140, 248, 0.45)' : 'rgba(99, 102, 241, 0.45)'; // Indigo-400 / Indigo-500

        return {
            label: kw.toUpperCase(),
            data: alignedData,
            borderColor: isSelected ? activeLineColor : inactiveLineColor,
            backgroundColor: 'transparent',
            borderWidth: isSelected ? 4 : 2,
            tension: 0.35,
            fill: false,
            pointRadius: isSelected ? 5 : 4,
            pointHoverRadius: isSelected ? 7 : 6,
            pointBackgroundColor: isSelected ? '#f97316' : (isDark ? '#818cf8' : '#6366f1'),
            pointBorderColor: isSelected ? '#fff' : (isDark ? '#1e293b' : '#fff'),
            spanGaps: true,
            order: isSelected ? 1 : 10
        };
    });

    // Sort datasets so the selected one is drawn last (on top)
    datasets.sort((a, b) => {
        if (a.label.toLowerCase() === selectedKeyword.toLowerCase()) return 1;
        if (b.label.toLowerCase() === selectedKeyword.toLowerCase()) return -1;
        return 0;
    });

    const labels = sortedDates.map(d => new Date(d).toLocaleDateString(undefined, { month: 'short', day: 'numeric' }));

    if (historyChartInstance) {
        historyChartInstance.data.labels = labels;
        historyChartInstance.data.datasets = datasets;
        historyChartInstance.update();
    } else {
        historyChartInstance = new Chart(canvas, {
            type: 'line',
            data: {
                labels: labels,
                datasets: datasets
            },
            options: {
                animation: false,
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {
                        display: true,
                        position: 'top',
                        labels: {
                            color: labelColor,
                            font: { family: 'Inter', weight: 'bold', size: 10 },
                            usePointStyle: true,
                            padding: 15
                        }
                    }
                },
                scales: {
                    x: {
                        grid: { display: false },
                        ticks: {
                            color: labelColor,
                            font: { family: 'Inter', weight: 'bold', size: 10 }
                        }
                    },
                    y: {
                        beginAtZero: true,
                        border: { display: false },
                        grid: { color: gridColor },
                        ticks: {
                            color: labelColor,
                            font: { family: 'Inter', weight: 'bold', size: 10 }
                        }
                    }
                }
            }
        });
    }
}

function updateInsights(keyword, historyData) {
    if (historyData.length === 0) return;

    // Get the latest trend data point
    const latest = historyData[historyData.length - 1];
    const volume = latest.videoCountThisMonth;
    const growth = latest.growthRate;

    // 1. Difficulty & Competition Assessment
    const compValEl = document.getElementById("competitionValue");
    const diffBadgeEl = document.getElementById("difficultyBadge");
    const compAdviceEl = document.getElementById("competitionAdvice");

    let diffText = "Easy";
    let diffBadgeClass = "bg-emerald-50 dark:bg-emerald-950/20 text-emerald-600";
    let adviceText = "Low competition opportunity. Perfect to create focused tutorials or content. High chances of ranking fast.";

    if (volume > 500000) {
        diffText = "Ultra High";
        diffBadgeClass = "bg-purple-50 dark:bg-purple-950/20 text-purple-600";
        adviceText = "Ultra-high volume broad term. Target long-tail variations to compete.";
    } else if (volume > 2000) {
        diffText = "Hard";
        diffBadgeClass = "bg-red-50 dark:bg-red-950/20 text-red-600";
        adviceText = "Highly competitive search term. Established channels dominate search results.";
    } else if (volume > 500) {
        diffText = "Moderate";
        diffBadgeClass = "bg-amber-50 dark:bg-amber-950/20 text-amber-600";
        adviceText = "Moderate competition. A well-optimized video has solid ranking chances.";
    }

    if (compValEl) {
        if (volume >= 1000000) {
            compValEl.textContent = "1,000,000+ Videos / mo";
        } else {
            compValEl.textContent = `${volume.toLocaleString()} Videos / mo`;
        }
    }
    if (diffBadgeEl) {
        diffBadgeEl.textContent = diffText;
        diffBadgeEl.className = `px-2 py-0.5 rounded text-[10px] font-black uppercase tracking-wide ${diffBadgeClass}`;
    }
    if (compAdviceEl) compAdviceEl.textContent = adviceText;

    // 2. SEO strategy advice
    const potentialValEl = document.getElementById("potentialValue");
    const recTextEl = document.getElementById("recommendationText");

    let potentialText = "Stable";
    let recText = "Healthy stable interest over time. Good candidate for evergreen videos.";

    if (growth > 15.0) {
        potentialText = "Exponential";
        recText = "This search term is experiencing rapid growth! Create content targeting this keyword immediately to capture new search intent.";
    } else if (growth < -5.0) {
        potentialText = "Declining";
        recText = "Publication volume is contracting. Consider alternative keywords unless this is highly specific to your niche.";
    } else if (growth > 0.0) {
        potentialText = "Growing";
        recText = "Publication volume is steadily rising. A great time to enter this niche.";
    }

    if (potentialValEl) {
        if (volume >= 1000000) {
            potentialValEl.textContent = potentialText;
        } else {
            potentialValEl.textContent = `${potentialText} (${growth >= 0 ? '+' : ''}${growth.toFixed(1)}%)`;
        }
    }
    if (recTextEl) recTextEl.textContent = recText;

    // 3. Concurrently fetch dynamic AI analysis from Gemini
    if (compAdviceEl) {
        compAdviceEl.innerHTML = `<span class="flex items-center gap-1.5 text-gray-400 dark:text-gray-500 font-medium"><i class="ph-bold ph-spinner animate-spin"></i> Generating AI competition analysis...</span>`;
    }
    if (recTextEl) {
        recTextEl.innerHTML = `<span class="flex items-center gap-1.5 text-gray-400 dark:text-gray-500 font-medium"><i class="ph-bold ph-spinner animate-spin"></i> Generating custom SEO advice...</span>`;
    }

    const currentKeywordId = keyword;
    compAdviceEl.dataset.currentKeyword = currentKeywordId;

    fetch(`/api/keywords/${encodeURIComponent(keyword)}/ai-analysis?volume=${volume}&growth=${growth}`)
        .then(res => {
            if (!res.ok) throw new Error("AI analysis failed");
            return res.json();
        })
        .then(aiData => {
            if (compAdviceEl.dataset.currentKeyword !== currentKeywordId) return;

            if (diffBadgeEl && aiData.difficulty) {
                const difficultyStyles = {
                    "Easy": "bg-emerald-50 dark:bg-emerald-950/20 text-emerald-600",
                    "Moderate": "bg-amber-50 dark:bg-amber-950/20 text-amber-600",
                    "Hard": "bg-red-50 dark:bg-red-950/20 text-red-600",
                    "Ultra High": "bg-purple-50 dark:bg-purple-950/20 text-purple-600"
                };
                const difficultyClass = difficultyStyles[aiData.difficulty] || "bg-gray-50 dark:bg-gray-800 text-gray-500";
                diffBadgeEl.textContent = aiData.difficulty;
                diffBadgeEl.className = `px-2 py-0.5 rounded text-[10px] font-black uppercase tracking-wide ${difficultyClass}`;
            }
            if (compAdviceEl && aiData.competitionAdvice) {
                compAdviceEl.innerHTML = renderMarkdownToHtml(aiData.competitionAdvice);
            }
            if (potentialValEl && aiData.growthPotential) {
                potentialValEl.textContent = aiData.growthPotential;
            }
            if (recTextEl && aiData.seoAdvice) {
                recTextEl.innerHTML = renderMarkdownToHtml(aiData.seoAdvice);
            }
        })
        .catch(err => {
            console.error("Gemini analysis error:", err);
            if (compAdviceEl.dataset.currentKeyword === currentKeywordId) {
                if (compAdviceEl) compAdviceEl.textContent = adviceText;
                if (recTextEl) recTextEl.textContent = recText;
            }
        });
}

// Dynamic updates on theme changes
document.addEventListener('theme-changed', async () => {
    const canvas = document.getElementById("keywordHistoryChart");
    if (canvas && !canvas.classList.contains("hidden") && historyChartInstance && currentSelectedKeyword) {
        await renderComparativeChart(canvas, currentSelectedKeyword);
    }
});

/**
 * Parse markdown formatting using marked library
 */
function renderMarkdownToHtml(markdown) {
    if (!markdown) return "";
    try {
        if (MarkedModule) {
            return MarkedModule.parse(markdown);
        }
        return markdown; // Fallback if marked is not yet fully loaded
    } catch (e) {
        console.error("Markdown parsing failed, using fallback:", e);
        return markdown;
    }
}
