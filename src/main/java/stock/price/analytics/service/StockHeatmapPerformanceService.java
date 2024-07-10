package stock.price.analytics.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;
import stock.price.analytics.controller.dto.StockPerformanceDTO;
import stock.price.analytics.model.prices.enums.StockTimeframe;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

import static stock.price.analytics.model.prices.enums.StockTimeframe.WEEKLY;
import static stock.price.analytics.model.prices.enums.StockTimeframe.dbTableForPerfHeatmapFrom;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockHeatmapPerformanceService {

    @PersistenceContext
    private final EntityManager entityManager;

    public List<StockPerformanceDTO> stockPerformanceForDateAndTimeframeAndFilters(StockTimeframe timeFrame, Boolean xtb, Boolean positivePerfFirst, Integer limit, Double cfdMargin) {
        String queryStr = queryFrom(timeFrame, positivePerfFirst, limit, xtb);

        Query nativeQuery = entityManager.createNativeQuery(queryStr, StockPerformanceDTO.class);
        LocalDate date = LocalDate.now();
        if (timeFrame == WEEKLY && date.getDayOfWeek().equals(DayOfWeek.MONDAY)) { // Monday import not done (use past week)
            date = date.minusDays(5);
        }
        Pair<LocalDate, LocalDate> intervalPair = intervalFor(date, timeFrame);
        nativeQuery.setParameter("startDate", intervalPair.getLeft());
        nativeQuery.setParameter("endDate", intervalPair.getRight());
        nativeQuery.setParameter("cfdMargin", cfdMargin);

        @SuppressWarnings("unchecked")
        List<StockPerformanceDTO> priceOHLCs = (List<StockPerformanceDTO>) nativeQuery.getResultList();

        return priceOHLCs;
    }

    private static String queryFrom(StockTimeframe timeFrame, Boolean positivePerfFirst, Integer limit, Boolean xtb) {
        String dbTable = dbTableForPerfHeatmapFrom(timeFrame);
        String query = STR."""
            SELECT p.ticker, p.performance FROM \{dbTable} p JOIN Stocks s ON s.ticker = p.ticker
            """;

        if (Boolean.TRUE.equals(xtb)) {
            query += " AND s.xtb_stock = true ";
        }
        query += STR."""
                WHERE p.start_date >= :startDate AND p.start_date <= :endDate
                AND (COALESCE(:cfdMargin, 0) = 0 OR s.cfd_margin = :cfdMargin)
                """;

        // default negative performance first (ascending by performance)
        query += STR."""
                ORDER BY p.performance
                """;
        if (Boolean.TRUE.equals(positivePerfFirst)) {
            query += " DESC";
        }
        if (limit != null) {
            query += STR." LIMIT \{limit}";
        }
        return query;
    }

    private Pair<LocalDate, LocalDate> intervalFor(LocalDate date, StockTimeframe timeFrame) {
        return switch (timeFrame) {
            case WEEKLY -> Pair.of(date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)), date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).with(TemporalAdjusters.nextOrSame(DayOfWeek.FRIDAY)));
            case MONTHLY -> Pair.of(date.with(TemporalAdjusters.firstDayOfMonth()), date.with(TemporalAdjusters.lastDayOfMonth()));
            case YEARLY -> Pair.of(date.with(TemporalAdjusters.firstDayOfYear()), date.with(TemporalAdjusters.lastDayOfYear()));
            case DAILY -> throw new IllegalStateException("Unexpected value: " + timeFrame);
        };
    }


}
