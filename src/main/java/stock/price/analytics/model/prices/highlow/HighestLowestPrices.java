package stock.price.analytics.model.prices.highlow;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.beans.BeanUtils;
import stock.price.analytics.model.prices.highlow.enums.HighLowPeriod;
import stock.price.analytics.model.stocks.Stock;

import java.time.LocalDate;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "highest_lowest")
public class HighestLowestPrices extends HighLowForPeriod {

    @Column(name = "low")
    private double lowest;
    @Column(name = "high")
    private double highest;

    public HighestLowestPrices(String ticker, LocalDate date, double low, double high) {
        super(ticker, date, low, high);
    }

    public HighestLowestPrices copyWith(LocalDate startDate) {
        HighestLowestPrices copy = new HighestLowestPrices();
        BeanUtils.copyProperties(this, copy, "id", "date");
        copy.setDate(startDate);
        return copy;
    }

    @Override
    public void setLow(double low) {
        setLowest(low);
    }

    @Override
    public void setHigh(double high) {
        setHighest(high);
    }

    @Override
    public double getHigh() {
        return highest;
    }

    @Override
    public double getLow() {
        return lowest;
    }

    @Override
    public HighLowPeriod getHighLowPeriod() {
        return HighLowPeriod.HIGH_LOW_ALL_TIME;
    }

    @Override
    public void updateStock(Stock stock) {
        stock.setLowest(this.getLowest());
        stock.setHighest(this.getHighest());
    }

    @Override
    public String toString() {
        return STR."HighestLowest { \{super.toString()}, lowest=\{lowest}, highest=\{highest}} ";
    }
}
