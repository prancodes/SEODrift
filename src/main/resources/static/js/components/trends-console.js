let ChartModule = null;

async function getChart() {
    if (!ChartModule) {
        const module = await import('chart.js/auto');
        ChartModule = module.default;
    }
    return ChartModule;
}

/**
 * Trends & Competitor Console Script
 * Initializes Chart.js benchmarks and handles saved keyword AJAX actions.
 */

let trendsInitialized = false;
let benchmarkChartInstance = null;
let rhythmChartInstance = null;

function destroyTrends() {
    trendsInitialized = false;
    
    if (benchmarkChartInstance) {
        benchmarkChartInstance.destroy();
        benchmarkChartInstance = null;
    }
    if (rhythmChartInstance) {
        rhythmChartInstance.destroy();
        rhythmChartInstance = null;
    }
}

function initializeTrends() {
    const benchmarkCanvas = document.getElementById("subscriberBenchmarkChart");
    const rhythmCanvas = document.getElementById("competitorRhythmChart");
    const form = document.getElementById("trendsAddCompetitorForm");
    if (!benchmarkCanvas && !rhythmCanvas && !form) return;

    if (trendsInitialized) return;
    trendsInitialized = true;

    // Run non-blockingly to reduce layout blocking on page load
    requestAnimationFrame(async () => {
        if (benchmarkCanvas) await initBenchmarkChart();
        
        setTimeout(async () => {
            if (rhythmCanvas) await initRhythmChart();
        }, 30);

        setTimeout(() => {
            initTrendsAddCompetitorForm();
        }, 60);
    });
}

document.addEventListener("turbo:load", () => {
    destroyTrends();
    initializeTrends();
});

if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", () => {
        initializeTrends();
    });
} else {
    initializeTrends();
}

async function initBenchmarkChart() {
    const canvas = document.getElementById("subscriberBenchmarkChart");
    if (!canvas) return;

    if (benchmarkChartInstance) {
        benchmarkChartInstance.destroy();
        benchmarkChartInstance = null;
    }

    const rawData = canvas.getAttribute("data-benchmark");
    if (!rawData) return;

    try {
        const Chart = await getChart();
        if (!Chart) return;

        const benchmarkData = JSON.parse(rawData);
        const isDark = document.documentElement.classList.contains('dark') || document.body.classList.contains('dark');
        const labelColor = isDark ? '#94a3b8' : '#64748b';
        const gridColor = isDark ? 'rgba(255, 255, 255, 0.06)' : 'rgba(15, 23, 42, 0.05)';

        // Prepare labels (we can gather all distinct dates and sort them)
        const allDates = new Set();
        benchmarkData.datasets.forEach(ds => {
            ds.data.forEach(pt => {
                if (pt.x) allDates.add(pt.x);
            });
        });
        const labels = Array.from(allDates).sort();

        // Format datasets for Chart.js
        const datasets = benchmarkData.datasets.map(ds => {
            // Map data points into ordered format aligned with labels
            const dataMap = {};
            ds.data.forEach(pt => {
                dataMap[pt.x] = pt.y;
            });
            const alignedData = labels.map(date => dataMap[date] !== undefined ? dataMap[date] : null);

            return {
                label: ds.label,
                data: alignedData,
                borderColor: ds.borderColor || '#3b82f6',
                backgroundColor: ds.backgroundColor || 'transparent',
                borderWidth: 3,
                tension: 0.3,
                fill: ds.backgroundColor !== 'transparent',
                pointRadius: 3,
                spanGaps: true
            };
        });

        benchmarkChartInstance = new Chart(canvas, {
            type: 'line',
            data: {
                labels: labels.map(d => new Date(d).toLocaleDateString(undefined, { month: 'short', day: 'numeric' })),
                datasets: datasets
            },
            options: {
                animation: false,
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {
                        position: 'top',
                        labels: {
                            color: labelColor,
                            font: { family: 'Inter', weight: 'bold', size: 10 }
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

    } catch (e) {
        console.error("Error parsing benchmarking chart data:", e);
    }
}

async function initRhythmChart() {
    const canvas = document.getElementById("competitorRhythmChart");
    if (!canvas) return;

    if (rhythmChartInstance) {
        rhythmChartInstance.destroy();
        rhythmChartInstance = null;
    }

    const rawData = canvas.getAttribute("data-rhythm");
    if (!rawData) return;

    try {
        const Chart = await getChart();
        if (!Chart) return;

        const rhythmData = JSON.parse(rawData);
        const isDark = document.documentElement.classList.contains('dark') || document.body.classList.contains('dark');
        const labelColor = isDark ? '#94a3b8' : '#64748b';
        const gridColor = isDark ? 'rgba(255, 255, 255, 0.06)' : 'rgba(15, 23, 42, 0.05)';

        // Map days map to lists
        const days = rhythmData.days || {};
        const labels = Object.keys(days).map(d => d.substring(0, 3)); // Mon, Tue, etc.
        const counts = Object.values(days);

        rhythmChartInstance = new Chart(canvas, {
            type: 'bar',
            data: {
                labels: labels,
                datasets: [{
                    label: 'Videos Uploaded',
                    data: counts,
                    backgroundColor: isDark ? '#f59e0b' : '#d97706', // amber-500 or amber-600
                    borderRadius: 6
                }]
            },
            options: {
                animation: false,
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: { display: false }
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

    } catch (e) {
        console.error("Error parsing posting rhythm chart data:", e);
    }
}

// Dynamic updates on theme changes
document.addEventListener('theme-changed', async () => {
    if (document.getElementById("subscriberBenchmarkChart") || document.getElementById("competitorRhythmChart")) {
        console.debug("Theme toggled, dynamically updating trends components...");
        await initBenchmarkChart();
        await initRhythmChart();
    }
});

function initTrendsAddCompetitorForm() {
    const form = document.getElementById("trendsAddCompetitorForm");
    if (!form || form.dataset.listenerAttached) return;
    form.dataset.listenerAttached = "true";

    form.addEventListener("submit", async (e) => {
        e.preventDefault();
        
        const input = document.getElementById("trendsCompetitorIdInput");
        const btn = form.querySelector("button[type='submit']");
        const channelId = input.value.trim();
        
        if (!channelId) return;

        // Disable input
        input.disabled = true;
        const originalBtnText = btn.innerHTML;
        btn.innerHTML = `<i class="ph-bold ph-spinner animate-spin"></i>`;
        
        try {
            const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
            const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;
            
            const response = await fetch('/api/competitors/add', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    [csrfHeader]: csrfToken
                },
                body: JSON.stringify({ channelId: channelId })
            });

            if (response.ok) {
                // Reload
                window.location.reload();
            } else {
                const data = await response.json();
                if (window.showToast) {
                    window.showToast("Failed to Add", data.error || "Failed to add competitor", "error");
                } else {
                    alert(data.error || "Failed to add competitor");
                }
            }
        } catch (error) {
            console.error(error);
            if (window.showToast) {
                window.showToast("Network Error", "Network error occurred.", "error");
            } else {
                alert("Network error occurred.");
            }
        } finally {
            input.disabled = false;
            btn.innerHTML = originalBtnText;
            input.value = "";
        }
    });
}
