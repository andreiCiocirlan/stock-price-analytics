package stock.price.analytics.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import stock.price.analytics.model.fvg.FairValueGap;
import stock.price.analytics.model.prices.enums.FvgStatus;
import stock.price.analytics.model.prices.enums.StockTimeframe;
import stock.price.analytics.repository.fvg.FVGRepository;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static stock.price.analytics.util.LoggingUtil.logTimeAndReturn;
import static stock.price.analytics.util.PartitionAndSavePriceEntityUtil.partitionDataAndSaveWithLogTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class FairValueGapService {

    private final FVGRepository fvgRepository;

    private List<FairValueGap> findNewByTimeframe(StockTimeframe timeframe) {
        LocalDate date = switch (timeframe) {
            case DAILY -> LocalDate.now().minusWeeks(1);
            case WEEKLY -> LocalDate.now().minusWeeks(3);
            case MONTHLY -> LocalDate.now().minusMonths(3);
            case QUARTERLY -> LocalDate.now().minusMonths(9);
            case YEARLY -> LocalDate.now().minusYears(3);
        };
        return switch (timeframe) {
            case DAILY -> fvgRepository.findAllDailyFVGsAfter(date);
            case WEEKLY -> fvgRepository.findAllWeeklyFVGsAfter(date);
            case MONTHLY -> fvgRepository.findAllMonthlyFVGsAfter(date);
            case QUARTERLY -> fvgRepository.findAllQuarterlyFVGsAfter(date);
            case YEARLY -> fvgRepository.findAllYearlyFVGsAfter(date);
        };
    }

    private List<FairValueGap> findAllByTimeframe(StockTimeframe timeframe) {
        LocalDate longTimeAgo = LocalDate.of(1950, 1, 1);
        return switch (timeframe) {
            case DAILY -> fvgRepository.findAllDailyFVGsAfter(LocalDate.now().minusYears(3));
            case WEEKLY -> fvgRepository.findAllWeeklyFVGsAfter(longTimeAgo);
            case MONTHLY -> fvgRepository.findAllMonthlyFVGsAfter(longTimeAgo);
            case QUARTERLY -> fvgRepository.findAllQuarterlyFVGsAfter(longTimeAgo);
            case YEARLY -> fvgRepository.findAllYearlyFVGsAfter(longTimeAgo);
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
        Set<String> newFvgTickers = new HashSet<>();
        List<FairValueGap> newFVGsFound = new ArrayList<>();
        List<FairValueGap> currentFVGs = findNewByTimeframe(timeframe);

        Map<String, FairValueGap> dbFVGsByCompositeId = fvgRepository.findByTimeframeAndStatusOpen(timeframe.name()).stream().collect(Collectors.toMap(FairValueGap::compositeId, p -> p));
        Map<String, FairValueGap> currentFVGsByCompositeId = currentFVGs.stream().collect(Collectors.toMap(FairValueGap::compositeId, p -> p));
        currentFVGsByCompositeId.forEach((compositeKey, fvg) -> {
            if (!dbFVGsByCompositeId.containsKey(compositeKey)) {
                newFVGsFound.add(new FairValueGap(fvg.getTicker(), fvg.getTimeframe(), fvg.getDate(), fvg.getType(), fvg.getStatus(), fvg.getLow(), fvg.getHigh()));
                newFvgTickers.add(fvg.getTicker());
            }
        });
        log.info("Found {} new {} FVGs for: {}", newFvgTickers.size(), timeframe, newFvgTickers);

        return newFVGsFound;
    }

    @Transactional
    public void updateFVGsHighLowAndClosedFor(StockTimeframe timeframe) {
        if (timeframe == null) { // update closed for all timeframes
            for (StockTimeframe stockTimeframe : StockTimeframe.values()) {
                List<FairValueGap> updatedFVGs = logTimeAndReturn(() -> findUpdatedFVGsHighLowAndClosedFor(stockTimeframe), "Found " + stockTimeframe + " FVGs to be updated");
                partitionDataAndSaveWithLogTime(updatedFVGs, fvgRepository, "updated " + updatedFVGs.size() + " FVGs for " + stockTimeframe);
            }
        } else {
            List<FairValueGap> updatedFVGs = logTimeAndReturn(() -> findUpdatedFVGsHighLowAndClosedFor(timeframe), "Found " + timeframe + " FVGs to be updated");
            partitionDataAndSaveWithLogTime(updatedFVGs, fvgRepository, "updated " + updatedFVGs.size() + " FVGs for " + timeframe);
        }
    }

    public List<FairValueGap> findUpdatedFVGsHighLowAndClosedFor(StockTimeframe timeframe) {
        List<FairValueGap> updatedFVGs = new ArrayList<>();
        List<FairValueGap> currentFVGs = findAllByTimeframe(timeframe);

        Map<String, FairValueGap> dbFVGsByCompositeId = fvgRepository.findByTimeframeAndStatusOpen(timeframe.name()).stream().collect(Collectors.toMap(FairValueGap::compositeId, p -> p));
        Map<String, FairValueGap> currentFVGsByCompositeId = currentFVGs.stream().collect(Collectors.toMap(FairValueGap::compositeId, p -> p));

        dbFVGsByCompositeId.forEach((compositeKey, fvg) -> {
            if (!currentFVGsByCompositeId.containsKey(compositeKey)) { // FVG is CLOSED
                fvg.setStatus(FvgStatus.CLOSED);
                updatedFVGs.add(fvg);
            } else { // update FVG high/low
                FairValueGap currentFVG = currentFVGsByCompositeId.get(compositeKey);
                boolean differentHighLow = currentFVG.getHigh() != fvg.getHigh() || currentFVG.getLow() != fvg.getLow();
                if (differentHighLow) {
                    fvg.setHigh(currentFVG.getHigh());
                    fvg.setLow(currentFVG.getLow());
                    updatedFVGs.add(fvg);
                }
            }
        });

        return updatedFVGs;
    }

    public void saveNewFVGsAndUpdateHighLowAndClosed() {
        for (StockTimeframe timeframe : StockTimeframe.values()) {
            findNewFVGsAndSaveFor(timeframe);
            updateFVGsHighLowAndClosedFor(timeframe);
        }
    }

    public void updateFVGPricesForStockSplit(String ticker, LocalDate stockSplitDate, double stockSplitMultiplier) {
        int updatedRows = fvgRepository.updateFVGPricesForStockSplit(ticker, stockSplitDate, stockSplitMultiplier);
        log.warn("updated {} FVG rows for {} and stockSplitDate {}", updatedRows, ticker, stockSplitDate);
    }

    public List<Object[]> findFvgDateDiscrepancies() {
        return fvgRepository.findFvgDateDiscrepancies();
    }
}