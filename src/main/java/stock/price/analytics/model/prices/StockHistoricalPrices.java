package stock.price.analytics.model.prices;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;


@Setter
@Getter
@NoArgsConstructor
@Entity
@Table(name = "stock_hist_prices")
public class StockHistoricalPrices implements PriceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequence_hist_prices")
    @SequenceGenerator(name = "seqGen_hist_prices", sequenceName = "seq_hist_prices")
    private Long id;

    @Column(name = "ticker")
    private String ticker;
    @Column(name = "low")
    private double low;
    @Column(name = "high")
    private double high;
    @Column(name = "open")
    private double open;
    @Column(name = "close")
    private double close;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Column(name = "date")
    private LocalDate date;

    public StockHistoricalPrices(String ticker, LocalDate date, double open, double high, double low, double close) {
        this.ticker = ticker;
        this.low = low;
        this.high = high;
        this.open = open;
        this.close = close;
        this.date = date;
    }

    @Override
    public String toString() {
        return "StockHistoricalPrices{" +
                "ticker='" + ticker + '\'' +
                ", low=" + low +
                ", high=" + high +
                ", open=" + open +
                ", close=" + close +
                ", date=" + date +
                '}';
    }
}