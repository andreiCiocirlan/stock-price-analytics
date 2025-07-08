let ohlcWindow;
let ohlcContainer;
let chart;
let isResizing = false;

function addProjectionBandsSVG(chart, projections) {
    if (!chart || !projections || !Array.isArray(projections) || projections.length === 0) return;
    if (!chart.xAxis || !chart.xAxis[0] || !chart.yAxis || !chart.yAxis[0]) return;

    // Destroy previous group if exists
    if (chart.customProjectionGroup) {
    chart.customProjectionGroup.destroy();
    chart.customProjectionGroup = null;  // Clear reference after destroy
    }

    const xAxis = chart.xAxis[0];
    const yAxis = chart.yAxis[0];
    const minVisible = xAxis.min; // timestamp of left visible edge
    const maxVisible = xAxis.max; // timestamp of right visible edge

    // Create new group and assign it immediately
    const group = chart.renderer.g('custom-projection-bands').add();
    chart.customProjectionGroup = group;

    // Loop over each projection and draw its bands
    projections.forEach((proj, index) => {
        if (!proj) return;

        const firstDate = new Date(proj.firstPointDate).getTime();
        const secondDate = new Date(proj.secondPointDate).getTime();

        // Skip if both projection points are outside the visible range on the right
        if (firstDate > maxVisible && secondDate > maxVisible) {
            return; // Do not render this projection band
        }

        // Optionally, also skip if both points are left of visible range (off screen left)
        if (firstDate < minVisible && secondDate < minVisible) {
            return;
        }

        // Convert data to pixels
        const x1 = Math.round(Math.max(0, Math.min(chart.plotWidth, xAxis.toPixels(firstDate))));
        const x2 = Math.round(Math.max(0, Math.min(chart.plotWidth, xAxis.toPixels(secondDate))));

        const yLevels = [
            { from: proj.level0, to: proj.level1, color: 'rgba(128,128,128,0.3)', label: '1' },
            { from: proj.level1, to: proj.level_minus1, color: 'rgba(128,128,128,0.3)', label: '0' },
            { from: proj.level_minus1, to: proj.level_minus2, color: 'rgba(255,255,0,0.3)', label: '-1' },
            { from: proj.level_minus2, to: proj.level_minus2_5, color: 'rgba(255,255,0,0.3)', label: '-2' },
            { from: proj.level_minus2_5, to: proj.level_minus4, color: 'rgba(255,0,0,0.3)', label: '-2.5' },
            { from: proj.level_minus4, to: proj.level_minus4_5, color: 'rgba(255,0,0,0.3)', label: '-4' },
            { from: proj.level_minus4_5, to: proj.level_minus4_5, color: 'rgba(255,0,0,0.3)', label: '-4.5' }
        ];

      yLevels.forEach(({ from, to, color, label }) => {
          const yFrom = chart.yAxis[0].toPixels(from);
          const yTo = chart.yAxis[0].toPixels(to);

          const rectX = Math.min(x1, x2);
          const rectY = Math.min(yFrom, yTo);
          const rectWidth = Math.abs(x2 - x1);
          const rectHeight = Math.abs(yTo - yFrom);
          if (rectWidth <= 1) return; // Skip very narrow or zero-width bands

          // Draw rectangle
          chart.renderer.rect(rectX, rectY, rectWidth, rectHeight)
            .attr({ fill: color, zIndex: 5 })
            .add(group);

          // Draw top horizontal line (bold)
          chart.renderer.path([
            'M', rectX, rectY,
            'L', rectX + rectWidth, rectY
          ])
          .attr({
            stroke: '#000000',
            'stroke-width': 2,
            zIndex: 10
          })
          .add(group);

          // Draw bottom horizontal line (bold)
          chart.renderer.path([
            'M', rectX, rectY + rectHeight,
            'L', rectX + rectWidth, rectY + rectHeight
          ])
          .attr({
            stroke: '#000000',
            'stroke-width': 2,
            zIndex: 10
          })
          .add(group);

          // Anchor label to top line:
            const labelX = rectX - 10;
            const labelY = proj.level0 < proj.level1 ? rectY + rectHeight : rectY + 1;

            chart.renderer.text(label, labelX, labelY)
              .css({ color: '#666', fontSize: '10px', fontWeight: 'bold' })
              .attr({ align: 'right', 'text-anchor': 'end' })
              .add(group);
        });
   });
}


function updateOHLCChart(stockData, projections) {
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
                // Store the current projections on the chart object
                chart.currentProjections = projections;

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
                chart.currentProjections = projections;  // update projections on existing chart
                if (projections) {
                    addProjectionBandsSVG(chart, projections);
                } else {
                    // No projection found: clear previous bands if any
                    if (chart.customProjectionGroup) {
                        chart.customProjectionGroup.destroy();
                        chart.customProjectionGroup = null;
                    }
                    chart.currentProjections = null; // Clear stored projections
                }
            } else {
               // If the chart doesn't exist, create a new one
               chart = Highcharts.stockChart('ohlc-container', {
                    chart: {
                        type: 'candlestick',
                        backgroundColor: '#171B26',
                        events: {
                            load: function () {
                                if (this.currentProjections) addProjectionBandsSVG(this, this.currentProjections);
                            },
                            redraw: function () {
                                if (this.currentProjections) addProjectionBandsSVG(this, this.currentProjections);
                            }
                        }
                    },
                    title: { text: `${stockData.ticker}`, style: { color: '#FFFFFF' } },
                    navigation: { buttonOptions: { enabled: false}}, // remove corner button tooltip
                    xAxis: {
                        crosshair: {
                            width: 1,
                            color: 'gray',
                            dashStyle: 'Dash',
                            label: {
                                enabled: true,
                                backgroundColor: '#444444',  // Solid gray background
                                borderRadius: 3,
                                style: {
                                    color: '#FFFFFF',       // White text for contrast
                                    fontWeight: 'bold',
                                    padding: '5px 10px'
                                },
                            }
                        },
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
                            name: '200 SMA',
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
                            name: '21 SMA',
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
                            name: '100 SMA',
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
                        headerFormat: '',
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
              chart.currentProjections = projections;
              // Draw bands initially
              if (projections) {
                addProjectionBandsSVG(chart, projections);
              } else {
                // No projection found: clear previous bands if any
                if (chart.customProjectionGroup) {
                    chart.customProjectionGroup.destroy();
                    chart.customProjectionGroup = null;
                }
                chart.currentProjections = null; // Clear stored projection
              }

           }
       })
       .catch(error => console.error(error));
}