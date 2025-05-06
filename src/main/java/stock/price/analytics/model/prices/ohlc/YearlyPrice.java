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
@Table(name = "yearly_prices")
@Getter
@Setter
@NoArgsConstructor
public class YearlyPrice extends AbstractPrice {

    public YearlyPrice(String ticker, LocalDate date, CandleOHLC candleOHLC) {
        super(ticker, date, candleOHLC);
    }

    public YearlyPrice(String ticker, LocalDate date, double performance, CandleOHLC candleOHLC) {
        super(ticker, date, candleOHLC);
        this.setPerformance(performance);
    }

    public static YearlyPrice newFrom(DailyPrice dailyPrices, double previousClose) {
        return new YearlyPrice(
                dailyPrices.getTicker(),
                dailyPrices.getDate(),
                performanceFrom(dailyPrices, previousClose),
                new CandleOHLC(dailyPrices.getOpen(), dailyPrices.getHigh(), dailyPrices.getLow(), dailyPrices.getClose()));
    }

    @Override
    public void updateStock(Stock stock) {
        stock.setYearlyOpen(this.getOpen());
        stock.setYearlyHigh(this.getHigh());
        stock.setYearlyLow(this.getLow());
        stock.setYearlyPerformance(this.getPerformance());
    }

    @Override
    public StockTimeframe getTimeframe() {
        return StockTimeframe.YEARLY;
    }

    @Override
    public String toString() {
        return STR."Yearly_OHLC {  \{super.toString()}";
    }

}