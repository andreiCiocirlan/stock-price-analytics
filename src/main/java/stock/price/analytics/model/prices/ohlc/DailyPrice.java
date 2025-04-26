package stock.price.analytics.model.prices.ohlc;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.springframework.format.annotation.DateTimeFormat;
import stock.price.analytics.model.prices.enums.StockTimeframe;
import stock.price.analytics.model.stocks.Stock;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

@Getter
@Entity
@Table(name = "daily_prices")
@NoArgsConstructor
public class DailyPrice extends AbstractPrice {

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Column(name = "date")
    private LocalDate date;

    public DailyPrice(String ticker, @NonNull LocalDate date, CandleOHLC candleOHLC) {
        super(ticker, candleOHLC);
        this.date = date;
    }

    public DailyPrice(String ticker, @NonNull LocalDate date, double performance, CandleOHLC candleOHLC) {
        super(ticker, candleOHLC);
        this.date = date;
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
    public LocalDate getStartDate() {
        return date;
    }

    @Override
    public LocalDate getEndDate() {
        return date;
    }

    @Override
    public StockTimeframe getTimeframe() {
        return StockTimeframe.DAILY;
    }

    public String getCompositeId() {
        return getTicker() + "_" + getDate().format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    @Override
    public void setStartDateFrom(LocalDate date) {
        throw new IllegalStateException("Unexpected setEndDateFrom in DailyPrice");
    }

    @Override
    public void setEndDateFrom(LocalDate date) {
        throw new IllegalStateException("Unexpected setEndDateFrom in DailyPrice");
    }

    @Override
    public String toString() {
        return STR."Daily_OHLC {  date=\{date} \{super.toString()}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DailyPrice that = (DailyPrice) o;

        return Objects.equals(getTicker(), that.getTicker()) &&
               Objects.equals(date, that.date) &&
               Double.compare(getOpen(), that.getOpen()) == 0 &&
               Double.compare(getHigh(), that.getHigh()) == 0 &&
               Double.compare(getLow(), that.getLow()) == 0 &&
               Double.compare(getClose(), that.getClose()) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getTicker(), date, getOpen(), getHigh(), getLow(), getClose());
    }
}