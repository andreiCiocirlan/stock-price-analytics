package stock.price.analytics.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import stock.price.analytics.cache.CacheService;
import stock.price.analytics.model.prices.enums.HighLowPeriod;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class HighLowPricesCacheService {

    private final CacheService cacheService;

    public void logNewHighLowsForHLPeriods() {
        for (HighLowPeriod highLowPeriod : HighLowPeriod.values()) {
            List<String> newHighLowsForHLPeriod = cacheService.getNewHighLowsForHLPeriod(highLowPeriod);
            if (!newHighLowsForHLPeriod.isEmpty()) {
                log.info("{} New {} : {}", newHighLowsForHLPeriod.size(), highLowPeriod, newHighLowsForHLPeriod);
            }
        }
    }

    public void logEqualHighLowsForHLPeriods() {
        for (HighLowPeriod highLowPeriod : HighLowPeriod.values()) {
            List<String> equalHighLowsForHLPeriod = cacheService.getEqualHighLowsForHLPeriod(highLowPeriod);
            if (!equalHighLowsForHLPeriod.isEmpty()) {
                log.info("{} Equal {} : {}", equalHighLowsForHLPeriod.size(), highLowPeriod, equalHighLowsForHLPeriod);
            }
        }
    }
}