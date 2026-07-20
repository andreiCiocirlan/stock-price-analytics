package stock.price.analytics.util.query.stocks.impl;

import org.springframework.stereotype.Component;
import stock.price.analytics.model.prices.enums.StockTimeframe;
import stock.price.analytics.util.query.stocks.StocksQueryProvider;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;

import static java.time.LocalDate.of;

@Component
public class StocksQueryProviderImpl implements StocksQueryProvider {

    @Override
    public String findMostExtendedAbove200SMA(StockTimeframe timeframe, LocalDate tradingDate, Double cfdMargin) {
        String dbTable = timeframe.dbTableOHLC();
        String date = (switch (timeframe) {
            case DAILY -> tradingDate;
            case WEEKLY -> tradingDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            case MONTHLY -> tradingDate.with(TemporalAdjusters.firstDayOfMonth());
            case QUARTERLY -> of(tradingDate.getYear(), tradingDate.getMonth().firstMonthOfQuarter().getValue(), 1);
            case YEARLY -> tradingDate.with(TemporalAdjusters.firstDayOfYear());
        }).format(DateTimeFormatter.ISO_LOCAL_DATE);

        return STR."""
                WITH target_date AS (
                    SELECT '\{date}'::date AS dt
                )
                SELECT
                    p.ticker,
                    round((p.close / x.sma_200)::numeric, 2) AS smaRatio
                FROM target_date d
                JOIN \{dbTable} p
                  ON p.date = d.dt
                CROSS JOIN LATERAL (
                    SELECT AVG(close) AS sma_200
                    FROM (
                        SELECT close
                        FROM \{dbTable} p2
                        WHERE p2.ticker = p.ticker
                          AND p2.date <= d.dt
                        ORDER BY p2.date DESC
                        LIMIT 200
                    ) t
                ) x
                where p.ticker in (select ticker from stocks where cfd_margin = \{cfdMargin})
                ORDER BY smaRatio DESC
                LIMIT 50;
                """;
    }
}
