package stock.price.analytics.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import stock.price.analytics.controller.dto.StandardDeviationProjectionDTO;
import stock.price.analytics.repository.projections.ProjectionRepositoryCustom;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StandardDeviationProjectionService {

    private final ProjectionRepositoryCustom projectionRepositoryCustom;

    public List<StandardDeviationProjectionDTO> getLast3Projections(String ticker) {
        // Delegate the call to the custom repository implementation
        return projectionRepositoryCustom.findLast3ProjectionsByTicker(ticker);
    }
}
