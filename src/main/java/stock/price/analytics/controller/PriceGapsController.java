package stock.price.analytics.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import stock.price.analytics.cache.CacheService;
import stock.price.analytics.model.prices.enums.StockTimeframe;
import stock.price.analytics.service.PriceGapService;
import stock.price.analytics.service.PriceService;

@RequiredArgsConstructor
@RestController
@RequestMapping("/price-gaps")
public class PriceGapsController {

    private final CacheService cacheService;
    private final PriceService priceService;
    private final PriceGapService priceGapService;

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    public void savePriceGaps() {
        priceGapService.savePriceGapsTodayFor(StockTimeframe.DAILY);
        StockTimeframe.higherTimeframes().stream()
                .filter(priceService::isFirstImportDoneFor)
                .forEach(priceGapService::savePriceGapsTodayFor);
    }

}
