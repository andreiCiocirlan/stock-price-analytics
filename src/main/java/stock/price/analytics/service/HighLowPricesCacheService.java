package stock.price.analytics.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import stock.price.analytics.cache.CacheService;

@Slf4j
@Service
@RequiredArgsConstructor
public class HighLowPricesCacheService {

    private final CacheService cacheService;

}