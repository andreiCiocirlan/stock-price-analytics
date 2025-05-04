package stock.price.analytics.model.prices.ohlc;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;
import stock.price.analytics.model.prices.enums.StockTimeframe;
import stock.price.analytics.model.stocks.Stock;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

@Entity
@Table(name = "monthly_prices")
@Getter
@Setter
@NoArgsConstructor
public class MonthlyPrice extends AbstractPrice {

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Column(name = "start_date")
    private LocalDate startDate;

    public MonthlyPrice(String ticker, LocalDate date, CandleOHLC candleOHLC) {
        super(ticker, candleOHLC);
        setStartDateFrom(date);
    }

    public MonthlyPrice(String ticker, LocalDate date, double performance, CandleOHLC candleOHLC) {
        super(ticker, candleOHLC);
        setStartDateFrom(date);
        this.setPerformance(performance);
    }

    public static MonthlyPrice newFrom(DailyPrice dailyPrices, double previousClose) {
        return new MonthlyPrice(
                dailyPrices.getTicker(),
                dailyPrices.getDate(),
                performanceFrom(dailyPrices, previousClose),
                new CandleOHLC(dailyPrices.getOpen(), dailyPrices.getHigh(), dailyPrices.getLow(), dailyPrices.getClose()));
    }

    @Override
    public void updateStock(Stock stock) {
        stock.setMonthlyOpen(this.getOpen());
        stock.setMonthlyHigh(this.getHigh());
        stock.setMonthlyLow(this.getLow());
        stock.setMonthlyPerformance(this.getPerformance());
    }

    @Override
    public StockTimeframe getTimeframe() {
        return StockTimeframe.MONTHLY;
    }

    @Override
    public void setStartDateFrom(LocalDate date) {
        startDate = date.with(TemporalAdjusters.firstDayOfMonth());
    }

    @Override
    public String toString() {
        return STR."Monthly_OHLC { StartDate=\{startDate} \{super.toString()}";
    }

}