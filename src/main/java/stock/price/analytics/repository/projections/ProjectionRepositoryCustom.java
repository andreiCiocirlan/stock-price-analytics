package stock.price.analytics.repository.projections;

import stock.price.analytics.model.dto.StandardDeviationProjectionDTO;

import java.util.List;

public interface ProjectionRepositoryCustom {
    List<StandardDeviationProjectionDTO> findLast3TopProjections(String ticker);
    List<StandardDeviationProjectionDTO> findLast3BottomProjections(String ticker);
}