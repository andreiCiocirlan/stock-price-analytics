package com.example.stockprices.model.prices;

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
@Table(name = "stock_weekly_hist_prices")
public class StockWeeklyHistoricalPrices implements PriceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequence_weekly_hist_prices")
    @SequenceGenerator(name = "seqGen_weekly_hist_prices", sequenceName = "seq_weekly_hist_prices")
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
    @Column(name = "week_start")
    private LocalDate weekStart;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Column(name = "week_end")
    private LocalDate weekEnd;

    public StockWeeklyHistoricalPrices(String ticker, LocalDate weekStart, LocalDate weekEnd, double open, double high, double low, double close) {
        this.ticker = ticker;
        this.low = low;
        this.high = high;
        this.open = open;
        this.close = close;
        this.weekStart = weekStart;
        this.weekEnd = weekEnd;
    }

    @Override
    public String toString() {
        return "StockWeeklyHistoricalPrices{" +
                "ticker='" + ticker + '\'' +
                ", low=" + low +
                ", open=" + open +
                ", high=" + high +
                ", close=" + close +
                ", weekStart=" + weekStart +
                ", weekEnd=" + weekEnd +
                '}';
    }
}
