package stock.price.analytics.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import stock.price.analytics.model.dto.StockPerformanceDTO;
import stock.price.analytics.model.prices.enums.StockTimeframe;
import stock.price.analytics.model.prices.ohlc.AbstractPriceOHLC;
import stock.price.analytics.repository.PricesOHLCRepository;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StockPerformanceService {

    private final PricesOHLCRepository pricesOHLCRepository;

    public List<StockPerformanceDTO> stockPerformanceForDateAndTimeframeAndFilters(StockTimeframe timeFrame, LocalDate date, boolean xtbOnly) {
        List<? extends AbstractPriceOHLC> list = switch (timeFrame) {
            case WEEKLY:
                yield xtbOnly
                        ? pricesOHLCRepository.findWeeklyOHLC_XTBOnlyBetween(date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)), date.with(TemporalAdjusters.previousOrSame(DayOfWeek.FRIDAY)))
                        : pricesOHLCRepository.findWeeklyOHLCBetween(date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)), date.with(TemporalAdjusters.previousOrSame(DayOfWeek.FRIDAY)));
            case MONTHLY:
                yield xtbOnly
                        ? pricesOHLCRepository.findMonthlyOHLC_XTBOnlyBetween(date.with(TemporalAdjusters.firstDayOfMonth()), date.with(TemporalAdjusters.lastDayOfMonth()))
                        : pricesOHLCRepository.findMonthlyOHLCBetween(date.with(TemporalAdjusters.firstDayOfMonth()), date.with(TemporalAdjusters.lastDayOfMonth()));
            case YEARLY:
                yield xtbOnly
                        ? pricesOHLCRepository.findYearlyOHLC_XTBOnlyBetween(date.with(TemporalAdjusters.firstDayOfYear()), date.with(TemporalAdjusters.lastDayOfYear()))
                        : pricesOHLCRepository.findYearlyOHLCBetween(date.with(TemporalAdjusters.firstDayOfYear()), date.with(TemporalAdjusters.lastDayOfYear()));
        };
        return list.stream()
                .map(item -> new StockPerformanceDTO(item.getTicker(), item.getPerformance()))
                .toList();
    }

}
