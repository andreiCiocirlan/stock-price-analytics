package stock.price.analytics.repository.projections;

import stock.price.analytics.model.dto.StdDevProjectionDTO;

import java.util.List;

public interface ProjectionRepositoryCustom {
    List<StdDevProjectionDTO> findLast3TopProjections(String ticker);
    List<StdDevProjectionDTO> findLast3BottomProjections(String ticker);
}