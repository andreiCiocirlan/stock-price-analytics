package stock.price.analytics.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import stock.price.analytics.repository.stocks.TickerRenameRepository;

@Service
@RequiredArgsConstructor
public class TickerRenameService {

    private final TickerRenameRepository tickerRenameRepository;

    public void renameTicker(String oldTicker, String newTicker) {
        tickerRenameRepository.updateStock(oldTicker, newTicker);
        tickerRenameRepository.updateDailyPrices(oldTicker, newTicker);
        tickerRenameRepository.updateWeeklyPrices(oldTicker, newTicker);
        tickerRenameRepository.updateMonthlyPrices(oldTicker, newTicker);
        tickerRenameRepository.updateQuarterlyPrices(oldTicker, newTicker);
        tickerRenameRepository.updateYearlyPrices(oldTicker, newTicker);
        tickerRenameRepository.updateDailyPricesJSON(oldTicker, newTicker);
        tickerRenameRepository.updateHighLow4w(oldTicker, newTicker);
        tickerRenameRepository.updateHighLow52Week(oldTicker, newTicker);
        tickerRenameRepository.updateHighestLowestPrices(oldTicker, newTicker);
        tickerRenameRepository.updateFairValueGap(oldTicker, newTicker);
        tickerRenameRepository.updatPriceGap(oldTicker, newTicker);
    }
}