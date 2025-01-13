package stock.price.analytics.model.prices.highlow;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;
import stock.price.analytics.model.prices.PriceEntity;
import stock.price.analytics.model.prices.ohlc.DailyPriceOHLC;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

@MappedSuperclass
@NoArgsConstructor
@Setter
@Getter
public abstract class HighLowForPeriod implements PriceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequence_high_low")
    @SequenceGenerator(name = "seqGen_high_low", sequenceName = "seq_high_low")
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "ticker")
    private String ticker;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Column(name = "end_date")
    private LocalDate endDate;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Column(name = "start_date")
    private LocalDate startDate;

    public HighLowForPeriod(String ticker, LocalDate startDate, LocalDate endDate) {
        this.ticker = ticker;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public boolean newHighLow(DailyPriceOHLC dailyPriceImported) {
        boolean newHighLowFound = false;

        startDate = dailyPriceImported.getDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        endDate = dailyPriceImported.getDate().with(TemporalAdjusters.nextOrSame(DayOfWeek.FRIDAY));
        if (dailyPriceImported.getHigh() > this.getHigh()) {
            this.setHigh(dailyPriceImported.getHigh());
            newHighLowFound = true;
        }
        if (dailyPriceImported.getLow() < this.getLow()) {
            this.setLow(dailyPriceImported.getLow());
            newHighLowFound = true;
        }

        return newHighLowFound;
    }

    public abstract void setLow(double low);
    public abstract void setHigh(double high);
    public abstract double getHigh();
    public abstract double getLow();

    @Override
    public String toString() {
        return STR."ticker=\{ticker}, startDate=\{startDate}, endDate=\{endDate}";
    }
}
