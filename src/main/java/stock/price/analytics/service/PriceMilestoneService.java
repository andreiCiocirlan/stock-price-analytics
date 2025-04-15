package stock.price.analytics.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import stock.price.analytics.cache.CacheService;
import stock.price.analytics.model.json.DailyPriceJSON;
import stock.price.analytics.model.prices.PriceMilestone;
import stock.price.analytics.model.prices.enums.IntradayPriceSpike;
import stock.price.analytics.model.prices.enums.PreMarketPriceMilestone;
import stock.price.analytics.model.prices.enums.PricePerformanceMilestone;
import stock.price.analytics.model.prices.enums.SimpleMovingAverageMilestone;
import stock.price.analytics.model.prices.highlow.HighLowForPeriod;
import stock.price.analytics.model.prices.ohlc.DailyPrice;
import stock.price.analytics.model.stocks.Stock;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static stock.price.analytics.model.stocks.enums.MarketState.PRE;
import static stock.price.analytics.model.stocks.enums.MarketState.REGULAR;
import static stock.price.analytics.util.Constants.*;

@Service
@RequiredArgsConstructor
public class PriceMilestoneService {

    private final CacheService cacheService;

    public List<String> tickersFor(List<PriceMilestone> priceMilestones, List<Double> cfdMargins) {
        List<String> tickers = new ArrayList<>();
        for (PriceMilestone priceMilestone : priceMilestones) {
            List<String> filteredTickers = cacheService.tickersFor(priceMilestone, cfdMargins);

            if (filteredTickers.isEmpty()) {
                return Collections.emptyList();
            }

            if (tickers.isEmpty()) {
                tickers = filteredTickers;
            } else {
                tickers = tickers.stream()
                        .filter(filteredTickers::contains)
                        .toList();

                // if no overlap return emptyList
                if (tickers.isEmpty()) return Collections.emptyList();
            }
        }

        return tickers;
    }

    public Map<PriceMilestone, List<String>> findTickersForMilestones(List<PriceMilestone> priceMilestones, List<Double> cfdMargins) {
        Map<PriceMilestone, List<String>> tickersByPriceMilestones = new HashMap<>();
        priceMilestones.forEach(priceMilestone -> {
            List<String> tickers = findTickersForMilestone(priceMilestone.name(), priceMilestone.getType(), cfdMargins);
            if (!tickers.isEmpty()) {
                tickersByPriceMilestones.put(priceMilestone, tickers);
            }
        });
        return tickersByPriceMilestones;
    }

    public List<String> findTickersForMilestone(String priceMilestone, String milestoneType, List<Double> cfdMargins) {
        if (isInvalidTypeMapping(priceMilestone, milestoneType)) {
            throw new IllegalArgumentException("Invalid milestone type combination");
        }

        return switch (milestoneType) {
            case "performance" -> filterByPerformanceMilestone(PricePerformanceMilestone.valueOf(priceMilestone), cfdMargins);
            case "premarket" -> filterByPreMarketMilestone(PreMarketPriceMilestone.valueOf(priceMilestone), cfdMargins);
            case "intraday-spike" -> filterByIntradaySpikeMilestone(IntradayPriceSpike.valueOf(priceMilestone), cfdMargins);
            case "sma-milestone" -> filterBySimpleMovingAvgMilestone(SimpleMovingAverageMilestone.valueOf(priceMilestone), cfdMargins);
            default -> throw new IllegalArgumentException("Invalid milestone type");
        };
    }

    private List<String> filterByPerformanceMilestone(PricePerformanceMilestone pricePerformanceMilestone, List<Double> cfdMargins) {
        Map<String, HighLowForPeriod> hlPricesCache = cacheService.highLowForPeriodPricesForMilestone(pricePerformanceMilestone)
                .stream()
                .collect(Collectors.toMap(HighLowForPeriod::getTicker, p -> p));

        return cacheService.getCachedStocks().stream()
                .filter(stock -> cfdMargins.isEmpty() || cfdMargins.contains(stock.getCfdMargin()))
                .filter(stock -> hlPricesCache.containsKey(stock.getTicker()))
                .filter(stock -> withinPerformanceMilestone(stock, hlPricesCache.get(stock.getTicker()), pricePerformanceMilestone))
                .map(Stock::getTicker)
                .toList();
    }

    private List<String> filterByPreMarketMilestone(PreMarketPriceMilestone milestone, List<Double> cfdMargins) {
        Map<String, DailyPrice> preMarketPricesCache = cacheService.getCachedDailyPrices(PRE)
                .stream()
                .collect(Collectors.toMap(DailyPrice::getTicker, p -> p));

        return cacheService.getCachedStocks().stream()
                .filter(stock -> cfdMargins.isEmpty() || cfdMargins.contains(stock.getCfdMargin()))
                .filter(stock -> preMarketPricesCache.containsKey(stock.getTicker()))
                .filter(stock -> withinPreMarketMilestone(stock, preMarketPricesCache.get(stock.getTicker()), milestone))
                .map(Stock::getTicker)
                .toList();
    }

    private List<String> filterByIntradaySpikeMilestone(IntradayPriceSpike milestone, List<Double> cfdMargins) {
        Map<String, DailyPrice> intradayPricesCache = cacheService.getCachedDailyPrices(REGULAR)
                .stream()
                .collect(Collectors.toMap(DailyPrice::getTicker, p -> p));

        return cacheService.getCachedStocks().stream()
                .filter(stock -> cfdMargins.isEmpty() || cfdMargins.contains(stock.getCfdMargin()))
                .filter(stock -> intradayPricesCache.containsKey(stock.getTicker()))
                .filter(stock -> withinIntradaySpikeMilestone(stock, intradayPricesCache.get(stock.getTicker()), milestone))
                .map(Stock::getTicker)
                .toList();
    }

    private List<String> filterBySimpleMovingAvgMilestone(SimpleMovingAverageMilestone smaMilestone, List<Double> cfdMargins) {
        Map<String, DailyPriceJSON> dailyPriceJsonCache = cacheService.dailyPriceJsonCache()
                .stream()
                .collect(Collectors.toMap(
                        DailyPriceJSON::getSymbol,
                        Function.identity(),
                        (dp1, dp2) -> dp1.getDate().isAfter(dp2.getDate()) ? dp1 : dp2
                ));

        return cacheService.getCachedStocks().stream()
                .filter(stock -> cfdMargins.isEmpty() || cfdMargins.contains(stock.getCfdMargin()))
                .map(Stock::getTicker)
                .filter(dailyPriceJsonCache::containsKey)
                .filter(ticker -> withinSimpleMovingAvgMilestone(dailyPriceJsonCache.get(ticker), smaMilestone))
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
            // new 4w, 52w, all-time high-low in pre-market
            case PRE_NEW_4W_HIGH -> preMarketPrice.getClose() > s.getHigh4w();
            case PRE_NEW_4W_LOW -> preMarketPrice.getClose() < s.getLow4w();
            case PRE_NEW_52W_HIGH -> preMarketPrice.getClose() > s.getHigh52w();
            case PRE_NEW_52W_LOW -> preMarketPrice.getClose() < s.getLow52w();
            case PRE_NEW_ALL_TIME_HIGH -> preMarketPrice.getClose() > s.getHighest();
            case PRE_NEW_ALL_TIME_LOW -> preMarketPrice.getClose() < s.getLowest();
        };
    }

    // called before stocks cache is updated (compare daily price imported to determine spikes)
    private boolean withinIntradaySpikeMilestone(Stock s, DailyPrice dailyPrice, IntradayPriceSpike milestone) {
        return switch (milestone) {
            case INTRADAY_SPIKE_UP -> dailyPrice.getClose() > s.getClose() * (1 + INTRADAY_SPIKE_PERCENTAGE);
            case INTRADAY_SPIKE_DOWN -> dailyPrice.getClose() < s.getClose() * (1 - INTRADAY_SPIKE_PERCENTAGE);
        };
    }

    private boolean withinSimpleMovingAvgMilestone(DailyPriceJSON dailyPriceJSON, SimpleMovingAverageMilestone milestone) {
        return switch (milestone) {
            case ABOVE_200_SMA -> dailyPriceJSON.getRegularMarketPrice() > dailyPriceJSON.getTwoHundredDayAverage();
            case ABOVE_50_SMA -> dailyPriceJSON.getRegularMarketPrice() > dailyPriceJSON.getFiftyDayAverage();
            case BELOW_200_SMA -> dailyPriceJSON.getRegularMarketPrice() < dailyPriceJSON.getTwoHundredDayAverage();
            case BELOW_50_SMA -> dailyPriceJSON.getRegularMarketPrice() < dailyPriceJSON.getFiftyDayAverage();
        };
    }

    private boolean isInvalidTypeMapping(String priceMilestone, String milestoneType) {
        return switch (milestoneType) {
            case "premarket" -> Arrays.stream(PreMarketPriceMilestone.values())
                    .map(Enum::name)
                    .noneMatch(pm -> pm.equals(priceMilestone));
            case "performance" -> Arrays.stream(PricePerformanceMilestone.values())
                    .map(Enum::name)
                    .noneMatch(pm -> pm.equals(priceMilestone));
            case "intraday-spike" -> Arrays.stream(IntradayPriceSpike.values())
                    .map(Enum::name)
                    .noneMatch(pm -> pm.equals(priceMilestone));
            case "sma-milestone" -> Arrays.stream(SimpleMovingAverageMilestone.values())
                    .map(Enum::name)
                    .noneMatch(pm -> pm.equals(priceMilestone));
            default -> false;
        };
    }

    public void cacheTickersForMilestones() {
        for (PricePerformanceMilestone pricePerformanceMilestone : PricePerformanceMilestone.values()) {
            cacheService.cachePriceMilestoneTickers(pricePerformanceMilestone, findTickersForMilestone(pricePerformanceMilestone.name(), pricePerformanceMilestone.getType(), CFD_MARGINS_5X_4X_3X_2X_1X));
        }
        for (PreMarketPriceMilestone preMarketPriceMilestone : PreMarketPriceMilestone.values()) {
            cacheService.cachePriceMilestoneTickers(preMarketPriceMilestone, findTickersForMilestone(preMarketPriceMilestone.name(), preMarketPriceMilestone.getType(), CFD_MARGINS_5X_4X_3X_2X_1X));
        }
        for (SimpleMovingAverageMilestone simpleMovingAverageMilestone : SimpleMovingAverageMilestone.values()) {
            cacheService.cachePriceMilestoneTickers(simpleMovingAverageMilestone, findTickersForMilestone(simpleMovingAverageMilestone.name(), simpleMovingAverageMilestone.getType(), CFD_MARGINS_5X_4X_3X_2X_1X));
        }
    }
}
