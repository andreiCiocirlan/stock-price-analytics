package stock.price.analytics.util.query.stddevprojection.impl;

import org.springframework.stereotype.Component;
import stock.price.analytics.model.prices.enums.StockTimeframe;
import stock.price.analytics.util.query.stddevprojection.StdDevProjectionQueryProvider;

@Component
public class StdDevProjectionQueryProviderImpl implements StdDevProjectionQueryProvider {

    @Override
    public String getConfluenceProjections(String ticker, StockTimeframe timeframe) {
        return STR."""
                SELECT
                   p.pivot_date,
                   p.point_b_offset,
                   p.pivot_type,
                   p.proj_4_0,
                   p.proj_4_5
                FROM std_dev p
                JOIN stocks l ON p.ticker = l.ticker
                WHERE
                    p.ticker = '\{ticker}'
                	AND timeframe = '\{timeframe}'
                    -- Is the current price between the stored 4.0 and 4.5?
                    AND (
                        (p.pivot_type = 'LOW' AND l.close BETWEEN p.proj_4_0 AND p.proj_4_5)
                        OR
                        (p.pivot_type = 'HIGH' AND l.close BETWEEN p.proj_4_5 AND p.proj_4_0)
                    )
                ORDER BY PIVOT_DATE;
                """;
    }

    @Override
    public String insertStdDevProjections(StockTimeframe timeframe) {
        String dbTable = timeframe.dbTableOHLC();
        return STR."""
                    INSERT INTO std_dev (
                        ticker, timeframe, pivot_date, pivot_type,
                        point_a_price, point_b_offset, point_b_source, point_b_price,
                        proj_4_0, proj_4_5
                    )
                    WITH recent_data AS (
                        SELECT d.*
                        FROM (SELECT DISTINCT ticker FROM \{dbTable}) t
                        CROSS JOIN LATERAL (
                            SELECT * FROM \{dbTable} WHERE ticker = t.ticker ORDER BY date DESC LIMIT 7
                        ) d
                    ),
                    numbered AS (
                        SELECT ticker, date, high, low, close,
                               ROW_NUMBER() OVER (PARTITION BY ticker ORDER BY date ASC) AS rn
                        FROM recent_data
                    ),
                    windowed_data AS (
                        SELECT
                            ticker, date, rn, high, low, close,
                            LAG(low, 1)  OVER w AS low_lag1,
                            LAG(low, 2)  OVER w AS low_lag2,
                            LAG(low, 3)  OVER w AS low_lag3,
                            LEAD(low, 1) OVER w AS low_lead1,
                            LEAD(low, 2) OVER w AS low_lead2,
                            LEAD(low, 3) OVER w AS low_lead3,
                            LAG(high, 1)  OVER w AS high_lag1,
                            LAG(high, 2)  OVER w AS high_lag2,
                            LAG(high, 3)  OVER w AS high_lag3,
                            LEAD(high, 1) OVER w AS high_lead1,
                            LEAD(high, 2) OVER w AS high_lead2,
                            LEAD(high, 3) OVER w AS high_lead3
                        FROM numbered
                        WINDOW w AS (PARTITION BY ticker ORDER BY date)
                    ),
                    pivots AS (
                        SELECT ticker, date AS pivot_date, rn AS pivot_rn, low AS point_a_price, 'LOW' AS pivot_type
                        FROM windowed_data
                        WHERE low < low_lag1 AND low < low_lag2 AND low < low_lag3
                          AND low < low_lead1 AND low < low_lead2 AND low < low_lead3
                          AND low_lead3 IS NOT NULL AND low <= 1000
                        UNION ALL
                        SELECT ticker, date, rn, high, 'HIGH'
                        FROM windowed_data
                        WHERE high > high_lag1 AND high > high_lag2 AND high > high_lag3
                          AND high > high_lead1 AND high > high_lead2 AND high > high_lead3
                          AND high_lead3 IS NOT NULL AND high <= 1000
                    ),
                    point_b_candidates AS (
                        SELECT
                            p.ticker, p.pivot_date, p.pivot_type, p.point_a_price,
                            o.offset_days,
                            src.source_type AS point_b_source,
                            CASE
                                WHEN p.pivot_type = 'LOW' AND src.source_type = 'WICK'  THEN nh.high
                                WHEN p.pivot_type = 'LOW' AND src.source_type = 'CLOSE' THEN nh.close
                                WHEN p.pivot_type = 'HIGH' AND src.source_type = 'WICK'  THEN nl.low
                                WHEN p.pivot_type = 'HIGH' AND src.source_type = 'CLOSE' THEN nl.close
                            END AS point_b_price
                        FROM pivots p
                        CROSS JOIN (VALUES (-3),(-2),(-1),(1),(2),(3)) AS o(offset_days)
                        CROSS JOIN (VALUES ('WICK'), ('CLOSE')) AS src(source_type)
                        LEFT JOIN numbered nh ON nh.ticker = p.ticker AND nh.rn = p.pivot_rn + o.offset_days
                        LEFT JOIN numbered nl ON nl.ticker = p.ticker AND nl.rn = p.pivot_rn + o.offset_days
                    ),
                    raw_calcs AS (
                        SELECT ticker, pivot_date, pivot_type, point_a_price,
                               offset_days AS point_b_offset, point_b_source, point_b_price,
                               ABS(point_a_price - point_b_price) AS vector_price
                        FROM point_b_candidates
                        WHERE point_b_price IS NOT NULL
                    )
                    SELECT
                        ticker, '\{timeframe}', pivot_date, pivot_type, point_a_price, point_b_offset, point_b_source, point_b_price,
                        CASE WHEN pivot_type = 'HIGH' THEN point_b_price - (vector_price * 4.0) ELSE point_b_price + (vector_price * 4.0) END,
                        CASE WHEN pivot_type = 'HIGH' THEN point_b_price - (vector_price * 4.5) ELSE point_b_price + (vector_price * 4.5) END
                    FROM raw_calcs
                    WHERE
                        (CASE WHEN pivot_type = 'HIGH' THEN point_b_price - (vector_price * 4.0) ELSE point_b_price + (vector_price * 4.0) END) > 0
                        AND (CASE WHEN pivot_type = 'HIGH' THEN point_b_price - (vector_price * 4.5) ELSE point_b_price + (vector_price * 4.5) END) > 0
                    ON CONFLICT (ticker, timeframe, pivot_date, pivot_type, point_b_offset, point_b_source) DO NOTHING;
                """;
    }
}
