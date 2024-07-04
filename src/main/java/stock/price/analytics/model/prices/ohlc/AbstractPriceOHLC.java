package stock.price.analytics.model.prices.ohlc;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import stock.price.analytics.model.prices.PriceEntity;

@Getter
@Setter
@MappedSuperclass
@NoArgsConstructor
public abstract class AbstractPriceOHLC  implements PriceEntity {

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

    public AbstractPriceOHLC(String ticker, CandleOHLC candleOHLC) {
        this.ticker = ticker;
        this.high = candleOHLC.high();
        this.low = candleOHLC.low();
        this.open = candleOHLC.open();
        this.close = candleOHLC.close();
    }

    public abstract double getPerformance();

    @Override
    public String toString() {
        return "ticker=" + ticker +
                ", Open=" + open +
                ", High=" + high +
                ", Low=" + low +
                ", Close=" + close +
                '}';
    }

}
