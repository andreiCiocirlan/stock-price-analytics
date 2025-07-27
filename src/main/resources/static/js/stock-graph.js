let ohlcWindow;
let ohlcContainer;
let chart = null;
let isResizing = false;

import { getChartConfig } from './chartConfig.js';
import { addProjectionBandsSVG } from './projection-utils.js';

export function updateOHLCChart(stockData, projections) {
	const urlParams = new URLSearchParams(window.location.search);
	const timeFrame = (urlParams.get('timeFrame') || 'daily').toLowerCase();

	// Adjust the rangeSelector buttons based on the timeFrame
	switch (timeFrame) {
		case 'weekly':
			rangeSelect.buttons = [
					{ type: 'month', count: 3, text: '3M' }, // monthly candles (3M of hist prices)
					{ type: 'month', count: 6, text: '6M' }, // monthly candles (6M of hist prices)
					{ type: 'year', count: 2, text: '2Y' }, // yearly candles (2Y of hist prices)
					{ type: 'all', text: 'All' }
				],
				rangeSelect.selected = 3 // Default to 1 week
			break;
		case 'monthly':
			rangeSelect.buttons = [
				{ type: 'month', count: 24, text: '2Y' }, // monthly candles (2Y of hist prices)
				{ type: 'year', count: 5, text: '5Y' }, // yearly candles (5Y of hist prices)
				{ type: 'all', text: 'All' }
			];
			rangeSelect.selected = 2
			break;
		case 'yearly':
			rangeSelect.buttons = [
				{ type: 'year', count: 10, text: '10Y' }, // yearly candles (10Y of hist prices)
				{ type: 'year', count: 25, text: '25Y' }, // yearly candles (25Y of hist prices)
				{ type: 'all', text: 'All' }
			];
			rangeSelect.selected = 1
			break;
	};

	let URL = `/ohlc/prices?timeFrame=daily&ticker=${stockData.ticker}`;
	fetch(URL)
		.then(response => response.json())
		.then(priceData => {
			if (chart) {
				// Store the current projections on the chart object
//				chart.currentProjections = projections;

				chart.update({
					title: {
						text: `${stockData.ticker}`
					},
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
//				chart.currentProjections = projections; // update projections on existing chart
//				if (projections) {
//					addProjectionBandsSVG(chart, projections);
//				}
//				else {
					// No projection found: clear previous bands if any
//					if (chart.customProjectionGroup) {
//						chart.customProjectionGroup.destroy();
//						chart.customProjectionGroup = null;
//					}
//					chart.currentProjections = null; // Clear stored projections
//				}
			}
			else {
				// If the chart doesn't exist, create a new one
				const chartConfig = getChartConfig(stockData, priceData, projections);
				chart = Highcharts.stockChart('ohlc-container', chartConfig);

				ohlcContainer = document.getElementById('ohlc-container');
				ohlcContainer.addEventListener('resize', () => {
					if (!isResizing) {
						chart.reflow();
					}
				});

				// Assign currentProjection to the newly created chart
//				chart.currentProjections = projections;
				// Draw bands initially
//				if (projections) {
//					addProjectionBandsSVG(chart, projections);
//				}
//				else {
//					// No projection found: clear previous bands if any
//					if (chart.customProjectionGroup) {
//						chart.customProjectionGroup.destroy();
//						chart.customProjectionGroup = null;
//					}
//					chart.currentProjections = null; // Clear stored projection
//				}

			}
		})
		.catch(error => console.error(error));
}