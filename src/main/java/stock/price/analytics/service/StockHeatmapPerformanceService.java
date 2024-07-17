package stock.price.analytics.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import stock.price.analytics.controller.dto.StockPerformanceDTO;
import stock.price.analytics.model.prices.enums.StockTimeframe;

import java.util.List;

import static stock.price.analytics.model.prices.enums.StockTimeframe.dbTablePerfHeatmapFrom;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockHeatmapPerformanceService {

    @PersistenceContext
    private final EntityManager entityManager;

    public List<StockPerformanceDTO> stockPerformanceForDateAndTimeframeAndFilters(StockTimeframe timeFrame, Boolean xtb, Boolean positivePerfFirst, Integer limit, Double cfdMargin) {
        String queryStr = queryFrom(timeFrame, positivePerfFirst, limit, xtb, cfdMargin);

        Query nativeQuery = entityManager.createNativeQuery(queryStr, StockPerformanceDTO.class);

        @SuppressWarnings("unchecked")
        List<StockPerformanceDTO> priceOHLCs = (List<StockPerformanceDTO>) nativeQuery.getResultList();

        return priceOHLCs;
    }

    private static String queryFrom(StockTimeframe timeFrame, Boolean positivePerfFirst, Integer limit, Boolean xtb, Double cfdMargin) {
        String dbTable = dbTablePerfHeatmapFrom(timeFrame);
        String query = STR."""
            SELECT p.ticker, p.performance FROM \{dbTable} p JOIN Stocks s ON s.ticker = p.ticker
            """;

        if (Boolean.TRUE.equals(xtb)) {
            query += " AND s.xtb_stock = true ";
            query += STR."""
                WHERE (COALESCE(\{cfdMargin}, -1) = -1 OR s.cfd_margin = \{cfdMargin})
                """; // only XTB tickers use cfdMargin field
        }

        // default negative performance first (ascending by performance)
        query += """
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


}
