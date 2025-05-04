package stock.price.analytics.model.prices.highlow;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import stock.price.analytics.model.prices.highlow.enums.HighLowPeriod;
import stock.price.analytics.model.stocks.Stock;

import java.time.LocalDate;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "high_low52w")
public class HighLow52Week extends HighLowForPeriod {

    @Column(name = "low")
    private double low52w;
    @Column(name = "high")
    private double high52w;

    public HighLow52Week(String ticker, LocalDate date, double low, double high) {
        super(ticker, date, low, high);
    }

    @Override
    public void setLow(double low) {
        setLow52w(low);
    }

    @Override
    public void setHigh(double high) {
        setHigh52w(high);
    }

    @Override
    public double getHigh() {
        return high52w;
    }

    @Override
    public double getLow() {
        return low52w;
    }

    @Override
    public HighLowPeriod getHighLowPeriod() {
        return HighLowPeriod.HIGH_LOW_52W;
    }

    @Override
    public void updateStock(Stock stock) {
        stock.setLow52w(this.getLow52w());
        stock.setHigh52w(this.getHigh52w());
    }

    @Override
    public String toString() {
        return STR."HighLow52W { \{super.toString()}, low52w=\{low52w}, high52w=\{high52w}} ";
    }
}
