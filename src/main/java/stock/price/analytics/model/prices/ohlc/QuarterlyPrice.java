package stock.price.analytics.model.prices.ohlc;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import stock.price.analytics.model.prices.enums.StockTimeframe;
import stock.price.analytics.model.stocks.Stock;

import java.time.LocalDate;

@Entity
@Table(name = "quarterly_prices")
@Getter
@Setter
@NoArgsConstructor
public class QuarterlyPrice extends AbstractPrice {

    public QuarterlyPrice(String ticker, LocalDate date, CandleOHLC candleOHLC) {
        super(ticker, date, candleOHLC);
    }

    public QuarterlyPrice(String ticker, LocalDate date, double performance, CandleOHLC candleOHLC) {
        super(ticker, date, candleOHLC);
        this.setPerformance(performance);
    }

    public static QuarterlyPrice newFrom(DailyPrice dailyPrices, double previousClose) {
        return new QuarterlyPrice(
                dailyPrices.getTicker(),
                dailyPrices.getDate(),
                performanceFrom(dailyPrices, previousClose),
                new CandleOHLC(dailyPrices.getOpen(), dailyPrices.getHigh(), dailyPrices.getLow(), dailyPrices.getClose()));
    }

    @Override
    public void updateStock(Stock stock) {
        stock.setQuarterlyOpen(this.getOpen());
        stock.setQuarterlyHigh(this.getHigh());
        stock.setQuarterlyLow(this.getLow());
        stock.setQuarterlyPerformance(this.getPerformance());
    }

    @Override
    public StockTimeframe getTimeframe() {
        return StockTimeframe.QUARTERLY;
    }

    @Override
    public String toString() {
        return STR."Quarterly_OHLC {  \{super.toString()}";
    }

}