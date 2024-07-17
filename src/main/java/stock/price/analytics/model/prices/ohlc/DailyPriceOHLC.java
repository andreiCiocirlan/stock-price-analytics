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
    public String toString() {
        return STR."Daily_OHLC {  date=\{date} \{super.toString()}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DailyPriceOHLC that = (DailyPriceOHLC) o;
        return Objects.equals(date, that.date);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(date);
    }
}