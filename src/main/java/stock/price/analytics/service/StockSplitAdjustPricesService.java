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
import static stock.price.analytics.util.PartitionAndSavePriceEntityUtil.partitionDataAndSave;

@Service
@RequiredArgsConstructor
public class StockSplitAdjustPricesService {

    private final PricesRepository pricesRepository;
    private final DailyPricesRepository dailyPricesRepository;
    private final WeeklyPricesRepository weeklyPricesRepository;
    private final MonthlyPricesRepository monthlyPricesRepository;
    private final QuarterlyPricesRepository quarterlyPricesRepository;
    private final YearlyPricesRepository yearlyPricesRepository;

    public void adjustPricesFor(String ticker, LocalDate stockSplitDate, double priceMultiplier) {
        List<DailyPrice> dailyPricesToUpdate = dailyPricesRepository.findByTickerAndDateLessThan(ticker, stockSplitDate);
        List<WeeklyPrice> weeklyPricesToUpdate = weeklyPricesRepository.findWeeklyByTickerAndStartDateBefore(ticker, stockSplitDate.with(previousOrSame(DayOfWeek.MONDAY)));
        List<MonthlyPrice> monthlyPricesToUpdate = monthlyPricesRepository.findMonthlyByTickerAndStartDateBefore(ticker, stockSplitDate.with(firstDayOfMonth()));
        List<QuarterlyPrice> quarterlyPricesToUpdate = quarterlyPricesRepository.findQuarterlyByTickerAndStartDateBefore(ticker, LocalDate.of(stockSplitDate.getYear(), stockSplitDate.getMonth().firstMonthOfQuarter().getValue(), 1));
        List<YearlyPrice> yearlyPricesToUpdate = yearlyPricesRepository.findYearlyByTickerAndStartDateBefore(ticker, stockSplitDate.with(firstDayOfYear()));

        dailyPricesToUpdate.forEach(dailyPrice -> updatePrices(dailyPrice, priceMultiplier));
        weeklyPricesToUpdate.forEach(weeklyPrices -> updatePrices(weeklyPrices, priceMultiplier));
        monthlyPricesToUpdate.forEach(monthlyPrices -> updatePrices(monthlyPrices, priceMultiplier));
        quarterlyPricesToUpdate.forEach(quarterlyPrices -> updatePrices(quarterlyPrices, priceMultiplier));
        yearlyPricesToUpdate.forEach(yearlyPrices -> updatePrices(yearlyPrices, priceMultiplier));

        partitionDataAndSave(dailyPricesToUpdate, dailyPricesRepository);
        partitionDataAndSave(weeklyPricesToUpdate, weeklyPricesRepository);
        partitionDataAndSave(monthlyPricesToUpdate, monthlyPricesRepository);
        partitionDataAndSave(quarterlyPricesToUpdate, quarterlyPricesRepository);
        partitionDataAndSave(yearlyPricesToUpdate, yearlyPricesRepository);
    }

    public List<? extends AbstractPrice> adjustPricesForDateAndTimeframe(String ticker, LocalDate date, double priceMultiplier, StockTimeframe timeframe, String ohlc) {
        List<? extends AbstractPrice> pricesToUpdate = switch (timeframe) {
            case DAILY -> dailyPricesRepository.findByTickerAndDate(ticker, date);
            case WEEKLY -> weeklyPricesRepository.findWeeklyByTickerAndStartDate(ticker, date);
            case MONTHLY -> monthlyPricesRepository.findMonthlyByTickerAndStartDate(ticker, date);
            case QUARTERLY -> quarterlyPricesRepository.findQuarterlyByTickerAndStartDate(ticker, date);
            case YEARLY -> yearlyPricesRepository.findYearlyByTickerAndStartDate(ticker, date);
        };

        pricesToUpdate.forEach(dailyPrice -> updatePrices(dailyPrice, ohlc, priceMultiplier));
        partitionDataAndSave(pricesToUpdate, pricesRepository);
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
