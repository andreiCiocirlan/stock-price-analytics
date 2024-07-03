package stock.price.analytics.model.prices.ohlc;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Getter
@Entity
@Table(name = "daily_prices")
@NoArgsConstructor
public class DailyPriceOHLC extends AbstractPriceOHLC {

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Column(name = "date")
    private LocalDate date;

    public DailyPriceOHLC(String ticker, LocalDate date, CandleOHLC candleOHLC) {
        super(ticker, candleOHLC);
        this.date = date;
    }

    @Override
    public String toString() {
        return "Daily_OHLC { " + super.toString();
    }

}