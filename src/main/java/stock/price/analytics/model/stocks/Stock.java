package stock.price.analytics.model.stocks;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;
import org.springframework.format.annotation.DateTimeFormat;
import stock.price.analytics.model.BusinessEntity;
import stock.price.analytics.model.prices.enums.StockTimeframe;
import stock.price.analytics.model.prices.highlow.HighLowForPeriod;
import stock.price.analytics.model.prices.ohlc.AbstractPrice;

import java.time.LocalDate;
import java.util.Objects;

@Setter
@Getter
@DynamicUpdate
@NoArgsConstructor
@Entity
@Table(name = "stocks")
public class Stock implements BusinessEntity {
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
    @Column(name = "close")
    private Double close; // closing price (real-time price, equal for all timeframes)
    @Column(name = "d_performance")
    private Double dailyPerformance; // Daily Performance (real-time price)

    // weekly prices
    @Column(name = "w_open")
    private Double weeklyOpen; // Weekly Opening Price
    @Column(name = "w_high")
    private Double weeklyHigh; // Weekly High
    @Column(name = "w_low")
    private Double weeklyLow; // Weekly Low
    @Column(name = "w_performance")
    private Double weeklyPerformance; // Weekly Performance

    // monthly prices
    @Column(name = "m_open")
    private Double monthlyOpen; // Monthly Opening Price
    @Column(name = "m_high")
    private Double monthlyHigh; // Monthly High
    @Column(name = "m_low")
    private Double monthlyLow; // Monthly Low
    @Column(name = "m_performance")
    private Double monthlyPerformance; // Monthly Performance

    // quarterly prices
    @Column(name = "q_open")
    private Double quarterlyOpen; // Quarterly Opening Price
    @Column(name = "q_high")
    private Double quarterlyHigh; // Quarterly High
    @Column(name = "q_low")
    private Double quarterlyLow; // Quarterly Low
    @Column(name = "q_performance")
    private Double quarterlyPerformance; // Quarterly Performance

    // yearly prices
    @Column(name = "y_open")
    private Double yearlyOpen; // Yearly Opening Price
    @Column(name = "y_high")
    private Double yearlyHigh; // Yearly High
    @Column(name = "y_low")
    private Double yearlyLow; // Yearly Low
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

    public Stock(String ticker, boolean xtbStock, boolean shortSell, double cfdMargin, LocalDate lastUpdated) {
        this.ticker = ticker;
        this.xtbStock = xtbStock;
        this.shortSell = shortSell;
        this.cfdMargin = cfdMargin;
        this.lastUpdated = lastUpdated;
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
        highLowForPeriod.updateStock(this);
    }

    public void updateFrom(AbstractPrice price) {
        price.updateStock(this);
    }

    public double performanceFor(StockTimeframe timeFrame) {
        return switch (timeFrame) {
            case DAILY -> getDailyPerformance();
            case WEEKLY -> getWeeklyPerformance();
            case MONTHLY -> getMonthlyPerformance();
            case QUARTERLY -> getQuarterlyPerformance();
            case YEARLY -> getYearlyPerformance();
        };
    }

    public boolean needsUpdate(AbstractPrice p) {
        double performance = performanceFor(p.getTimeframe());
        if (this.getClose() != p.getClose() || performance != p.getPerformance()) return true;

        return switch (p.getTimeframe()) {
            case DAILY ->
                    this.getDailyHigh() != p.getHigh() || this.getDailyLow() != p.getLow() || this.getDailyOpen() != p.getOpen();
            case WEEKLY ->
                    this.getWeeklyHigh() != p.getHigh() || this.getWeeklyLow() != p.getLow() || this.getWeeklyOpen() != p.getOpen();
            case MONTHLY ->
                    this.getMonthlyHigh() != p.getHigh() || this.getMonthlyLow() != p.getLow() || this.getMonthlyOpen() != p.getOpen();
            case QUARTERLY ->
                    this.getQuarterlyHigh() != p.getHigh() || this.getQuarterlyLow() != p.getLow() || this.getQuarterlyOpen() != p.getOpen();
            case YEARLY ->
                    this.getYearlyHigh() != p.getHigh() || this.getYearlyLow() != p.getLow() || this.getYearlyOpen() != p.getOpen();
        };
    }

    public boolean needsUpdate(HighLowForPeriod hl) {
        return switch (hl.getHighLowPeriod()) {
            case HIGH_LOW_4W -> this.getHigh4w() != hl.getHigh() || this.getLow4w() != hl.getLow();
            case HIGH_LOW_52W -> this.getHigh52w() != hl.getHigh() || this.getLow52w() != hl.getLow();
            case HIGH_LOW_ALL_TIME -> this.getHighest() != hl.getHigh() || this.getLowest() != hl.getLow();
        };
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