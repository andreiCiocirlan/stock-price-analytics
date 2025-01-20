package stock.price.analytics.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import stock.price.analytics.model.prices.ohlc.DailyPriceOHLC;
import stock.price.analytics.repository.prices.DailyPriceOHLCRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static stock.price.analytics.util.Constants.MAX_TICKER_COUNT_PRINT;

@Slf4j
@Service
@RequiredArgsConstructor
public class DailyPriceOHLCService {

    private final DailyPriceOHLCRepository dailyPriceOHLCRepository;
    private final DailyPricesCacheService dailyPricesCacheService;

    @Transactional
    public List<DailyPriceOHLC> getDailyImportedPrices(List<DailyPriceOHLC> dailyPrices, Map<String, DailyPriceOHLC> latestPricesByTicker) {
        List<DailyPriceOHLC> importedDailyPrices = new ArrayList<>();
        List<String> sameDailyPrices = new ArrayList<>();
        for (DailyPriceOHLC dailyPrice : dailyPrices) {
            String ticker = dailyPrice.getTicker();
            if (latestPricesByTicker.containsKey(ticker)) {
                DailyPriceOHLC latestPrice = latestPricesByTicker.get(ticker);
                if (latestPrice.getDate().equals(dailyPrice.getDate())) {
                    if (needsUpdate(dailyPrice, latestPrice)) { // update prices
                        log.info("updated ticker {} which has different prices compared to DB {}", ticker, dailyPrice);
                        BeanUtils.copyProperties(dailyPrice, latestPrice, "id", "date"); // date won't change (opening price might be adjusted)
                        importedDailyPrices.add(latestPrice);
                    } else {
                        sameDailyPrices.add(ticker);
                    }
                } else if (dailyPrice.getDate().isAfter(latestPrice.getDate())) { // only add if the import date is after latest date from DB
                    importedDailyPrices.add(dailyPrice);
                }
            } else {
                log.info("new stock daily price: {}", dailyPrice);
                importedDailyPrices.add(dailyPrice);
            }
        }
        if (!sameDailyPrices.isEmpty()) {
            log.warn("same {} daily prices as in DB", sameDailyPrices.size());
            if (sameDailyPrices.size() <= MAX_TICKER_COUNT_PRINT) {
                log.warn("{}", sameDailyPrices);
            }
        }
        return importedDailyPrices;
    }

    public List<DailyPriceOHLC> addDailyPricesInCacheAndReturn(List<DailyPriceOHLC> dailyPriceOHLCs) {
        return dailyPricesCacheService.addDailyPricesInCacheAndReturn(dailyPriceOHLCs);
    }

    public List<DailyPriceOHLC> findXTBLatestDailyPricesWithDateAfter(LocalDate date) {
        return dailyPriceOHLCRepository.findXTBLatestDailyPricesWithDateAfter(date);
    }

    private static boolean needsUpdate(DailyPriceOHLC dailyPrice, DailyPriceOHLC latestPrice) {
        return dailyPrice.getClose() != latestPrice.getClose() || dailyPrice.getOpen() != latestPrice.getOpen()
                || dailyPrice.getHigh() != latestPrice.getHigh() || dailyPrice.getLow() != latestPrice.getLow()
                || dailyPrice.getPerformance() != latestPrice.getPerformance();
    }

}
