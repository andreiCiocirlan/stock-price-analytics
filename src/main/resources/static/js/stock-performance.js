fetchStockPerformance();

function fetchStockPerformance() {
    const urlParams = new URLSearchParams(window.location.search);
    const timeFrame = urlParams.has('timeFrame') ? urlParams.get('timeFrame') : 'MONTHLY';
    const numRows = urlParams.has('rows') ? urlParams.get('rows') : 20;
    const numCols = urlParams.has('cols') ? urlParams.get('cols') : 25;
    const xtb = urlParams.has('xtb') ? urlParams.get('xtb') === 'true' : false;
    const positivePerfFirst = urlParams.has('positivePerfFirst') ? urlParams.get('positivePerfFirst') : 'true';
    const cfdMargin = urlParams.get('cfdMargin');
    let url = '/stock-performance-json';
    url += `?timeFrame=${timeFrame}&xtb=${xtb}&positivePerfFirst=${positivePerfFirst}`;

    if (urlParams.has(('cfdMargin'))) {
        url += '&cfdMargin=' + urlParams.get('cfdMargin');
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
                            { from: -100, to: -10.3, color: '#E53935' }, // Bright red for performance < -8.3
                            { from: -10.3, to: -5.6, color: '#C82925' },
                            { from: -5.6, to: -2.9, color: '#AF3835' },
                            { from: -2.9, to: -0.5, color: '#7D2A27' },
                            { from: -0.5, to: 0, color: '#494949' }, // Gray for performance between -0.5 and 0
                            { from: 0, to: 0.5, color: '#494949' },
                            { from: 0.5, to: 2.9, color: '#165E45' },
                            { from: 2.9, to: 5.6, color: '#027B52' },
                            { from: 5.6, to: 10.3, color: '#00A06A' },
                            { from: 10.3, to: 10000, color: '#00B24D' } // Very green for performance > 8.3
                     ]
                },
                plotOptions: {
                    heatmap: { borderWidth: 0 }
                },
                series: [{
                    name: 'Stock Performance',
                    borderWidth: 0.2,
                    data:   data.sort((a, b) => {
                                    if (positivePerfFirst === 'true') return b.performance - a.performance;
                                    return a.performance - b.performance;
                                })
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