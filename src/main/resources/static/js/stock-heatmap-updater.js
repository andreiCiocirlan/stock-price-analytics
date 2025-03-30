function pollForHeatmapUpdates() {
    setInterval(() => {
        fetch('/stock-performance-heatmap/refresh-ui')
            .then(response => response.json())
            .then(isUpdated => {
                if (isUpdated) {
                    updateStockPerformanceChartCurrentTimeFrame();
                }
            })
            .catch(error => console.error('Error checking for updates:', error));
    }, 1000 * 60 * 1); // Poll every minute
}

window.onload = pollForHeatmapUpdates;
