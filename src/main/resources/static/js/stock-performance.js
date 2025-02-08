let currentTimeFrame = 'WEEKLY';
let stockPerformanceChart;
window.onload = onloadFunction;
setInterval(showHidePreMarketOptions, 300000); // poll every 5 minute to show/hide GAP_* premarket options

function onloadFunction() {
    showHidePreMarketOptions();
    pollForHeatmapUpdates();
}

function pollForHeatmapUpdates() {
    setInterval(() => {
        fetch('/stock-performance-heatmap/refresh-ui')
            .then(response => response.json())
            .then(isUpdated => {
                if (isUpdated) {
                    updateStockPerformanceChart(currentTimeFrame);
                }
            })
            .catch(error => console.error('Error checking for updates:', error));
    }, 1000 * 60 * 1); // Poll every minute
}

function showHidePreMarketOptions() {
    const selectElement = document.getElementById('priceMilestone');
    const options = selectElement.options;

    // Get current time in New York
    const now = new Date();
    const nyTimeString = now.toLocaleString('en-US', { timeZone: 'America/New_York' });
    const nyTime = new Date(nyTimeString);
    const day = nyTime.getDay(); // 0 (Sun) - 6 (Sat)
    const hour = nyTime.getHours();
    const minute = nyTime.getMinutes();

    // Check if it's Mon-Fri (1-5) and between 8:00 - 9:30 AM (NY time)
    const isWeekday = day >= 1 && day <= 5;
    const isTimeInRange = hour === 8 || (hour === 9 && minute < 30);

    if (!(isWeekday && isTimeInRange)) {
        for (let i = 0; i < options.length; i++) {
            const optionValue = options[i].value;
            if (optionValue.startsWith('GAP_')) {
                options[i].style.display = 'none';
            }
        }
    }
}

function dispatchTimeFrameChangeEvent() {
    currentTimeFrame = determineSelectedTimeFrame();
    window.dispatchEvent(new CustomEvent('timeFrameChange', { detail: { timeFrame: currentTimeFrame } }));
}

window.addEventListener('timeFrameChange', (event) => {
    handleTimeFrameButtonClick(event.detail.timeFrame);
});

function determineSelectedTimeFrame() {
    let selectedButton = document.querySelector('.time-frame-button-group button.active');
    if (!selectedButton) {
        selectedButton = document.querySelector('.time-frame-button-group button[onclick*="WEEKLY"]');
        selectedButton.classList.add('active');
        currentTimeFrame = 'WEEKLY';
    } else {
        currentTimeFrame = selectedButton.textContent.trim().toUpperCase();
    }
    return currentTimeFrame;
}

function handleTimeFrameButtonClick(timeFrame) {
    // Remove 'active' class from all buttons
    const buttons = document.querySelectorAll('.time-frame-button-group button');
    buttons.forEach(button => button.classList.remove('active'));

    // Add 'active' class to the selected button
    const selectedButton = document.querySelector(`.time-frame-button-group button[onclick*="${timeFrame}"]`);
    selectedButton.classList.add('active');

    currentTimeFrame = timeFrame;
    const url = new URL(window.location);
    url.searchParams.set('timeFrame', timeFrame);
    window.history.pushState({}, '', url.toString());
    updateStockPerformanceChart(currentTimeFrame);
}

function updatePricesIntraday() {
    const url = `/yahoo-prices/import`;
    fetch(url)
        .then(response => {
            if (!response.ok) {
                throw new Error('Network response was not ok');
            }
        })
        .then(() => {
            updateStockPerformanceChart(currentTimeFrame);
        })
        .catch(error => console.error(error));
}

function updateStockPerformanceChart(timeFrame) {
    const numRows = document.getElementById('numRows').value || 5;
    const numCols = document.getElementById('numCols').value || 5;
    const positivePerfFirst = document.getElementById('positivePerfFirst').checked || false;
    const cfdMarginValues = document.getElementById('cfdMarginValues').value.split(',');
    const priceMilestone = document.getElementById('priceMilestone');

    if (timeFrame == undefined) {
        timeFrame = 'WEEKLY';
    }
    const limit = numRows * numCols;
    url = `/stock-performance-json?timeFrame=${timeFrame}&positivePerfFirst=${positivePerfFirst}&limit=${limit}`;

    // set cfdMargins from multi-select
    cfdMarginValues.forEach(margin => { url += '&cfdMargin=' + margin; });

    if (priceMilestone) {
        url += '&priceMilestone=' + priceMilestone.value;
    }

    fetch(url)
        .then(response => response.json())
        .then(data => {
            updateStockPerformanceChartWithData(data, timeFrame, numRows, numCols, positivePerfFirst);
        })
        .catch(error => console.error(error));
}

function updateStockPerformanceChartWithData(data, timeFrame, numRows, numCols, positivePerfFirst) {
    stockPerformanceChart = Highcharts.chart('heatmap-container', {
        chart: { type: 'heatmap',backgroundColor: '#171B26' },
        navigation: { buttonOptions: { enabled: false}},
        title: null,
        xAxis: { visible: false, categories: Array.from({ length: numCols }, (_, i) => `Col ${i + 1}`) },
        yAxis: { visible: false, categories: Array.from({ length: numRows }, (_, i) => `Row ${i + 1}`) },
        tooltip: { enabled: false },
        credits: { enabled: false },
        legend: {
            enabled: false,
            align: 'center',
            layout: 'horizontal',
            symbolType: 'square',
            itemDistance: -21,  // this glues legend items together
            labelFormatter: function() {
                let legendItem = `${this.from.toFixed(1)}`;
                if (`${legendItem}` >= 0.5) {
                    return `<div class="legend-item-container">
                                         <div class="legend-item" style="background-color:${this.color};color:white;font-weight:bold;">${this.to.toFixed(1)}%</div>
                                      </div>`;
                }
                return `<div class="legend-item-container">
                             <div class="legend-item" style="background-color:${this.color};color:white;font-weight:bold;">${legendItem}%</div>
                          </div>`;
            },
            useHTML: true
        },
        colorAxis: {
            dataClasses: [
                { from: -100, to: -10.3, color: '#E53935' }, // Bright red for performance < -8.3
                { from: -10.3, to: -5.6, color: '#C82925' },
                { from: -5.6, to: -2.9, color: '#AF3835' },
                { from: -2.9, to: -0.5, color: '#7D2A27' },
                { from: -0.5, to: 0.5, color: '#494949' }, // Gray for performance between -0.5 and 0
                { from: 0.5, to: 2.9, color: '#165E45' },
                { from: 2.9, to: 5.6, color: '#027B52' },
                { from: 5.6, to: 10.3, color: '#00A06A' },
                { from: 10.3, to: 9999, color: '#00B24D' } // Very green for performance > 10.3
            ]
        },
        plotOptions: { heatmap: { borderWidth: 1 } },
        series: [{
            name: 'Stock Performance',
            dataLabels: {
                enabled: true,
                color: '#FFFFFF',
                formatter: function() {
                    const squareSize = Math.min(this.point.shapeArgs.width, this.point.shapeArgs.height);
                    const fontSize = `${Math.max(12.5, squareSize * 0.1)}px`; // Adjust the scaling factor as needed
                    return `<span style="font-size: ${fontSize};">${data[this.point.index].ticker}<br>${data[this.point.index].performance}%</span>`;
                }
            },
            point: {
                events: {
                    click: function() {
                        updateOHLCChart({ ticker: data[this.index].ticker });
                    }
                }
            },
            data:
                data.map((item, index) => [
                        Math.floor(index / numRows),
                        (numRows - 1) - (index % numRows),
                        item.performance,
                        item.ticker
                ])
        }]
    });
}

// cfdMargin init and multi-select logic
document.addEventListener('DOMContentLoaded', function() {
    const cfdMarginDiv = document.getElementById('cfdMargin');
    const buttons = cfdMarginDiv.querySelectorAll('.btn');
    const hiddenInput = document.getElementById('cfdMarginValues');
    let selectedValues = hiddenInput.value ? hiddenInput.value.split(',') : []; // Initialize as array

    // Function to update the hidden input
    function updateHiddenInput() {
        hiddenInput.value = selectedValues.join(',');
    }

    // Add click event listeners to the buttons
    buttons.forEach(button => {
        button.addEventListener('click', function() {
            const value = this.value;

            // Check if the button is already active
            if (this.classList.contains('active')) {
                // Remove the value from the selectedValues array
                const index = selectedValues.indexOf(value);
                if (index > -1) {
                    selectedValues.splice(index, 1);
                }
                this.classList.remove('active');
            } else {
                // Add the value to the selectedValues array
                selectedValues.push(value);
                this.classList.add('active');
            }

            // Update the hidden input field
            updateHiddenInput();
            dispatchTimeFrameChangeEvent(selectedValues);
        });
    });

    // Initialize active buttons on load
    buttons.forEach(button => {
        if (selectedValues.includes(button.value)) {
            button.classList.add('active');
        }
    });

     dispatchTimeFrameChangeEvent(selectedValues);
});

// Call the function to determine the selected timeframe
const selectedTimeFrame = determineSelectedTimeFrame();

updateStockPerformanceChart();