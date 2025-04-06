package stock.price.analytics.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

import static stock.price.analytics.util.TradingDateUtil.tradingDateNow;

@Service
@RequiredArgsConstructor
public class StockSplitService {

    private final PriceService priceService;
    private final HighLowForPeriodService highLowForPeriodService;
    private final FairValueGapService fairValueGapService;
    private final StockService stockService;

    public void splitAdjustFor(String ticker, LocalDate stockSplitDate, double priceMultiplier) {
        priceService.adjustPricesFor(ticker, stockSplitDate, priceMultiplier);
        highLowForPeriodService.saveAllHistoricalHighLowPrices(List.of(ticker), stockSplitDate);
        fairValueGapService.updateFVGPricesForStockSplit(ticker, stockSplitDate, priceMultiplier);

        // stockSplitDate within the last_updated week
        if (stockSplitDate.isAfter(tradingDateNow().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)))) {
            stockService.updateStockHigherTimeframePricesFor(ticker);
            stockService.updateHighLowForPeriodPrices(ticker);
            if (stockSplitDate.isEqual(tradingDateNow())) {
                stockService.updateStockDailyPricesFor(ticker);
            }
        }
    }

}
