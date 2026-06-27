package stock.price.analytics.service;

import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import stock.price.analytics.model.prices.ohlc.AbstractPrice;
import stock.price.analytics.repository.prices.ohlc.PriceRepository;

import java.util.List;

@Slf4j
@Service
public class PriceBatchSaver {

    private final PriceRepository priceRepository;
    private final EntityManager entityManager;

    public PriceBatchSaver(PriceRepository priceRepository, EntityManager entityManager) {
        this.priceRepository = priceRepository;
        this.entityManager = entityManager;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public <T extends AbstractPrice> void saveBatch(List<T> batch) {
        priceRepository.saveAll(batch);
        priceRepository.flush();
        entityManager.clear();
    }
}