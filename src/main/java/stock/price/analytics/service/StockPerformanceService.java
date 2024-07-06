package stock.price.analytics.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;
import stock.price.analytics.controller.dto.StockPerformanceDTO;
import stock.price.analytics.model.prices.enums.StockTimeframe;
import stock.price.analytics.model.prices.ohlc.AbstractPriceOHLC;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

import static stock.price.analytics.model.prices.enums.StockTimeframe.dbTableFromTimeframe;
import static stock.price.analytics.model.prices.enums.StockTimeframe.tableClassFrom;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockPerformanceService {

    private final EntityManager entityManager;

    public List<StockPerformanceDTO> stockPerformanceForDateAndTimeframeAndFilters(StockTimeframe timeFrame, LocalDate date, Boolean xtb, Double cfdMargin) {
        String queryStr = queryFrom(timeFrame, xtb);

        Query nativeQuery = entityManager.createNativeQuery(queryStr, tableClassFrom(timeFrame));
        Pair<LocalDate, LocalDate> intervalPair = intervalFor(date, timeFrame);
        nativeQuery.setParameter("startDate", intervalPair.getLeft());
        nativeQuery.setParameter("endDate", intervalPair.getRight());
        nativeQuery.setParameter("cfdMargin", cfdMargin);

        Object[] args = new Object[] { intervalPair.getLeft(), intervalPair.getRight(), cfdMargin };
        log.info("Executing SQL: {}", String.format(queryStr, args));

        @SuppressWarnings("unchecked")
        List<? extends AbstractPriceOHLC> priceOHLCs = (List<? extends AbstractPriceOHLC>) nativeQuery.getResultList();

        return priceOHLCs.stream()
                .map(item -> new StockPerformanceDTO(item.getTicker(), item.getPerformance()))
                .toList();
    }

    private static String queryFrom(StockTimeframe timeFrame, boolean xtb) {
        String dbTable = dbTableFromTimeframe(timeFrame);
        String query = STR."""
            SELECT p.* FROM \{dbTable} p JOIN Stocks s ON s.ticker = p.ticker
            """;

        if (Boolean.TRUE.equals(xtb)) {
            query += " AND s.xtb_stock = true ";
        } else if (Boolean.FALSE.equals(xtb)) {
            query += " AND s.xtb_stock = false ";
        }
        query += """
                WHERE p.start_date >= :startDate AND p.start_date <= :endDate
                AND (COALESCE(:cfdMargin, 0) = 0 OR s.cfd_margin = :cfdMargin)
                """;
        return query;
    }

    private Pair<LocalDate, LocalDate> intervalFor(LocalDate date, StockTimeframe timeFrame) {
        return switch (timeFrame) {
            case WEEKLY -> Pair.of(date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)), date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).with(TemporalAdjusters.nextOrSame(DayOfWeek.FRIDAY)));
            case MONTHLY -> Pair.of(date.with(TemporalAdjusters.firstDayOfMonth()), date.with(TemporalAdjusters.lastDayOfMonth()));
            case YEARLY -> Pair.of(date.with(TemporalAdjusters.firstDayOfYear()), date.with(TemporalAdjusters.lastDayOfYear()));
        };
    }


}
