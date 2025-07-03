let ohlcWindow;
let ohlcContainer;
let chart;
let isResizing = false;

function addProjectionBandsSVG(chart, proj) {
  if (!chart || !proj) return;
  if (!chart.xAxis || !chart.xAxis[0] || !chart.yAxis || !chart.yAxis[0]) return;

  // Destroy previous group if exists
  if (chart.customProjectionGroup) {
    chart.customProjectionGroup.destroy();
    chart.customProjectionGroup = null;  // Clear reference after destroy
  }

  // Create new group and assign it immediately
  const group = chart.renderer.g('custom-projection-bands').add();
  chart.customProjectionGroup = group;

  // Convert data to pixels
  const x1 = chart.xAxis[0].toPixels(new Date(proj.localTopDate));
  const x2 = chart.xAxis[0].toPixels(new Date(proj.secondPointDate));

  const level0 = proj.level0;
  const level1 = proj.level1;
  const diff = proj.diff;

  const yLevels = [
    { from: level1 - diff, to: level0, color: 'rgba(128,128,128,0.3)', label: '-1 to 0' },
    { from: level0, to: level1, color: 'rgba(128,128,128,0.15)', label: '0 to 1' },
    { from: level1, to: level1 - 2.5 * diff, color: 'rgba(255,255,0,0.3)', label: '1 to 2.5' },
    { from: level1 - 2.5 * diff, to: level1 - 4.5 * diff, color: 'rgba(255,0,0,0.3)', label: '2.5 to 4.5' }
  ];

  yLevels.forEach(({ from, to, color, label }) => {
    const yFrom = chart.yAxis[0].toPixels(from);
    const yTo = chart.yAxis[0].toPixels(to);

    const rectX = Math.min(x1, x2);
    const rectY = Math.min(yFrom, yTo);
    const rectWidth = Math.abs(x2 - x1);
    const rectHeight = Math.abs(yTo - yFrom);

    // Add rectangle to the group
    chart.renderer.rect(rectX, rectY, rectWidth, rectHeight)
      .attr({ fill: color, zIndex: 5 })
      .add(group);

    // Add label text to the group
    chart.renderer.text(label, rectX + 5, rectY + 15)
      .css({ color: '#666', fontSize: '10px' })
      .add(group);
  });
}


function updateOHLCChart(stockData, projection) {
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
            if (chart) {
                // Store the current projection on the chart object
                chart.currentProjection = projection;

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
                }, true);
                chart.currentProjection = projection;  // update projection on existing chart
                if (projection) {
                    addProjectionBandsSVG(chart, projection);
                } else {
                    // No projection found: clear previous bands if any
                    if (chart.customProjectionGroup) {
                        chart.customProjectionGroup.destroy();
                        chart.customProjectionGroup = null;
                    }
                    chart.currentProjection = null; // Clear stored projection
                }
            } else {
               // If the chart doesn't exist, create a new one
               chart = Highcharts.stockChart('ohlc-container', {
                    chart: {
                        type: 'candlestick',
                        backgroundColor: '#171B26',
                        events: {
                            load: function () {
                                if (this.currentProjection) addProjectionBandsSVG(this, this.currentProjection);
                            },
                            redraw: function () {
                                if (this.currentProjection) addProjectionBandsSVG(this, this.currentProjection);
                            }
                        }
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
               ohlcContainer = document.getElementById('ohlc-container');
               ohlcContainer.addEventListener('resize', () => {
                   if (!isResizing) {
                       chart.reflow();
                   }
               });

              // Assign currentProjection to the newly created chart
              chart.currentProjection = projection;
              // Draw bands initially
              if (projection) {
                addProjectionBandsSVG(chart, projection);
              } else {
                // No projection found: clear previous bands if any
                if (chart.customProjectionGroup) {
                    chart.customProjectionGroup.destroy();
                    chart.customProjectionGroup = null;
                }
                chart.currentProjection = null; // Clear stored projection
              }

           }
       })
       .catch(error => console.error(error));
}