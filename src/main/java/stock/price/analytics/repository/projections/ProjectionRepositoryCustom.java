package stock.price.analytics.repository.projections;

import stock.price.analytics.controller.dto.StandardDeviationProjectionDTO;

import java.util.List;

public interface ProjectionRepositoryCustom {
    List<StandardDeviationProjectionDTO> findLast3ProjectionsByTicker(String ticker);
}