package stock.price.analytics.model.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import stock.price.analytics.model.prices.PriceMilestone;
import stock.price.analytics.model.prices.ohlc.enums.CandleStickType;
import stock.price.analytics.model.stocks.enums.MarketState;
import stock.price.analytics.util.PriceMilestoneFactory;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class StockHeatmapRequest {
    private String timeFrame;
    private Boolean positivePerfFirst;
    private Integer limit;
    private List<Double> cfdMargins;
    private List<String> priceMilestones;
    private MarketState marketState;
    private CandleStickType candleStickType;

    public List<PriceMilestone> priceMilestones() {
        return PriceMilestoneFactory.priceMilestonesFrom(this.getPriceMilestones());
    }

    public boolean hasMilestonesOrCandlestickFilters() {
        return !this.getPriceMilestones().isEmpty() || this.getCandleStickType() != CandleStickType.ANY;
    }
}
