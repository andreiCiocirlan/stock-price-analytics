package stock.price.analytics.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import stock.price.analytics.cache.HighLowPricesCache;
import stock.price.analytics.cache.StocksCache;
import stock.price.analytics.model.prices.enums.HighLowPeriod;
import stock.price.analytics.model.prices.highlow.*;
import stock.price.analytics.model.stocks.Stock;
import stock.price.analytics.repository.prices.DailyPricesJSONRepository;
import stock.price.analytics.repository.prices.HighLowForPeriodRepository;
import stock.price.analytics.repository.stocks.StockRepository;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

import static stock.price.analytics.util.PartitionAndSavePriceEntityUtil.partitionDataAndSave;
import static stock.price.analytics.util.TradingDateUtil.tradingDateNow;

@Slf4j
@Service
@RequiredArgsConstructor
public class HighLowPricesCacheService {

    private final StocksCache stocksCache;
    private final StockRepository stockRepository;
    private final HighLowPricesCache highLowPricesCache;
    private final HighLowForPeriodRepository highLowForPeriodRepository;
    private final DailyPricesJSONRepository dailyPricesJSONRepository;

    private static HighLowForPeriod convertToHighLowForPeriod(TickerHighLowView dto, LocalDate newWeekStartDate, LocalDate newWeekEndDate, HighLowPeriod highLowPeriod) {
        HighLowForPeriod highLowForPeriod = switch (highLowPeriod) {
            case HIGH_LOW_4W -> new HighLow4w(dto.getTicker(), newWeekStartDate, newWeekEndDate);
            case HIGH_LOW_52W -> new HighLow52Week(dto.getTicker(), newWeekStartDate, newWeekEndDate);
            case HIGH_LOW_ALL_TIME -> throw new IllegalArgumentException("HIGH_LOW_ALL_TIME is not supported.");
        };
        highLowForPeriod.setLow(dto.getLow());
        highLowForPeriod.setHigh(dto.getHigh());
        return highLowForPeriod;
    }

    public void initHighLowPricesCache() {
        LocalDate latestDailyPriceImportDate = dailyPricesJSONRepository.findLastImportedDate();
        initHighLowPricesCache(HighLowPeriod.HIGH_LOW_4W, latestDailyPriceImportDate);
        initHighLowPricesCache(HighLowPeriod.HIGH_LOW_52W, latestDailyPriceImportDate);
        initHighLowPricesCache(HighLowPeriod.HIGH_LOW_ALL_TIME, latestDailyPriceImportDate);
    }

    public void initHighLowPricesCache(HighLowPeriod highLowPeriod, LocalDate latestDailyPriceImportDate) {
        LocalDate startDate = latestDailyPriceImportDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate endDate = latestDailyPriceImportDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.FRIDAY));
        boolean firstImportMonday = tradingDateNow().getDayOfWeek().equals(DayOfWeek.MONDAY) && latestDailyPriceImportDate.getDayOfWeek().equals(DayOfWeek.FRIDAY);
        if (firstImportMonday) { // on Monday first import need to find min/max prices for the past 3 weeks and 51 weeks respectively (new objects)
            LocalDate newWeekStartDate = startDate.plusWeeks(1);
            LocalDate newWeekEndDate = endDate.plusWeeks(1);
            if (highLowPeriod == HighLowPeriod.HIGH_LOW_ALL_TIME) { // for all-time highs/lows simply copy the existing row on Mondays
                List<HighestLowestPrices> highestLowestPrices = new ArrayList<>();
                highLowForPeriodRepository.highestLowestPrices(startDate).forEach(hlp -> highestLowestPrices.add(hlp.copyWith(newWeekStartDate)));
                partitionDataAndSave(highestLowestPrices, highLowForPeriodRepository);
                highLowPricesCache.addHighLowPrices(highestLowestPrices, highLowPeriod);
            } else { // for 4w, 52w need sql select for the period (for all-time it would simply be a copy)
                int weekCount = switch (highLowPeriod) {
                    case HIGH_LOW_4W -> 3; // last imported date was Friday -> new week -> look back 3 instead of 4 weeks
                    case HIGH_LOW_52W -> 51;
                    case HIGH_LOW_ALL_TIME -> throw new IllegalArgumentException("HIGH_LOW_ALL_TIME is not supported.");
                };
                List<HighLowForPeriod> highLowForPeriods = highLowForPeriodRepository.highLowPricesInPastWeeks(startDate, weekCount)
                        .stream()
                        .map(dto -> convertToHighLowForPeriod(dto, newWeekStartDate, newWeekEndDate, highLowPeriod))
                        .toList();
                partitionDataAndSave(highLowForPeriods, highLowForPeriodRepository);
                highLowPricesCache.addHighLowPrices(highLowForPeriods, highLowPeriod);
            }
        } else {
            List<? extends HighLowForPeriod> highLowPrices = switch (highLowPeriod) {
                case HIGH_LOW_4W -> highLowForPeriodRepository.highLow4wPricesFor(startDate);
                case HIGH_LOW_52W -> highLowForPeriodRepository.highLow52wPricesFor(startDate);
                case HIGH_LOW_ALL_TIME -> highLowForPeriodRepository.highestLowestPrices(startDate);
            };
            highLowPricesCache.addHighLowPrices(highLowPrices, highLowPeriod);
        }
    }

    public void updateStocksHighLowFromHighLowCache() {
        Map<String, Stock> stocksMap = stocksCache.stocksMap();
        List<HighLowForPeriod> newHighLowPrices = highLowPricesCache.getNewHighLowPrices();
        Set<Stock> updatedStocks = newHighLowPrices.stream()
                .map(HighLowForPeriod::getTicker)
                .filter(stocksMap::containsKey)
                .map(stocksMap::get)
                .collect(Collectors.toSet());
        if (!updatedStocks.isEmpty()) {
            for (HighLowForPeriod newHighLowPrice : newHighLowPrices) {
                String ticker = newHighLowPrice.getTicker();
                Stock stock = stocksMap.get(ticker);
                stock.updateFrom(newHighLowPrice);
            }
            List<Stock> stocks = updatedStocks.stream().toList();
            partitionDataAndSave(stocks, stockRepository);
            stocksCache.addStocks(stocks);
            highLowPricesCache.clearNewHighLowPrices(); // clear new high low prices for next import
        }
    }

    public List<String> getNewHighLowsForHLPeriod(HighLowPeriod highLowPeriod) {
        return new ArrayList<>(highLowPricesCache.getDailyNewHighLowsByHLPeriod().getOrDefault(highLowPeriod, Collections.emptySet()));
    }
}