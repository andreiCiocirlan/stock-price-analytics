package stock.price.analytics.client.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class StockPrices {

    private String ticker;
    private LocalDate date;
    private double currentPrice;

    public StockPrices(String ticker, LocalDate date, double currentPrice) {
        this.ticker = ticker;
        this.date = date;
        this.currentPrice = currentPrice;
    }

    @Override
    public String toString() {
        return "StockPrices[" +
                "ticker=" + ticker + ", " +
                "date=" + date + ", " +
                "currentPrice=" + currentPrice + ']';
    }

}