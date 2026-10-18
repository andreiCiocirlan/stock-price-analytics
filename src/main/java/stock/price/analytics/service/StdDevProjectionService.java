package stock.price.analytics.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import stock.price.analytics.model.dto.StdDevProjectionDTO;
import stock.price.analytics.model.prices.enums.StockTimeframe;
import stock.price.analytics.util.query.stddevprojection.StdDevProjectionQueryProvider;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StdDevProjectionService {

    @PersistenceContext
    private EntityManager entityManager;
    private final StdDevProjectionQueryProvider stdDevProjectionQueryProvider;

    public List<StdDevProjectionDTO> getConfluenceProjections(String ticker, StockTimeframe timeframe) {
        String sql = stdDevProjectionQueryProvider.getConfluenceProjections(ticker, timeframe);
        Query query = entityManager.createNativeQuery(sql);
        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        return mapToDTO(results);
    }

    @Transactional
    public void insertStdDevProjectionsFor(StockTimeframe timeframe) {
        String query = stdDevProjectionQueryProvider.insertStdDevProjections(timeframe);
        int rowsAffected = entityManager.createNativeQuery(query).executeUpdate();
        log.warn("Inserted {} {} std dev projection rows", rowsAffected, timeframe);
    }


    public List<StdDevProjectionDTO> mapToDTO(List<Object[]> results) {
        return results.stream().map(row -> {
            LocalDate pivotDate = ((java.sql.Date) row[0]).toLocalDate();
            int offset = ((Number) row[1]).intValue();
            String type = (String) row[2];
            double proj40 = ((Number) row[3]).doubleValue();
            double proj45 = ((Number) row[4]).doubleValue();

            return new StdDevProjectionDTO(pivotDate, offset, type, proj40, proj45);
        }).collect(Collectors.toList());
    }
}
