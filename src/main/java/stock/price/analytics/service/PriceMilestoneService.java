package stock.price.analytics.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import stock.price.analytics.cache.CacheService;
import stock.price.analytics.model.candlestick.CandleStickType;
import stock.price.analytics.model.dto.StockHeatmapRequest;
import stock.price.analytics.model.json.DailyPriceJSON;
import stock.price.analytics.model.prices.PriceMilestone;
import stock.price.analytics.model.prices.context.StockDailyPriceContext;
import stock.price.analytics.model.prices.context.StockHighLowForPeriodContext;
import stock.price.analytics.model.prices.enums.*;
import stock.price.analytics.model.prices.highlow.HighLowForPeriod;
import stock.price.analytics.model.prices.ohlc.DailyPrice;
import stock.price.analytics.model.stocks.Stock;
import stock.price.analytics.util.PriceMilestoneFactory;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static stock.price.analytics.model.stocks.enums.MarketState.PRE;
import static stock.price.analytics.util.Constants.CFD_MARGINS_5X_4X_3X_2X_1X;

@Service
@RequiredArgsConstructor
public class PriceMilestoneService {

    private final CacheService cacheService;

    public List<String> tickersFor(StockHeatmapRequest request, StockTimeframe stockTimeframe) {
        List<String> tickers = request.getCandleStickType() != CandleStickType.ANY ? cacheService.tickersWith(stockTimeframe, request.getCandleStickType()) : new ArrayList<>();

        for (PriceMilestone priceMilestone : request.priceMilestones()) {
            List<String> filteredTickers = cacheService.tickersFor(priceMilestone, request.getCfdMargins());

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
            List<String> tickers = findTickersForMilestone(priceMilestone, cfdMargins);
            if (!tickers.isEmpty()) {
                tickersByPriceMilestones.put(priceMilestone, tickers);
            }
        });
        return tickersByPriceMilestones;
    }

    public List<String> findTickersForMilestone(PriceMilestone milestone, List<Double> cfdMargins) {
        return switch (milestone) {
            case NewHighLowMilestone newHighLowMilestone -> filterByNewHighLowMilestone(newHighLowMilestone, cfdMargins);
            case PricePerformanceMilestone pricePerformanceMilestone -> filterByPerformanceMilestone(pricePerformanceMilestone, cfdMargins);
            case PreMarketPriceMilestone preMarketMilestone -> filterByPreMarketMilestone(preMarketMilestone, cfdMargins);
            case PreMarketGap preMarketGap -> filterByPreMarketPerformanceMilestone(preMarketGap, cfdMargins);
            case SimpleMovingAverageMilestone smaMilestone -> filterBySimpleMovingAvgMilestone(smaMilestone, cfdMargins);
            default -> throw new IllegalArgumentException("Invalid milestone type");
        };
    }

    private List<String> filterByPerformanceMilestone(PricePerformanceMilestone pricePerformanceMilestone, List<Double> cfdMargins) {
        Map<String, HighLowForPeriod> hlPricesCache = cacheService.highLowForPeriodPricesForPricePerformanceMilestone(pricePerformanceMilestone)
                .stream()
                .collect(Collectors.toMap(HighLowForPeriod::getTicker, Function.identity()));

        return cacheService.getCachedStocks().stream()
                .filter(stock -> cfdMargins.isEmpty() || cfdMargins.contains(stock.getCfdMargin()))
                .filter(stock -> hlPricesCache.containsKey(stock.getTicker()))
                .filter(stock -> pricePerformanceMilestone.isMetFor(new StockHighLowForPeriodContext(stock, hlPricesCache.get(stock.getTicker()))))
                .map(Stock::getTicker)
                .toList();
    }

    private List<String> filterByNewHighLowMilestone(NewHighLowMilestone newHighLowMilestone, List<Double> cfdMargins) {
        Map<String, HighLowForPeriod> hlPricesCache = cacheService.highLowForPeriodPricesForNewHighLowMilestone(newHighLowMilestone)
                .stream()
                .collect(Collectors.toMap(HighLowForPeriod::getTicker, Function.identity()));

        return cacheService.getCachedStocks().stream()
                .filter(stock -> cfdMargins.isEmpty() || cfdMargins.contains(stock.getCfdMargin()))
                .filter(stock -> hlPricesCache.containsKey(stock.getTicker()))
                .filter(stock -> newHighLowMilestone.isMetFor(new StockHighLowForPeriodContext(stock, hlPricesCache.get(stock.getTicker()))))
                .map(Stock::getTicker)
                .toList();
    }

    private List<String> filterByPreMarketMilestone(PreMarketPriceMilestone preMarketMilestone, List<Double> cfdMargins) {
        Map<String, DailyPrice> preMarketPricesCache = cacheService.getCachedDailyPrices(PRE)
                .stream()
                .collect(Collectors.toMap(DailyPrice::getTicker, Function.identity()));

        return cacheService.getCachedStocks().stream()
                .filter(stock -> cfdMargins.isEmpty() || cfdMargins.contains(stock.getCfdMargin()))
                .filter(stock -> preMarketPricesCache.containsKey(stock.getTicker()))
                .filter(stock -> preMarketMilestone.isMetFor(new StockDailyPriceContext(stock, preMarketPricesCache.get(stock.getTicker()))))
                .map(Stock::getTicker)
                .toList();
    }

    private List<String> filterByPreMarketPerformanceMilestone(PreMarketGap preMarketGap, List<Double> cfdMargins) {
        Map<String, DailyPrice> preMarketPricesCache = cacheService.getCachedDailyPrices(PRE)
                .stream()
                .collect(Collectors.toMap(DailyPrice::getTicker, Function.identity()));

        return cacheService.getCachedStocks().stream()
                .filter(stock -> cfdMargins.isEmpty() || cfdMargins.contains(stock.getCfdMargin()))
                .map(Stock::getTicker)
                .filter(preMarketPricesCache::containsKey)
                .filter(ticker -> preMarketGap.isMetFor(preMarketPricesCache.get(ticker)))
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
                .filter(ticker -> smaMilestone.isMetFor(dailyPriceJsonCache.get(ticker)))
                .toList();
    }

    public void cacheTickersForMilestones() {
        for (PriceMilestone priceMilestone : PriceMilestoneFactory.registry()) {
            if (!(priceMilestone instanceof IntradayPriceSpike)) {
                cacheService.cachePriceMilestoneTickers(priceMilestone, findTickersForMilestone(priceMilestone, CFD_MARGINS_5X_4X_3X_2X_1X));
            }
        }
    }
}
