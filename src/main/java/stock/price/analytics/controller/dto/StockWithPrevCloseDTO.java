package stock.price.analytics.controller.dto;

import java.sql.Date;

public record StockWithPrevCloseDTO(String ticker, Date date, double prevClose) {

}
