package stock.price.analytics.util;

import stock.price.analytics.model.prices.enums.StockTimeframe;

import java.util.List;
import java.util.stream.Collectors;

public final class QueryUtil {




    public static String updateStocksWithOpeningPriceDiscrepancyFor(StockTimeframe timeframe) {
        String prefix = timeframe.stockPrefix();
        String dbTable = timeframe.dbTableOHLC();
        String intervalPeriod = timeframe.toIntervalPeriod();
        return STR."""
                WITH discrepancies AS (
                SELECT s.ticker, p.open
                FROM public.stocks s
                JOIN \{dbTable} p ON p.date = DATE_TRUNC('\{intervalPeriod}', s.last_updated) AND p.ticker = s.ticker
                WHERE s.delisted_date IS NULL AND (p.open <> s.\{prefix}open)
                )
                UPDATE stocks s SET \{prefix}open = dscr.open
                FROM discrepancies dscr
                WHERE s.ticker = dscr.ticker;
                """;
    }
}
