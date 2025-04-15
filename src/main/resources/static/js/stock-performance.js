let currentTimeFrame = 'WEEKLY';
let stockPerformanceChart;

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
    updateStockPerformanceChart(currentTimeFrame);
}

function getMarketState() {
    let now = new Date();
    let nytzString = now.toLocaleString("en-US", { timeZone: "America/New_York" });
    let nytzDate = new Date(nytzString);

    // Check if within specified time frame
    let weekday = (nytzDate.getDay() >= 1 && nytzDate.getDay() <= 5),
        hours = ((nytzDate.getHours() === 8) || ((nytzDate.getHours() === 9 && nytzDate.getMinutes() < 30)));

    return weekday && hours ? "PRE" : "REGULAR";
}

function calculateGridSize(mapSize) {
    const maxRows = 20;
    const maxColumns = 30;

    const scalingFactor = Math.sqrt(mapSize / 600);
    const numRows = Math.round(maxRows * scalingFactor);
    const numCols = Math.round(maxColumns * scalingFactor);

    return {
        numRows: Math.min(numRows, maxRows),
        numCols: Math.min(numCols, maxColumns),
    };
}

function updateStockPerformanceChartCurrentTimeframe() {
    updateStockPerformanceChart(currentTimeFrame);
}

function updateStockPerformanceChart(timeFrame) {
    const positivePerfFirst = document.getElementById('positivePerfFirst').checked || false;
    const cfdMarginValues = document.getElementById('cfdMarginValues').value === '' ? [] : document.getElementById('cfdMarginValues').value.split(',');
    const priceMilestone = document.getElementById('priceMilestone');
    const marketState = getMarketState();

    if (timeFrame == undefined) {
        timeFrame = 'WEEKLY';
    }
    const limit = 600;

    const requestBody = {
        timeFrame,
        positivePerfFirst,
        limit,
        marketState,
        cfdMargins: cfdMarginValues
    };

    if (priceMilestone) {
        const dropdowns = priceMilestone.querySelectorAll('select[milestone-type]');

        const typeMap = {
            'pre-market': 'premarket',
            'intraday-spike': 'intraday-spike',
            'all-time': 'performance',
            '52-week': 'performance',
            '4-week': 'performance',
            'sma-200': 'sma-milestone',
            'sma-50': 'sma-milestone'
        };

        const priceMilestones = [];
        const milestoneTypes = [];

        dropdowns.forEach(dropdown => {
            const selectedValue = dropdown.value;
            if (selectedValue !== "ANY") {
                priceMilestones.push(selectedValue);
                milestoneTypes.push(typeMap[dropdown.getAttribute('milestone-type')]);
            }
        });
        requestBody.priceMilestones = priceMilestones;
        requestBody.milestoneTypes = milestoneTypes;
    }

    fetch('/stock-performance-json', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(requestBody)
    })
    .then(response => response.json())
    .then(data => {
        const { numRows, numCols } = calculateGridSize(data.length);
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
                    const fontSize = `${squareSize * 0.35}px`; // Adjust the scaling factor as needed
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