package stock.price.analytics.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import stock.price.analytics.cache.HighLowPricesCache;
import stock.price.analytics.model.prices.enums.HighLowPeriod;
import stock.price.analytics.model.prices.highlow.*;
import stock.price.analytics.repository.prices.HighLowForPeriodRepository;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.emptySet;
import static stock.price.analytics.util.PartitionAndSavePriceEntityUtil.partitionDataAndSave;
import static stock.price.analytics.util.TradingDateUtil.isFirstImportMonday;

@Slf4j
@Service
@RequiredArgsConstructor
public class HighLowPricesCacheService {

    private final StockService stockService;
    private final HighLowPricesCache highLowPricesCache;
    private final HighLowForPeriodRepository highLowForPeriodRepository;

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
        LocalDate latestDailyPriceImportDate = stockService.findLastUpdate();
        for (HighLowPeriod highLowPeriod : HighLowPeriod.values()) {
            initHighLowPricesCache(highLowPeriod, latestDailyPriceImportDate);
            initPrevWeekHighLowPricesCache(highLowPeriod, latestDailyPriceImportDate);
        }
        boolean firstImportMonday = isFirstImportMonday(latestDailyPriceImportDate);
        if (firstImportMonday) {
            stockService.updateHighLowForPeriodFromHLCachesAndAdjustWeekend();
        }
    }

    private void initPrevWeekHighLowPricesCache(HighLowPeriod highLowPeriod, LocalDate latestDailyPriceImportDate) {
        boolean firstImportMonday = isFirstImportMonday(latestDailyPriceImportDate);
        LocalDate prevWeekStartDate = latestDailyPriceImportDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        if (!firstImportMonday) { // on Monday first import need to find min/max prices for the past 3 weeks and 51 weeks respectively (new objects)
            prevWeekStartDate = prevWeekStartDate.minusWeeks(1);
        }
        List<? extends HighLowForPeriod> prevWeekHighLowPrices = switch (highLowPeriod) {
            case HIGH_LOW_4W -> highLowForPeriodRepository.highLow4wPricesFor(prevWeekStartDate);
            case HIGH_LOW_52W -> highLowForPeriodRepository.highLow52wPricesFor(prevWeekStartDate);
            case HIGH_LOW_ALL_TIME -> highLowForPeriodRepository.highestLowestPrices(prevWeekStartDate);
        };
        highLowPricesCache.addPrevWeekHighLowPrices(prevWeekHighLowPrices, highLowPeriod);
    }

    public void initHighLowPricesCache(HighLowPeriod highLowPeriod, LocalDate latestDailyPriceImportDate) {
        LocalDate startDate = latestDailyPriceImportDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate endDate = latestDailyPriceImportDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.FRIDAY));
        boolean firstImportMonday = isFirstImportMonday(latestDailyPriceImportDate);
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

    public List<String> getNewHighLowsForHLPeriod(HighLowPeriod highLowPeriod) {
        return new ArrayList<>(highLowPricesCache.getDailyNewHighLowsByHLPeriod().getOrDefault(highLowPeriod, emptySet()));
    }

    public void logNewHighLowsForHLPeriods() {
        for (HighLowPeriod highLowPeriod : HighLowPeriod.values()) {
            List<String> newHighLowsForHLPeriod = getNewHighLowsForHLPeriod(highLowPeriod);
            log.info("{} New {} : {}", newHighLowsForHLPeriod.size(), highLowPeriod, newHighLowsForHLPeriod);
        }
    }
}