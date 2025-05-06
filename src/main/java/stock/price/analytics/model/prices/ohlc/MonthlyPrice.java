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
@Table(name = "monthly_prices")
@Getter
@Setter
@NoArgsConstructor
public class MonthlyPrice extends AbstractPrice {

    public MonthlyPrice(String ticker, LocalDate date, CandleOHLC candleOHLC) {
        super(ticker, date, candleOHLC);
    }

    public MonthlyPrice(String ticker, LocalDate date, double performance, CandleOHLC candleOHLC) {
        super(ticker, date, candleOHLC);
        this.setPerformance(performance);
    }

    public static MonthlyPrice newFrom(DailyPrice dailyPrices, double previousClose) {
        return new MonthlyPrice(
                dailyPrices.getTicker(),
                dailyPrices.getDate(),
                performanceFrom(dailyPrices, previousClose),
                new CandleOHLC(dailyPrices.getOpen(), dailyPrices.getHigh(), dailyPrices.getLow(), dailyPrices.getClose()));
    }

    @Override
    public void updateStock(Stock stock) {
        stock.setMonthlyOpen(this.getOpen());
        stock.setMonthlyHigh(this.getHigh());
        stock.setMonthlyLow(this.getLow());
        stock.setMonthlyPerformance(this.getPerformance());
    }

    @Override
    public StockTimeframe getTimeframe() {
        return StockTimeframe.MONTHLY;
    }

    @Override
    public String toString() {
        return STR."Monthly_OHLC { \{super.toString()}";
    }

}