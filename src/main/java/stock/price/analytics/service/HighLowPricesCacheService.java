package stock.price.analytics.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import stock.price.analytics.cache.HighLowPricesCache;
import stock.price.analytics.model.prices.enums.HighLowPeriod;
import stock.price.analytics.model.prices.highlow.*;
import stock.price.analytics.repository.prices.HighLowForPeriodRepository;
import stock.price.analytics.repository.stocks.StockRepository;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HighLowPricesCacheService {

    private final HighLowPricesCache highLowPricesCache;
    private final StockRepository stockRepository;
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
        LocalDate lastImportedDate = stockRepository.findLastUpdate();
        initHighLowPricesCache(HighLowPeriod.HIGH_LOW_4W, lastImportedDate);
        initHighLowPricesCache(HighLowPeriod.HIGH_LOW_52W, lastImportedDate);
        initHighLowPricesCache(HighLowPeriod.HIGH_LOW_ALL_TIME, lastImportedDate);
    }

    public void initHighLowPricesCache(HighLowPeriod highLowPeriod, LocalDate lastImportedDate) {
        // startDate in DB is always on a Monday (even if it's a holiday on Monday)
        LocalDate startDate = lastImportedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate endDate = lastImportedDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.FRIDAY));
        if (!lastImportedDate.getDayOfWeek().equals(DayOfWeek.FRIDAY)) { // get the data from the high-low tables directly
            List<? extends HighLowForPeriod> highLowPrices = switch (highLowPeriod) {
                case HIGH_LOW_4W -> highLowForPeriodRepository.highLow4wPricesFor(startDate);
                case HIGH_LOW_52W -> highLowForPeriodRepository.highLow52wPricesFor(startDate);
                case HIGH_LOW_ALL_TIME -> highLowForPeriodRepository.highestLowestPrices(startDate);
            };
            highLowPricesCache.addHighLowPrices(highLowPrices, highLowPeriod);
        } else { // for Friday latest date imported need to find min/max prices for the past 3 weeks and 51 weeks respectively (new objects)
            LocalDate newWeekStartDate = startDate.plusWeeks(1);
            LocalDate newWeekEndDate = endDate.plusWeeks(1);
            if (highLowPeriod == HighLowPeriod.HIGH_LOW_ALL_TIME) { // for all-time highs/lows simply copy the existing row on Mondays
                List<HighestLowestPrices> highestLowestPrices = new ArrayList<>();
                highLowForPeriodRepository.highestLowestPrices(startDate).forEach(hlp -> highestLowestPrices.add(hlp.copyWith(newWeekStartDate)));
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
                highLowPricesCache.addHighLowPrices(highLowForPeriods, highLowPeriod);
            }
        }
    }

}