package stock.price.analytics.model.prices.ohlc;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import stock.price.analytics.model.prices.enums.StockTimeframe;
import stock.price.analytics.model.stocks.Stock;

import java.time.LocalDate;
import java.util.Objects;

@Getter
@Entity
@Table(name = "daily_prices")
@NoArgsConstructor
public class DailyPrice extends AbstractPrice {

    public DailyPrice(String ticker, @NonNull LocalDate date, CandleOHLC candleOHLC) {
        super(ticker, date, candleOHLC);
    }

    public DailyPrice(String ticker, @NonNull LocalDate date, double performance, CandleOHLC candleOHLC) {
        super(ticker, date, candleOHLC);
        this.setPerformance(performance);
    }

    public static AbstractPrice newFrom(DailyPrice dailyPrice, double previousClose) {
        return new DailyPrice(
                dailyPrice.getTicker(),
                dailyPrice.getDate(),
                performanceFrom(dailyPrice, previousClose),
                new CandleOHLC(dailyPrice.getOpen(), dailyPrice.getHigh(), dailyPrice.getLow(), dailyPrice.getClose()));
    }

    @Override
    public void updateStock(Stock stock) {
        stock.setDailyOpen(this.getOpen());
        stock.setDailyHigh(this.getHigh());
        stock.setDailyLow(this.getLow());
        stock.setClose(this.getClose());
        stock.setDailyPerformance(this.getPerformance());
        stock.setLastUpdated(this.getDate());
    }

    @Override
    public StockTimeframe getTimeframe() {
        return StockTimeframe.DAILY;
    }

    @Override
    public String toString() {
        return STR."Daily_OHLC {  \{super.toString()}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DailyPrice that = (DailyPrice) o;

        return Objects.equals(getTicker(), that.getTicker()) &&
               Objects.equals(getDate(), that.getDate()) &&
               Double.compare(getOpen(), that.getOpen()) == 0 &&
               Double.compare(getHigh(), that.getHigh()) == 0 &&
               Double.compare(getLow(), that.getLow()) == 0 &&
               Double.compare(getClose(), that.getClose()) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getTicker(), getDate(), getOpen(), getHigh(), getLow(), getClose());
    }
}