package stock.price.analytics.util.query.importstatus.impl;

import org.springframework.stereotype.Component;
import stock.price.analytics.model.prices.enums.StockTimeframe;
import stock.price.analytics.util.query.importstatus.ImportStatusQueryProvider;

@Component
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
