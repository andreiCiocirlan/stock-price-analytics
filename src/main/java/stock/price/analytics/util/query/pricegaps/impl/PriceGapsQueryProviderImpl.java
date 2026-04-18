package stock.price.analytics.util.query.pricegaps.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import stock.price.analytics.model.prices.enums.StockTimeframe;
import stock.price.analytics.repository.json.DailyPriceJSONRepository;
import stock.price.analytics.util.query.pricegaps.PriceGapsQueryProvider;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.stream.Collectors;

import static stock.price.analytics.util.TradingDateUtil.tradingDateNow;

@RequiredArgsConstructor
@Component
public class PriceGapsQueryProviderImpl implements PriceGapsQueryProvider {

    private final DailyPriceJSONRepository dailyPriceJSONRepository;

    @Override
    public String saveHistoricalPriceGapsQueryFor(List<String> tickers, StockTimeframe timeframe) {
        String tickersFormatted = tickers.stream().map(ticker -> STR."'\{ticker}'").collect(Collectors.joining(", "));
        String dbTable = timeframe.dbTableOHLC();
        String dateTruncPeriod = timeframe.toDateTruncPeriod();
        String interval = timeframe.toInterval();
        String intervalPeriod = timeframe.toIntervalPeriod();
        int lookBackCount = switch (timeframe) {
            case DAILY -> 500;
            case WEEKLY -> 300;
            case MONTHLY -> 200;
            case QUARTERLY -> 100;
            case YEARLY -> 10;
        };

        return STR."""
            WITH max_date_cte AS (
                select date_trunc('\{dateTruncPeriod}', (select max(last_updated) from stocks)) - INTERVAL '\{interval}' as max_date
            ),
            ranked_prices AS (
                SELECT
                    ticker,
                    close AS closing_price,
                    date AS closing_date,
                    ROW_NUMBER() OVER (PARTITION BY ticker ORDER BY date DESC) AS row_num
                FROM \{dbTable}
                WHERE ticker in (\{tickersFormatted})
            	    AND ticker in (select ticker from stocks where cfd_margin in (0.2, 0.25, 0.33, 0.5))
                    AND date between CURRENT_DATE - INTERVAL '\{lookBackCount} \{intervalPeriod}' and (SELECT max_date from max_date_cte)
            ),
            unfilled_gaps AS (
                SELECT
                    p1.ticker,
                    p1.closing_price,
                    p1.closing_date
                FROM ranked_prices p1
                WHERE row_num <= \{lookBackCount} and NOT EXISTS (
                    SELECT 1
                    FROM \{dbTable} p2
                    WHERE p2.ticker = p1.ticker
                    AND p2.date > p1.closing_date
                    AND p1.closing_price BETWEEN p2.low AND p2.high
                )
            )
            INSERT INTO price_gaps (id, ticker, close, timeframe, status, date)
            SELECT
            	nextval('sequence_prices_gaps') AS id,
            	ticker,
            	closing_price,
            	'\{timeframe}',
            	'OPEN',
            	closing_date
            FROM unfilled_gaps
            ON CONFLICT (ticker, timeframe, date)
            DO UPDATE SET
                close = EXCLUDED.close,
                status = EXCLUDED.status,
                id = EXCLUDED.id;
            """;
    }

    @Override
    public String saveTodayPriceGapsQueryFor(StockTimeframe timeframe) {
        String dbTable = timeframe.dbTableOHLC();
        LocalDate tradingDateNow = tradingDateNow();
        LocalDate curr_date = switch (timeframe) {
            case DAILY -> tradingDateNow;
            case WEEKLY -> tradingDateNow.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            case MONTHLY -> tradingDateNow.with(TemporalAdjusters.firstDayOfMonth());
            case QUARTERLY -> LocalDate.of(tradingDateNow.getYear(), tradingDateNow.getMonth().firstMonthOfQuarter().getValue(), 1);
            case YEARLY -> tradingDateNow.with(TemporalAdjusters.firstDayOfYear());
        };
        LocalDate prev_date = switch (timeframe) {
            case DAILY -> dailyPriceJSONRepository.getPreviousTradingDate();
            case WEEKLY -> curr_date.minusWeeks(1);
            case MONTHLY -> curr_date.minusMonths(1);
            case QUARTERLY -> curr_date.minusMonths(3);
            case YEARLY -> curr_date.minusYears(1);
        };

        return STR."""
                INSERT INTO price_gaps (id, ticker, close, timeframe, status, date)
                SELECT
                  nextval('sequence_prices_gaps') AS id,
                  p.ticker,
                  prev_p.close,
                  '\{timeframe}',
                  'OPEN',
                  prev_p.date
                FROM \{dbTable} p
                JOIN \{dbTable} prev_p ON prev_p.ticker = p.ticker
                WHERE p.date = '\{curr_date}'
                  AND prev_p.date = '\{prev_date}'
                  AND p.ticker in (select ticker from stocks where delisted_date is null and cfd_margin in (0.2, 0.25, 0.33, 0.5))
                  AND (prev_p.close < p.low OR prev_p.close > p.high)
                ON CONFLICT (ticker, timeframe, date)
                DO UPDATE SET
                  close = EXCLUDED.close,
                  status = EXCLUDED.status;
                """;
    }

    @Override
    public String gapUpTickersQueryFor(StockTimeframe timeframe, String cfdMargins) {
        String prefix = timeframe.stockPrefix();
        String interval = timeframe.toInterval();
        String intervalPeriod = timeframe.toIntervalPeriod();
        if (cfdMargins.isBlank()) {
            cfdMargins = "0.2, 0.25, 0.33, 0.5, 0";
        }

        return STR."""
                    SELECT s.ticker FROM price_gaps pg
                    JOIN stocks s on s.ticker = pg.ticker AND
                         pg.date = date_trunc('\{intervalPeriod}', s.last_updated) - interval '\{interval}' AND
                         s.delisted_date is null
                    WHERE
                        s.cfd_margin IN (\{cfdMargins}) AND
                        s.\{prefix}low > pg.close AND
                        pg.timeframe = '\{timeframe}'
                """;
    }

    @Override
    public String gapDownTickersQueryFor(StockTimeframe timeframe, String cfdMargins) {
        String prefix = timeframe.stockPrefix();
        String interval = timeframe.toInterval();
        String intervalPeriod = timeframe.toIntervalPeriod();
        if (cfdMargins.isBlank()) {
            cfdMargins = "0.2, 0.25, 0.33, 0.5, 0";
        }

        return STR."""
                    SELECT s.ticker FROM price_gaps pg
                    JOIN stocks s on s.ticker = pg.ticker AND
                         pg.date = date_trunc('\{intervalPeriod}', s.last_updated) - interval '\{interval}' AND
                         s.delisted_date is null
                    WHERE
                        s.cfd_margin IN (\{cfdMargins}) AND
                        s.\{prefix}high < pg.close AND
                        pg.timeframe = '\{timeframe}'
                """;
    }
}


