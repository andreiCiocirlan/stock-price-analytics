package stock.price.analytics.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import stock.price.analytics.cache.CacheService;
import stock.price.analytics.cache.model.*;
import stock.price.analytics.controller.dto.CandleWithDateDTO;
import stock.price.analytics.model.prices.enums.StockTimeframe;
import stock.price.analytics.model.prices.ohlc.*;
import stock.price.analytics.repository.prices.ohlc.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static stock.price.analytics.model.prices.enums.StockTimeframe.DAILY;
import static stock.price.analytics.model.prices.enums.StockTimeframe.higherTimeframes;
import static stock.price.analytics.model.stocks.enums.MarketState.REGULAR;
import static stock.price.analytics.util.Constants.DAILY_FVG_MIN_DATE;
import static stock.price.analytics.util.LoggingUtil.logTimeAndReturn;
import static stock.price.analytics.util.PartitionAndSavePriceEntityUtil.partitionDataAndSave;
import static stock.price.analytics.util.PartitionAndSavePriceEntityUtil.partitionDataAndSaveWithLogTime;
import static stock.price.analytics.util.StockDateUtils.isWithinSameTimeframe;


@Slf4j
@Service
@RequiredArgsConstructor
public class PricesService {

    @PersistenceContext
    private final EntityManager entityManager;

    private final PricesRepository pricesRepository;
    private final DailyPricesRepository dailyPricesRepository;
    private final WeeklyPricesRepository weeklyPricesRepository;
    private final MonthlyPricesRepository monthlyPricesRepository;
    private final QuarterlyPricesRepository quarterlyPricesRepository;
    private final YearlyPricesRepository yearlyPricesRepository;
    private final CacheService cacheService;

    public List<AbstractPrice> htfPricesFor(StockTimeframe timeframe) {
        return switch (timeframe) {
            case DAILY -> new ArrayList<>(cacheService.getCachedDailyPrices(REGULAR));
            case WEEKLY, MONTHLY, QUARTERLY, YEARLY -> cacheService.htfPricesFor(timeframe);
        };
    }

    @SuppressWarnings("unchecked")
    public List<stock.price.analytics.model.prices.ohlc.AbstractPrice> previousThreePricesFor(List<String> tickers, StockTimeframe timeframe) {
        return (List<stock.price.analytics.model.prices.ohlc.AbstractPrice>) (switch (timeframe) {
            case DAILY -> throw new IllegalStateException("Unexpected value DAILY");
            case WEEKLY -> weeklyPricesRepository.findPreviousThreeWeeklyPricesForTickers(tickers);
            case MONTHLY -> monthlyPricesRepository.findPreviousThreeMonthlyPricesForTickers(tickers);
            case QUARTERLY -> quarterlyPricesRepository.findPreviousThreeQuarterlyPricesForTickers(tickers);
            case YEARLY -> yearlyPricesRepository.findPreviousThreeYearlyPricesForTickers(tickers);
        });
    }

    public List<CandleWithDateDTO> findFor(String ticker, StockTimeframe timeframe) {
        String tableNameOHLC = timeframe.dbTableOHLC();
        String orderByIdField = timeframe == DAILY ? "date" : "start_date";
        String queryStr = STR."SELECT \{orderByIdField}, open, high, low, close FROM \{tableNameOHLC} WHERE ticker = :ticker ORDER BY \{orderByIdField} ASC";

        Query nativeQuery = entityManager.createNativeQuery(queryStr, CandleWithDateDTO.class);
        nativeQuery.setParameter("ticker", ticker);

        @SuppressWarnings("unchecked")
        List<CandleWithDateDTO> candles = (List<CandleWithDateDTO>) nativeQuery.getResultList();

        return candles;
    }

    public List<? extends AbstractPrice> findAllPricesFor(List<String> tickers, StockTimeframe timeframe) {
        return switch (timeframe) {
            case DAILY -> dailyPricesRepository.findByDateBetween(DAILY_FVG_MIN_DATE, LocalDate.now());
            case WEEKLY -> weeklyPricesRepository.findByTickerIn(tickers);
            case MONTHLY -> monthlyPricesRepository.findByTickerIn(tickers);
            case QUARTERLY -> quarterlyPricesRepository.findByTickerIn(tickers);
            case YEARLY -> yearlyPricesRepository.findByTickerIn(tickers);
        };
    }

    public List<AbstractPrice> updatePricesForHigherTimeframes(List<DailyPrice> importedDailyPrices) {
        return logTimeAndReturn(() -> updateHTF(importedDailyPrices), "updated prices for higher timeframes");
    }

    private List<AbstractPrice> updateHTF(List<DailyPrice> importedDailyPrices) {
        List<String> tickers = new ArrayList<>(importedDailyPrices.stream().map(DailyPrice::getTicker).toList());

        // Update prices for each timeframe and return (used for stocks cache update)
        List<AbstractPrice> htfPricesUpdated = new ArrayList<>();
        for (StockTimeframe timeframe : higherTimeframes()) {
            List<PriceWithPrevClose> htfPricesWithPrevCloseUpdated = updateAndSavePrices(importedDailyPrices, timeframe,
                    cacheService.htfPricesWithPrevCloseFor(tickers, timeframe));
            htfPricesUpdated.addAll(htfPricesWithPrevCloseUpdated.stream().map(PriceWithPrevClose::getPrice).toList());
            cacheService.addHtfPricesWithPrevClose(htfPricesWithPrevCloseUpdated);
        }
        partitionDataAndSaveWithLogTime(htfPricesUpdated, pricesRepository, "saved HTF prices");

        return htfPricesUpdated;
    }


    private List<PriceWithPrevClose> updateAndSavePrices(List<DailyPrice> importedDailyPrices,
                                                         StockTimeframe timeframe,
                                                         List<PriceWithPrevClose> pricesWithPrevClose) {
        List<PriceWithPrevClose> result = new ArrayList<>();
        Map<String, PriceWithPrevClose> pricesWithPrevCloseByTicker = pricesWithPrevClose.stream()
                .collect(Collectors.toMap(priceWithPrevClose -> priceWithPrevClose.getPrice().getTicker(), p -> p));
        for (DailyPrice importedDailyPrice : importedDailyPrices) {
            String ticker = importedDailyPrice.getTicker();
            PriceWithPrevClose priceWithPrevClose = pricesWithPrevCloseByTicker.get(ticker);
            AbstractPrice price = priceWithPrevClose.getPrice();
            LocalDate latestEndDateWMQY = price.getEndDate(); // latest cached w,m,q,y end_date per ticker
            if (isWithinSameTimeframe(importedDailyPrice.getDate(), latestEndDateWMQY, timeframe)) {
                price.convertFrom(importedDailyPrice, priceWithPrevClose.previousClose());
                result.add(priceWithPrevClose);
            } else { // new week, month, quarter, year
                result.add(newPriceWithPrevCloseFrom(importedDailyPrice, timeframe, price.getClose()));
            }
        }

        return result;
    }

    private PriceWithPrevClose newPriceWithPrevCloseFrom(DailyPrice importedDailyPrice, StockTimeframe timeframe, double previousClose) {
        AbstractPrice price = createNewHTFPrice(importedDailyPrice, timeframe, previousClose);

        return switch (timeframe) {
            case DAILY -> throw new IllegalStateException("Unexpected timeframe DAILY");
            case WEEKLY -> new WeeklyPriceWithPrevClose((WeeklyPrice) price, previousClose);
            case MONTHLY -> new MonthlyPriceWithPrevClose((MonthlyPrice) price, previousClose);
            case QUARTERLY -> new QuarterlyPriceWithPrevClose((QuarterlyPrice) price, previousClose);
            case YEARLY -> new YearlyPriceWithPrevClose((YearlyPrice) price, previousClose);
        };
    }

    private AbstractPrice createNewHTFPrice(DailyPrice dailyPrice, StockTimeframe timeframe, double previousClose) {
        return switch (timeframe) {
            case DAILY -> throw new IllegalStateException("Unexpected value DAILY");
            case WEEKLY -> WeeklyPrice.newFrom(dailyPrice, previousClose);
            case MONTHLY -> MonthlyPrice.newFrom(dailyPrice, previousClose);
            case QUARTERLY -> QuarterlyPrice.newFrom(dailyPrice, previousClose);
            case YEARLY -> YearlyPrice.newFrom(dailyPrice, previousClose);
        };
    }

    @Transactional
    public void updateAllHigherTimeframesPricesForTickers(LocalDate date, String tickers) {
        updateHigherTimeframesPricesFor(date, higherTimeframes(), tickers);
    }

    @Transactional
    public void updateHigherTimeframesPricesFor(LocalDate date, List<StockTimeframe> timeframes, String tickers) {
        for (StockTimeframe timeframe : timeframes) {
            updateHigherTimeframeHistPrices(timeframe.toDateTruncPeriod(), timeframe.sequenceName(), timeframe.toSQLInterval(), timeframe.dbTableOHLC(), timeframe.htfDateFrom(date), tickers);
        }
    }

    private void updateHigherTimeframeHistPrices(String dateTruncPeriod, String sequenceName, String sqlInterval, String tableName, LocalDate date, String tickers) {
        String dateFormatted = date.format(DateTimeFormatter.ISO_LOCAL_DATE);
        int savedOrUpdatedCount = entityManager.createNativeQuery(
                queryFrom(tickers, dateTruncPeriod, sequenceName, sqlInterval, tableName, dateFormatted)
        ).executeUpdate();
        if (savedOrUpdatedCount != 0) {
            log.info("saved/updated {} {} rows for date {} and tickers {}", savedOrUpdatedCount, dateTruncPeriod, dateFormatted, tickers);
        }
    }

    private String queryFrom(String tickers, String dateTruncPeriod, String sequenceName, String sqlInterval, String tableName, String dateFormatted) {
        String query = STR."""
            WITH interval_data AS (
            SELECT
                ticker,
                MIN(date) AS start_date,
                MAX(date) AS end_date,
                MAX(high) AS high,
                MIN(low) AS low
            FROM daily_prices
            WHERE date BETWEEN '\{dateFormatted}'::date - INTERVAL '\{sqlInterval}' AND '\{dateFormatted}'
            """;

        if (!tickers.isEmpty()) {
            query = query.concat(
                    STR."""
                    AND ticker in (\{tickers})
                    """);
        }

        query = query.concat(STR."""
            GROUP BY ticker, DATE_TRUNC('\{dateTruncPeriod}', date)
            ),
            last_week AS (
                SELECT
                    dp_o.ticker,
                    start_date,
                    end_date,
                    interval_data.high,
                    interval_data.low,
                    dp_o.open,
                    dp_c.close,
                    CASE
                        WHEN (LAG(dp_c.close, 1) OVER (PARTITION BY dp_o.ticker ORDER BY start_date) IS NULL) THEN
                            CASE
                                WHEN (dp_o.open <> 0) THEN ROUND((100.0 * (dp_c.close - dp_o.open) / dp_o.open)::numeric, 2)
                                ELSE NULL
                            END
                        ELSE
                            ROUND(((dp_c.close - LAG(dp_c.close) OVER (PARTITION BY dp_o.ticker ORDER BY start_date)) / LAG(dp_c.close) OVER (PARTITION BY dp_o.ticker ORDER BY start_date) * 100)::numeric, 2)
                    END AS performance
                FROM interval_data
                JOIN daily_prices dp_o ON dp_o.ticker = interval_data.ticker AND dp_o.date = interval_data.start_date
                JOIN daily_prices dp_c ON dp_c.ticker = interval_data.ticker AND dp_c.date = interval_data.end_date
            ),
            final_result AS (
                SELECT *
                FROM last_week
                    WHERE start_date >= DATE_TRUNC('\{dateTruncPeriod}', '\{dateFormatted}'::date)
            )
            INSERT INTO \{tableName} (id, ticker, start_date, end_date, high, low, open, close, performance)
            SELECT nextval('\{sequenceName}') AS id, ticker, DATE_TRUNC('\{dateTruncPeriod}', start_date), end_date, high, low, open, close, performance
            FROM final_result
            ON CONFLICT (ticker, start_date)
                DO UPDATE SET
                    open = EXCLUDED.open,
                    high = EXCLUDED.high,
                    low = EXCLUDED.low,
                    close = EXCLUDED.close,
                    performance = EXCLUDED.performance,
                    end_date = EXCLUDED.end_date
            """);

        return query;
    }

    @Transactional
    public void savePrices(List<? extends AbstractPrice> prices) {
        partitionDataAndSave(prices, pricesRepository);
    }
}
