package stock.price.analytics.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import stock.price.analytics.model.dto.StdDevProjectionDTO;
import stock.price.analytics.repository.projections.ProjectionRepositoryCustom;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StandardDeviationProjectionService {

    private final ProjectionRepositoryCustom projectionRepositoryCustom;

    public List<StdDevProjectionDTO> getLast3TopProjections(String ticker) {
        return projectionRepositoryCustom.findLast3TopProjections(ticker);
    }
    public List<StdDevProjectionDTO> getLast3BottomProjections(String ticker) {
        return projectionRepositoryCustom.findLast3BottomProjections(ticker);
    }
}
