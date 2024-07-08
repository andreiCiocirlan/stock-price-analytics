let currentTimeFrame = 'MONTHLY';
let stockPerformanceChart;

function handleTimeFrameButtonClick(timeFrame) {
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
        title: { text: `${timeFrame}` + ' Heatmap', style: { color: '#FFFFFF' } },
        xAxis: { visible: false, categories: Array.from({ length: numCols }, (_, i) => `Col ${i + 1}`) },
        yAxis: { visible: false, categories: Array.from({ length: numRows }, (_, i) => `Row ${i + 1}`) },
        legend: { itemStyle: { color: '#FFFFFF' } },
        colorAxis: {
             dataClasses: [
//                        { from: -100, to: -10.3, color: '#E53935' }, // Bright red for performance < -8.3
                    { from: -100, to: -10.3, color: '#171B26' }, // Bright red for performance < -8.3
                    { from: -10.3, to: -5.6, color: '#171B26' },
                    { from: -5.6, to: -2.9, color: '#171B26' },
                    { from: -2.9, to: -0.5, color: '#171B26' },
                    { from: -0.5, to: 0, color: '#494949' }, // Gray for performance between -0.5 and 0
                    { from: 0, to: 0.5, color: '#494949' },
                    { from: 0.5, to: 2.9, color: '#171B26' },
                    { from: 2.9, to: 5.6, color: '#171B26' },
                    { from: 5.6, to: 10.3, color: '#171B26' },
//                        { from: 10.3, to: 10000, color: '#00B24D' } // Very green for performance > 8.3
                    { from: 10.3, to: 10000, color: '#171B26' } // Very green for performance > 8.3
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

updateStockPerformanceChart();