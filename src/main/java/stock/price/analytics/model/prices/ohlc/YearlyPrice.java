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
@Table(name = "yearly_prices")
@Getter
@Setter
@NoArgsConstructor
public class YearlyPrice extends AbstractPrice {

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Column(name = "end_date")
    private LocalDate endDate;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Column(name = "start_date")
    private LocalDate startDate;

    public YearlyPrice(String ticker, LocalDate date, CandleOHLC candleOHLC) {
        super(ticker, candleOHLC);
        setStartDateFrom(date);
        setEndDateFrom(date);
    }

    public YearlyPrice(String ticker, LocalDate date, double performance, CandleOHLC candleOHLC) {
        super(ticker, candleOHLC);
        setStartDateFrom(date);
        setEndDateFrom(date);
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
    public StockTimeframe getTimeframe() {
        return StockTimeframe.YEARLY;
    }

    @Override
    public void setStartDateFrom(LocalDate date) {
        startDate = date.with(TemporalAdjusters.firstDayOfYear());
    }

    @Override
    public void setEndDateFrom(LocalDate date) {
        endDate = date.with(TemporalAdjusters.lastDayOfYear());
    }

    @Override
    public String toString() {
        return STR."Yearly_OHLC {  StartDate=\{startDate} EndDate=\{endDate} \{super.toString()}";
    }

}