package stock.price.analytics.controller.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import stock.price.analytics.model.candlestick.CandleStickType;
import stock.price.analytics.model.prices.PriceMilestone;
import stock.price.analytics.model.prices.enums.*;
import stock.price.analytics.model.stocks.enums.MarketState;

import java.util.ArrayList;
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
    private List<String> milestoneTypes;
    private MarketState marketState;
    private CandleStickType candleStickType;

    public List<PriceMilestone> priceMilestones() {
        List<PriceMilestone> priceMilestones = new ArrayList<>();
        for (int i = 0; i < this.getPriceMilestones().size(); i++) {
            String priceMilestoneStr = this.getPriceMilestones().get(i);
            String milestoneType = this.getMilestoneTypes().get(i);
            priceMilestones.add(switch (milestoneType) {
                case "performance" ->
                        priceMilestoneStr.startsWith("NEW") ? NewHighLowMilestone.valueOf(priceMilestoneStr) : PricePerformanceMilestone.valueOf(priceMilestoneStr);
                case "premarket" -> PreMarketPriceMilestone.valueOf(priceMilestoneStr);
                case "intraday-spike" -> IntradayPriceSpike.valueOf(priceMilestoneStr);
                case "sma-milestone" -> SimpleMovingAverageMilestone.valueOf(priceMilestoneStr);
                default -> throw new IllegalArgumentException("Invalid milestone type");
            });
        }
        return priceMilestones;
    }
}
