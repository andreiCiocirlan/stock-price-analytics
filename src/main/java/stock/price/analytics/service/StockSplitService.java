package stock.price.analytics.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import stock.price.analytics.model.prices.enums.StockTimeframe;
import stock.price.analytics.model.prices.ohlc.*;
import stock.price.analytics.repository.prices.ohlc.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

import static java.time.temporal.TemporalAdjusters.*;

@Service
@RequiredArgsConstructor
public class StockSplitService {

    private final PriceRepository priceRepository;
    private final DailyPriceRepository dailyPriceRepository;
    private final WeeklyPriceRepository weeklyPriceRepository;
    private final MonthlyPriceRepository monthlyPriceRepository;
    private final QuarterlyPriceRepository quarterlyPriceRepository;
    private final YearlyPriceRepository yearlyPriceRepository;
    private final AsyncPersistenceService asyncPersistenceService;

    public List<? extends AbstractPrice> adjustPricesForDateAndTimeframe(String ticker, LocalDate date, double priceMultiplier, StockTimeframe timeframe, String ohlc) {
        List<? extends AbstractPrice> pricesToUpdate = switch (timeframe) {
            case DAILY -> dailyPriceRepository.findByTickerAndDate(ticker, date);
            case WEEKLY -> weeklyPriceRepository.findWeeklyByTickerAndStartDate(ticker, date);
            case MONTHLY -> monthlyPriceRepository.findMonthlyByTickerAndStartDate(ticker, date);
            case QUARTERLY -> quarterlyPriceRepository.findQuarterlyByTickerAndStartDate(ticker, date);
            case YEARLY -> yearlyPriceRepository.findYearlyByTickerAndStartDate(ticker, date);
        };

        pricesToUpdate.forEach(dailyPrice -> updatePrices(dailyPrice, ohlc, priceMultiplier));
        asyncPersistenceService.partitionDataAndSave(pricesToUpdate, priceRepository);
        return pricesToUpdate;
    }

    private void updatePrices(AbstractPrice price, double priceMultiplier) {
        price.setOpen(Math.round((priceMultiplier * price.getOpen()) * 100.0) / 100.0);
        price.setHigh(Math.round((priceMultiplier * price.getHigh()) * 100.0) / 100.0);
        price.setLow(Math.round((priceMultiplier * price.getLow()) * 100.0) / 100.0);
        price.setClose(Math.round((priceMultiplier * price.getClose()) * 100.0) / 100.0);
    }

    private void updatePrices(AbstractPrice dailyPrice, String ohlc, double priceMultiplier) {
        ohlc = ohlc.toUpperCase();
        if (ohlc.contains("O")) dailyPrice.setOpen(Math.round((priceMultiplier * dailyPrice.getOpen()) * 100.0) / 100.0);
        if (ohlc.contains("H")) dailyPrice.setHigh(Math.round((priceMultiplier * dailyPrice.getHigh()) * 100.0) / 100.0);
        if (ohlc.contains("L")) dailyPrice.setLow(Math.round((priceMultiplier * dailyPrice.getLow()) * 100.0) / 100.0);
        if (ohlc.contains("C")) dailyPrice.setClose(Math.round((priceMultiplier * dailyPrice.getClose()) * 100.0) / 100.0);
    }
}
