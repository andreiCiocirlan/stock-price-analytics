package stock.price.analytics.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import stock.price.analytics.cache.DailyPricesCacheService;
import stock.price.analytics.cache.HighLowPricesCacheService;
import stock.price.analytics.model.prices.enums.IntradaySpike;
import stock.price.analytics.model.prices.enums.PreMarketPriceMilestone;
import stock.price.analytics.model.prices.enums.PriceMilestone;
import stock.price.analytics.model.prices.enums.PricePerformanceMilestone;
import stock.price.analytics.model.prices.highlow.HighLowForPeriod;
import stock.price.analytics.model.prices.ohlc.DailyPrice;
import stock.price.analytics.model.stocks.Stock;

import java.util.*;
import java.util.stream.Collectors;

import static stock.price.analytics.model.stocks.enums.MarketState.PRE;
import static stock.price.analytics.model.stocks.enums.MarketState.REGULAR;
import static stock.price.analytics.util.Constants.INTRADAY_SPIKE_PERCENTAGE;
import static stock.price.analytics.util.Constants.MIN_GAP_AND_GO_PERCENTAGE;
import static stock.price.analytics.util.EnumParser.parseEnumWithNoneValue;

@Service
@RequiredArgsConstructor
public class PriceMilestoneService {

    private static List<Stock> cachedStocks;
    private final DailyPricesCacheService dailyPricesCacheService;
    private final StockService stockService;
    private final HighLowPricesCacheService highLowPricesCacheService;

    public Map<PriceMilestone, List<String>> findTickersForMilestones(List<PriceMilestone> priceMilestones, List<Double> cfdMargins) {
        Map<PriceMilestone, List<String>> tickersByPriceMilestones = new HashMap<>();
        priceMilestones.forEach(priceMilestone -> {
            List<String> tickers = findTickersForMilestone(priceMilestone.name(), cfdMargins);
            if (!tickers.isEmpty()) {
                tickersByPriceMilestones.put(priceMilestone, tickers);
            }
        });
        return tickersByPriceMilestones;
    }

    public List<String> findTickersForMilestone(String priceMilestone, List<Double> cfdMargins) {
        final List<String> tickers = new ArrayList<>();
        Optional<PricePerformanceMilestone> pricePerformanceMilestone = parseEnumWithNoneValue(priceMilestone, PricePerformanceMilestone.class);
        cachedStocks = stockService.getCachedStocks();
        if (pricePerformanceMilestone.isPresent()) {
            tickers.addAll(findTickersForMilestone(pricePerformanceMilestone.get(), cfdMargins));
        } else {
            Optional<PreMarketPriceMilestone> preMarketPriceMilestone = parseEnumWithNoneValue(priceMilestone, PreMarketPriceMilestone.class);
            if (preMarketPriceMilestone.isPresent()) {
                parseEnumWithNoneValue(priceMilestone, PreMarketPriceMilestone.class)
                        .ifPresent(milestone -> tickers.addAll(findTickersForPreMarketMilestone(milestone, cfdMargins)));
            } else {
                parseEnumWithNoneValue(priceMilestone, IntradaySpike.class)
                        .ifPresent(milestone -> tickers.addAll(findTickersForIntradaySpikeMilestone(milestone, cfdMargins)));
            }
        }
        return tickers;
    }

    private List<String> findTickersForMilestone(PricePerformanceMilestone pricePerformanceMilestone, List<Double> cfdMargins) {
        Map<String, HighLowForPeriod> hlPricesCache = highLowPricesCacheService.cacheForMilestone(pricePerformanceMilestone)
                .stream()
                .collect(Collectors.toMap(HighLowForPeriod::getTicker, p -> p));

        return cachedStocks.stream()
                .filter(stock -> cfdMargins.isEmpty() || cfdMargins.contains(stock.getCfdMargin()))
                .filter(stock -> hlPricesCache.containsKey(stock.getTicker()))
                .filter(stock -> withinPerformanceMilestone(stock, hlPricesCache.get(stock.getTicker()), pricePerformanceMilestone))
                .map(Stock::getTicker)
                .toList();
    }

    private List<String> findTickersForPreMarketMilestone(PreMarketPriceMilestone milestone, List<Double> cfdMargins) {
        Map<String, DailyPrice> preMarketPricesCache = dailyPricesCacheService.dailyPricesCache(PRE)
                .stream()
                .collect(Collectors.toMap(DailyPrice::getTicker, p -> p));

        return cachedStocks.stream()
                .filter(stock -> cfdMargins.isEmpty() || cfdMargins.contains(stock.getCfdMargin()))
                .filter(stock -> preMarketPricesCache.containsKey(stock.getTicker()))
                .filter(stock -> withinPreMarketMilestone(stock, preMarketPricesCache.get(stock.getTicker()), milestone))
                .map(Stock::getTicker)
                .toList();
    }

    private List<String> findTickersForIntradaySpikeMilestone(IntradaySpike milestone, List<Double> cfdMargins) {
        Map<String, DailyPrice> intradayPricesCache = dailyPricesCacheService.dailyPricesCache(REGULAR)
                .stream()
                .collect(Collectors.toMap(DailyPrice::getTicker, p -> p));

        return cachedStocks.stream()
                .filter(stock -> cfdMargins.isEmpty() || cfdMargins.contains(stock.getCfdMargin()))
                .filter(stock -> intradayPricesCache.containsKey(stock.getTicker()))
                .filter(stock -> withinIntradaySpikeMilestone(stock, intradayPricesCache.get(stock.getTicker()), milestone))
                .map(Stock::getTicker)
                .toList();
    }

    private boolean withinPerformanceMilestone(Stock s, HighLowForPeriod highLowForPeriod, PricePerformanceMilestone pricePerformanceMilestone) {
        return switch (pricePerformanceMilestone) {
            case NEW_52W_HIGH, NEW_ALL_TIME_HIGH, NEW_4W_HIGH -> s.getWeeklyHigh() > highLowForPeriod.getHigh();
            case NEW_52W_LOW, NEW_4W_LOW, NEW_ALL_TIME_LOW -> s.getWeeklyLow() < highLowForPeriod.getLow();
            case HIGH_52W_95, HIGH_4W_95, HIGH_ALL_TIME_95 ->
                    highLowForPeriod.getLow() != highLowForPeriod.getHigh() && (1 - (1 - (s.getClose() - highLowForPeriod.getLow()) / (highLowForPeriod.getHigh() - highLowForPeriod.getLow()))) > 0.95;
            case LOW_52W_95, LOW_4W_95, LOW_ALL_TIME_95 ->
                    highLowForPeriod.getLow() != highLowForPeriod.getHigh() && (1 - (s.getClose() - highLowForPeriod.getLow()) / (highLowForPeriod.getHigh() - highLowForPeriod.getLow())) > 0.95;
            case NONE -> throw new IllegalStateException("Unexpected value NONE");
        };
    }

    private boolean withinPreMarketMilestone(Stock s, DailyPrice preMarketPrice, PreMarketPriceMilestone milestone) {
        return switch (milestone) {
            // previous day DOWN more than 1% && pre-market GAP UP more than 4%
            case KICKING_CANDLE_UP ->
                    s.getDailyPerformance() < -1.0d && preMarketPrice.getClose() > s.getClose() * (1 + MIN_GAP_AND_GO_PERCENTAGE);
            // previous day UP more than 1% && pre-market GAP DOWN more than 4%
            case KICKING_CANDLE_DOWN ->
                    s.getDailyPerformance() > 1.0d && preMarketPrice.getClose() < s.getClose() * (1 - MIN_GAP_AND_GO_PERCENTAGE);
            case GAP_UP -> preMarketPrice.getClose() > s.getClose();
            case GAP_DOWN -> preMarketPrice.getClose() < s.getClose();
            case GAP_UP_10_PERCENT -> preMarketPrice.getClose() > s.getClose() * 1.10;
            case GAP_DOWN_10_PERCENT -> preMarketPrice.getClose() < s.getClose() * 0.90;
            // pre-market GAP UP more than 4%
            case GAP_UP_AND_GO -> preMarketPrice.getClose() > s.getClose() * (1 + MIN_GAP_AND_GO_PERCENTAGE);
            // pre-market GAP DOWN more than 4%
            case GAP_DOWN_AND_GO -> preMarketPrice.getClose() < s.getClose() * (1 - MIN_GAP_AND_GO_PERCENTAGE);
            case NEW_ATH -> preMarketPrice.getClose() > s.getHighest();
            case NEW_ATL -> preMarketPrice.getClose() < s.getLowest();
            case NONE -> throw new IllegalStateException("Unexpected value NONE");
        };
    }

    // called before stocks cache is updated (compare daily price imported to determine spikes)
    private boolean withinIntradaySpikeMilestone(Stock s, DailyPrice dailyPrice, IntradaySpike milestone) {
        return switch (milestone) {
            case SPIKE_UP -> dailyPrice.getClose() > s.getClose() * (1 + INTRADAY_SPIKE_PERCENTAGE);
            case SPIKE_DOWN -> dailyPrice.getClose() < s.getClose() * (1 - INTRADAY_SPIKE_PERCENTAGE);
            case NONE -> throw new IllegalStateException("Unexpected value NONE");
        };
    }
}
