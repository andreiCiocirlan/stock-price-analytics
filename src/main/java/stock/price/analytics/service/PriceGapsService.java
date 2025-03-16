package stock.price.analytics.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import stock.price.analytics.repository.prices.gaps.PriceGapsRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PriceGapsService {

    private final PriceGapsRepository priceGapsRepository;

    public void saveAllPriceGapsFor(List<String> tickers) {
        priceGapsRepository.saveDailyPriceGaps(tickers);
        priceGapsRepository.saveWeeklyPriceGaps(tickers);
        priceGapsRepository.saveMonthlyPriceGaps(tickers);
        priceGapsRepository.saveQuarterlyPriceGaps(tickers);
        priceGapsRepository.saveYearlyPriceGaps(tickers);
    }

}
