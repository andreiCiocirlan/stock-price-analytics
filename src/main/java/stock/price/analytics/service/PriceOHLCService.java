package stock.price.analytics.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import stock.price.analytics.controller.dto.CandleOHLCWithDateDTO;
import stock.price.analytics.model.prices.enums.StockTimeframe;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PriceOHLCService {

    @PersistenceContext
    private final EntityManager entityManager;

    public List<CandleOHLCWithDateDTO> findOHLCFor(String ticker, StockTimeframe timeframe) {
        String tableNameOHLC = StockTimeframe.dbTableOHLCFrom(timeframe);
        String orderByIdField = timeframe == StockTimeframe.DAILY ? "date" : "start_date";
        String queryStr = STR."SELECT \{orderByIdField}, open, high, low, close FROM \{tableNameOHLC} WHERE ticker = :ticker ORDER BY \{orderByIdField} ASC";

        Query nativeQuery = entityManager.createNativeQuery(queryStr, CandleOHLCWithDateDTO.class);
        nativeQuery.setParameter("ticker", ticker);

        @SuppressWarnings("unchecked")
        List<CandleOHLCWithDateDTO> priceOHLCs = (List<CandleOHLCWithDateDTO>) nativeQuery.getResultList();

        return priceOHLCs;
    }
}
