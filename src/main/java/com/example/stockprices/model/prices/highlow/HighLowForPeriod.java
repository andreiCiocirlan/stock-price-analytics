package com.example.stockprices.model.prices.highlow;

import com.example.stockprices.model.prices.PriceEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@MappedSuperclass
@NoArgsConstructor
@Setter
@Getter
public abstract class HighLowForPeriod implements PriceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequence_high_low")
    @SequenceGenerator(name = "seqGen_high_low", sequenceName = "seq_high_low")
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "ticker")
    private String ticker;

    @Column(name = "weekly_close")
    private double weeklyClose;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Column(name = "week_start")
    private LocalDate weekStart;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Column(name = "week_end")
    private LocalDate weekEnd;

    public HighLowForPeriod(String ticker, double weeklyClose, LocalDate weekStart, LocalDate weekEnd) {
        this.ticker = ticker;
        this.weeklyClose = weeklyClose;
        this.weekStart = weekStart;
        this.weekEnd = weekEnd;
    }

    public abstract void setLow(double low);
    public abstract void setHigh(double high);

    @Override
    public String toString() {
        return  "ticker=" + ticker +
                ", weeklyClose=" + weeklyClose +
                ", weekStart=" + weekStart +
                ", weekEnd=" + weekEnd;
    }
}
