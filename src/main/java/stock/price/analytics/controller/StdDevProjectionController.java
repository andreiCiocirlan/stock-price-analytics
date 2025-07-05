package stock.price.analytics.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import stock.price.analytics.model.dto.StdDevProjectionDTO;
import stock.price.analytics.service.StdDevProjectionService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/projections")
public class StdDevProjectionController {

    private final StdDevProjectionService stdDevProjectionService;

    @GetMapping("/top/{ticker}")
    public List<StdDevProjectionDTO> getLast3TopProjections(@PathVariable String ticker) {
        return stdDevProjectionService.getLast3TopProjections(ticker.toUpperCase());
    }

    @GetMapping("/bottom/{ticker}")
    public List<StdDevProjectionDTO> getLast3BottomProjections(@PathVariable String ticker) {
        return stdDevProjectionService.getLast3BottomProjections(ticker.toUpperCase());
    }
}
