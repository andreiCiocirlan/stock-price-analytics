package stock.price.analytics.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import stock.price.analytics.model.prices.enums.StockTimeframe;
import stock.price.analytics.model.prices.ohlc.*;
import stock.price.analytics.repository.prices.PricesRepository;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

import static java.time.temporal.TemporalAdjusters.*;
import static stock.price.analytics.util.PartitionAndSavePriceEntityUtil.partitionDataAndSave;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockSplitAdjustPricesService {

    private final PricesRepository pricesRepository;

    public void adjustPricesFor(String ticker, LocalDate stockSplitDate, double priceMultiplier) {
        List<DailyPriceOHLC> dailyPricesToUpdate = pricesRepository.findByTickerAndDateLessThan(ticker, stockSplitDate);
        List<WeeklyPriceOHLC> weeklyPricesToUpdate = pricesRepository.findWeeklyByTickerAndStartDateBefore(ticker, stockSplitDate.with(previousOrSame(DayOfWeek.MONDAY)));
        List<MonthlyPriceOHLC> monthlyPricesToUpdate = pricesRepository.findMonthlyByTickerAndStartDateBefore(ticker, stockSplitDate.with(firstDayOfMonth()));
        List<QuarterlyPriceOHLC> quarterlyPricesToUpdate = pricesRepository.findQuarterlyByTickerAndStartDateBefore(ticker, LocalDate.of(stockSplitDate.getYear(), stockSplitDate.getMonth().firstMonthOfQuarter().getValue(), 1));
        List<YearlyPriceOHLC> yearlyPricesToUpdate = pricesRepository.findYearlyByTickerAndStartDateBefore(ticker, stockSplitDate.with(firstDayOfYear()));

        dailyPricesToUpdate.forEach(dailyPrice -> updatePrices(dailyPrice, priceMultiplier));
        weeklyPricesToUpdate.forEach(weeklyPrices -> updatePrices(weeklyPrices, priceMultiplier));
        monthlyPricesToUpdate.forEach(monthlyPrices -> updatePrices(monthlyPrices, priceMultiplier));
        quarterlyPricesToUpdate.forEach(quarterlyPrices -> updatePrices(quarterlyPrices, priceMultiplier));
        yearlyPricesToUpdate.forEach(yearlyPrices -> updatePrices(yearlyPrices, priceMultiplier));

        partitionDataAndSave(dailyPricesToUpdate, pricesRepository);
        partitionDataAndSave(weeklyPricesToUpdate, pricesRepository);
        partitionDataAndSave(monthlyPricesToUpdate, pricesRepository);
        partitionDataAndSave(quarterlyPricesToUpdate, pricesRepository);
        partitionDataAndSave(yearlyPricesToUpdate, pricesRepository);
    }

    public List<? extends AbstractPriceOHLC> adjustPricesForDateAndTimeframe(String ticker, LocalDate date, double priceMultiplier, StockTimeframe timeframe, String ohlc) {
        List<? extends AbstractPriceOHLC> pricesToUpdate = switch (timeframe) {
            case DAILY -> pricesRepository.findByTickerAndDate(ticker, date);
            case WEEKLY -> pricesRepository.findWeeklyByTickerAndStartDate(ticker, date);
            case MONTHLY -> pricesRepository.findMonthlyByTickerAndStartDate(ticker, date);
            case QUARTERLY -> pricesRepository.findQuarterlyByTickerAndStartDate(ticker, date);
            case YEARLY -> pricesRepository.findYearlyByTickerAndStartDate(ticker, date);
        };

        pricesToUpdate.forEach(dailyPrice -> updatePrices(dailyPrice, ohlc, priceMultiplier));
        log.info("{}", pricesToUpdate);
        pricesRepository.saveAll(pricesToUpdate);
        return pricesToUpdate;
    }

    private void updatePrices(AbstractPriceOHLC price, double priceMultiplier) {
        price.setOpen(Math.round((priceMultiplier * price.getOpen()) * 100.0) / 100.0);
        price.setHigh(Math.round((priceMultiplier * price.getHigh()) * 100.0) / 100.0);
        price.setLow(Math.round((priceMultiplier * price.getLow()) * 100.0) / 100.0);
        price.setClose(Math.round((priceMultiplier * price.getClose()) * 100.0) / 100.0);
    }

    private void updatePrices(AbstractPriceOHLC dailyPrice, String ohlc, double priceMultiplier) {
        ohlc = ohlc.toUpperCase();
        if (ohlc.contains("O")) dailyPrice.setOpen(Math.round((priceMultiplier * dailyPrice.getOpen()) * 100.0) / 100.0);
        if (ohlc.contains("H")) dailyPrice.setHigh(Math.round((priceMultiplier * dailyPrice.getHigh()) * 100.0) / 100.0);
        if (ohlc.contains("L")) dailyPrice.setLow(Math.round((priceMultiplier * dailyPrice.getLow()) * 100.0) / 100.0);
        if (ohlc.contains("C")) dailyPrice.setClose(Math.round((priceMultiplier * dailyPrice.getClose()) * 100.0) / 100.0);
    }
}
