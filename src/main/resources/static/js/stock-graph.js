let ohlcWindow;
let ohlcContainer;
let chart = null;
let isResizing = false;

import { getChartConfig } from './chartConfig.js';
import { addProjectionBandsSVG } from './projection-utils.js';

export function updateOHLCChart(stockData, timeFrame, projections) {
    // 1. Ensure timeFrame is lowercase (e.g., 'weekly')
    const normalizedTimeframe = (timeFrame || 'daily').toLowerCase();

    // 2. Configure Highcharts rangeSelector based on the passed timeframe
    let rangeSelect = { buttons: [], selected: 0 };

    switch (normalizedTimeframe) {
        case 'weekly':
            rangeSelect.buttons = [
                { type: 'month', count: 3, text: '3M' },
                { type: 'month', count: 6, text: '6M' },
                { type: 'year', count: 2, text: '2Y' },
                { type: 'all', text: 'All' }
            ];
            rangeSelect.selected = 3;
            break;
        case 'monthly':
            rangeSelect.buttons = [
                { type: 'month', count: 24, text: '2Y' },
                { type: 'year', count: 5, text: '5Y' },
                { type: 'all', text: 'All' }
            ];
            rangeSelect.selected = 2;
            break;
        case 'quarterly':
            rangeSelect.buttons = [
                { type: 'year', count: 5, text: '5Y' },
                { type: 'year', count: 10, text: '10Y' },
                { type: 'all', text: 'All' }
            ];
            rangeSelect.selected = 1;
            break;
        case 'yearly':
            rangeSelect.buttons = [
                { type: 'year', count: 10, text: '10Y' },
                { type: 'year', count: 25, text: '25Y' },
                { type: 'all', text: 'All' }
            ];
            rangeSelect.selected = 1;
            break;
        default: // Daily
            rangeSelect.buttons = [
                { type: 'month', count: 1, text: '1M' },
                { type: 'month', count: 3, text: '3M' },
                { type: 'year', count: 1, text: '1Y' },
                { type: 'all', text: 'All' }
            ];
            rangeSelect.selected = 2;
            break;
    }

    // 3. Fetch OHLC data using the requested timeframe
    const URL = `/ohlc/prices?timeFrame=${normalizedTimeframe}&ticker=${stockData.ticker}`;

    fetch(URL)
        .then(response => response.json())
        .then(priceData => {
            if (chart) {
                // Update existing chart
                chart.update({
                    title: { text: `${stockData.ticker} (${normalizedTimeframe})` },
                    rangeSelector: rangeSelect,
                    series: [{
                        name: stockData.ticker,
                        data: priceData.map(item => [
                            new Date(item.date).getTime(),
                            item.open,
                            item.high,
                            item.low,
                            item.close
                        ])
                    }]
                }, true);

                chart.currentProjections = projections;
                if (projections && projections.length > 0) {
                    addProjectionBandsSVG(chart, projections);
                } else if (chart.customProjectionGroup) {
                    chart.customProjectionGroup.destroy();
                    chart.customProjectionGroup = null;
                }
            } else {
                // Create new chart
                const chartConfig = getChartConfig(stockData, priceData, projections, normalizedTimeframe);

                // Inject the dynamic rangeSelector into the config
                chartConfig.rangeSelector = rangeSelect;
                chartConfig.title = { text: `${stockData.ticker} (${normalizedTimeframe})` };

                chart = Highcharts.stockChart('ohlc-container', chartConfig);

                ohlcContainer = document.getElementById('ohlc-container');
                ohlcContainer.addEventListener('resize', () => {
                    if (!isResizing) {
                        chart.reflow();
                    }
                });

                chart.currentProjections = projections;
                if (projections && projections.length > 0) {
                    addProjectionBandsSVG(chart, projections);
                }
            }
        })
        .catch(error => console.error("Failed to load OHLC data: ", error));
}