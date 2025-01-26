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
        List<FairValueGap> entities = switch (timeframe) {
            case DAILY -> fvgRepository.findDailyFVGs();
            case WEEKLY -> fvgRepository.findWeeklyFVGs();
            case MONTHLY -> fvgRepository.findMonthlyFVGs();
            case QUARTERLY -> fvgRepository.findQuarterlyFVGs();
            case YEARLY -> fvgRepository.findYearlyFVGs();
        };
        partitionDataAndSave(entities, fvgRepository);
    }

}