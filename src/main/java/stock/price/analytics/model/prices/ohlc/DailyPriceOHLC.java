package stock.price.analytics.model.prices.ohlc;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.Objects;

@Getter
@Entity
@Table(name = "daily_prices")
@NoArgsConstructor
public class DailyPriceOHLC extends AbstractPriceOHLC {

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Column(name = "date")
    private LocalDate date;

    public DailyPriceOHLC(String ticker, @NonNull LocalDate date, CandleOHLC candleOHLC) {
        super(ticker, candleOHLC);
        this.date = date;
    }

    public DailyPriceOHLC(String ticker, @NonNull LocalDate date, double performance, CandleOHLC candleOHLC) {
        super(ticker, candleOHLC);
        this.date = date;
        this.setPerformance(performance);
    }

    @Override
    public LocalDate getStartDate() {
        return date;
    }

    @Override
    public LocalDate getEndDate() {
        return date;
    }

    // used for real-time import and update (create new daily price if dates do not match, update otherwise)
    public DailyPriceOHLC updateFrom(DailyPriceOHLC newPrice) {
        if (getDate().equals(newPrice.getDate())) {
            // Update properties of the existing object
            setOpen(newPrice.getOpen());
            setHigh(newPrice.getHigh());
            setLow(newPrice.getLow());
            setClose(newPrice.getClose());
            setPerformance(newPrice.getPerformance());
            return this;
        } else if (newPrice.getDate().isAfter(getDate())) { // newPrice must be newer compared to DB existing price (first daily import)
            return newPrice;
        }
        return this; // sometimes DB price is newer, default to existing price
    }

    @Override
    public String toString() {
        return STR."Daily_OHLC {  date=\{date} \{super.toString()}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DailyPriceOHLC that = (DailyPriceOHLC) o;

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