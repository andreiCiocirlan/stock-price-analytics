package stock.price.analytics.model.gaps;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;
import stock.price.analytics.model.BusinessEntity;
import stock.price.analytics.model.gaps.enums.GapStatus;
import stock.price.analytics.model.prices.enums.StockTimeframe;
import stock.price.analytics.model.gaps.enums.FvgType;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

@Getter
@Entity
@DynamicUpdate
@Table(name = "fvg")
@NoArgsConstructor
public class FairValueGap implements BusinessEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequence_fvg")
    @SequenceGenerator(name = "seqGen_fvg", sequenceName = "seq_fvg")
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "ticker")
    private String ticker;

    @Enumerated(EnumType.STRING)
    @Column(name = "timeframe")
    private StockTimeframe timeframe;

    @Column(name = "date")
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private FvgType type;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private GapStatus status;

    @Setter
    @Column(name = "low")
    private double low;

    @Setter
    @Column(name = "high")
    private double high;

    // Track unfilled portions
    @Setter
    @Column(name = "unfilled_low1")
    private Double unfilledLow1;

    @Setter
    @Column(name = "unfilled_high1")
    private Double unfilledHigh1;

    @Setter
    @Column(name = "unfilled_low2")
    private Double unfilledLow2;

    @Setter
    @Column(name = "unfilled_high2")
    private Double unfilledHigh2;

    public FairValueGap(@NonNull FairValueGap fvg) {
        this.id = fvg.getId();
        this.ticker = fvg.getTicker();
        this.timeframe = fvg.getTimeframe();
        this.date = fvg.getDate();
        this.type = fvg.getType();
        this.status = fvg.getStatus();
        this.low = fvg.getLow();
        this.high = fvg.getHigh();
        this.unfilledLow1 = fvg.getUnfilledLow1();
        this.unfilledHigh1 = fvg.getUnfilledHigh1();
        this.unfilledLow2 = fvg.getUnfilledLow2();
        this.unfilledHigh2 = fvg.getUnfilledHigh2();
    }

    public FairValueGap(String ticker, StockTimeframe timeframe, LocalDate date, FvgType type, GapStatus status, double low, double high) {
        this.ticker = ticker;
        this.timeframe = timeframe;
        this.date = date;
        this.type = type;
        this.status = status;
        this.low = low;
        this.high = high;
    }


    public String compositeId() {
        return getTicker() + "_" + getTimeframe() + "_" + date.format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FairValueGap that = (FairValueGap) o;
        return Double.compare(low, that.low) == 0 &&
               Double.compare(high, that.high) == 0 &&
               Objects.equals(ticker, that.ticker) &&
               timeframe == that.timeframe &&
               Objects.equals(date, that.date) &&
               type == that.type &&
               status == that.status &&
               Objects.equals(unfilledLow1, that.unfilledLow1) &&
               Objects.equals(unfilledHigh1, that.unfilledHigh1) &&
               Objects.equals(unfilledLow2, that.unfilledLow2) &&
               Objects.equals(unfilledHigh2, that.unfilledHigh2);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getTicker(), getTimeframe(), getDate(), getType(), getStatus(), getLow(), getHigh(), getUnfilledLow1(), getUnfilledHigh1(), getUnfilledLow2(), getUnfilledHigh2());
    }

    @Override
    public String toString() {
        return "FairValueGap{" +
               "ticker='" + ticker + '\'' +
               ", timeframe=" + timeframe +
               ", date=" + date +
               ", type=" + type +
               ", status=" + status +
               ", low=" + low +
               ", high=" + high +
               ", unfilledLow1=" + unfilledLow1 +
               ", unfilledHigh1=" + unfilledHigh1 +
               ", unfilledLow2=" + unfilledLow2 +
               ", unfilledHigh2=" + unfilledHigh2 +
               '}';
    }
}