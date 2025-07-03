package stock.price.analytics.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import stock.price.analytics.controller.dto.StandardDeviationProjectionDTO;
import stock.price.analytics.service.StandardDeviationProjectionService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/projections")
public class StandardDeviationProjectionController {

    private final StandardDeviationProjectionService projectionService;

    @GetMapping("/{ticker}")
    public List<StandardDeviationProjectionDTO> getProjections(@PathVariable String ticker) {
        return projectionService.getLast3Projections(ticker.toUpperCase());
    }
}
