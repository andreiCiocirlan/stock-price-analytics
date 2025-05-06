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
@Table(name = "weekly_prices")
@Getter
@Setter
@NoArgsConstructor
public class WeeklyPrice extends AbstractPrice {

    public WeeklyPrice(String ticker, LocalDate date, CandleOHLC candleOHLC) {
        super(ticker, date, candleOHLC);
    }

    public WeeklyPrice(String ticker, LocalDate date, double performance, CandleOHLC candleOHLC) {
        super(ticker, date, candleOHLC);
        this.setPerformance(performance);
    }

    public static WeeklyPrice newFrom(DailyPrice dailyPrices, double previousClose) {
        return new WeeklyPrice(
                dailyPrices.getTicker(),
                dailyPrices.getDate(),
                performanceFrom(dailyPrices, previousClose),
                new CandleOHLC(dailyPrices.getOpen(), dailyPrices.getHigh(), dailyPrices.getLow(), dailyPrices.getClose()));
    }

    @Override
    public void updateStock(Stock stock) {
        stock.setWeeklyOpen(this.getOpen());
        stock.setWeeklyHigh(this.getHigh());
        stock.setWeeklyLow(this.getLow());
        stock.setWeeklyPerformance(this.getPerformance());
    }

    @Override
    public StockTimeframe getTimeframe() {
        return StockTimeframe.WEEKLY;
    }

    @Override
    public String toString() {
        return STR."Weekly_OHLC {  \{super.toString()}";
    }

}