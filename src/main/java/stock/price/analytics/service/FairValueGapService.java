package stock.price.analytics.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import stock.price.analytics.model.fvg.FairValueGap;
import stock.price.analytics.model.prices.enums.FvgTimeframe;
import stock.price.analytics.repository.fvg.FVGRepository;

import java.util.List;

import static stock.price.analytics.util.PartitionAndSavePriceEntityUtil.partitionDataAndSave;

@Service
@RequiredArgsConstructor
public class FairValueGapService {

    private final FVGRepository fvgRepository;

    public void findAndSaveFVGsFor(FvgTimeframe timeframe) {
        List<FairValueGap> entities = switch (timeframe) {
            case DAY -> fvgRepository.findDailyFVGs();
            case WEEK -> fvgRepository.findWeeklyFVGs();
            case MONTH -> fvgRepository.findMonthlyFVGs();
            case QUARTER -> fvgRepository.findQuarterlyFVGs();
            case YEAR -> fvgRepository.findYearlyFVGs();
        };
        partitionDataAndSave(entities, fvgRepository);
    }

}