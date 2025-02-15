package stock.price.analytics.model.fvg;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import stock.price.analytics.model.prices.PriceEntity;
import stock.price.analytics.model.prices.enums.FvgStatus;
import stock.price.analytics.model.prices.enums.FvgType;
import stock.price.analytics.model.prices.enums.StockTimeframe;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Getter
@Entity
@Table(name = "fvg")
@NoArgsConstructor
public class FairValueGap implements PriceEntity {

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
    private FvgStatus status;

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

    public FairValueGap(String ticker, StockTimeframe timeframe, LocalDate date, FvgType type, FvgStatus status, double low, double high) {
        this.ticker = ticker;
        this.timeframe = timeframe;
        this.date = date;
        this.type = type;
        this.status = status;
        this.low = low;
        this.high = high;
    }

    public FairValueGap(String ticker, StockTimeframe timeframe, LocalDate date, FvgType type, FvgStatus status, double low, double high, Double unfilledLow1, Double unfilledHigh1, Double unfilledLow2, Double unfilledHigh2) {
        this.ticker = ticker;
        this.timeframe = timeframe;
        this.date = date;
        this.type = type;
        this.status = status;
        this.low = low;
        this.high = high;
        this.unfilledLow1 = unfilledLow1;
        this.unfilledHigh1 = unfilledHigh1;
        this.unfilledLow2 = unfilledLow2;
        this.unfilledHigh2 = unfilledHigh2;
    }

    public String compositeId() {
        return getTicker() + "_" + getTimeframe() + "_" + date.format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    @Override
    public String toString() {
        return STR."FairValueGap{ticker='\{ticker}', status='\{status}', type='\{type}', timeframe='\{timeframe}, date=\{date}, high=\{high}, low=\{low}'}";
    }
}