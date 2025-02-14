package stock.price.analytics.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import stock.price.analytics.model.prices.ohlc.DailyPrice;
import stock.price.analytics.model.stocks.enums.MarketState;
import stock.price.analytics.repository.prices.DailyPricesRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DailyPricesService {

    private final DailyPricesCacheService dailyPricesCacheService;
    private final DailyPricesRepository dailyPricesRepository;

    public void initLatestTwoDaysPricesCache() {
        List<DailyPrice> latestPrices = new ArrayList<>();
        List<DailyPrice> previousDayPrices = new ArrayList<>();

        dailyPricesRepository.findLatestDailyPrices().stream()
                .sorted(Comparator.comparing(DailyPrice::getDate).reversed())
                .collect(Collectors.groupingBy(DailyPrice::getTicker))
                .forEach((_, dailyPrices) -> {
                    if (!dailyPrices.isEmpty()) {
                        latestPrices.add(dailyPrices.getFirst()); // Latest day
                    }
                    if (dailyPrices.size() > 1) {
                        previousDayPrices.add(dailyPrices.get(1)); // Previous day
                    }
                });

        initDailyPricesCache(latestPrices);
        initPreviousDayPricesCache(previousDayPrices);
    }

    private void initDailyPricesCache(List<DailyPrice> latestDailyPrices) {
        dailyPricesCacheService.initDailyPricesCache(latestDailyPrices);
    }

    private void initPreviousDayPricesCache(List<DailyPrice> previousDayPrices) {
        dailyPricesCacheService.initPreviousDayPricesCache(previousDayPrices);
    }

    public void initPreMarketDailyPricesCache() {
        dailyPricesCacheService.initPreMarketDailyPricesCache();
    }

    public List<DailyPrice> dailyPricesCache(MarketState marketState) {
        return dailyPricesCacheService.dailyPricesCache(marketState);
    }

    public List<DailyPrice> addDailyPricesInCacheAndReturn(List<DailyPrice> dailyPrices) {
        return dailyPricesCacheService.addDailyPricesInCacheAndReturn(dailyPrices);
    }

    public void addPreMarketDailyPricesInCache(List<DailyPrice> preMarketPrices) {
        dailyPricesCacheService.addPreMarketDailyPricesInCache(preMarketPrices);
    }
}
