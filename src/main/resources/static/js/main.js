//import { handleTimeFrameButtonClick, dispatchTimeFrameChangeEvent } from './stock-performance.js';

import { getChartConfig } from './chartConfig.js';
import { handleTimeFrameButtonClick } from './stock-performance.js';
import { addProjectionBandsSVG } from './projection-utils.js';
import { updateOHLCChart } from './stock-graph.js';

// Wait for DOM to be fully loaded
document.addEventListener('DOMContentLoaded', () => {
  document.getElementById('btn-daily').addEventListener('click', () => {
    handleTimeFrameButtonClick('DAILY');
  });
  document.getElementById('btn-weekly').addEventListener('click', () => {
    handleTimeFrameButtonClick('WEEKLY');
  });
  document.getElementById('btn-monthly').addEventListener('click', () => {
    handleTimeFrameButtonClick('MONTHLY');
  });
  document.getElementById('btn-quarterly').addEventListener('click', () => {
    handleTimeFrameButtonClick('QUARTERLY');
  });
  document.getElementById('btn-yearly').addEventListener('click', () => {
    handleTimeFrameButtonClick('YEARLY');
  });
});