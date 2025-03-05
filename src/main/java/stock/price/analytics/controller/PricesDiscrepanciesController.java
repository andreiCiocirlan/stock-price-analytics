package stock.price.analytics.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import stock.price.analytics.service.PricesDiscrepanciesService;

import java.util.List;

@RequestMapping("/weekly-price-discrepancies")
@RestController
@RequiredArgsConstructor
public class PricesDiscrepanciesController {

    private final PricesDiscrepanciesService pricesDiscrepanciesService;

    @GetMapping("/find-all")
    public List<String> findAllWeeklyPriceDiscrepancies() {
        return pricesDiscrepanciesService.findAllWeeklyPriceDiscrepancies();
    }
}
