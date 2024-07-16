package stock.price.analytics.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import stock.price.analytics.model.prices.ohlc.*;
import stock.price.analytics.repository.prices.PriceOHLCRepository;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SplitAdjustPricesService {

    private final PriceOHLCRepository priceOHLCRepository;

    public void adjustPricesFor(String ticker, LocalDate stockSplitDate, double priceMultiplier) {
        List<DailyPriceOHLC> dailyPricesToUpdate = priceOHLCRepository.findByTickerAndDateLessThanEqual(ticker, stockSplitDate);
        List<WeeklyPriceOHLC> weeklyPricesToUpdate = priceOHLCRepository.findWeeklyByTickerAndStartDateBefore(ticker, stockSplitDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)));
        List<MonthlyPriceOHLC> monthlyPricesToUpdate = priceOHLCRepository.findMonthlyByTickerAndStartDateBefore(ticker, stockSplitDate.with(TemporalAdjusters.firstDayOfMonth()));
        List<YearlyPriceOHLC> yearlyPricesToUpdate = priceOHLCRepository.findYearlyByTickerAndStartDateBefore(ticker, stockSplitDate.with(TemporalAdjusters.firstDayOfYear()));

        dailyPricesToUpdate.forEach(dailyPriceOHLC -> updatePrices(dailyPriceOHLC, priceMultiplier));
        weeklyPricesToUpdate.forEach(weeklyPriceOHLC -> updatePrices(weeklyPriceOHLC, priceMultiplier));
        monthlyPricesToUpdate.forEach(monthlyPriceOHLC -> updatePrices(monthlyPriceOHLC, priceMultiplier));
        yearlyPricesToUpdate.forEach(yearlyPriceOHLC -> updatePrices(yearlyPriceOHLC, priceMultiplier));

        priceOHLCRepository.saveAll(dailyPricesToUpdate);
        priceOHLCRepository.saveAll(weeklyPricesToUpdate);
        priceOHLCRepository.saveAll(monthlyPricesToUpdate);
        priceOHLCRepository.saveAll(yearlyPricesToUpdate);
    }

    private void updatePrices(AbstractPriceOHLC dailyPriceOHLC, double priceMultiplier) {
        dailyPriceOHLC.setClose(Math.round((priceMultiplier * dailyPriceOHLC.getClose()) * 100.0) / 100.0);
        dailyPriceOHLC.setOpen(Math.round((priceMultiplier * dailyPriceOHLC.getOpen()) * 100.0) / 100.0);
        dailyPriceOHLC.setLow(Math.round((priceMultiplier * dailyPriceOHLC.getLow()) * 100.0) / 100.0);
        dailyPriceOHLC.setHigh(Math.round((priceMultiplier * dailyPriceOHLC.getHigh()) * 100.0) / 100.0);
    }
}
