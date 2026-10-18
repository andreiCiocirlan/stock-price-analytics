export function addProjectionBandsSVG(chart, projections) {
    if (!chart || !projections || !Array.isArray(projections) || projections.length === 0) return;
    if (!chart.xAxis || !chart.xAxis[0] || !chart.yAxis || !chart.yAxis[0]) return;

    if (chart.customProjectionGroup) {
        chart.customProjectionGroup.destroy();
        chart.customProjectionGroup = null;
    }

    const xAxis = chart.xAxis[0];
    const yAxis = chart.yAxis[0];
    const minVisible = xAxis.min;
    const maxVisible = xAxis.max;

    const mainSeries = chart.series.find(s => s.options.id === 'main-series');
    if (!mainSeries || !mainSeries.options.data) return;

    const ohlcData = mainSeries.options.data;
    const timestamps = ohlcData.map(d => d[0]);

    const group = chart.renderer.g('custom-projection-bands').attr({ zIndex: 10 }).add();
    chart.customProjectionGroup = group;

    projections.forEach((proj) => {
        if (!proj) return;

        const pivotDateMs = new Date(proj.pivotDate).getTime();

        let pivotIndex = timestamps.indexOf(pivotDateMs);
        if (pivotIndex === -1) {
            pivotIndex = timestamps.findIndex(ts => Math.abs(ts - pivotDateMs) < 86400000 * 7);
        }
        if (pivotIndex === -1) return;

        const pointBIndex = pivotIndex + proj.pointBOffset;
        if (pointBIndex < 0 || pointBIndex >= ohlcData.length) return;

        const pointADateMs = ohlcData[pivotIndex][0];
        const pointBDateMs = ohlcData[pointBIndex][0];

        if (pointADateMs > maxVisible && pointBDateMs > maxVisible) return;
        if (pointADateMs < minVisible && pointBDateMs < minVisible) return;

        const x1 = Math.round(Math.max(0, Math.min(chart.plotWidth, xAxis.toPixels(pointADateMs))));
        const x2 = Math.round(Math.max(0, Math.min(chart.plotWidth, xAxis.toPixels(pointBDateMs))));

        const rectX = Math.min(x1, x2);
        const rectWidth = Math.max(Math.abs(x2 - x1), 2);

        // Point A Price (The Pivot Candle)
        const pivotPrice = proj.pivotType === 'HIGH'
            ? ohlcData[pivotIndex][2]
            : ohlcData[pivotIndex][3];

        // Point B Price (The Swing Candle / Level 0)
        const vector = Math.abs(proj.proj40 - proj.proj45) * 2;
        let pointBPrice;
        if (proj.pivotType === 'HIGH') {
            pointBPrice = proj.proj40 + (vector * 4.0);
        } else {
            pointBPrice = proj.proj40 - (vector * 4.0);
        }

        let yLevels;
        if (proj.pivotType === 'HIGH') {
            yLevels = [
                { from: pointBPrice, to: proj.proj40, color: 'rgba(128,128,128,0.3)' },
                { from: proj.proj40, to: proj.proj45, color: 'rgba(255,0,0,0.3)' }
            ];
        } else {
            yLevels = [
                { from: pointBPrice, to: proj.proj40, color: 'rgba(128,128,128,0.3)' },
                { from: proj.proj40, to: proj.proj45, color: 'rgba(255,0,0,0.3)' }
            ];
        }

        // 1. Draw the core rectangular bands
        yLevels.forEach(({ from, to, color }) => {
            const yFrom = yAxis.toPixels(from);
            const yTo = yAxis.toPixels(to);

            const rectY = Math.min(yFrom, yTo);
            const rectHeight = Math.abs(yTo - yFrom);

            if (rectHeight <= 1) return;

            chart.renderer.rect(rectX, rectY, rectWidth, rectHeight)
                .attr({ fill: color, zIndex: 5 })
                .add(group);
        });

        // 2. Draw the Wedge (The Gray Rectangle from Point A to Point B)
        const yA = yAxis.toPixels(pivotPrice);
        const yB = yAxis.toPixels(pointBPrice);

        // We use a path to create a polygon that connects Point A to Point B
        chart.renderer.path([
            'M', x1, yA, // Start at Point A (Pivot)
            'L', x2, yB, // Line to Point B (Swing)
            'L', x2, yB, // Drop down (for LOW) or up (for HIGH)
            'L', x1, yA, // Return to Point A
            'Z'          // Close path
        ]).attr({
            fill: 'rgba(128,128,128,0.3)',
            zIndex: 4
        }).add(group);

        const wedgeRectY = Math.min(yA, yB);
        const wedgeRectHeight = Math.abs(yA - yB);

        // Check if we need to fill it (if height > 0)
        if (wedgeRectHeight > 1) {
            // Because the old image had a horizontal top line at Point A and a horizontal bottom line at Point B
            // We can just draw a grey rectangle.
            // But if the lines are angled, a rectangle won't perfectly connect them.
            // We'll use the polygon to create the vertical connection.

            chart.renderer.path([
                'M', x1, yA,
                'L', x2, yA, // Top horizontal (at Pivot level)
                'L', x2, yB, // Right vertical
                'L', x1, yB, // Bottom horizontal (at Point B level)
                'Z'
            ]).attr({
                fill: 'rgba(128,128,128,0.3)',
                zIndex: 4
            }).add(group);
        }

        // --- Draw the Labels ---
        const y0 = yAxis.toPixels(pointBPrice);
        const y4 = yAxis.toPixels(proj.proj40);
        const y45 = yAxis.toPixels(proj.proj45);

        chart.renderer.text('0', rectX - 4, y0 + 4)
            .css({ color: '#FFFFFF', fontSize: '10px', fontWeight: 'bold' })
            .attr({ align: 'right', 'text-anchor': 'end', zIndex: 20 })
            .add(group);

        chart.renderer.text(proj.pivotType === 'HIGH' ? '-4' : '4', rectX - 4, y4 + 4)
            .css({ color: '#FFFFFF', fontSize: '10px', fontWeight: 'bold' })
            .attr({ align: 'right', 'text-anchor': 'end', zIndex: 20 })
            .add(group);

        const y45Text = proj.pivotType === 'HIGH' ? y45 - 2 : y45 + 10;
        chart.renderer.text(proj.pivotType === 'HIGH' ? '-4.5' : '4.5', rectX - 4, y45Text)
            .css({ color: '#FFFFFF', fontSize: '10px', fontWeight: 'bold' })
            .attr({ align: 'right', 'text-anchor': 'end', zIndex: 20 })
            .add(group);

        // --- Draw the horizontal delimiting lines ---

        // Top line at Pivot (Point A)
        chart.renderer.path(['M', x1, yA, 'L', x2, yA])
            .attr({ stroke: '#000000', 'stroke-width': 2, zIndex: 15 })
            .add(group);

        // Bottom line at Point B (Level 0)
        chart.renderer.path(['M', x1, yB, 'L', x2, yB])
            .attr({ stroke: '#000000', 'stroke-width': 2, zIndex: 15 })
            .add(group);

        // Line at 4.5 level
        chart.renderer.path(['M', x1, y45, 'L', x2, y45])
            .attr({ stroke: '#000000', 'stroke-width': 2, zIndex: 15 })
            .add(group);
    });
}