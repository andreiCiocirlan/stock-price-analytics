package stock.price.analytics.model.stocks;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.Objects;

@Setter
@Getter
@NoArgsConstructor
@Entity
@Table(name = "stocks")
public class Stock {
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