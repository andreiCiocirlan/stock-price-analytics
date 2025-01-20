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

    @Column(name = "type")
    private String type;

    @Column(name = "low")
    private double low;

    @Column(name = "high")
    private double high;

    @Override
    public String toString() {
        return STR."FairValueGap{ticker='\{ticker}', type='\{type}', timeframe='\{timeframe}, date=\{date}, high=\{high}, low=\{low}'}";
    }
}