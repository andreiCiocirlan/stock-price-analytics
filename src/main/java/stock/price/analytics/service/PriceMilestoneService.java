package stock.price.analytics.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import stock.price.analytics.cache.DailyPricesCacheService;
import stock.price.analytics.cache.HighLowPricesCacheService;
import stock.price.analytics.model.prices.enums.PreMarketPriceMilestone;
import stock.price.analytics.model.prices.enums.PriceMilestone;
import stock.price.analytics.model.prices.enums.PricePerformanceMilestone;
import stock.price.analytics.model.prices.highlow.HighLowForPeriod;
import stock.price.analytics.model.prices.ohlc.DailyPrice;
import stock.price.analytics.model.stocks.Stock;

import java.util.*;
import java.util.stream.Collectors;

import static stock.price.analytics.model.stocks.enums.MarketState.PRE;
import static stock.price.analytics.util.EnumParser.parseEnumWithNoneValue;

@Service
@RequiredArgsConstructor
public class PriceMilestoneService {

    private static final Double MIN_GAP_AND_GO_PERCENTAGE = 0.04d;
    private final DailyPricesCacheService dailyPricesCacheService;
    private final StockService stockService;
    private final HighLowPricesCacheService highLowPricesCacheService;

    public Map<String, List<String>> findTickersForMilestones(List<PriceMilestone> priceMilestones, List<Double> cfdMargins) {
        Map<String, List<String>> tickersByPriceMilestones = new HashMap<>();
        for (PriceMilestone priceMilestone : priceMilestones) {
            tickersByPriceMilestones.put(priceMilestone.toString(), findTickersForMilestone(priceMilestone.name(), cfdMargins));
        }
        return tickersByPriceMilestones;
    }

    public List<String> findTickersForMilestone(String priceMilestone, List<Double> cfdMargins) {
        final List<String> tickers = new ArrayList<>();
        Optional<PricePerformanceMilestone> pricePerformanceMilestone = parseEnumWithNoneValue(priceMilestone, PricePerformanceMilestone.class);
        if (pricePerformanceMilestone.isPresent()) {
            tickers.addAll(findTickersForMilestone(pricePerformanceMilestone.get(), cfdMargins));
        } else {
            parseEnumWithNoneValue(priceMilestone, PreMarketPriceMilestone.class)
                    .ifPresent(milestone -> tickers.addAll(findTickersForPreMarketMilestone(milestone, cfdMargins)));
        }
        return tickers;
    }

    private List<String> findTickersForMilestone(PricePerformanceMilestone pricePerformanceMilestone, List<Double> cfdMargins) {
        Map<String, HighLowForPeriod> hlPricesCache = highLowPricesCacheService.cacheForMilestone(pricePerformanceMilestone)
                .stream()
                .collect(Collectors.toMap(HighLowForPeriod::getTicker, p -> p));

        return stockService.getCachedStocks().stream()
                .filter(stock -> cfdMargins.isEmpty() || cfdMargins.contains(stock.getCfdMargin()))
                .filter(stock -> priceWithinPerformanceMilestone(stock, hlPricesCache.get(stock.getTicker()), pricePerformanceMilestone))
                .map(Stock::getTicker)
                .toList();
    }

    private List<String> findTickersForPreMarketMilestone(PreMarketPriceMilestone milestone, List<Double> cfdMargins) {
        Map<String, DailyPrice> preMarketPricesCache = dailyPricesCacheService.dailyPricesCache(PRE)
                .stream()
                .collect(Collectors.toMap(DailyPrice::getTicker, p -> p));

        return stockService.getCachedStocks().stream()
                .filter(stock -> cfdMargins.isEmpty() || cfdMargins.contains(stock.getCfdMargin()))
                .filter(stock -> preMarketPricesCache.containsKey(stock.getTicker()))
                .filter(stock -> priceWithinMilestone(stock, preMarketPricesCache.get(stock.getTicker()), milestone))
                .map(Stock::getTicker)
                .toList();
    }

    private boolean priceWithinPerformanceMilestone(Stock s, HighLowForPeriod highLowForPeriod, PricePerformanceMilestone pricePerformanceMilestone) {
        return switch (pricePerformanceMilestone) {
            case NEW_52W_HIGH, NEW_ALL_TIME_HIGH, NEW_4W_HIGH -> s.getWeeklyHigh() > highLowForPeriod.getHigh();
            case NEW_52W_LOW, NEW_4W_LOW, NEW_ALL_TIME_LOW -> s.getWeeklyLow() < highLowForPeriod.getLow();
            case HIGH_52W_95, HIGH_4W_95, HIGH_ALL_TIME_95 ->
                    highLowForPeriod.getLow() != highLowForPeriod.getHigh() && (1 - (1 - (s.getWeeklyClose() - highLowForPeriod.getLow()) / (highLowForPeriod.getHigh() - highLowForPeriod.getLow()))) > 0.95;
            case LOW_52W_95, LOW_4W_95, LOW_ALL_TIME_95 ->
                    highLowForPeriod.getLow() != highLowForPeriod.getHigh() && (1 - (s.getWeeklyClose() - highLowForPeriod.getLow()) / (highLowForPeriod.getHigh() - highLowForPeriod.getLow())) > 0.95;
            case NONE -> throw new IllegalStateException("Unexpected value NONE");
        };
    }

    private boolean priceWithinMilestone(Stock s, DailyPrice preMarketPrice, PreMarketPriceMilestone milestone) {
        return switch (milestone) {
            // previous day DOWN more than 1% && pre-market GAP UP more than 4%
            case KICKING_CANDLE_UP ->
                    s.getDailyPerformance() < -1.0d && preMarketPrice.getClose() > s.getDailyClose() * (1 + MIN_GAP_AND_GO_PERCENTAGE);
            // previous day UP more than 1% && pre-market GAP DOWN more than 4%
            case KICKING_CANDLE_DOWN ->
                    s.getDailyPerformance() > 1.0d && preMarketPrice.getClose() < s.getDailyClose() * (1 - MIN_GAP_AND_GO_PERCENTAGE);
            case GAP_UP -> preMarketPrice.getClose() > s.getDailyClose();
            case GAP_DOWN -> preMarketPrice.getClose() < s.getDailyClose();
            case GAP_UP_10_PERCENT -> preMarketPrice.getClose() > s.getDailyClose() * 1.10;
            case GAP_DOWN_10_PERCENT -> preMarketPrice.getClose() < s.getDailyClose() * 0.90;
            // pre-market GAP UP more than 4%
            case GAP_UP_AND_GO -> preMarketPrice.getClose() > s.getDailyClose() * (1 + MIN_GAP_AND_GO_PERCENTAGE);
            // pre-market GAP DOWN more than 4%
            case GAP_DOWN_AND_GO -> preMarketPrice.getClose() < s.getDailyClose() * (1 - MIN_GAP_AND_GO_PERCENTAGE);
            case NONE -> throw new IllegalStateException("Unexpected value NONE");
        };
    }


}
