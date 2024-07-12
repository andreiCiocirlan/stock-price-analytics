package stock.price.analytics.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import stock.price.analytics.model.prices.ohlc.DailyPriceOHLC;
import stock.price.analytics.repository.prices.PriceOHLCRepository;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SplitAdjustPricesService {

    private final PriceOHLCRepository priceOHLCRepository;

    public void adjustPricesFor(String ticker, LocalDate stockSplitDate, double priceMultiplier) {
        List<DailyPriceOHLC> dailyPricesToUpdate = priceOHLCRepository.findByTickerAndDateBefore(ticker, stockSplitDate);
        dailyPricesToUpdate.forEach(dailyPriceOHLC -> updatePrices(dailyPriceOHLC, priceMultiplier));

        priceOHLCRepository.saveAll(dailyPricesToUpdate);
    }

    private void updatePrices(DailyPriceOHLC dailyPriceOHLC, double priceMultiplier) {
        dailyPriceOHLC.setClose(Math.round((priceMultiplier * dailyPriceOHLC.getClose()) * 100.0) / 100.0);
        dailyPriceOHLC.setOpen(Math.round((priceMultiplier * dailyPriceOHLC.getOpen()) * 100.0) / 100.0);
        dailyPriceOHLC.setLow(Math.round((priceMultiplier * dailyPriceOHLC.getLow()) * 100.0) / 100.0);
        dailyPriceOHLC.setHigh(Math.round((priceMultiplier * dailyPriceOHLC.getHigh()) * 100.0) / 100.0);
    }
}
