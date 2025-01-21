package stock.price.analytics.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import stock.price.analytics.model.prices.enums.StockTimeframe;
import stock.price.analytics.model.prices.ohlc.*;
import stock.price.analytics.repository.prices.PriceOHLCRepository;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

import static java.time.temporal.TemporalAdjusters.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockSplitAdjustPricesService {

    private final PriceOHLCRepository priceOHLCRepository;

    public void adjustPricesFor(String ticker, LocalDate stockSplitDate, double priceMultiplier) {
        List<DailyPriceOHLC> dailyPricesToUpdate = priceOHLCRepository.findByTickerAndDateLessThan(ticker, stockSplitDate);
        List<WeeklyPriceOHLC> weeklyPricesToUpdate = priceOHLCRepository.findWeeklyByTickerAndStartDateBefore(ticker, stockSplitDate.with(previousOrSame(DayOfWeek.MONDAY)));
        List<MonthlyPriceOHLC> monthlyPricesToUpdate = priceOHLCRepository.findMonthlyByTickerAndStartDateBefore(ticker, stockSplitDate.with(firstDayOfMonth()));
        List<MonthlyPriceOHLC> quarterlyPricesToUpdate = priceOHLCRepository.findQuarterlyByTickerAndStartDateBefore(ticker, LocalDate.of(stockSplitDate.getYear(), stockSplitDate.getMonth().firstMonthOfQuarter().getValue(), 1));
        List<YearlyPriceOHLC> yearlyPricesToUpdate = priceOHLCRepository.findYearlyByTickerAndStartDateBefore(ticker, stockSplitDate.with(firstDayOfYear()));

        dailyPricesToUpdate.forEach(dailyPriceOHLC -> updatePrices(dailyPriceOHLC, priceMultiplier));
        weeklyPricesToUpdate.forEach(weeklyPriceOHLC -> updatePrices(weeklyPriceOHLC, priceMultiplier));
        monthlyPricesToUpdate.forEach(monthlyPriceOHLC -> updatePrices(monthlyPriceOHLC, priceMultiplier));
        quarterlyPricesToUpdate.forEach(quarterlyPriceOHLC -> updatePrices(quarterlyPriceOHLC, priceMultiplier));
        yearlyPricesToUpdate.forEach(yearlyPriceOHLC -> updatePrices(yearlyPriceOHLC, priceMultiplier));

        priceOHLCRepository.saveAll(dailyPricesToUpdate);
        priceOHLCRepository.saveAll(weeklyPricesToUpdate);
        priceOHLCRepository.saveAll(monthlyPricesToUpdate);
        priceOHLCRepository.saveAll(quarterlyPricesToUpdate);
        priceOHLCRepository.saveAll(yearlyPricesToUpdate);
    }

    public List<? extends AbstractPriceOHLC> adjustPricesForDateAndTimeframe(String ticker, LocalDate date, double priceMultiplier, StockTimeframe timeframe, String ohlc) {
        List<? extends AbstractPriceOHLC> pricesToUpdate = switch (timeframe) {
            case DAILY -> priceOHLCRepository.findByTickerAndDate(ticker, date);
            case WEEKLY -> priceOHLCRepository.findWeeklyByTickerAndStartDate(ticker, date);
            case MONTHLY -> priceOHLCRepository.findMonthlyByTickerAndStartDate(ticker, date);
            case QUARTERLY -> priceOHLCRepository.findQuarterlyByTickerAndStartDate(ticker, date);
            case YEARLY -> priceOHLCRepository.findYearlyByTickerAndStartDate(ticker, date);
        };

        pricesToUpdate.forEach(dailyPriceOHLC -> updatePrices(dailyPriceOHLC, ohlc, priceMultiplier));
        log.info("{}", pricesToUpdate);
        priceOHLCRepository.saveAll(pricesToUpdate);
        return pricesToUpdate;
    }

    private void updatePrices(AbstractPriceOHLC priceOHLC, double priceMultiplier) {
        priceOHLC.setOpen(Math.round((priceMultiplier * priceOHLC.getOpen()) * 100.0) / 100.0);
        priceOHLC.setHigh(Math.round((priceMultiplier * priceOHLC.getHigh()) * 100.0) / 100.0);
        priceOHLC.setLow(Math.round((priceMultiplier * priceOHLC.getLow()) * 100.0) / 100.0);
        priceOHLC.setClose(Math.round((priceMultiplier * priceOHLC.getClose()) * 100.0) / 100.0);
    }

    private void updatePrices(AbstractPriceOHLC dailyPriceOHLC, String ohlc, double priceMultiplier) {
        ohlc = ohlc.toUpperCase();
        if (ohlc.contains("O")) dailyPriceOHLC.setOpen(Math.round((priceMultiplier * dailyPriceOHLC.getOpen()) * 100.0) / 100.0);
        if (ohlc.contains("H")) dailyPriceOHLC.setHigh(Math.round((priceMultiplier * dailyPriceOHLC.getHigh()) * 100.0) / 100.0);
        if (ohlc.contains("L")) dailyPriceOHLC.setLow(Math.round((priceMultiplier * dailyPriceOHLC.getLow()) * 100.0) / 100.0);
        if (ohlc.contains("C")) dailyPriceOHLC.setClose(Math.round((priceMultiplier * dailyPriceOHLC.getClose()) * 100.0) / 100.0);
    }
}
