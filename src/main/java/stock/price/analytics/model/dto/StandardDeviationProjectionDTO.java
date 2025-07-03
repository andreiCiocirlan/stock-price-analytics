package stock.price.analytics.model.dto;

import lombok.Getter;

import java.time.LocalDate;

@Getter
public class StandardDeviationProjectionDTO {
    private final String ticker;
    private final LocalDate firstPointDate;
    private final LocalDate secondPointDate;
    private final double diff;
    private final double level0;
    private final double level1;
    private final double level_minus1;
    private final double level_minus2;
    private final double level_minus2_5;
    private final double level_minus4;
    private final double level_minus4_5;

    public StandardDeviationProjectionDTO(String ticker, LocalDate firstPointDate, LocalDate secondPointDate, double diff, double level0, double level1, double levelMinus1, double levelMinus2, double levelMinus25, double levelMinus4, double levelMinus45) {
        this.ticker = ticker;
        this.firstPointDate = firstPointDate;
        this.secondPointDate = secondPointDate;
        this.diff = Math.round(diff * 100.0) / 100.0; // round to 2 decimals
        this.level0 = Math.round(level0 * 100.0) / 100.0; // round to 2 decimals
        this.level1 = Math.round(level1 * 100.0) / 100.0; // round to 2 decimals;
        this.level_minus1 = Math.round(levelMinus1 * 100.0) / 100.0; // round to 2 decimals;
        this.level_minus2 = Math.round(levelMinus2 * 100.0) / 100.0; // round to 2 decimals;
        this.level_minus2_5 = Math.round(levelMinus25 * 100.0) / 100.0; // round to 2 decimals;
        this.level_minus4 = Math.round(levelMinus4 * 100.0) / 100.0; // round to 2 decimals;
        this.level_minus4_5 = Math.round(levelMinus45 * 100.0) / 100.0; // round to 2 decimals;
    }

    @Override
    public String toString() {
        return "StandardDeviationProjectionDTO{" +
               "ticker='" + ticker + '\'' +
               ", firstPointDate=" + firstPointDate +
               ", secondPointDate=" + secondPointDate +
               ", diff=" + diff +
               ", level0=" + level0 +
               ", level1=" + level1 +
               ", level_minus1=" + level_minus1 +
               ", level_minus2=" + level_minus2 +
               ", level_minus2_5=" + level_minus2_5 +
               ", level_minus4=" + level_minus4 +
               ", level_minus4_5=" + level_minus4_5 +
               '}';
    }
}
