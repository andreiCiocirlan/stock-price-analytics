package stock.price.analytics.util.importstatus.impl;

import stock.price.analytics.model.prices.enums.StockTimeframe;
import stock.price.analytics.util.importstatus.ImportStatusQueryProvider;

public class ImportStatusQueryProviderImpl implements ImportStatusQueryProvider {

    @Override
    public String checkImportStatusQueryFor(StockTimeframe timeframe, boolean checkFirstImport) {
        String timeframePeriod = timeframe.toDateTruncPeriod();
        return STR."""
                SELECT
                    COUNT(*) = \{checkFirstImport ? "0" : "1"}
                FROM
                    daily_prices
                WHERE
                    ticker = 'AAPL'
                    AND date_trunc('\{timeframePeriod}', date) = date_trunc('\{timeframePeriod}', current_date);
                """;
    }


}
