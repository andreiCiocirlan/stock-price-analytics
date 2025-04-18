package stock.price.analytics.model.dto;

public record StockPerformanceDTO(String ticker, double performance) {
    public StockPerformanceDTO {
        performance = Math.round(performance * 100.0) / 100.0; // round to 2 decimals
    }
}
