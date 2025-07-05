package stock.price.analytics.util.highlow;

import stock.price.analytics.model.prices.highlow.enums.HighLowPeriod;

import java.time.LocalDate;
import java.util.List;

public interface HighLowQueryProvider {
    String weeklyHighLowExistsQuery();

    String queryAllHistoricalHighLowPricesFor(List<String> tickers, LocalDate tradingDate, HighLowPeriod highLowPeriod);

    String highLowPricesNotDelistedForDateQuery(HighLowPeriod highLowPeriod);
}
