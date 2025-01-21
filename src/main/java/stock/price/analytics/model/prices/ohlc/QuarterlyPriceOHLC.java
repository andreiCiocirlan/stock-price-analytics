package stock.price.analytics.model.prices.ohlc;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;
import stock.price.analytics.model.prices.enums.StockTimeframe;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

@Entity
@Table(name = "quarterly_prices")
@Getter
@Setter
@NoArgsConstructor
public class QuarterlyPriceOHLC extends AbstractPriceOHLC {

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Column(name = "end_date")
    private LocalDate endDate;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Column(name = "start_date")
    private LocalDate startDate;

    public QuarterlyPriceOHLC(String ticker, LocalDate startDate, LocalDate endDate, CandleOHLC candleOHLC) {
        super(ticker, candleOHLC);
        this.startDate = LocalDate.of(startDate.getYear(), startDate.getMonth().firstMonthOfQuarter().getValue(), 1);
        this.endDate = endDate.with(TemporalAdjusters.lastDayOfMonth());
    }

    public QuarterlyPriceOHLC(String ticker, LocalDate startDate, LocalDate endDate, double performance, CandleOHLC candleOHLC) {
        super(ticker, candleOHLC);
        this.startDate = LocalDate.of(startDate.getYear(), startDate.getMonth().firstMonthOfQuarter().getValue(), 1);
        this.endDate = endDate;
        this.setPerformance(performance);
    }

    public static QuarterlyPriceOHLC newFrom(DailyPriceOHLC dailyPrices, double previousClose) {
        return new QuarterlyPriceOHLC(
                dailyPrices.getTicker(),
                dailyPrices.getDate(),
                dailyPrices.getDate(),
                performanceFrom(dailyPrices, previousClose),
                new CandleOHLC(dailyPrices.getOpen(), dailyPrices.getHigh(), dailyPrices.getLow(), dailyPrices.getClose()));
    }

    @Override
    public StockTimeframe getTimeframe() {
        return StockTimeframe.QUARTERLY;
    }

    @Override
    public String toString() {
        return STR."Quarterly_OHLC {  StartDate=\{startDate} EndDate=\{endDate} \{super.toString()}";
    }

}