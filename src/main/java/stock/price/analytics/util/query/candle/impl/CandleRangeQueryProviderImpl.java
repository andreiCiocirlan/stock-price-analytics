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
        return STR."""
                SELECT ticker, AVG(high - low) AS avg_range
                FROM (
                    SELECT
                        ticker,
                        high,
                        low,
                        ROW_NUMBER() OVER (PARTITION BY ticker ORDER BY date DESC) AS rn
                    FROM \{tableName}
                	where date > current_date - interval '4 \{intervalPeriod}'
                ) sub
                WHERE rn <= 15
                GROUP BY ticker
                """;
    }

}