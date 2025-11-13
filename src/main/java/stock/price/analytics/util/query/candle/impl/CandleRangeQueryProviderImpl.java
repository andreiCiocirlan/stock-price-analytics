package stock.price.analytics.util.query.candle.impl;

import org.springframework.stereotype.Component;
import stock.price.analytics.model.prices.enums.StockTimeframe;
import stock.price.analytics.util.query.candle.CandleRangeQueryProvider;

@Component
public class CandleRangeQueryProviderImpl implements CandleRangeQueryProvider {

    @Override
    public String compressedPriceQuery(StockTimeframe timeframe, String cfdMargins) {
        String prefix = timeframe.stockPrefix();
        String tableName = timeframe.dbTableOHLC();
        String intervalPeriod = timeframe.toIntervalPeriod();
        String dateTruncPeriod = timeframe.toDateTruncPeriod();
        int lookbackCount = timeframe == StockTimeframe.QUARTERLY ? 15 : 5;
        if (cfdMargins.isBlank()) {
            cfdMargins = "0.2, 0.25, 0.33, 0.5, 0";
        }
        return STR."""
                WITH avg_ranges AS (
                    SELECT ticker, AVG(high - low) AS avg_range
                    FROM (
                        SELECT
                            ticker,
                            high,
                            low,
                            ROW_NUMBER() OVER (PARTITION BY ticker ORDER BY date DESC) AS rn
                        FROM \{tableName}
                        WHERE date >= date_trunc('\{dateTruncPeriod}', current_date) - interval '\{lookbackCount} \{intervalPeriod}'
                            and ticker in (select ticker from stocks where cfd_margin in (\{cfdMargins}))
                    )
                    WHERE rn <= 10
                    GROUP BY ticker
                ),
                current_quarter AS (
                    SELECT ticker, (\{prefix}high - \{prefix}low) AS current_range
                    FROM stocks
                    WHERE LAST_UPDATED = (
                        SELECT LAST_UPDATED
                        FROM STOCKS
                        WHERE ticker = 'AAPL'
                    )
                )
                SELECT
                    c.ticker
                FROM
                    current_quarter c
                JOIN
                    avg_ranges a ON c.ticker = a.ticker
                WHERE
                    c.current_range <= 0.33 * a.avg_range;
                """;
    }


}