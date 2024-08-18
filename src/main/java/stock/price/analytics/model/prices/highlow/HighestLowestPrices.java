package stock.price.analytics.model.prices.highlow;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "highest_lowest_prices")
public class HighestLowestPrices {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequence_highest_lowest")
    @SequenceGenerator(name = "seqGen_highest_lowest", sequenceName = "seq_highest_lowest")
    @Column(name = "id", nullable = false)
    private Long id;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Column(name = "start_date")
    private LocalDate start_date;   // marking the week for which highest/lowest prices are calculated for
    @Column(name = "ticker")
    private String ticker;
    @Column(name = "low")
    private double low;
    @Column(name = "high")
    private double high;
}
