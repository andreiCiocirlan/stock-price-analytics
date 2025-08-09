package stock.price.analytics.model.prices.ohlc;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;
import org.springframework.format.annotation.DateTimeFormat;
import stock.price.analytics.model.BusinessEntity;
import stock.price.analytics.model.gaps.FairValueGap;
import stock.price.analytics.model.prices.enums.StockTimeframe;
import stock.price.analytics.model.prices.ohlc.enums.CandleStickType;
import stock.price.analytics.model.stocks.Stock;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;

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

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Column(name = "date")
    private LocalDate date;
    
    public AbstractPrice(String ticker, LocalDate date, CandleOHLC candleOHLC) {
        this.ticker = ticker;
        this.high = candleOHLC.high();
        this.low = candleOHLC.low();
        this.open = candleOHLC.open();
        this.close = candleOHLC.close();
        setDateFrom(date);
    }

    private void setDateFrom(LocalDate date) {
        switch (getTimeframe()) {
            case DAILY -> this.setDate(date);
            case WEEKLY -> this.setDate(date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)));
            case MONTHLY -> this.setDate(date.with(TemporalAdjusters.firstDayOfMonth()));
            case QUARTERLY -> this.setDate(LocalDate.of(date.getYear(), date.getMonth().firstMonthOfQuarter().getValue(), 1));
            case YEARLY -> this.setDate(date.with(TemporalAdjusters.firstDayOfYear()));
        }
    }

    public abstract StockTimeframe getTimeframe();

    public abstract void updateStock(Stock stock);

    public void convertFrom(DailyPrice dailyPrice, Double previousClose) {
        this.setClose(dailyPrice.getClose());
        this.setLow(Math.min(getLow(), dailyPrice.getLow()));
        this.setHigh(Math.max(getHigh(), dailyPrice.getHigh()));
        this.setPerformance(performanceFrom(dailyPrice, previousClose));
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
            case DAILY -> this.getDate().minusDays(1);
            case WEEKLY -> this.getDate().minusWeeks(1);
            case MONTHLY -> this.getDate().minusMonths(1);
            case QUARTERLY -> this.getDate().minusMonths(3);
            case YEARLY -> this.getDate().minusYears(1);
        }).isEqual(fvg.getDate());
    }

    public CandleStickType toCandleStickType() {
        double bodySize = Math.abs(close - open);
        double upperWick = high - Math.max(open, close);
        double lowerWick = Math.min(open, close) - low;
        double totalRange = high - low;

        // Check for doji first
        if (bodySize < (totalRange) * 0.05) { // Assuming a doji if body is less than 5% of the total range
            if (upperWick > lowerWick * 2) {
                return CandleStickType.GRAVESTONE_DOJI;
            } else if (lowerWick > upperWick * 2) {
                return CandleStickType.DRAGONFLY_DOJI;
            } else {
                return CandleStickType.DOJI;
            }
        }

        double epsilon = totalRange * 0.05; // small margin for hammer candles (closing at the lows/highs)
        double bodyThreshold = totalRange * 0.3;  // body smaller than 30% of range
        double wickThreshold = totalRange * 0.6;  // wick larger than 60% of range

        // inverted hammer (small body, long upper wick, close at the lows)
        if (lowerWick < bodyThreshold && upperWick > wickThreshold && bodySize < bodyThreshold) {
            if (Math.abs(close - low) < epsilon || Math.abs(open - low) < epsilon) {
                return CandleStickType.INVERTED_HAMMER;
            }
        }

        // hammer (small body, long lower wick, close at the highs)
        if (upperWick < bodyThreshold && lowerWick > 2 * bodySize && bodySize < bodyThreshold) {
            // close at the lows, within small margin
            if (Math.abs(close - high) < epsilon || Math.abs(open - high) < epsilon) {
                return CandleStickType.HAMMER;
            }
        }

        return CandleStickType.ANY;
    }

    public String getCompositeId() {
        return ticker + "_" + date.format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    @Override
    public String toString() {
        return STR."ticker=\{ticker}, date=\{date}, O=\{open}, H=\{high}, L=\{low}, C=\{close}, P=\{performance}\{'}'}";
    }

}
