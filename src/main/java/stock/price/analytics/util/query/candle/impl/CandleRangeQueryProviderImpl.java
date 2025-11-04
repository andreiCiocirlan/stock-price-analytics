package stock.price.analytics.util.query.candle.impl;

import org.springframework.stereotype.Component;
import stock.price.analytics.model.prices.enums.StockTimeframe;
import stock.price.analytics.util.query.candle.CandleRangeQueryProvider;

@Component
public class CandleRangeQueryProviderImpl implements CandleRangeQueryProvider {

    @Override
    public String averageCandleRangeQuery(StockTimeframe timeframe) {
        String tableName = timeframe.dbTableOHLC();
        String intervalPeriod = timeframe.toIntervalPeriod();
        String dateTruncPeriod = timeframe.toDateTruncPeriod();
        int lookbackCount = timeframe == StockTimeframe.QUARTERLY ? 12 : 4;
        return STR."""
                SELECT ticker, AVG(high - low) AS avg_range
                FROM (
                    SELECT
                        ticker,
                        high,
                        low,
                        ROW_NUMBER() OVER (PARTITION BY ticker ORDER BY date DESC) AS rn
                    FROM \{tableName}
                	where date > date_trunc('\{dateTruncPeriod}', current_date)::date - interval '\{lookbackCount} \{intervalPeriod}'
                ) sub
                WHERE rn <= 15
                GROUP BY ticker
                """;
    }

}