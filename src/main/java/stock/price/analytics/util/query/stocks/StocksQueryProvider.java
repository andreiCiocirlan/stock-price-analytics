package stock.price.analytics.util.query.stocks;

import stock.price.analytics.model.prices.enums.StockTimeframe;

import java.time.LocalDate;

public interface StocksQueryProvider {

    String findMostExtendedAbove200SMA(StockTimeframe timeframe, LocalDate tradingDate, Double cfdMargin);
}
