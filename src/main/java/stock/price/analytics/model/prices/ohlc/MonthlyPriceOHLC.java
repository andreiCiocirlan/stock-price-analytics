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

    public MonthlyPriceOHLC(String ticker, LocalDate startDate, LocalDate endDate, CandleOHLC candleOHLC) {
        super(ticker, candleOHLC);
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public MonthlyPriceOHLC(String ticker, LocalDate startDate, LocalDate endDate, double performance, CandleOHLC candleOHLC) {
        super(ticker, candleOHLC);
        this.startDate = startDate;
        this.endDate = endDate;
        this.setPerformance(performance);
    }

    public static MonthlyPriceOHLC newFrom(DailyPriceOHLC dailyPrices, double previousClose) {
        return new MonthlyPriceOHLC(
                dailyPrices.getTicker(),
                dailyPrices.getDate(),
                dailyPrices.getDate(),
                performanceFrom(dailyPrices, previousClose),
                new CandleOHLC(dailyPrices.getOpen(), dailyPrices.getHigh(), dailyPrices.getLow(), dailyPrices.getClose()));
    }

    @Override
    public String toString() {
        return STR."Monthly_OHLC { StartDate=\{startDate} EndDate=\{endDate} \{super.toString()}";
    }

}