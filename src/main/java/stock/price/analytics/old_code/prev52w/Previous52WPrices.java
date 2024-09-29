package stock.price.analytics.old_code.prev52w;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;
import stock.price.analytics.model.prices.ohlc.CandleOHLC;
import stock.price.analytics.model.prices.ohlc.WeeklyPriceOHLC;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "prev_52w")
public class Previous52WPrices {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequence_prices")
    @SequenceGenerator(name = "seqGen_prices", sequenceName = "seq_prices")
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "ticker")
    private String ticker;

    @Column(name = "high")
    private double high;

    @Column(name = "low")
    private double low;

    @Column(name = "open")
    private double open;

    @Column(name = "close")
    private double close;

    @Column(name = "performance")
    private double performance;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Column(name = "end_date")
    private LocalDate endDate;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Column(name = "start_date")
    private LocalDate startDate;

    public Previous52WPrices(String ticker, LocalDate startDate, LocalDate endDate, double performance, CandleOHLC candleOHLC) {
        this.ticker = ticker;
        this.high = candleOHLC.high();
        this.low = candleOHLC.low();
        this.open = candleOHLC.open();
        this.close = candleOHLC.close();
        this.startDate = startDate;
        this.endDate = endDate;
        this.setPerformance(performance);
    }

    public static Previous52WPrices newFrom(WeeklyPriceOHLC weeklyPrice) {
        return new Previous52WPrices(
                weeklyPrice.getTicker(),
                weeklyPrice.getStartDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),
                weeklyPrice.getEndDate().with(TemporalAdjusters.nextOrSame(DayOfWeek.FRIDAY)),
                weeklyPrice.getPerformance(),
                new CandleOHLC(weeklyPrice.getOpen(), weeklyPrice.getHigh(), weeklyPrice.getLow(), weeklyPrice.getClose()));
    }

    public void updateFrom(WeeklyPriceOHLC weeklyPrice) {
        this.setOpen(weeklyPrice.getOpen());
        this.setHigh(weeklyPrice.getHigh());
        this.setLow(weeklyPrice.getLow());
        this.setClose(weeklyPrice.getClose());
        this.setPerformance(weeklyPrice.getPerformance());
        this.setStartDate(weeklyPrice.getStartDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)));
        this.setEndDate(weeklyPrice.getEndDate().with(TemporalAdjusters.nextOrSame(DayOfWeek.FRIDAY)));
    }

    @Override
    public String toString() {
        return STR."Prev52W {  StartDate=\{startDate} EndDate=\{endDate} ticker=\{ticker}, O=\{open}, H=\{high}, L=\{low}, C=\{close}, P=\{performance}\{'}'} ";
    }

}