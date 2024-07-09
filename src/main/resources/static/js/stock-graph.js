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
    const timeFrame = (urlParams.get('timeFrame') || 'monthly').toLowerCase();

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

    let URL = `/ohlc/${timeFrame}?ticker=${stockData.ticker}`;
    fetch(URL)
        .then(response => response.json())
        .then(priceData => {
            // Render the OHLC chart in the new window
            if (ohlcContainer) {
                // If the chart container exists, create or update the chart
                if (chart) {
                    chart.update({
                        title: {
                            text: `${stockData.ticker} - ${timeFrame}`
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
                            chart: { type: 'candlestick', backgroundColor: '#171B26' },
                            title: { text: `${stockData.ticker} - ${timeFrame}`, style: { color: '#FFFFFF' } },
                            xAxis: {
                                type: 'datetime',
                                dateTimeLabelFormats: { month: '%b', year: '%Y' },
                                labels: { style: { color: '#FFFFFF' }, }
                            },
                            yAxis: {
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
                            series: [{
                                name: stockData.ticker,
                                data: priceData.map(item => [
                                   new Date(item.date).getTime(),
                                   item.open,
                                   item.high,
                                   item.low,
                                   item.close
                                ]),
                                tooltip: {
                                    pointFormat: '<span style="color:{point.color}">\u25CF</span> <b>{series.name}</b><br/>' +
                                              'Date: {point.x:%Y-%m-%d}<br/>' +
                                              'O: {point.open}<br/>' +
                                              'H: {point.high}<br/>' +
                                              'L: {point.low}<br/>' +
                                              'C: {point.close}<br/>'
                                }
                            }]
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