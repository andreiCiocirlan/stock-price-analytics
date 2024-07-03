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

    @Column(name = "low52w")
    private double low52week;
    @Column(name = "high52w")
    private double high52week;

    public HighLow52Week(String ticker, LocalDate startDate, LocalDate endDate, double weeklyClose) {
        super(ticker, weeklyClose, startDate, endDate);
    }

    @Override
    public void setLow(double low) {setLow52week(low);}

    @Override
    public void setHigh(double high) {
        setHigh52week(high);
    }

    @Override
    public String toString() {
        return "HighLow52Weeks { " +
                super.toString() +
                ", low52week=" + low52week +
                ", high52week=" + high52week +
                "} ";
    }
}
