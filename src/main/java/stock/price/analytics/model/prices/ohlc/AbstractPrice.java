package stock.price.analytics.model.prices.ohlc;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;
import stock.price.analytics.model.BusinessEntity;
import stock.price.analytics.model.candlestick.CandleStickType;
import stock.price.analytics.model.gaps.FairValueGap;
import stock.price.analytics.model.prices.enums.StockTimeframe;
import stock.price.analytics.model.stocks.Stock;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Getter
@Setter
@DynamicUpdate
@MappedSuperclass
@NoArgsConstructor
public abstract class AbstractPrice implements BusinessEntity {

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
    public abstract void updateStock(Stock stock);

    public String compositeId() {
        return getTicker() + "_" + getTimeframe() + "_" + getStartDate().format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    public void updateFrom(AbstractPrice that) {
        this.setOpen(that.getOpen());
        this.setHigh(that.getHigh());
        this.setLow(that.getLow());
        this.setClose(that.getClose());
        this.setPerformance(that.getPerformance());
    }

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

    // Check if the price date is immediately after the FVG date based on the timeframe
    public boolean isImmediatelyAfter(FairValueGap fvg) {
        return (switch (this.getTimeframe()) {
            case DAILY -> this.getStartDate().minusDays(1);
            case WEEKLY -> this.getStartDate().minusWeeks(1);
            case MONTHLY -> this.getStartDate().minusMonths(1);
            case QUARTERLY -> this.getStartDate().minusMonths(3);
            case YEARLY -> this.getStartDate().minusYears(1);
        }).isEqual(fvg.getDate());
    }

    public CandleStickType toCandleStickType() {
        double bodySize = Math.abs(close - open);
        double upperWick = high - Math.max(open, close);
        double lowerWick = Math.min(open, close) - low;

        // Check for doji first
        if (bodySize < (high - low) * 0.05) { // Assuming a doji if body is less than 5% of the total range
            if (upperWick > lowerWick * 2) {
                return CandleStickType.GRAVESTONE_DOJI;
            } else if (lowerWick > upperWick * 2) {
                return CandleStickType.DRAGONFLY_DOJI;
            } else {
                return CandleStickType.DOJI;
            }
        }

        if (upperWick > bodySize * 2 && bodySize > 0) {
            return CandleStickType.INVERTED_HAMMER;
        }

        return CandleStickType.NONE;
    }

    @Override
    public String toString() {
        return STR."ticker=\{ticker}, O=\{open}, H=\{high}, L=\{low}, C=\{close}, P=\{performance}\{'}'}";
    }

}
