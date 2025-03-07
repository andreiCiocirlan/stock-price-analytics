package stock.price.analytics.cache.util;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import stock.price.analytics.model.prices.enums.StockTimeframe;
import stock.price.analytics.service.DailyPricesService;

import java.time.LocalDate;

import static stock.price.analytics.util.StockDateUtils.isWithinSameTimeframe;
import static stock.price.analytics.util.TradingDateUtil.tradingDateNow;

@Component
@RequiredArgsConstructor
public class ImportDateUtil {

    private final DailyPricesService dailyPricesService;

    public boolean isFirstImportFor(StockTimeframe timeframe) {
        return switch (timeframe) {
            case DAILY -> throw new IllegalStateException("Unexpected value DAILY");
            case WEEKLY, YEARLY, QUARTERLY, MONTHLY -> {
                LocalDate previousImportDate = dailyPricesService.previousDailyPrices().getFirst().getDate();
                yield isWithinSameTimeframe(tradingDateNow(), previousImportDate, timeframe);
            }
        };
    }

}
