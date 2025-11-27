package stock.price.analytics.model.demark;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;
import stock.price.analytics.model.demark.enums.TDType;
import stock.price.analytics.model.prices.enums.StockTimeframe;

import java.time.LocalDate;

@Getter
@Setter
@Entity
@DynamicUpdate
@NoArgsConstructor
@Table(name = "demark")
public class Demark {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequence_demark")
    @SequenceGenerator(name = "seqGen_demark", sequenceName = "seq_demark")
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "ticker")
    private String ticker;

    @Column(name = "date")
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(name = "timeframe")
    private StockTimeframe timeframe;

    @Column(name = "td")
    private int td;

    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private TDType type;

    public Demark(String ticker, LocalDate date, StockTimeframe timeframe, int td) {
        this.ticker = ticker;
        this.date = date;
        this.timeframe = timeframe;
        this.td = td;
    }

    @Override
    public String toString() {
        return "Demark{" +
               "timeframe=" + timeframe +
               ", td=" + td +
               ", date=" + date +
               ", ticker='" + ticker + '\'' +
               '}';
    }
}
