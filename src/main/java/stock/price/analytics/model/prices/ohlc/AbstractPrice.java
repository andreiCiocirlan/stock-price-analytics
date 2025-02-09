package stock.price.analytics.model.prices.ohlc;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import stock.price.analytics.model.prices.PriceEntity;
import stock.price.analytics.model.prices.enums.StockTimeframe;

import java.time.LocalDate;

@Getter
@Setter
@MappedSuperclass
@NoArgsConstructor
public abstract class AbstractPrice implements PriceEntity {

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

    @Column(name = "performance")
    private double performance;

    public AbstractPrice(String ticker, CandleOHLC candleOHLC) {
        this.ticker = ticker;
        this.high = candleOHLC.high();
        this.low = candleOHLC.low();
        this.open = candleOHLC.open();
        this.close = candleOHLC.close();
    }

    public abstract LocalDate getStartDate();
    public abstract LocalDate getEndDate();
    public abstract void setStartDateFrom(LocalDate date);
    public abstract void setEndDateFrom(LocalDate date);
    public abstract StockTimeframe getTimeframe();

    public AbstractPrice convertFrom(DailyPrice dailyPrice, Double previousClose) {
        this.setClose(dailyPrice.getClose());
        this.setLow(Math.min(getLow(), dailyPrice.getLow()));
        this.setHigh(Math.max(getHigh(), dailyPrice.getHigh()));
        this.setPerformance(performanceFrom(dailyPrice, previousClose));

        return this;
    }

    protected static double performanceFrom(DailyPrice dailyPrice, Double previousClose) {
        double currentClose = dailyPrice.getClose();
        double performance;
        if (previousClose != null) {
            performance = Math.round((((currentClose - previousClose) / previousClose) * 100) * 100) / 100.0;
        } else { // first week, month, year since IPO
            double currentOpen = dailyPrice.getOpen();
            performance = Math.round((((currentClose - currentOpen) / currentOpen) * 100) * 100) / 100.0;
        }
        return performance;
    }

    @Override
    public String toString() {
        return STR."ticker=\{ticker}, O=\{open}, H=\{high}, L=\{low}, C=\{close}, P=\{performance}\{'}'}";
    }

}
