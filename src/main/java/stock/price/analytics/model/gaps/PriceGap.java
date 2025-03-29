package stock.price.analytics.model.gaps;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;
import stock.price.analytics.model.BusinessEntity;
import stock.price.analytics.model.gaps.enums.GapStatus;
import stock.price.analytics.model.prices.enums.StockTimeframe;

import java.time.LocalDate;

@Getter
@Setter
@Entity
@DynamicUpdate
@NoArgsConstructor
@Table(name = "price_gaps")
public class PriceGap implements BusinessEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequence_prices_gaps")
    @SequenceGenerator(name = "seqGen_prices_gaps", sequenceName = "seq_prices_gaps")
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "ticker")
    private String ticker;

    @Column(name = "close")
    private double close;

    @Enumerated(EnumType.STRING)
    @Column(name = "timeframe")
    private StockTimeframe timeframe;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private GapStatus status;

    @Column(name = "date")
    private LocalDate date;

    public PriceGap(String ticker, double close, StockTimeframe timeframe, GapStatus status, LocalDate date) {
        this.ticker = ticker;
        this.close = close;
        this.timeframe = timeframe;
        this.status = status;
        this.date = date;
    }

    @Override
    public String toString() {
        return "PriceGap{" +
               "ticker='" + ticker + '\'' +
               ", close=" + close +
               ", timeframe=" + timeframe +
               ", status=" + status +
               ", date=" + date +
               '}';
    }
}