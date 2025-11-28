package stock.price.analytics.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import stock.price.analytics.model.prices.enums.StockTimeframe;
import stock.price.analytics.service.DemarkService;

@RequiredArgsConstructor
@RestController
@RequestMapping("/demark")
public class DemarkController {

    private final DemarkService demarkService;

    @PutMapping("/update-htf")
    @ResponseStatus(HttpStatus.OK)
    public void updateDemarkForHigherTimeframes() {
        for (StockTimeframe timeframe : StockTimeframe.higherTimeframes()) {
            demarkService.demarkForTimeframe(timeframe);
        }
    }
}
