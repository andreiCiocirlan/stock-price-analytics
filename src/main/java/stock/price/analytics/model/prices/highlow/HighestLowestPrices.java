package stock.price.analytics.model.prices.highlow;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

    @Override
    public void setLow(double low) {
        setLowest(low);
    }

    @Override
    public void setHigh(double high) {
        setHighest(high);
    }

    @Override
    public String toString() {
        return STR."HighestLowest { \{super.toString()}, lowest=\{lowest}, highest=\{highest}} ";
    }
}
