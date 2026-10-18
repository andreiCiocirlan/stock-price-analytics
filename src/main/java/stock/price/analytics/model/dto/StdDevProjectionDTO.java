package stock.price.analytics.model.dto;

import java.time.LocalDate;

public record StdDevProjectionDTO(
        LocalDate pivotDate,
        int pointBOffset,
        String pivotType,
        double proj40,
        double proj45
) {}