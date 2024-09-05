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
@Table(name = "high_low4w")
public class HighLow4w extends HighLowForPeriod {

    @Column(name = "low")
    private double low4w;
    @Column(name = "high")
    private double high4w;

    public HighLow4w(String ticker, LocalDate startDate, LocalDate endDate, double weeklyClose) {
        super(ticker, weeklyClose, startDate, endDate);
    }

    @Override
    public void setLow(double low) {
        setLow4w(low);
    }

    @Override
    public void setHigh(double high) {
        setHigh4w(high);
    }

    @Override
    public String toString() {
        return STR."HighLow4W { \{super.toString()}, low4w=\{low4w}, high4w=\{high4w}} ";
    }
}
