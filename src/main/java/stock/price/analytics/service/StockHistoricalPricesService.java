package stock.price.analytics.service;


import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import stock.price.analytics.controller.dto.StockWithPrevCloseDTO;
import stock.price.analytics.model.prices.enums.StockTimeframe;
import stock.price.analytics.model.prices.ohlc.*;
import stock.price.analytics.repository.prices.PriceOHLCRepository;
import stock.price.analytics.util.Constants;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.nio.file.Files.walk;
import static stock.price.analytics.util.PartitionAndSavePriceEntityUtil.partitionDataAndSave;
import static stock.price.analytics.util.PricesOHLCUtil.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockHistoricalPricesService {

    @PersistenceContext
    private final EntityManager entityManager;

    private final PriceOHLCService priceOHLCService;
    private final PriceOHLCRepository priceOhlcRepository;

    private static List<? extends AbstractPriceOHLC> pricesOHLCForTimeframe(StockTimeframe stockTimeframe) {
        List<AbstractPriceOHLC> priceOHLCS = new ArrayList<>();
        try (Stream<Path> walk = walk(Paths.get(Constants.STOCKS_LOCATION))) {
            walk.filter(Files::isRegularFile)
                    .parallel().forEachOrdered(srcFile -> { // must be forEachOrdered
                        priceOHLCS.addAll(pricesOHLCForFileAndTimeframe(srcFile, stockTimeframe));
                    });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return switch (stockTimeframe) {
            case DAILY -> priceOHLCS.stream().map(DailyPriceOHLC.class::cast).toList();
            case WEEKLY -> priceOHLCS.stream().map(WeeklyPriceOHLC.class::cast).toList();
            case MONTHLY -> priceOHLCS.stream().map(MonthlyPriceOHLC.class::cast).toList();
            case YEARLY -> priceOHLCS.stream().map(YearlyPriceOHLC.class::cast).toList();
        };
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
                        tickerAndPrevDaysPricesImported.put(stockTicker, dailyPrices.stream().filter(dp -> dp.getDate().isAfter(tradingDate)).toList());
                    });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        tickerAndPrevDaysPricesImported.forEach((_, dailyPricesPrevDays) -> prevDaysHistPrices.addAll(dailyPriceWithPerformance(dailyPricesPrevDays)));

        partitionDataAndSave(prevDaysHistPrices, priceOhlcRepository);

        // insert/update higher timeframe prices
        priceOHLCService.updateHigherTimeframesPricesFor(tradingDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
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
                        ohlcList.addAll(dailyPricesOHLCFromFile(srcFile));
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

        partitionDataAndSave(dailyOHLCList, priceOhlcRepository);
    }

    @Transactional
    public void savePricesForTimeframe(StockTimeframe stockTimeframe) {
        List<? extends AbstractPriceOHLC> pricesOHLCs = pricesOHLCForTimeframe(stockTimeframe);
        partitionDataAndSave(pricesOHLCs, priceOhlcRepository);
    }

}