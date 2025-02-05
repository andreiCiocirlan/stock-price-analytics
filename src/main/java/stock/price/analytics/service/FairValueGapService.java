package stock.price.analytics.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import stock.price.analytics.model.fvg.FairValueGap;
import stock.price.analytics.model.prices.enums.FvgStatus;
import stock.price.analytics.model.prices.enums.StockTimeframe;
import stock.price.analytics.repository.fvg.FVGRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static stock.price.analytics.util.PartitionAndSavePriceEntityUtil.partitionDataAndSaveWithLogTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class FairValueGapService {

    private final FVGRepository fvgRepository;

    private List<FairValueGap> findAllByTimeframe(StockTimeframe timeframe) {
        return switch (timeframe) {
            case DAILY -> fvgRepository.findAllDailyFVGs();
            case WEEKLY -> fvgRepository.findAllWeeklyFVGs();
            case MONTHLY -> fvgRepository.findAllMonthlyFVGs();
            case QUARTERLY -> fvgRepository.findAllQuarterlyFVGs();
            case YEARLY -> fvgRepository.findAllYearlyFVGs();
        };
    }

    public void findNewFVGsAndSaveFor(StockTimeframe timeframe) {
        if (timeframe == null) { // find new FVGs and save for all timeframes
            for (StockTimeframe stockTimeframe : StockTimeframe.values()) {
                partitionDataAndSaveWithLogTime(findNewFVGsFor(stockTimeframe), fvgRepository, "saved new FVGs for " + stockTimeframe);
            }
        } else {
            partitionDataAndSaveWithLogTime(findNewFVGsFor(timeframe), fvgRepository, "saved new FVGs for " + timeframe);
        }
    }

    public List<FairValueGap> findNewFVGsFor(StockTimeframe timeframe) {
        List<FairValueGap> newFVGsFound = new ArrayList<>();
        List<FairValueGap> currentFVGs = findAllByTimeframe(timeframe);

        Map<String, FairValueGap> dbFVGsByCompositeId = fvgRepository.findByTimeframeAndStatusOpen(timeframe.name()).stream().collect(Collectors.toMap(FairValueGap::compositeId, p -> p));
        Map<String, FairValueGap> currentFVGsByCompositeId = currentFVGs.stream().collect(Collectors.toMap(FairValueGap::compositeId, p -> p));
        currentFVGsByCompositeId.forEach((compositeKey, fvg) -> {
            if (!dbFVGsByCompositeId.containsKey(compositeKey)) {
                newFVGsFound.add(new FairValueGap(fvg.getTicker(), fvg.getTimeframe(), fvg.getDate(), fvg.getType(), fvg.getStatus(), fvg.getLow(),  fvg.getHigh()));
            }
        });
        log.info("Found {} new {} FVGs for: {}", newFVGsFound.size(), timeframe, newFVGsFound.stream().map(FairValueGap::getTicker).toList());

        return newFVGsFound;
    }

    public void updateClosedFVGsFor(StockTimeframe timeframe) {
        if (timeframe == null) { // update closed for all timeframes
            for (StockTimeframe stockTimeframe : StockTimeframe.values()) {
                partitionDataAndSaveWithLogTime(findClosedFVGsFor(stockTimeframe), fvgRepository, "updated closed FVGs for " + stockTimeframe);
            }
        } else {
            partitionDataAndSaveWithLogTime(findClosedFVGsFor(timeframe), fvgRepository, "updated closed FVGs for " + timeframe);
        }
    }

    public List<FairValueGap> findClosedFVGsFor(StockTimeframe timeframe) {
        List<FairValueGap> closedFVGsFound = new ArrayList<>();
        List<FairValueGap> currentFVGs = findAllByTimeframe(timeframe);

        Map<String, FairValueGap> dbFVGsByCompositeId = fvgRepository.findByTimeframeAndStatusOpen(timeframe.name()).stream().collect(Collectors.toMap(FairValueGap::compositeId, p -> p));
        Map<String, FairValueGap> currentFVGsByCompositeId = currentFVGs.stream().collect(Collectors.toMap(FairValueGap::compositeId, p -> p));

        dbFVGsByCompositeId.forEach((compositeKey, fvg) -> {
            if (!currentFVGsByCompositeId.containsKey(compositeKey)) {
                fvg.setStatus(FvgStatus.CLOSED);
                closedFVGsFound.add(fvg);
            }
        });
        log.info("Closed {} {} FVGs for: {}", closedFVGsFound.size(), timeframe, closedFVGsFound.stream().map(FairValueGap::getTicker).toList());

        return closedFVGsFound;
    }

    public void updateFVGPricesForStockSplit(String ticker, LocalDate stockSplitDate, double stockSplitMultiplier) {
        int updatedRows = fvgRepository.updateFVGPricesForStockSplit(ticker, stockSplitDate, stockSplitMultiplier);
        log.warn("updated {} FVG rows for {} and stockSplitDate {}", updatedRows, ticker, stockSplitDate);
    }
}