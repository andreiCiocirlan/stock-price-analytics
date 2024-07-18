let ohlcWindow;
let ohlcContainer;
let chart;
let isResizing = false;

function openStockGraph(stockData) {
    // Check if the window is already open
    if (ohlcWindow && !ohlcWindow.closed) {
        // If the window is open, focus on it and update the chart
        ohlcWindow.focus();
        updateOHLCChart(stockData);
    } else {
        // If the window is not open, create a new one
        ohlcWindow = window.open('', 'OHLC Chart', 'width=1200,height=750');
        ohlcWindow.onload = () => {
            // Set the background color of the <body> element
            ohlcWindow.document.body.style.backgroundColor = '#171B26';

            // Create the OHLC chart container in the new window
            ohlcContainer = ohlcWindow.document.createElement('div');
            ohlcContainer.id = 'ohlc-container';
            ohlcContainer.style.height = '100%';
            ohlcWindow.document.body.appendChild(ohlcContainer);

            // Add a resize event listener to the window
            ohlcWindow.addEventListener('resize', () => {
                isResizing = true;
                if (chart) {
                    chart.reflow();
                }
                isResizing = false;
            });

            // Add an unload event listener to the window
            ohlcWindow.addEventListener('unload', () => {
                // Remove the chart container when the window is closed
                ohlcContainer.remove();
                ohlcContainer = null;
                chart = null;
            });

            // Add a keydown event listener to the window
            ohlcWindow.addEventListener('keydown', (event) => {
                if (event.key === 'Escape') {
                    ohlcWindow.close();
                }
            });

            // Fetch the price data and render the OHLC chart
            updateOHLCChart(stockData);
        };
    }
}

function updateOHLCChart(stockData) {
    const urlParams = new URLSearchParams(window.location.search);
    const timeFrame = (urlParams.get('timeFrame') || 'daily').toLowerCase();

    let rangeSelect = {
        buttons: [
            { type: 'week', count: 12, text: '3M' },    // weekly candles (3M of hist prices)
            { type: 'month', count: 6, text: '6M' },   // monthly candles (6M years of hist prices)
            { type: 'year', count: 2, text: '2Y' },    // yearly candles (2 years of hist prices)
            { type: 'all', text: 'All' }
        ],
        selected: 2 // Default to 1 week
    };

    // Adjust the rangeSelector buttons based on the timeFrame
    switch (timeFrame) {
        case 'weekly':
            rangeSelect.buttons = [
                { type: 'month', count: 3, text: '3M' },   // monthly candles (3M of hist prices)
                { type: 'month', count: 6, text: '6M' },   // monthly candles (6M of hist prices)
                { type: 'year', count: 2, text: '2Y' },    // yearly candles (2Y of hist prices)
                { type: 'all', text: 'All' }
            ],
            rangeSelect.selected = 3 // Default to 1 week
            break;
        case 'monthly':
            rangeSelect.buttons = [
                { type: 'month', count: 24, text: '2Y' },   // monthly candles (2Y of hist prices)
                { type: 'year', count: 5, text: '5Y' },    // yearly candles (5Y of hist prices)
                { type: 'all', text: 'All' }
            ];
            rangeSelect.selected = 2
            break;
        case 'yearly':
            rangeSelect.buttons = [
               { type: 'year', count: 10, text: '10Y' },    // yearly candles (10Y of hist prices)
               { type: 'year', count: 25, text: '25Y' },    // yearly candles (25Y of hist prices)
               { type: 'all', text: 'All' }
            ];
            rangeSelect.selected = 1
            break;
    };

    let URL = `/ohlc/prices?timeFrame=daily&ticker=${stockData.ticker}`;
    fetch(URL)
        .then(response => response.json())
        .then(priceData => {
            // Render the OHLC chart in the new window
            if (ohlcContainer) {
                // If the chart container exists, create or update the chart
                if (chart) {
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
                    });
                } else {
                   // If the chart doesn't exist, create a new one
                   chart = Highcharts.stockChart(ohlcContainer, {
                        chart: {
                            type: 'candlestick',
                            backgroundColor: '#171B26'
                        },
                        title: { text: `${stockData.ticker}`, style: { color: '#FFFFFF' } },
                        navigation: { buttonOptions: { enabled: false}}, // remove corner button tooltip
                        xAxis: {
                            crosshair: { width: 1, color: 'gray', dashStyle: 'Dash' },
                            type: 'datetime',
                            labels: { style: { color: '#FFFFFF' } }
                        },
                        yAxis: {
                            crosshair: { width: 1, color: 'gray', dashStyle: 'Dash', label: { enabled: true, style: { color: '#FFFFFF' } } },
                            gridLineWidth: 0.2,
                            title: { text: 'Price' },
                            labels: { style: { color: '#FFFFFF'} },
                            opposite: true, // Position the y-axis on the right side
                            offset: 30 // Position the y-axis on the right side
                        },
                        legend: {
                            itemStyle: { color: '#FFFFFF', bold: true }
                        },
                        plotOptions: {
                            candlestick: {
                                color: '#E53935', // Red color for negative change
                                upColor: '#00B34D', // Green color for positive change
                                lineColor: '#E53935', // Red color for negative wicks
                                upLineColor: '#00B34D', // Green color for positive wicks
                                minPointLength: 8, // Minimum height of the candlestick
                                pointPadding: 0.15, // Padding between candlesticks
                                groupPadding: 0.02 // Padding between groups of candlesticks
                            }
                        },
                        rangeSelector: rangeSelect,
                        series: [
                            {
                                id: 'main-series',
                                name: 'Stock Data',
                                type: 'candlestick',
                                data: priceData.map(item => [
                                    new Date(item.date).getTime(),
                                    item.open,
                                    item.high,
                                    item.low,
                                    item.close
                                ]),
                                tooltip: {
                                     pointFormat:
                                            '<span style="font-size:15px;color:white">' +
                                                  'O<span style="color:{point.color}">{point.open:.2f}</span> ' +
                                                  'H<span style="color:{point.color}">{point.high:.2f}</span> ' +
                                                  'L<span style="color:{point.color}">{point.low:.2f}</span> ' +
                                                  'C<span style="color:{point.color}">{point.close:.2f}</span></span>'
                                }
                            },
                            {
                                type: 'sma',
                                id: 'sma-200',
                                name: '200SMA',
                                color: 'red',
                                linkedTo: 'main-series',
                                params: { period: 200 },
                                lineWidth: 0.5,
                                marker: { enabled: false },
                                tooltip: {
                                    pointFormat: '<span style="font-size:15px;color:red">{series.name}: {point.y:.2f}</span>'
                                }
                            },
                            {
                                type: 'sma',
                                id: 'sma-21',
                                name: '21SMA',
                                color: 'yellow',
                                linkedTo: 'main-series',
                                params: { period: 21 },
                                lineWidth: 0.5,
                                marker: { enabled: false },
                                tooltip: {
                                    pointFormat: '<span style="font-size:15px;color:yellow">{series.name}: {point.y:.2f}</span>'
                                }
                            },
                            {
                                type: 'sma',
                                id: 'sma-100',
                                name: '100SMA',
                                color: 'orange',
                                linkedTo: 'main-series',
                                params: { period: 100 },
                                lineWidth: 0.5,
                                marker: { enabled: false },
                                tooltip: {
                                    pointFormat: '<span style="font-size:15px;color:orange">{series.name}: {point.y:.2f}</span>'
                                }
                            }
                        ],
                        tooltip: {
                            enabled: true,
                            shared: true,
                            useHTML: true,
                            style: {
                                fontSize: '12px',
                                color: '#FFFFFF',
                                backgroundColor: 'transparent'
                            },
                            backgroundColor: 'rgba(0, 0, 0, 0.05)',
                            borderColor: 'transparent',
                            borderWidth: 0,
                            shadow: false,
                            split: true,
                            xDateFormat: '%d %b \'%y',
                            positioner: function(labelWidth, labelHeight, point) {
                                const chart = this.chart;
                                let x, y;

                                if (point.isHeader) {
                                    return {
                                        x: point.plotX + this.chart.plotLeft - labelWidth / 2,
                                        y: this.chart.chartHeight - labelHeight
                                    };
                                } else {
                                    return { x : 10, y : 0 };
                                }
                            }
                        }
                   });
                   ohlcContainer.addEventListener('resize', () => {
                       if (!isResizing) {
                           chart.reflow();
                       }
                   });
               }
           } else {
               console.error('ohlcContainer is not defined');
           }
       })
       .catch(error => console.error(error));
}