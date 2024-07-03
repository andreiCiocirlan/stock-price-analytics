package stock.price.analytics.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import stock.price.analytics.model.dto.StockPerformanceDTO;
import stock.price.analytics.model.prices.enums.StockTimeframe;
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
        if (Boolean.TRUE.equals(xtbOnly)) {
            return switch (timeFrame) {
                case WEEKLY ->
                        pricesOHLCRepository.findWeeklyOHLC_XTBOnlyBetween(date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)), date.with(TemporalAdjusters.previousOrSame(DayOfWeek.FRIDAY)))
                                .stream()
                                .map(item -> new StockPerformanceDTO(item.getTicker(), item.getPerformance()))
                                .toList();
                case MONTHLY ->
                        pricesOHLCRepository.findMonthlyOHLC_XTBOnlyBetween(date.with(TemporalAdjusters.firstDayOfMonth()), date.with(TemporalAdjusters.lastDayOfMonth()))
                                .stream()
                                .map(item -> new StockPerformanceDTO(item.getTicker(), item.getPerformance()))
                                .toList();
                case YEARLY ->
                        pricesOHLCRepository.findYearlyOHLC_XTBOnlyBetween(date.with(TemporalAdjusters.firstDayOfYear()), date.with(TemporalAdjusters.lastDayOfYear())).stream()
                                .map(item -> new StockPerformanceDTO(item.getTicker(), item.getPerformance()))
                                .toList();
            };
        }

        return switch (timeFrame) {
            case WEEKLY ->
                    pricesOHLCRepository.findWeeklyOHLCBetween(date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)), date.with(TemporalAdjusters.previousOrSame(DayOfWeek.FRIDAY)))
                            .stream()
                            .map(item -> new StockPerformanceDTO(item.getTicker(), item.getPerformance()))
                            .toList();
            case MONTHLY ->
                    pricesOHLCRepository.findMonthlyOHLCBetween(date.with(TemporalAdjusters.firstDayOfMonth()), date.with(TemporalAdjusters.lastDayOfMonth()))
                            .stream()
                            .map(item -> new StockPerformanceDTO(item.getTicker(), item.getPerformance()))
                            .toList();
            case YEARLY ->
                    pricesOHLCRepository.findYearlyOHLCBetween(date.with(TemporalAdjusters.firstDayOfYear()), date.with(TemporalAdjusters.lastDayOfYear())).stream()
                            .map(item -> new StockPerformanceDTO(item.getTicker(), item.getPerformance()))
                            .toList();
        };
    }

}
