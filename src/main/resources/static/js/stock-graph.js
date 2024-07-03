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
        console.log('opening window');
        // If the window is not open, create a new one
        ohlcWindow = window.open('', 'OHLC Chart', 'width=1200,height=850');
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
                chart = null;
            });

            // Fetch the price data and render the OHLC chart
            updateOHLCChart(stockData);
        };
    }
}

function updateOHLCChart(stockData) {
    const urlParams = new URLSearchParams(window.location.search);
    const timeFrame = (urlParams.get('timeFrame') || 'monthly').toLowerCase();

    let URL = `/ohlc/${timeFrame}?ticker=${stockData.ticker}`;
    fetch(URL)
        .then(response => response.json())
        .then(priceData => {
            // Render the OHLC chart in the new window
            if (ohlcContainer) {
                // If the chart container exists, create or update the chart
                if (chart) {
                    console.log('chart is defined UPDATE');
                    chart.update({
                        title: {
                            text: `${stockData.ticker} - ${timeFrame}`
                        },
                        series: [{
                            name: stockData.ticker,
                            borderWidth: 0.2,
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
                    console.log('chart is not defined');
                       // If the chart doesn't exist, create a new one
                       chart = Highcharts.chart(ohlcContainer, {
                           chart: {
                                type: 'candlestick',
                                backgroundColor: '#171B26'
                           },
                           title: {
                               text: `${stockData.ticker} - ${timeFrame}`,
                               style: { color: '#FFFFFF' }
                           },
                           xAxis: {
                                type: 'datetime',
                                labels: {
                                    style: { color: '#FFFFFF' },
                                    formatter: function() { return Highcharts.dateFormat('%Y', this.value); }
                                }
                           },
                           yAxis: {
                                title: { text: 'Price' },
                                labels: {
                                    style: { color: '#FFFFFF'}
                                },
                                opposite: true // Position the y-axis on the right side
                           },
                           legend: {
                                itemStyle: { color: '#FFFFFF', bold: true }
                           },
                           plotOptions: {
                               candlestick: {
                                   color: '#E53935', // Red color for negative change
                                   upColor: '#00B34D', // Green color for positive change
                                   lineColor: '#E53935', // Red color for negative wicks
                                   upLineColor: '#00B34D' // Green color for positive wicks
                               }
                           },
                           series: [{
                                name: stockData.ticker,
                                borderWidth: 0.2,
                                data: priceData.map(item => [
                                   new Date(item.date).getTime(),
                                   item.open,
                                   item.high,
                                   item.low,
                                   item.close
                                ])
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