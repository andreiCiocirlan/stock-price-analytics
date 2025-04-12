package stock.price.analytics.controller.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import stock.price.analytics.model.stocks.enums.MarketState;

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
}
