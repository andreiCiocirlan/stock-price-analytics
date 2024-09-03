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

@Slf4j
@Service
@RequiredArgsConstructor
public class DailyPriceOHLCService {

    private final DailyPriceOHLCRepository dailyPriceOHLCRepository;

    @Transactional
    public List<DailyPriceOHLC> getDailyImportedPrices(List<DailyPriceOHLC> dailyPrices, Map<String, DailyPriceOHLC> latestPricesByTicker) {
        List<DailyPriceOHLC> importedDailyPrices = new ArrayList<>();
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
                        log.warn("same daily prices as in DB, not saved for {}", ticker);
                    }
                } else { // insert new daily prices
                    importedDailyPrices.add(dailyPrice);
                }
            } else {
                log.info("new stock daily price: {}", dailyPrice);
                importedDailyPrices.add(dailyPrice);
            }
        }
        return importedDailyPrices;
    }

    public List<DailyPriceOHLC> findAllLatestByTickerWithDateAfter(LocalDate date) {
        return dailyPriceOHLCRepository.findAllLatestByTickerWithDateAfter(date);
    }

    public List<DailyPriceOHLC> findXTBLatestByTickerWithDateAfter(LocalDate date) {
        return dailyPriceOHLCRepository.findXTBLatestByTickerWithDateAfter(date);
    }

    private static boolean needsUpdate(DailyPriceOHLC dailyPrice, DailyPriceOHLC latestPrice) {
        return dailyPrice.getClose() != latestPrice.getClose() || dailyPrice.getOpen() != latestPrice.getOpen()
                || dailyPrice.getHigh() != latestPrice.getHigh() || dailyPrice.getLow() != latestPrice.getLow()
                || dailyPrice.getPerformance() != latestPrice.getPerformance();
    }

    public void saveDailyPrices(List<DailyPriceOHLC> dailyImportedPrices) {
        dailyPriceOHLCRepository.saveAll(dailyImportedPrices);
    }

}
