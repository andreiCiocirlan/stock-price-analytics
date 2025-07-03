package stock.price.analytics.model.dto;

import lombok.Getter;

import java.time.LocalDate;

@Getter
public class StandardDeviationProjectionDTO {
    private String ticker;
    private LocalDate localTopDate;
    private LocalDate secondPointDate;
    private double diff;
    private double level0;
    private double level1;

    public StandardDeviationProjectionDTO(String ticker, LocalDate localTopDate, LocalDate secondPointDate, double diff, double level0, double level1) {
        this.ticker = ticker;
        this.localTopDate = localTopDate;
        this.secondPointDate = secondPointDate;
        this.diff = diff;
        this.level0 = level0;
        this.level1 = level1;
    }

    // Getters and setters omitted for brevity (use Lombok @Data if preferred)

    @Override
    public String toString() {
        return "StandardDeviationProjectionDTO{" +
               "ticker='" + ticker + '\'' +
               ", localTopDate=" + localTopDate +
               ", secondPointDate=" + secondPointDate +
               ", diff=" + diff +
               ", level0=" + level0 +
               ", level1=" + level1 +
               '}';
    }
}
