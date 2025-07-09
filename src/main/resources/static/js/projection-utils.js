export function addProjectionBandsSVG(chart, projections) {
	if (!chart || !projections || !Array.isArray(projections) || projections.length === 0) return;
	if (!chart.xAxis || !chart.xAxis[0] || !chart.yAxis || !chart.yAxis[0]) return;

	// Destroy previous group if exists
	if (chart.customProjectionGroup) {
		chart.customProjectionGroup.destroy();
		chart.customProjectionGroup = null; // Clear reference after destroy
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
			{
				from: proj.level_minus4_5,
				to: proj.level_minus4_5,
				color: 'rgba(255,0,0,0.3)',
				label: '-4.5'
			}
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