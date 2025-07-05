package stock.price.analytics.util.query.fvg;

import stock.price.analytics.model.gaps.enums.FvgType;
import stock.price.analytics.model.prices.enums.PricePerformanceMilestone;
import stock.price.analytics.model.prices.enums.StockTimeframe;

import java.util.List;

public interface FvgQueryProvider {
    String findTickersFVGsTaggedQueryFor(StockTimeframe timeframe, FvgType fvgType, PricePerformanceMilestone pricePerformanceMilestone, String cfdMargins);

    String findFVGsQueryFrom(StockTimeframe timeframe, List<String> tickers, boolean allHistoricalData);
}
