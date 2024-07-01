fetchStockPerformance();

function fetchStockPerformance() {
    const urlParams = new URLSearchParams(window.location.search);
    const timeFrame = urlParams.has('timeFrame') ? urlParams.get('timeFrame') : 'MONTHLY';
    const numRows = urlParams.has('rows') ? urlParams.get('rows') : 30;
    const numCols = urlParams.has('cols') ? urlParams.get('cols') : 33;
    const xtbOnly = urlParams.has('xtb') ? urlParams.get('xtb') : true;
    let url = '/stock-performance-json';
    if (timeFrame) {
        url += `?timeFrame=${timeFrame}`;
    }
    if (xtbOnly) {
        url += `&xtb=${xtbOnly}`;
    }
    fetch(url)
        .then(response => response.json())
        .then(data => {
            Highcharts.chart('container', {
                chart: { type: 'heatmap', backgroundColor: '#171B26' },
                tooltip: { enabled: false },
                title: { text: `${timeFrame}` + ' Heatmap', style: { color: '#FFFFFF' } },
                xAxis: { visible: false, categories: Array.from({ length: numCols }, (_, i) => `Col ${i + 1}`) },
                yAxis: { visible: false, categories: Array.from({ length: numRows }, (_, i) => `Row ${i + 1}`) },
                legend: { itemStyle: { color: '#FFFFFF' } },
                colorAxis: {
                     dataClasses: [
                            { from: -10000, to: -10.3, color: '#E53935' }, // Bright red for performance < -8.3
                            { from: -10.3, to: -5.6, color: '#C82925' }, // Bright red to yellow for performance between -8.3 and -5.6
                            { from: -5.6, to: -2.9, color: '#AF3835' }, // Yellow for performance between -5.6 and 0
                            { from: -2.9, to: -0.5, color: '#7D2A27' }, // Yellow for performance between -5.6 and 0
                            { from: -0.5, to: 0, color: '#494949' }, // Yellow for performance between -5.6 and 0
                            { from: 0, to: 0.5, color: '#494949' }, // Yellow for performance between 0 and 5.6
                            { from: 0.5, to: 2.9, color: '#165E45' }, // Yellow for performance between -5.6 and 0
                            { from: 2.9, to: 5.6, color: '#027B52' }, // Yellow for performance between -5.6 and 0
                            { from: 5.6, to: 10.3, color: '#00A06A' }, // Yellow to very green for performance between 5.6 and 8.3
                            { from: 10.3, to: 10000, color: '#00B24D' } // Very green for performance > 8.3
                     ]
                },
                plotOptions: {
                    heatmap: { borderWidth: 0, colsize: 1, rowsize: 1 }
                },
                series: [{
                    name: 'Stock Performance',
                    borderWidth: 0.2,
                    data: data  .sort((a, b) => b.performance - a.performance)
                                .slice(0, numRows * numCols).reduce((acc, item, index) => {
                                    const rowIndex = Math.floor(index / numRows); // Adjust the number of columns per row as needed
                                    const ticker = item.ticker;
                                    acc.push([rowIndex, index % numRows, item.performance, ticker]);

                                    return acc;
                                }, []),
                    dataLabels: {
                        enabled: true,
                        color: '#FFFFFF',
                        formatter: function() { return data[this.point.index].ticker + '<br>' + data[this.point.index].performance; }
                    },
                     point: {
                         events: {
                             click: function() {
                                 openStockGraph({ ticker: data[this.index].ticker });
                             }
                         }
                     }
                }]
            });
        })
        .catch(error => console.error(error));
}