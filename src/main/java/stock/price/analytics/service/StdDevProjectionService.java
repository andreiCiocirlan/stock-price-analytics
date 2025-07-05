package stock.price.analytics.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import stock.price.analytics.model.dto.StdDevProjectionDTO;
import stock.price.analytics.util.query.stddevprojection.StdDevProjectionQueryProvider;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StdDevProjectionService {

    @PersistenceContext
    private final EntityManager entityManager;
    private final StdDevProjectionQueryProvider stdDevProjectionQueryProvider;

    public List<StdDevProjectionDTO> getLast3TopProjections(String ticker) {
        String sql = stdDevProjectionQueryProvider.findLast3TopProjections(ticker);
        Query query = entityManager.createNativeQuery(sql);
        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        return mapToDTO(results);
    }
    public List<StdDevProjectionDTO> getLast3BottomProjections(String ticker) {
        String sql = stdDevProjectionQueryProvider.findLast3BottomProjections(ticker);
        Query query = entityManager.createNativeQuery(sql);
        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        return mapToDTO(results);
    }

    public List<StdDevProjectionDTO> mapToDTO(List<Object[]> results) {
        return results.stream().map(row -> {
            String ticker = (String) row[0];
            LocalDate firstPointDate = ((java.sql.Date) row[1]).toLocalDate();
            LocalDate secondPointDate = ((java.sql.Date) row[2]).toLocalDate();
            double level1 = ((Number) row[3]).doubleValue();
            double level0 = ((Number) row[4]).doubleValue();
            double diff = ((Number) row[5]).doubleValue();
            double level_minus1 = ((Number) row[6]).doubleValue();
            double level_minus2 = ((Number) row[7]).doubleValue();
            double level_minus2_5 = ((Number) row[8]).doubleValue();
            double level_minus4 = ((Number) row[9]).doubleValue();
            double level_minus4_5 = ((Number) row[10]).doubleValue();

            return new StdDevProjectionDTO(
                    ticker,
                    firstPointDate,
                    secondPointDate,
                    diff,
                    level0,
                    level1,
                    level_minus1,
                    level_minus2,
                    level_minus2_5,
                    level_minus4,
                    level_minus4_5
            );
        }).collect(Collectors.toList());
    }
}
