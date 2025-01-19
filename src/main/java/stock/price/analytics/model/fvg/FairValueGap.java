package stock.price.analytics.model.fvg;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import stock.price.analytics.model.prices.PriceEntity;

import java.time.LocalDate;

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

    @Column(name = "timeframe")
    private String timeframe;

    @Column(name = "date")
    private LocalDate date;

    @Column(name = "fvg_type")
    private String fvgType;

    @Column(name = "fvg_low")
    private double fvgLow;

    @Column(name = "fvg_high")
    private double fvgHigh;

    public FairValueGap(String ticker, String timeframe, LocalDate date, String fvgType, double fvgLow, double fvgHigh) {
        this.timeframe = timeframe;
        this.fvgType = fvgType;
        this.ticker = ticker;
        this.fvgLow = fvgLow;
        this.fvgHigh = fvgHigh;
        this.date = date;
    }

    @Override
    public String toString() {
        return STR."FairValueGap{fvgHigh=\{fvgHigh}, date=\{date}, fvgLow=\{fvgLow}, ticker='\{ticker}', fvgType='\{fvgType}', timeframe='\{timeframe}'}";
    }
}