package stock.price.analytics.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import stock.price.analytics.model.dto.StandardDeviationProjectionDTO;
import stock.price.analytics.service.StandardDeviationProjectionService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/projections")
public class StandardDeviationProjectionController {

    private final StandardDeviationProjectionService projectionService;

    @GetMapping("/top/{ticker}")
    public List<StandardDeviationProjectionDTO> getLast3TopProjections(@PathVariable String ticker) {
        return projectionService.getLast3TopProjections(ticker.toUpperCase());
    }

    @GetMapping("/bottom/{ticker}")
    public List<StandardDeviationProjectionDTO> getLast3BottomProjections(@PathVariable String ticker) {
        return projectionService.getLast3BottomProjections(ticker.toUpperCase());
    }
}
