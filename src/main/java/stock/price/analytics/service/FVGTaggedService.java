package stock.price.analytics.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import stock.price.analytics.model.fvg.FairValueGap;
import stock.price.analytics.model.prices.enums.PricePerformanceMilestone;
import stock.price.analytics.repository.fvg.FVGTaggedRepository;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FVGTaggedService {

    private final FVGTaggedRepository fvgTaggedRepository;

    public List<String> findWeeklyTaggedFVGsBy(PricePerformanceMilestone pricePerformanceMilestone, List<Double> cfdMargins) {
        return (switch (pricePerformanceMilestone) {
                    case NEW_ALL_TIME_HIGH, NEW_52W_HIGH, NEW_4W_HIGH, NEW_52W_LOW, NEW_4W_LOW, NEW_ALL_TIME_LOW, NONE ->
                            throw new IllegalStateException("Unexpected value " + pricePerformanceMilestone.name());
                    case HIGH_52W_95 -> fvgTaggedRepository.findWeeklyTaggedFVGsBearish95thPercentile52wHigh(cfdMargins);
                    case HIGH_4W_95 -> fvgTaggedRepository.findWeeklyTaggedFVGsBearish95thPercentile4wHigh(cfdMargins);
                    case HIGH_ALL_TIME_95 -> fvgTaggedRepository.findWeeklyTaggedFVGsBearish95thPercentileAllTimeHigh(cfdMargins);
                    case LOW_52W_95 -> fvgTaggedRepository.findWeeklyTaggedFVGsBullish95thPercentile52wLow(cfdMargins);
                    case LOW_4W_95 -> fvgTaggedRepository.findWeeklyTaggedFVGsBullish95thPercentile4wLow(cfdMargins);
                    case LOW_ALL_TIME_95 -> fvgTaggedRepository.findWeeklyTaggedFVGsBullish95thPercentileAllTimeLow(cfdMargins);
                }).stream().map(FairValueGap::getTicker).toList();
    }
}
