import { addProjectionBandsSVG } from './projection-utils.js';



const rangeSelect = {
    buttons: [
        { type: 'week', count: 12, text: '3M' },    // weekly candles (3M of hist prices)
        { type: 'month', count: 6, text: '6M' },   // monthly candles (6M years of hist prices)
        { type: 'year', count: 2, text: '2Y' },    // yearly candles (2 years of hist prices)
        { type: 'all', text: 'All' }
    ],
    selected: 2 // Default to 1 week
};

const crosshairLabelConfig = {
    enabled: true,
    backgroundColor: '#444444',
    borderRadius: 3,
    style: {
        color: '#FFFFFF',
        fontWeight: 'bold',
        padding: '5px 10px'
    },
    snap : false,
    verticalAlign: 'top',
    y: -10
};

const xAxisCrosshairConfig = {
    width: 1,
    color: 'gray',
    dashStyle: 'Dash',
    label: crosshairLabelConfig
};

const tooltipConfig = {
    headerFormat: '',
    enabled: true,
    shared: true,
    split: true,
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
};

export function getChartConfig(stockData, priceData, projections) {
    return {
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
       navigation: { buttonOptions: { enabled: false }},
       xAxis: {
           type: 'datetime',
           labels: { style: { color: '#FFFFFF' } },
           crosshair: xAxisCrosshairConfig
       },
       yAxis: {
           crosshair: {
               width: 1,
               color: 'gray',
               dashStyle: 'Dash',
               snap : false,
               label: { enabled: true, style: { color: '#FFFFFF' } }
           },
           gridLineWidth: 0.2,
           title: { text: 'Price' },
           labels: { style: { color: '#FFFFFF'} },
           opposite: true,
           offset: 30
       },
       legend: {
           itemStyle: { color: '#FFFFFF', bold: true }
       },
       plotOptions: {
           candlestick: {
               color: '#E53935',
               upColor: '#00B34D',
               lineColor: '#E53935',
               upLineColor: '#00B34D',
               minPointLength: 8,
               pointPadding: 0.15,
               groupPadding: 0.02
           }
       },
       rangeSelector: rangeSelect,  // your existing external config
       tooltip: tooltipConfig,      // use the external tooltip config
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
       ]
   };
}
