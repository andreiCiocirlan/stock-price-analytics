import { getChartConfig } from './chartConfig.js';
import { handleTimeFrameButtonClick, dispatchTimeFrameChangeEvent } from './stock-performance.js';
import { addProjectionBandsSVG } from './projection-utils.js';
import { updateOHLCChart } from './stock-graph.js';

// Wait for DOM to be fully loaded
document.addEventListener('DOMContentLoaded', () => {
	const dropdown = document.getElementById('positivePerfFirst');
	if (dropdown) {
		dropdown.addEventListener('change', dispatchTimeFrameChangeEvent);
	}

	document.querySelectorAll('select.form-control').forEach(el => {
		el.addEventListener('change', dispatchTimeFrameChangeEvent);
	});

	const timeFrames = ['DAILY', 'WEEKLY', 'MONTHLY', 'QUARTERLY', 'YEARLY'];

	timeFrames.forEach(tf => {
		const btn = document.getElementById(`btn-${tf.toLowerCase()}`);
		if (btn) {
			btn.addEventListener('click', () => handleTimeFrameButtonClick(tf));
		}
	});
});