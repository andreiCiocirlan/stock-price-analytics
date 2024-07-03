package stock.price.analytics.model.prices.ohlc;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Entity
@Table(name = "monthly_prices")
@Getter
@Setter
@NoArgsConstructor
public class MonthlyPriceOHLC extends AbstractPriceOHLC {

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Column(name = "end_date")
    private LocalDate endDate;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "performance")
    private double performance;

    public MonthlyPriceOHLC(String ticker, LocalDate startDate, LocalDate endDate, CandleOHLC candleOHLC) {
        super(ticker, candleOHLC);
        this.startDate = startDate;
        this.endDate = endDate;
    }

    @Override
    public String toString() {
        return "Monthly_OHLC { " +
                " StartDate=" + startDate +
                " EndDate=" + endDate +
                super.toString();
    }

}