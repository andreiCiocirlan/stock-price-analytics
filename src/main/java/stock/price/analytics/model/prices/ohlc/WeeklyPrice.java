package stock.price.analytics.model.prices.ohlc;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;
import stock.price.analytics.model.prices.enums.StockTimeframe;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

@Entity
@Table(name = "weekly_prices")
@Getter
@Setter
@NoArgsConstructor
public class WeeklyPrice extends AbstractPrice {

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Column(name = "end_date")
    private LocalDate endDate;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Column(name = "start_date")
    private LocalDate startDate;

    public WeeklyPrice(String ticker, LocalDate startDate, LocalDate endDate, CandleOHLC candleOHLC) {
        super(ticker, candleOHLC);
        this.startDate = startDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        this.endDate = endDate;
    }

    public WeeklyPrice(String ticker, LocalDate startDate, LocalDate endDate, double performance, CandleOHLC candleOHLC) {
        super(ticker, candleOHLC);
        this.startDate = startDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        this.endDate = endDate;
        this.setPerformance(performance);
    }

    public static WeeklyPrice newFrom(DailyPrice dailyPrices, double previousClose) {
        return new WeeklyPrice(
                dailyPrices.getTicker(),
                dailyPrices.getDate(),
                dailyPrices.getDate(),
                performanceFrom(dailyPrices, previousClose),
                new CandleOHLC(dailyPrices.getOpen(), dailyPrices.getHigh(), dailyPrices.getLow(), dailyPrices.getClose()));
    }

    @Override
    public StockTimeframe getTimeframe() {
        return StockTimeframe.WEEKLY;
    }

    @Override
    public String toString() {
        return STR."Weekly_OHLC {  StartDate=\{startDate} EndDate=\{endDate} \{super.toString()}";
    }

}