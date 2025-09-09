package stock.price.analytics.model.dto;

import stock.price.analytics.model.prices.PriceMilestone;
import stock.price.analytics.model.prices.ohlc.enums.CandleStickType;
import stock.price.analytics.util.PriceMilestoneFactory;

import java.util.List;

public record StockHeatmapRequest(
        String timeFrame,
        Boolean positivePerfFirst,
        Integer limit,
        List<Double> cfdMargins,
        List<String> priceMilestones,
        CandleStickType candleStickType
) {
    public List<PriceMilestone> priceMilestonesFrom() {
        return PriceMilestoneFactory.priceMilestonesFrom(this.priceMilestones());
    }

    public boolean hasMilestonesOrCandlestickFilters() {
        return !this.priceMilestones().isEmpty() || this.candleStickType() != CandleStickType.ANY;
    }
}
