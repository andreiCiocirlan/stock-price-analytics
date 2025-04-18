package stock.price.analytics.model.dto;

import java.sql.Date;

public record StockWithPrevCloseDTO(String ticker, Date date, double prevClose) {

}
