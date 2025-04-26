package stock.price.analytics.model.prices.highlow;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.tuple.MutablePair;
import org.hibernate.annotations.DynamicUpdate;
import org.springframework.format.annotation.DateTimeFormat;
import stock.price.analytics.model.BusinessEntity;
import stock.price.analytics.model.prices.highlow.enums.HighLowPeriod;
import stock.price.analytics.model.prices.ohlc.DailyPrice;
import stock.price.analytics.model.stocks.Stock;

import java.time.LocalDate;

@Setter
@Getter
@DynamicUpdate
@MappedSuperclass
@NoArgsConstructor
public abstract class HighLowForPeriod implements BusinessEntity {

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

    public HighLowForPeriod(String ticker, LocalDate startDate, LocalDate endDate, double low, double high) {
        this.ticker = ticker;
        this.startDate = startDate;
        this.endDate = endDate;
        this.setLow(low);
        this.setHigh(high);
    }

    public MutablePair<Boolean, Boolean> newHighLowOrEqual(DailyPrice dailyPriceImported) {
        boolean newHighLowFound = false;
        boolean equalHighLowFound = false;
        if (dailyPriceImported.getHigh() > this.getHigh()) {
            this.setHigh(dailyPriceImported.getHigh());
            newHighLowFound = true;
        } else if (dailyPriceImported.getClose() == this.getHigh()) {
            equalHighLowFound = true;
        }
        if (dailyPriceImported.getLow() < this.getLow()) {
            this.setLow(dailyPriceImported.getLow());
            newHighLowFound = true;
        } else if (dailyPriceImported.getClose() == this.getLow()) {
            equalHighLowFound = true;
        }

        return new MutablePair<>(newHighLowFound, equalHighLowFound);
    }

    public abstract void setLow(double low);
    public abstract void setHigh(double high);
    public abstract double getHigh();
    public abstract double getLow();
    public abstract HighLowPeriod getHighLowPeriod();
    public abstract void updateStock(Stock stock);

    @Override
    public String toString() {
        return STR."id=\{id}, ticker=\{ticker}, startDate=\{startDate}, endDate=\{endDate}";
    }
}
