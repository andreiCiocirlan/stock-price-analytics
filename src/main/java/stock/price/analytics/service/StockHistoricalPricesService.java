package stock.price.analytics.service;


import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import stock.price.analytics.controller.dto.StockWithPrevCloseDTO;
import stock.price.analytics.model.prices.enums.StockTimeframe;
import stock.price.analytics.model.prices.ohlc.*;
import stock.price.analytics.repository.prices.DailyPricesRepository;
import stock.price.analytics.repository.prices.PricesRepository;
import stock.price.analytics.util.Constants;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.nio.file.Files.walk;
import static stock.price.analytics.util.PartitionAndSavePriceEntityUtil.partitionDataAndSave;
import static stock.price.analytics.util.PricesUtil.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockHistoricalPricesService {

    @PersistenceContext
    private final EntityManager entityManager;

    private final PricesService pricesService;
    private final PricesRepository pricesRepository;
    private final DailyPricesRepository dailyPricesRepository;
    private final RefreshMaterializedViewsService refreshMaterializedViewsService;

    private static List<? extends AbstractPriceOHLC> pricesOHLCForTimeframe(StockTimeframe stockTimeframe) {
        List<AbstractPriceOHLC> prices = new ArrayList<>();
        try (Stream<Path> walk = walk(Paths.get(Constants.STOCKS_LOCATION))) {
            walk.filter(Files::isRegularFile)
                    .parallel().forEachOrdered(srcFile -> { // must be forEachOrdered
                        prices.addAll(pricesOHLCForFileAndTimeframe(srcFile, stockTimeframe));
                    });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return switch (stockTimeframe) {
            case DAILY -> prices.stream().map(DailyPriceOHLC.class::cast).toList();
            case WEEKLY -> prices.stream().map(WeeklyPriceOHLC.class::cast).toList();
            case MONTHLY -> prices.stream().map(MonthlyPriceOHLC.class::cast).toList();
            case QUARTERLY -> prices.stream().map(QuarterlyPriceOHLC.class::cast).toList();
            case YEARLY -> prices.stream().map(YearlyPriceOHLC.class::cast).toList();
        };
    }

    @Transactional
    public void savePricesForTradingDate(List<String> tickers, LocalDate tradingDate, LocalDate higherTimeFrameDate) {
        List<DailyPriceOHLC> prevDaysHistPrices = new ArrayList<>();
        Map<String, List<DailyPriceOHLC>> tickerAndPrevDaysPricesImported = new HashMap<>();
        try (Stream<Path> walk = walk(Paths.get(Constants.STOCKS_LOCATION))) {
            walk.filter(Files::isRegularFile)
                    .filter(srcFile -> tickers.contains(tickerFrom(srcFile)))
                    .parallel().forEachOrdered(srcFile -> { // must be forEachOrdered
                        List<DailyPriceOHLC> dailyPrices = dailyPricesFromFileWithDate(srcFile, tradingDate);
                        tickerAndPrevDaysPricesImported.put(tickerFrom(srcFile), dailyPrices);
                    });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        tickerAndPrevDaysPricesImported.forEach((_, dailyPricesPrevDays) -> prevDaysHistPrices.addAll(dailyPriceWithPerformance(dailyPricesPrevDays)));
        List<DailyPriceOHLC> dailyPricesDB = dailyPricesRepository.findByDate(tradingDate);
        Map<String, DailyPriceOHLC> importedPricesMap = prevDaysHistPrices.stream().filter(dp -> dp.getDate().isEqual(tradingDate)).collect(Collectors.toMap(DailyPriceOHLC::getTicker, p -> p));
        dailyPricesDB.forEach(dp -> BeanUtils.copyProperties(importedPricesMap.get(dp.getTicker()), dp, "id", "date"));
        partitionDataAndSave(dailyPricesDB, pricesRepository);

        // insert/update higher timeframe prices
        String tickersQuery = tickers.stream().map(ticker -> STR."'\{ticker}'").collect(Collectors.joining(", "));
        pricesService.updateAllHigherTimeframesPricesForTickers(higherTimeFrameDate, tickersQuery);

        // refresh views
        refreshMaterializedViewsService.refreshMaterializedViews();
    }

    @Transactional
    public void savePricesAfterTradingDate(String tickers, int prevDaysCount, LocalDate tradingDate) {
        List<DailyPriceOHLC> prevDaysHistPrices = new ArrayList<>();
        Map<String, List<DailyPriceOHLC>> tickerAndPrevDaysPricesImported = new HashMap<>();
        try (Stream<Path> walk = walk(Paths.get(Constants.STOCKS_LOCATION))) {
            walk.filter(Files::isRegularFile)
                    .parallel().forEachOrdered(srcFile -> { // must be forEachOrdered
                        String stockTicker = tickerFrom(srcFile);
                        if (tickers != null && Arrays.stream(tickers.split(",")).noneMatch(ticker -> ticker.equals(stockTicker)))
                            return;
                        List<DailyPriceOHLC> dailyPrices = dailyPricesFromFileWithCount(srcFile, prevDaysCount + 1); // +1 to get previous day close
                        tickerAndPrevDaysPricesImported.put(stockTicker, dailyPrices);
                    });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        tickerAndPrevDaysPricesImported.forEach((_, dailyPricesPrevDays) -> prevDaysHistPrices.addAll(dailyPriceWithPerformance(dailyPricesPrevDays)));

        partitionDataAndSave(prevDaysHistPrices.stream().filter(dp -> dp.getDate().isAfter(tradingDate)).toList(), pricesRepository);

        // insert/update higher timeframe prices
        String tickersQueryParam = tickers != null ? STR."'\{tickers.replace(",", "','")}'" : null;
        pricesService.updateAllHigherTimeframesPricesForTickers(tradingDate, tickersQueryParam);
    }

    private List<DailyPriceOHLC> dailyPriceWithPerformance(List<DailyPriceOHLC> dailyPrices) {
        for (int i = dailyPrices.size() - 1; i >= 1; i--) {
            double previousClose = dailyPrices.get(i - 1).getClose();
            double performance = ((dailyPrices.get(i).getClose() - previousClose) / previousClose) * 100;
            dailyPrices.get(i).setPerformance(Math.round(performance * 100.0) / 100.0);
        }
        return dailyPrices;
    }

    private Map<String, StockWithPrevCloseDTO> tickerAndPrevCloseFor(int prevDaysCount) {
        String queryStr = STR."""
            SELECT ticker, date, close as prev_close
            FROM (SELECT ticker, date, close, ROW_NUMBER() OVER (PARTITION BY ticker ORDER BY date DESC) AS rn FROM daily_prices) AS subquery
            WHERE rn = \{prevDaysCount}
        """;

        Query nativeQuery = entityManager.createNativeQuery(queryStr, StockWithPrevCloseDTO.class);

        @SuppressWarnings("unchecked")
        List<StockWithPrevCloseDTO> prevDayTickerAndClosingPrices = (List<StockWithPrevCloseDTO>) nativeQuery.getResultList();

        return prevDayTickerAndClosingPrices.stream().collect(Collectors.toMap(
                StockWithPrevCloseDTO::ticker,
                stock -> new StockWithPrevCloseDTO(stock.ticker(), stock.date(), stock.prevClose())
        ));
    }

    @Transactional
    public void saveAllDailyPricesFromFiles() {
        List<AbstractPriceOHLC> ohlcList = new ArrayList<>();
        try (Stream<Path> walk = walk(Paths.get(Constants.STOCKS_LOCATION))) {
            walk.filter(Files::isRegularFile)
                    .parallel().forEachOrdered(srcFile -> { // must be forEachOrdered
                        ohlcList.addAll(dailyPricesFromFile(srcFile));
                    });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        List<DailyPriceOHLC> dailyOHLCList = ohlcList.stream()
                .map(DailyPriceOHLC.class::cast)
                .toList();
        log.info("OPEN_IS_ZERO_ERROR {} problems", OPEN_IS_ZERO_ERROR.get());
        log.info("HIGH_LOW_ERROR {} problems", HIGH_LOW_ERROR.get());
        log.info("ohlcList size {}", dailyOHLCList.size());

        partitionDataAndSave(dailyOHLCList, pricesRepository);
    }

    @Transactional
    public void savePricesForTimeframe(StockTimeframe stockTimeframe) {
        List<? extends AbstractPriceOHLC> pricesOHLCs = pricesOHLCForTimeframe(stockTimeframe);
        partitionDataAndSave(pricesOHLCs, pricesRepository);
    }

}