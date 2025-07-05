package stock.price.analytics.util.query.pricediscrepancy.impl;

import org.springframework.stereotype.Component;
import stock.price.analytics.model.prices.enums.StockTimeframe;
import stock.price.analytics.util.query.pricediscrepancy.PriceDiscrepancyQueryProvider;

@Component
public class PriceDiscrepancyQueryProviderImpl implements PriceDiscrepancyQueryProvider {

    @Override
    public String updateStocksWithOpeningPriceDiscrepancyFor(StockTimeframe timeframe) {
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
