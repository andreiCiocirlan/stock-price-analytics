package stock.price.analytics.model.prices.highlow;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

    public HighLow52Week(String ticker, LocalDate startDate, LocalDate endDate, double weeklyClose) {
        super(ticker, weeklyClose, startDate, endDate);
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
    public String toString() {
        return STR."HighLow52W { \{super.toString()}, low52w=\{low52w}, high52w=\{high52w}} ";
    }
}
