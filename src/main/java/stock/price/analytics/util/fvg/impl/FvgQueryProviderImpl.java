package stock.price.analytics.util.fvg.impl;

import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;
import stock.price.analytics.model.gaps.enums.FvgType;
import stock.price.analytics.model.prices.enums.PricePerformanceMilestone;
import stock.price.analytics.model.prices.enums.StockTimeframe;
import stock.price.analytics.util.fvg.FvgQueryProvider;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class FvgQueryProviderImpl implements FvgQueryProvider {

    @Override
    public String findTickersFVGsTaggedQueryFor(StockTimeframe timeframe, FvgType fvgType, PricePerformanceMilestone pricePerformanceMilestone, String cfdMargins) {
        String prefix = timeframe.stockPrefix();
        Pair<String, String> queryFields = switch (pricePerformanceMilestone) {
            case HIGH_52W_95, LOW_52W_95, HIGH_52W_90, LOW_52W_90 -> new MutablePair<>("low52w", "high52w");
            case HIGH_4W_95, LOW_4W_95, HIGH_4W_90, LOW_4W_90 -> new MutablePair<>("low4w", "high4w");
            case HIGH_ALL_TIME_95, LOW_ALL_TIME_95, HIGH_ALL_TIME_90, LOW_ALL_TIME_90 ->
                    new MutablePair<>("lowest", "highest");
        };
        String lowField = queryFields.getLeft();
        String highField = queryFields.getRight();
        String fvgTypeStr = fvgType.name();
        String highLowWhereClause = highLowWhereClauseFVGsTagged(pricePerformanceMilestone, lowField, highField);

        return STR."""
                SELECT fvg.*
                FROM stocks s
                JOIN fvg on fvg.ticker = s.ticker AND fvg.status = 'OPEN' AND fvg.timeframe = '\{timeframe}' and fvg.type = '\{fvgTypeStr}'
                WHERE
                s.cfd_margin in (\{cfdMargins})
                \{highLowWhereClause}
                AND (s.\{prefix}high between fvg.low AND fvg.high OR s.\{prefix}low between fvg.low AND fvg.high)
                """;
    }

    private String highLowWhereClauseFVGsTagged(PricePerformanceMilestone pricePerformanceMilestone, String lowField, String highField) {
        String highLowWhereClause;
        double percentage = pricePerformanceMilestone.is95thPercentileValue() ? 0.95 : 0.9;
        if (PricePerformanceMilestone.highPercentileValues().contains(pricePerformanceMilestone)) {
            highLowWhereClause = STR."AND s.\{lowField} <> s.\{highField} AND (1 - (1 - ((s.close - s.\{lowField}) / (s.\{highField} - s.\{lowField})))) > \{percentage}";
        } else {
            highLowWhereClause = STR."AND s.\{lowField} <> s.\{highField} AND (1 - (s.close - s.\{lowField}) / (s.\{highField} - s.\{lowField})) > \{percentage}";
        }
        return highLowWhereClause;
    }

    @Override
    public String findFVGsQueryFrom(StockTimeframe timeframe, List<String> tickers, boolean allHistoricalData) {
        String date = lookbackDateFor(timeframe, allHistoricalData).format(DateTimeFormatter.ISO_LOCAL_DATE);
        String tickersFormatted = tickers.stream().map(ticker -> STR."'\{ticker}'").collect(Collectors.joining(", "));
        String dateTruncPeriod = timeframe.toDateTruncPeriod();
        String dbTable = timeframe.dbTableOHLC();

        return STR."""
                WITH price_data AS (
                    SELECT
                        ticker,
                        date_trunc('\{dateTruncPeriod}', date)::date AS wmy_date,
                        open, high, low, close,
                        ROW_NUMBER() OVER (PARTITION BY ticker ORDER BY date) AS rn
                    FROM \{dbTable}
                    WHERE date >= '\{date}'::date and ticker in (\{tickersFormatted})
                ),
                fvg_candidates AS (
                    SELECT a.ticker,
                        a.wmy_date AS date1, b.wmy_date AS date2, c.wmy_date AS date3,
                        a.high AS high1, b.high AS high2, c.high AS high3,
                        a.low AS low1, b.low AS low2, c.low AS low3
                    FROM price_data a
                    JOIN price_data b ON b.rn = a.rn + 1 AND b.ticker = a.ticker
                    JOIN price_data c ON c.rn = a.rn + 2 AND c.ticker = a.ticker
                ),
                identified_fvgs AS (
                    SELECT
                        *,
                        CASE
                            WHEN low1 > high3 THEN 'BEARISH'
                            WHEN high1 < low3 THEN 'BULLISH'
                        END AS type
                    FROM fvg_candidates
                    WHERE (low1 > high3 OR high1 < low3)
                )
                SELECT
                    nextval('sequence_fvg') as id,
                    '\{timeframe}' as timeframe,
                    'OPEN' as status,
                    fvg.ticker as ticker,
                    fvg.date2 as date,
                    fvg.type,
                    CASE
                        WHEN fvg.type = 'BULLISH' THEN
                            CASE
                                WHEN low1 > high3 THEN low1
                                WHEN high1 < low3 THEN high1
                            END
                        ELSE
                            CASE
                                WHEN low1 > high3 THEN high3
                                WHEN high1 < low3 THEN low3
                            END
                    END AS LOW,
                    CASE
                        WHEN fvg.type = 'BULLISH' THEN
                            CASE
                                WHEN low1 > high3 THEN high3
                                WHEN high1 < low3 THEN low3
                            END
                        ELSE
                            CASE
                                WHEN low1 > high3 THEN low1
                                WHEN high1 < low3 THEN high1
                            END
                    END AS high,
                    null as unfilled_low1,
                    null as unfilled_high1,
                    null as unfilled_low2,
                    null as unfilled_high2
                FROM identified_fvgs fvg
                WHERE
                    NOT EXISTS (
                        SELECT 1
                        FROM price_data next_wmy
                        WHERE next_wmy.wmy_date >= fvg.date3 AND next_wmy.ticker = fvg.ticker
                          AND (
                              (fvg.type = 'BULLISH' AND next_wmy.low <= fvg.high1)
                              OR (fvg.type = 'BEARISH' AND next_wmy.high >= fvg.low1)
                          )
                    )
                order by ticker, fvg.date2 desc;
                """;
    }

    private LocalDate lookbackDateFor(StockTimeframe timeframe, boolean allHistoricalData) {
        if (allHistoricalData) return LocalDate.of(1990, 1, 1);

        return switch (timeframe) {
            case DAILY -> LocalDate.now().minusWeeks(1);
            case WEEKLY -> LocalDate.now().minusWeeks(3);
            case MONTHLY -> LocalDate.now().minusMonths(3);
            case QUARTERLY -> LocalDate.now().minusMonths(9);
            case YEARLY -> LocalDate.now().minusYears(3);
        };
    }
}
