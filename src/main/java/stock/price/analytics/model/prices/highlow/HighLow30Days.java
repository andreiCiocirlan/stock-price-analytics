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
@Table(name = "high_low30d")
public class HighLow30Days extends HighLowForPeriod {

    @Column(name = "low30d")
    private double low30day;
    @Column(name = "high30d")
    private double high30day;

    public HighLow30Days(String ticker, LocalDate startDate, LocalDate endDate, double weeklyClose) {
        super(ticker, weeklyClose, startDate, endDate);
    }

    @Override
    public void setLow(double low) {
        setLow30day(low);
    }

    @Override
    public void setHigh(double high) {
        setHigh30day(high);
    }

    @Override
    public String toString() {
        return STR."HighLow30Days { \{super.toString()}, low30day=\{low30day}, high30day=\{high30day}} ";
    }
}
