/**
 * Dashboard Console Script
 * Initializes Chart.js and handles SVG World Map interactivity.
 */

let dashboardInitialized = false;
let subscriberChartInstance = null;
let uploadsChartInstance = null;
let worldMapInstance = null;

function destroyDashboard() {
    dashboardInitialized = false;
    
    if (subscriberChartInstance) {
        subscriberChartInstance.destroy();
        subscriberChartInstance = null;
    }
    if (uploadsChartInstance) {
        uploadsChartInstance.destroy();
        uploadsChartInstance = null;
    }
    if (worldMapInstance) {
        if (typeof worldMapInstance.destroy === 'function') {
            worldMapInstance.destroy();
        }
        worldMapInstance = null;
    }
    // Clean up any orphaned tooltips from the DOM body
    document.querySelectorAll('.jvm-tooltip').forEach(el => el.remove());
}

function initializeDashboard() {
    // Check if Chart.js and jsVectorMap are loaded
    if (typeof Chart === "undefined" || typeof jsVectorMap === "undefined") {
        console.warn("Libraries not loaded yet. Retrying in 100ms...");
        setTimeout(initializeDashboard, 100);
        return;
    }

    if (dashboardInitialized) return;
    dashboardInitialized = true;
    initCharts();
    initWorldMap();
    initCompetitorForm();
}

document.addEventListener("turbo:load", () => {
    destroyDashboard();
    initializeDashboard();
});

if (document.readyState === "complete" || document.readyState === "interactive") {
    initializeDashboard();
} else {
    document.addEventListener("DOMContentLoaded", initializeDashboard);
}

function initCharts() {
    const isDark = document.documentElement.classList.contains('dark') || document.body.classList.contains('dark');
    const labelColor = isDark ? '#94a3b8' : '#64748b'; // slate-400 or slate-500
    const gridColor = isDark ? 'rgba(255, 255, 255, 0.06)' : 'rgba(15, 23, 42, 0.05)';

    // 1. Subscriber Growth Chart
    const growthCanvas = document.getElementById("subscriberGrowthChart");
    if (growthCanvas) {
        if (subscriberChartInstance) {
            subscriberChartInstance.destroy();
            subscriberChartInstance = null;
        }
        const rawData = growthCanvas.getAttribute("data-snapshots");
        if (rawData) {
            try {
                const snapshots = JSON.parse(rawData);
                // If we only have 1 snapshot, it's just a single point.
                // To make the graph look like a growth line for first-time users, 
                // we artificially inject a starting point of 0 from 30 days ago.
                if (snapshots.length === 1) {
                    const thirtyDaysAgo = new Date(new Date(snapshots[0].recordedAt).getTime() - (30 * 24 * 60 * 60 * 1000));
                    snapshots.unshift({
                        recordedAt: thirtyDaysAgo,
                        subscriberCount: 0
                    });
                }
                
                const labels = snapshots.map(s => new Date(s.recordedAt).toLocaleDateString());
                const dataPoints = snapshots.map(s => s.subscriberCount);
                
                subscriberChartInstance = new Chart(growthCanvas, {
                    type: 'line',
                    data: {
                        labels: labels,
                        datasets: [{
                            label: 'Subscribers',
                            data: dataPoints,
                            borderColor: '#3b82f6',
                            backgroundColor: isDark ? 'rgba(59, 130, 246, 0.15)' : 'rgba(59, 130, 246, 0.07)',
                            borderWidth: 3,
                            tension: 0.4,
                            fill: true,
                            pointBackgroundColor: isDark ? '#1e293b' : '#fff',
                            pointBorderColor: '#3b82f6',
                            pointBorderWidth: 2,
                            pointRadius: 4
                        }]
                    },
                    options: {
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
                console.error("Error parsing snapshot data:", e);
            }
        }
    }

    // 2. Uploads Performance Chart
    const performanceCanvas = document.getElementById("uploadPerformanceChart");
    if (performanceCanvas) {
        if (uploadsChartInstance) {
            uploadsChartInstance.destroy();
            uploadsChartInstance = null;
        }
        const rawData = performanceCanvas.getAttribute("data-uploads");
        if (rawData) {
            try {
                const uploads = JSON.parse(rawData);
                // Reverse to show oldest to newest left to right
                uploads.reverse();
                
                const labels = uploads.map(u => u.title.substring(0, 15) + '...');
                const dataPoints = uploads.map(u => u.views);
                
                uploadsChartInstance = new Chart(performanceCanvas, {
                    type: 'bar',
                    data: {
                        labels: labels,
                        datasets: [{
                            label: 'Views',
                            data: dataPoints,
                            backgroundColor: isDark ? '#a78bfa' : '#8b5cf6', // violet-400 or violet-500
                            borderRadius: 6
                        }]
                    },
                    options: {
                        responsive: true,
                        maintainAspectRatio: false,
                        plugins: {
                            legend: { display: false },
                            tooltip: {
                                callbacks: {
                                    title: (context) => uploads[context[0].dataIndex].title
                                }
                            }
                        },
                        scales: {
                            x: { display: false },
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
                console.error("Error parsing uploads data:", e);
            }
        }
    }
}

function initWorldMap() {
    const mapEl = document.getElementById("worldMap");
    if (!mapEl) return;

    if (worldMapInstance) {
        if (typeof worldMapInstance.destroy === 'function') {
            worldMapInstance.destroy();
        }
        worldMapInstance = null;
    }

    // Clear any previous map SVG layers and tooltips before initialization
    mapEl.innerHTML = '';
    document.querySelectorAll('.jvm-tooltip').forEach(el => el.remove());

    // Read geo distribution data
    const rawData = mapEl.getAttribute("data-geo");
    let geoData = {};
    if (rawData) {
        try { 
            geoData = JSON.parse(rawData); 
            
            // Prevent jsVectorMap division-by-zero (NaN) interpolation error when min == max
            // We inject a non-existent dummy region code "XX" with value 0.0.
            // Since "XX" does not map to any SVG path on the world map, it remains hidden,
            // but forces the scale bounds [0, max] to calculate properly.
            const keys = Object.keys(geoData);
            if (keys.length > 0) {
                const values = Object.values(geoData);
                const allEqual = values.every(v => v === values[0]);
                if (allEqual) {
                    geoData["XX"] = 0.0;
                }
            }
        } catch(e) { 
            console.error("Error parsing geo data:", e); 
        }
    }

    const isDark = document.documentElement.classList.contains('dark') || document.body.classList.contains('dark');

    try {
        worldMapInstance = new jsVectorMap({
            selector: '#worldMap',
            map: 'world',
            backgroundColor: 'transparent',
            draggable: true,
            zoomButtons: false,
            zoomOnScroll: false,
            regionStyle: {
                initial: {
                    fill: isDark ? '#1e293b' : '#f1f5f9', // slate-800 (subtle dark slate) or slate-100 (clean grey)
                    fillOpacity: 1,
                    stroke: isDark ? '#0f172a' : '#cbd5e1', // slate-900 or slate-300 boundary
                    strokeWidth: 1.0,
                    strokeOpacity: 0.8
                },
                hover: {
                    fill: '#8b5cf6', // premium brand purple hover highlight
                    fillOpacity: 0.95,
                    cursor: 'pointer'
                }
            },
            visualizeData: {
                scale: isDark ? ['#1e3a8a', '#60a5fa'] : ['#dbeafe', '#2563eb'], // brand-900 to brand-400 or brand-100 to brand-600
                values: geoData
            },
            onRegionTooltipShow(event, tooltip, code) {
                const val = geoData[code] || 0;
                tooltip.text(
                    `<div class="px-3 py-2 text-xs font-bold text-slate-100">
                        ${tooltip.text()}: <span class="text-blue-400 dark:text-blue-300 font-black">${val.toFixed(1)}%</span>
                     </div>`,
                    true
                );
            }
        });
    } catch (err) {
        console.error("Error building jsVectorMap:", err);
    }
}

function initCompetitorForm() {
    const form = document.getElementById("addCompetitorForm");
    if (!form) return;

    form.addEventListener("submit", async (e) => {
        e.preventDefault();
        
        const input = document.getElementById("competitorIdInput");
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
                // Reload via Turbo
                window.location.reload();
            } else {
                const data = await response.json();
                alert(data.error || "Failed to add competitor");
            }
        } catch (error) {
            console.error(error);
            alert("Network error occurred.");
        } finally {
            input.disabled = false;
            btn.innerHTML = originalBtnText;
            input.value = "";
        }
    });
}

// Dynamic updates on theme changes
document.addEventListener('theme-changed', () => {
    if (document.getElementById("worldMap") || document.getElementById("subscriberGrowthChart")) {
        console.debug("Theme toggled, dynamically updating dashboard components...");
        // Reinitialize charts and world map with updated colors
        initCharts();
        initWorldMap();
    }
});
