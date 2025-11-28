package stock.price.analytics.util.query.demark;

import stock.price.analytics.model.prices.enums.StockTimeframe;

public interface DemarkQueryProvider {

    String demarkForTimeframeQuery(StockTimeframe timeframe);

    String tickersForTimeframeTdAndCfdMarginsQuery(StockTimeframe timeframe, int tdCount, String cfdMargins);
}
