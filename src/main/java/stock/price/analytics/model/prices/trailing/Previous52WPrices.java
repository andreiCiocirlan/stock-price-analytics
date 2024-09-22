package stock.price.analytics.model.prices.trailing;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;
import stock.price.analytics.model.prices.ohlc.CandleOHLC;
import stock.price.analytics.model.prices.ohlc.WeeklyPriceOHLC;

import java.time.LocalDate;

@Getter
@Setter
@Entity
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
                weeklyPrice.getStartDate(),
                weeklyPrice.getEndDate(),
                weeklyPrice.getPerformance(),
                new CandleOHLC(weeklyPrice.getOpen(), weeklyPrice.getHigh(), weeklyPrice.getLow(), weeklyPrice.getClose()));
    }

    @Override
    public String toString() {
        return STR."Prev52W {  StartDate=\{startDate} EndDate=\{endDate} ticker=\{ticker}, O=\{open}, H=\{high}, L=\{low}, C=\{close}, P=\{performance}\{'}'} ";
    }

}