package stock.price.analytics.util.query.demark.impl;

import org.springframework.stereotype.Component;
import stock.price.analytics.model.prices.enums.StockTimeframe;
import stock.price.analytics.util.query.demark.DemarkQueryProvider;

import java.util.stream.Collectors;

import static stock.price.analytics.util.Constants.CFD_MARGINS_5X_4X;
import static stock.price.analytics.util.Constants.CFD_MARGINS_5X_4X_3X_2X;

@Component
public class DemarkQueryProviderImpl implements DemarkQueryProvider {

    @Override
    public String demarkForTimeframeQuery(StockTimeframe timeframe) {
        if (timeframe == StockTimeframe.DAILY) {
            throw new IllegalArgumentException("Unexpected DAILY timeframe for demark indicator");
        }
        String cfdMargins = CFD_MARGINS_5X_4X_3X_2X.stream().map(cfdMargin -> STR."'\{cfdMargin}'").collect(Collectors.joining(", "));
        String dbTable = timeframe.dbTableOHLC();
        return STR."""
                WITH base AS (
                  SELECT
                    ticker,
                    date,
                    close,
                    LAG(close, 4) OVER (PARTITION BY ticker ORDER BY date) AS close_4_days_ago
                  FROM \{dbTable}
                  WHERE ticker in (select ticker from stocks where delisted_date is null and cfd_margin in (\{cfdMargins}))
                ),
                flags AS (
                  SELECT
                    *,
                    CASE WHEN close > close_4_days_ago THEN 1 ELSE 0 END AS is_up,
                    CASE WHEN close < close_4_days_ago THEN 1 ELSE 0 END AS is_down
                  FROM base
                ),
                sequences AS (
                  SELECT
                    *,
                    SUM(CASE WHEN is_up = 0 THEN 1 ELSE 0 END) OVER (PARTITION BY ticker ORDER BY date ROWS UNBOUNDED PRECEDING) AS up_group,
                    SUM(CASE WHEN is_down = 0 THEN 1 ELSE 0 END) OVER (PARTITION BY ticker ORDER BY date ROWS UNBOUNDED PRECEDING) AS down_group
                  FROM flags
                ),
                counts AS (
                  SELECT
                    ticker,
                    date,
                    close,
                    close_4_days_ago,
                    is_up,
                    is_down,
                    CASE
                	  WHEN is_up = 1 THEN ROW_NUMBER() OVER (PARTITION BY ticker, up_group ORDER BY date) - 1
                	  ELSE 0
                	END AS td_up_count_corrected,
                	CASE
                	  WHEN is_down = 1 THEN ROW_NUMBER() OVER (PARTITION BY ticker, down_group ORDER BY date) - 1
                	  ELSE 0
                	END AS td_down_count_corrected
                  FROM sequences
                ),
                ranked_counts AS (
                  SELECT
                    ticker,
                    date,
                    td_up_count_corrected,
                    td_down_count_corrected,
                    is_up,
                    is_down,
                    ROW_NUMBER() OVER (PARTITION BY ticker ORDER BY date DESC) AS rn
                  FROM counts
                  WHERE
                    ((td_up_count_corrected > 0 AND is_up = 1)
                    OR (td_down_count_corrected > 0 AND is_down = 1))
                )
                INSERT INTO demark (id, ticker, date, timeframe, td, type)
                SELECT
                  nextval('sequence_demark') AS id,
                  ticker,
                  date,
                  '\{timeframe}' AS timeframe,
                  td_up_count_corrected + td_down_count_corrected AS td,
                  CASE
                    WHEN td_up_count_corrected > 0 THEN 'UP'
                    WHEN td_down_count_corrected > 0 THEN 'DOWN'
                    ELSE NULL
                  END AS type
                FROM ranked_counts
                WHERE rn = 1
                ON CONFLICT (ticker, timeframe) DO UPDATE
                SET
                  date = EXCLUDED.date,
                  td = EXCLUDED.td,
                  type = EXCLUDED.type;
                """;
    }
}
