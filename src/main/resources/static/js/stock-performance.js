let currentTimeFrame = 'MONTHLY';
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
        selectedButton = document.querySelector('.time-frame-button-group button[onclick*="MONTHLY"]');
        selectedButton.classList.add('active');
        currentTimeFrame = 'MONTHLY';
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

function updateStockPerformanceChart(timeFrame) {
    const urlParams = new URLSearchParams(window.location.search);
    const numRows = document.getElementById('numRows').value || 5;
    const numCols = document.getElementById('numCols').value || 5;
    const positivePerfFirst = document.getElementById('positivePerfFirst').checked || false;
    const xtb = document.getElementById('xtbOnly').checked;
    const cfdMargin = document.getElementById('cfdMargin');

    if (timeFrame == undefined) {
        timeFrame = 'MONTHLY';
    }
    const limit = numRows * numCols;
    url = `/stock-performance-json?timeFrame=${timeFrame}&positivePerfFirst=${positivePerfFirst}&limit=${limit}`;
    if (cfdMargin) {
        url += '&cfdMargin=' + cfdMargin.value;
    }
    if (xtb) {
        url += '&xtb=true';
    }

    fetch(url)
        .then(response => response.json())
        .then(data => {
            updateStockPerformanceChartWithData(data, timeFrame, numRows, numCols, positivePerfFirst);
        })
        .catch(error => console.error(error));
}

function updateStockPerformanceChartWithData(data, timeFrame, numRows, numCols, positivePerfFirst) {
    stockPerformanceChart = Highcharts.chart('container', {
        chart: { type: 'heatmap', backgroundColor: '#171B26' },
        tooltip: { enabled: false },
        title: null,
        xAxis: { visible: false, categories: Array.from({ length: numCols }, (_, i) => `Col ${i + 1}`) },
        yAxis: { visible: false, categories: Array.from({ length: numRows }, (_, i) => `Row ${i + 1}`) },
        legend: {
            align: 'center',
            layout: 'horizontal',
            symbolType: 'square',
            labelFormatter: function() {
                let legendItem = `${this.to.toFixed(1)}`;
                if (legendItem != 0.5) {
                    if (legendItem > 0) {
                        legendItem = `${this.from.toFixed(1)}`;
                    }
                } else {
                    legendItem = 0;
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
                { from: 10.3, to: 100, color: '#00B24D' },
                { from: 100, to: 100000, color: '#00B24D' } // Very green for performance > 10.3
            ]
        },
        plotOptions: {
            heatmap: { borderWidth: 1 }
        },
        series: [{
            name: 'Stock Performance',
            dataLabels: {
                enabled: true,
                color: '#FFFFFF',
                style: {
                    fontSize: '100%',
                    fontWeight: 'bold',
                    textAlign: 'center',
                    verticalAlign: 'middle'
                },
                formatter: function() { return data[this.point.index].ticker + '<br>' + data[this.point.index].performance + ' %'; }
            },
            point: {
                events: {
                    click: function() {
                     openStockGraph({ ticker: data[this.index].ticker });
                    }
                }
            },
            data:
                data.map((item, index) => [
                        Math.floor(index / numRows),
                        index % numRows,
                        item.performance,
                        item.ticker
                ])
        }]
    });
}

// Call the function to determine the selected timeframe
const selectedTimeFrame = determineSelectedTimeFrame();

updateStockPerformanceChart();