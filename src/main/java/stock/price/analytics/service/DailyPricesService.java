package stock.price.analytics.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import stock.price.analytics.cache.DailyPricesCacheService;
import stock.price.analytics.model.prices.ohlc.DailyPrice;
import stock.price.analytics.model.stocks.enums.MarketState;
import stock.price.analytics.repository.prices.DailyPricesRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static stock.price.analytics.model.prices.enums.IntradaySpike.intradaySpikes;
import static stock.price.analytics.util.Constants.CFD_MARGINS_5X_4X_3X;

@Slf4j
@Service
@RequiredArgsConstructor
public class DailyPricesService {

    private final DailyPricesCacheService dailyPricesCacheService;
    private final DailyPricesRepository dailyPricesRepository;
    private final PriceMilestoneService priceMilestoneService;

    public void initLatestTwoDaysPricesCache() {
        List<DailyPrice> latestPrices = new ArrayList<>();
        List<DailyPrice> previousDayPrices = new ArrayList<>();

        dailyPricesRepository.findLatestTwoDailyPrices().stream()
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

    public List<DailyPrice> previousDailyPrices() {
        return dailyPricesCacheService.previousDailyPrices();
    }

    public List<DailyPrice> addDailyPricesInCacheAndReturn(List<DailyPrice> dailyPrices) {
        return dailyPricesCacheService.addDailyPricesInCacheAndReturn(dailyPrices);
    }

    public void addPreMarketDailyPricesInCache(List<DailyPrice> preMarketPrices) {
        dailyPricesCacheService.addPreMarketDailyPricesInCache(preMarketPrices);
    }

    public Map<String, List<String>> tickersWithIntradaySpike() {
        return priceMilestoneService.findTickersForMilestones(intradaySpikes(), CFD_MARGINS_5X_4X_3X);
    }

}
