package stock.price.analytics.util;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import stock.price.analytics.model.prices.enums.StockTimeframe;

import java.time.LocalDate;

import static stock.price.analytics.util.StockDateUtils.isWithinSameTimeframe;

@Component
@RequiredArgsConstructor
public class ImportDateUtil {

    public static boolean isFirstImportFor(StockTimeframe timeframe, LocalDate previousImportDate) {
        return switch (timeframe) {
            case DAILY -> throw new IllegalStateException("Unexpected value DAILY");
            case WEEKLY, YEARLY, QUARTERLY, MONTHLY -> !isWithinSameTimeframe(LocalDate.now(), previousImportDate, timeframe);
        };
    }

}
