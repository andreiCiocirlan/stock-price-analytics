package stock.price.analytics.util.importstatus;

import stock.price.analytics.model.prices.enums.StockTimeframe;

public interface ImportStatusQueryProvider {
    String checkImportStatusQueryFor(StockTimeframe timeframe, boolean checkFirstImport);
}
