package stock.price.analytics.model.dto;

import java.time.LocalDate;

public record StdDevProjectionDTO(String ticker, LocalDate firstPointDate, LocalDate secondPointDate,
                                  double diff, double level0, double level1, double level_minus1,
                                  double level_minus2, double level_minus2_5, double level_minus4,
                                  double level_minus4_5) {

    public StdDevProjectionDTO {
        diff = Math.round(diff * 100.0) / 100.0; // round to 2 decimals
        level0 = Math.round(level0 * 100.0) / 100.0; // round to 2 decimals
        level1 = Math.round(level1 * 100.0) / 100.0; // round to 2 decimals;
        level_minus1 = Math.round(level_minus1 * 100.0) / 100.0; // round to 2 decimals;
        level_minus2 = Math.round(level_minus2 * 100.0) / 100.0; // round to 2 decimals;
        level_minus2_5 = Math.round(level_minus2_5 * 100.0) / 100.0; // round to 2 decimals;
        level_minus4 = Math.round(level_minus4 * 100.0) / 100.0; // round to 2 decimals;
        level_minus4_5 = Math.round(level_minus4_5 * 100.0) / 100.0; // round to 2 decimals;

    }
}
