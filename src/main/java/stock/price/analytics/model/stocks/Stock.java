package stock.price.analytics.model.stocks;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;
import stock.price.analytics.model.prices.PriceEntity;
import stock.price.analytics.model.prices.highlow.HighLow4w;
import stock.price.analytics.model.prices.highlow.HighLow52Week;
import stock.price.analytics.model.prices.highlow.HighLowForPeriod;
import stock.price.analytics.model.prices.highlow.HighestLowestPrices;
import stock.price.analytics.model.prices.ohlc.DailyPriceOHLC;
import stock.price.analytics.model.prices.ohlc.MonthlyPriceOHLC;
import stock.price.analytics.model.prices.ohlc.WeeklyPriceOHLC;
import stock.price.analytics.model.prices.ohlc.YearlyPriceOHLC;

import java.time.LocalDate;
import java.util.Objects;

@Setter
@Getter
@NoArgsConstructor
@Entity
@Table(name = "stocks")
public class Stock implements PriceEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "stocks_gen")
    @SequenceGenerator(name = "stocks_gen", sequenceName = "stocks_seq")
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "ticker")
    private String ticker;

    @Column(name = "xtb_stock")
    private boolean xtbStock; // defaults to false

    @Column(name = "short_sell")
    private boolean shortSell; // defaults to false

    @Column(name = "cfd_margin")
    private double cfdMargin; // 20% for 5:1, 50% for 2:1 etc.

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Column(name = "ipo_date")
    private LocalDate ipoDate; // date stock was added (IPO)

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Column(name = "delisted_date")
    private LocalDate delistedDate; // date stock was added (IPO)

    // daily prices
    @Column(name = "d_open")
    private Double dailyOpen; // Daily Opening Price
    @Column(name = "d_high")
    private Double dailyHigh; // Daily High
    @Column(name = "d_low")
    private Double dailyLow; // Daily Low
    @Column(name = "d_close")
    private Double dailyClose; // Daily Close (real-time price)
    @Column(name = "d_performance")
    private Double dailyPerformance; // Daily Performance (real-time price)

    // weekly prices
    @Column(name = "w_open")
    private Double weeklyOpen; // Weekly Opening Price
    @Column(name = "w_high")
    private Double weeklyHigh; // Weekly High
    @Column(name = "w_low")
    private Double weeklyLow; // Weekly Low
    @Column(name = "w_close")
    private Double weeklyClose; // Weekly Close (real-time price)
    @Column(name = "w_performance")
    private Double weeklyPerformance; // Weekly Performance

    // monthly prices
    @Column(name = "m_open")
    private Double monthlyOpen; // Monthly Opening Price
    @Column(name = "m_high")
    private Double monthlyHigh; // Monthly High
    @Column(name = "m_low")
    private Double monthlyLow; // Monthly Low
    @Column(name = "m_close")
    private Double monthlyClose; // Monthly Close (real-time price)
    @Column(name = "m_performance")
    private Double monthlyPerformance; // Monthly Performance

    // yearly prices
    @Column(name = "y_open")
    private Double yearlyOpen; // Yearly Opening Price
    @Column(name = "y_high")
    private Double yearlyHigh; // Yearly High
    @Column(name = "y_low")
    private Double yearlyLow; // Yearly Low
    @Column(name = "y_close")
    private Double yearlyClose; // Yearly Close (real-time price)
    @Column(name = "y_performance")
    private Double yearlyPerformance; // Yearly Performance

    @Column(name = "high4w")
    private double high4w; // high 4w

    @Column(name = "low4w")
    private double low4w; // low 4w

    @Column(name = "high52w")
    private double high52w; // high 52w

    @Column(name = "low52w")
    private double low52w; // low 52w

    @Column(name = "highest")
    private double highest; // highest price All-Time

    @Column(name = "lowest")
    private double lowest; // lowest price All-Time

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Column(name = "last_updated")
    private LocalDate lastUpdated; // used as fast-fetch max(date) per ticker

    public Stock(String ticker, LocalDate ipoDate, LocalDate delistedDate, boolean xtbStock) {
        this.ticker = ticker;
        this.ipoDate = ipoDate;
        this.delistedDate = delistedDate;
        this.xtbStock = xtbStock;
    }

    public Stock(String ticker, boolean xtbStock, boolean shortSell, double cfdMargin) {
        this.ticker = ticker;
        this.xtbStock = xtbStock;
        this.shortSell = shortSell;
        this.cfdMargin = cfdMargin;
    }

    public Stock(String ticker, LocalDate ipoDate, boolean xtbStock) {
        this.ticker = ticker;
        this.ipoDate = ipoDate;
        this.xtbStock = xtbStock;
    }

    public Stock(String ticker, LocalDate ipoDate) {
        this.ticker = ticker;
        this.ipoDate = ipoDate;
    }

    public void updateFrom(HighLowForPeriod highLowForPeriod) {
        switch (highLowForPeriod) {
            case HighLow4w highLow4w -> updateFromHighLow4w(highLow4w);
            case HighLow52Week highLow52Week -> updateFromHighLow52Week(highLow52Week);
            case HighestLowestPrices highestLowestPrices -> updateFromHighestLowestPrices(highestLowestPrices);
            default -> throw new IllegalArgumentException("Unknown type: " + highLowForPeriod.getClass().getSimpleName());
        }
    }

    private void updateFromHighestLowestPrices(HighestLowestPrices highestLowestPrices) {
        this.setLowest(highestLowestPrices.getLowest());
        this.setHighest(highestLowestPrices.getHighest());
    }

    private void updateFromHighLow52Week(HighLow52Week highLow52Week) {
        this.setLow52w(highLow52Week.getLow52w());
        this.setHigh52w(highLow52Week.getHigh52w());
    }

    public void updateFromDailyPrice(DailyPriceOHLC dailyPrice) {
        this.setDailyOpen(dailyPrice.getOpen());
        this.setDailyHigh(dailyPrice.getHigh());
        this.setDailyLow(dailyPrice.getLow());
        this.setDailyClose(dailyPrice.getClose());
        this.setDailyPerformance(dailyPrice.getPerformance());
        this.setLastUpdated(dailyPrice.getDate());
    }

    public void updateFromWeeklyPrice(WeeklyPriceOHLC weeklyPrice) {
        this.setWeeklyOpen(weeklyPrice.getOpen());
        this.setWeeklyHigh(weeklyPrice.getHigh());
        this.setWeeklyLow(weeklyPrice.getLow());
        this.setWeeklyClose(weeklyPrice.getClose());
        this.setWeeklyPerformance(weeklyPrice.getPerformance());
    }

    public void updateFromMonthlyPrice(MonthlyPriceOHLC monthlyPrice) {
        this.setMonthlyOpen(monthlyPrice.getOpen());
        this.setMonthlyHigh(monthlyPrice.getHigh());
        this.setMonthlyLow(monthlyPrice.getLow());
        this.setMonthlyClose(monthlyPrice.getClose());
        this.setMonthlyPerformance(monthlyPrice.getPerformance());
    }

    public void updateFromYearlyPrice(YearlyPriceOHLC yearlyPrice) {
        this.setYearlyOpen(yearlyPrice.getOpen());
        this.setYearlyHigh(yearlyPrice.getHigh());
        this.setYearlyLow(yearlyPrice.getLow());
        this.setYearlyClose(yearlyPrice.getClose());
        this.setYearlyPerformance(yearlyPrice.getPerformance());
    }

    private void updateFromHighLow4w(HighLow4w highLow4w) {
        this.setLow4w(highLow4w.getLow4w());
        this.setHigh4w(highLow4w.getHigh4w());
    }

    @Override
    public String toString() {
        return STR."Stock{ticker='\{ticker}\{'\''}, xtbStock=\{xtbStock}, ipoDate=\{ipoDate}, delistedDate=\{delistedDate}, shortSell=\{shortSell}, cfdMargin=\{cfdMargin}\{'}'}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Stock stock = (Stock) o;
        return xtbStock == stock.xtbStock && Objects.equals(ticker, stock.ticker);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, ticker, xtbStock);
    }
}