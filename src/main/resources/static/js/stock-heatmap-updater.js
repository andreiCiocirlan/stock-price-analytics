let lastKnownUpdateTimestamp = 0;

function pollForHeatmapUpdates() {
    setInterval(() => {
        fetch('/cache/last-update-timestamp')
            .then(response => response.json())
            .then(timestamp => {
                if (lastKnownUpdateTimestamp === 0) {
                    lastKnownUpdateTimestamp = timestamp;
                } else if (timestamp > lastKnownUpdateTimestamp) {
                    lastKnownUpdateTimestamp = timestamp;
                    updateStockPerformanceChartCurrentTimeframe();
                }
            })
            .catch(error => console.error('Error checking for updates:', error));
    }, 1000 * 60 * 1); // Poll every minute
}

window.onload = pollForHeatmapUpdates;
