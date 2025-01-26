package stock.price.analytics.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import stock.price.analytics.model.fvg.FairValueGap;
import stock.price.analytics.model.prices.enums.StockTimeframe;
import stock.price.analytics.repository.fvg.FVGRepository;

import java.util.List;

import static stock.price.analytics.util.PartitionAndSavePriceEntityUtil.partitionDataAndSave;

@Service
@RequiredArgsConstructor
public class FairValueGapService {

    private final FVGRepository fvgRepository;

    public void findAndSaveFVGsFor(StockTimeframe timeframe) {
        List<FairValueGap> fvgs = findAllByTimeframe(timeframe);
        partitionDataAndSave(fvgs, fvgRepository);
    }

    private List<FairValueGap> findAllByTimeframe(StockTimeframe timeframe) {
        return switch (timeframe) {
            case DAILY -> fvgRepository.findAllDailyFVGs();
            case WEEKLY -> fvgRepository.findAllWeeklyFVGs();
            case MONTHLY -> fvgRepository.findAllMonthlyFVGs();
            case QUARTERLY -> fvgRepository.findAllQuarterlyFVGs();
            case YEARLY -> fvgRepository.findAllYearlyFVGs();
        };
    }

}