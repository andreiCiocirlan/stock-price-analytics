package stock.price.analytics.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import stock.price.analytics.model.fvg.FairValueGap;
import stock.price.analytics.model.fvg.enums.GapStatus;
import stock.price.analytics.model.fvg.enums.FvgType;
import stock.price.analytics.model.prices.enums.PricePerformanceMilestone;
import stock.price.analytics.model.prices.enums.StockTimeframe;
import stock.price.analytics.model.prices.ohlc.AbstractPrice;
import stock.price.analytics.repository.fvg.FVGRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static stock.price.analytics.model.fvg.enums.FvgType.BEARISH;
import static stock.price.analytics.model.fvg.enums.FvgType.BULLISH;
import static stock.price.analytics.model.prices.enums.PricePerformanceMilestone.*;
import static stock.price.analytics.util.Constants.*;
import static stock.price.analytics.util.PartitionAndSavePriceEntityUtil.partitionDataAndSaveNoLogging;

@Slf4j
@Service
@RequiredArgsConstructor
public class FairValueGapService {

    private final FVGRepository fvgRepository;
    private final PricesService pricesService;
    private final FVGTaggedService fvgTaggedService;

    private boolean updateUnfilledGapsHighLowAndStatus(FairValueGap fvg, List<AbstractPrice> pricesForTicker) {
        // copy original state and compare at the end
        FairValueGap originalFVG = new FairValueGap(fvg);

        // Initialize unfilled portions if they are null (first time processing)
        if (fvg.getStatus() == GapStatus.OPEN && fvg.getUnfilledLow1() == null) {
            fvg.setUnfilledLow1(fvg.getLow());
            fvg.setUnfilledHigh1(fvg.getHigh());
        }
        for (AbstractPrice price : pricesForTicker) {
            boolean isBeforeOrEqualFVGDate = price.getStartDate().isBefore(fvg.getDate()) || price.getStartDate().isEqual(fvg.getDate());
            boolean isImmediatelyAfterFVG = price.isImmediatelyAfter(fvg);
            if (isBeforeOrEqualFVGDate || isImmediatelyAfterFVG) {
                // Skip prices before, or immediately after FVG date
                continue;
            }

            // Check if the FVG is already filled
            if (fvg.getStatus() == GapStatus.CLOSED) {
                break; // Skip further processing for this FVG
            }

            //  Engulfing Scenario
            if (price.getLow() <= fvg.getLow() && price.getHigh() >= fvg.getHigh()) {
                // Candlestick completely engulfs the original FVG
                fvg.setStatus(GapStatus.CLOSED);
                fvg.setUnfilledLow1(null);
                fvg.setUnfilledHigh1(null);
                fvg.setUnfilledLow2(null);
                fvg.setUnfilledHigh2(null);
                log.info("{} FVG {} {} completely filled", fvg.getTimeframe(), fvg.getId(), fvg.compositeId());
                break; // Move to the next FVG
            }

            // Gap Handling
            if (fvg.getUnfilledLow1() != null && price.getOpen() > fvg.getUnfilledLow1() && price.getOpen() < fvg.getUnfilledHigh1()) {
                if (price.getClose() > fvg.getUnfilledHigh1()) {
                    // Gap Up
                    fvg.setUnfilledLow2(price.getOpen());
                    fvg.setUnfilledHigh2(fvg.getUnfilledHigh1());
                    fvg.setUnfilledHigh1(price.getOpen());  //Reduce the original gap to end at the open
                } else if (price.getClose() < fvg.getUnfilledLow1()) {
                    // Gap Down
                    fvg.setUnfilledLow2(fvg.getUnfilledLow1());
                    fvg.setUnfilledHigh2(price.getOpen());
                    fvg.setUnfilledLow1(price.getOpen());  //reduce the original gap to start at the open
                }
            }

            // Intersection with Unfilled Portion 1
            if (fvg.getUnfilledLow1() != null && price.getLow() <= fvg.getUnfilledHigh1() && price.getHigh() >= fvg.getUnfilledLow1()) {
                // Price intersects with the first unfilled range

                if (price.getHigh() >= fvg.getUnfilledHigh1() && price.getLow() < fvg.getUnfilledLow1()) {
                    //Candlestick engulfs unfilled1
                    fvg.setUnfilledLow1(null);
                    fvg.setUnfilledHigh1(null);
                } else if (price.getHigh() > fvg.getUnfilledHigh1()) {
                    // Intersects top of unfilled1
                    fvg.setUnfilledHigh1(price.getLow());
                } else if (price.getLow() < fvg.getUnfilledLow1()) {
                    // Intersects bottom of unfilled1
                    fvg.setUnfilledLow1(price.getHigh());
                } else if (price.getLow() >= fvg.getUnfilledLow1() && price.getHigh() <= fvg.getUnfilledHigh1()) {
                    // Candlestick is completely within the FVG
                    fvg.setUnfilledLow2(fvg.getUnfilledLow1());
                    fvg.setUnfilledHigh2(price.getLow());
                    fvg.setUnfilledLow1(price.getHigh());
                }
            }

            // Intersection with Unfilled Portion 2
            if (fvg.getUnfilledLow2() != null && price.getLow() <= fvg.getUnfilledHigh2() && price.getHigh() >= fvg.getUnfilledLow2()) {
                // Price intersects with the second unfilled range
                if (price.getHigh() >= fvg.getUnfilledHigh2() && price.getLow() < fvg.getUnfilledLow2()) {
                    //Candlestick engulfs unfilled2
                    fvg.setUnfilledLow2(null);
                    fvg.setUnfilledHigh2(null);
                } else if (price.getHigh() > fvg.getUnfilledHigh2()) {
                    // Intersects top of unfilled2
                    fvg.setUnfilledHigh2(price.getLow());
                } else if (price.getLow() < fvg.getUnfilledLow2()) {
                    // Intersects bottom of unfilled2
                    fvg.setUnfilledLow2(price.getHigh());
                } else if (price.getLow() >= fvg.getUnfilledLow2() && price.getHigh() <= fvg.getUnfilledHigh2()) {
                    // Candlestick is completely within the FVG
                    fvg.setUnfilledLow2(price.getHigh());
                    fvg.setUnfilledHigh2(price.getLow());
                }
            }

            // Check if both unfilled portions are filled
            if (fvg.getUnfilledLow1() == null && fvg.getUnfilledHigh1() == null && fvg.getUnfilledLow2() == null && fvg.getUnfilledHigh2() == null) {
                fvg.setStatus(GapStatus.CLOSED);
                log.info("{} FVG {} {} completely filled", fvg.getTimeframe(), fvg.getId(), fvg.compositeId());
                break;
            }
        }
        return !originalFVG.equals(fvg);
    }

    private List<FairValueGap> findRecentByTimeframe(StockTimeframe timeframe) {
        return switch (timeframe) {
            case DAILY -> fvgRepository.findAllDailyFVGsAfter(LocalDate.now().minusWeeks(1));
            case WEEKLY -> fvgRepository.findAllWeeklyFVGsAfter(LocalDate.now().minusWeeks(3));
            case MONTHLY -> fvgRepository.findAllMonthlyFVGsAfter(LocalDate.now().minusMonths(3));
            case QUARTERLY -> fvgRepository.findAllQuarterlyFVGsAfter(LocalDate.now().minusMonths(9));
            case YEARLY -> fvgRepository.findAllYearlyFVGsAfter(LocalDate.now().minusYears(3));
        };
    }

    public void findNewFVGsAndSaveFor(StockTimeframe timeframe) {
        List<FairValueGap> newFVGs = findNewFVGsFor(timeframe);
        if (!newFVGs.isEmpty()) {
            partitionDataAndSaveNoLogging(newFVGs, fvgRepository);
        }
    }

    public void findNewFVGsAndSaveForAllTimeframes() {
        for (StockTimeframe timeframe : StockTimeframe.values()) {
            findNewFVGsAndSaveFor(timeframe);
        }
    }

    public List<FairValueGap> findNewFVGsFor(StockTimeframe timeframe) {
        List<FairValueGap> newFVGsFound = new ArrayList<>();
        List<FairValueGap> recentFVGs = findRecentByTimeframe(timeframe);

        Map<String, FairValueGap> dbFVGsByCompositeId = fvgRepository.findByTimeframe(timeframe.name()).stream().collect(Collectors.toMap(FairValueGap::compositeId, p -> p));
        Map<String, FairValueGap> currentFVGsByCompositeId = recentFVGs.stream().collect(Collectors.toMap(FairValueGap::compositeId, p -> p));
        currentFVGsByCompositeId.forEach((compositeKey, fvg) -> {
            if (!dbFVGsByCompositeId.containsKey(compositeKey)) {
                FairValueGap newFVG = new FairValueGap(fvg.getTicker(), fvg.getTimeframe(), fvg.getDate(), fvg.getType(), fvg.getStatus(), fvg.getLow(), fvg.getHigh());
                newFVG.setUnfilledLow1(fvg.getLow());
                newFVG.setUnfilledHigh1(fvg.getHigh());
                newFVGsFound.add(newFVG);
            }
        });
        if (!newFVGsFound.isEmpty()) {
            log.info("Found {} new {} FVGs", newFVGsFound.size(), timeframe);
        }

        return newFVGsFound;
    }

    @Transactional
    public void updateFVGsHighLowAndClosedForAllTimeframes() {
        for (StockTimeframe timeframe : StockTimeframe.values()) {
            updateFVGsHighLowAndClosedFor(timeframe);
        }
    }

    @Transactional
    public void updateFVGsHighLowAndClosedFor(StockTimeframe timeframe) {
        List<FairValueGap> updatedFVGs = findUpdatedFVGsHighLowAndClosedFor(timeframe);
        if (!updatedFVGs.isEmpty()) {
            partitionDataAndSaveNoLogging(updatedFVGs, fvgRepository);
        }
    }

    public List<FairValueGap> findUpdatedFVGsHighLowAndClosedFor(StockTimeframe timeframe) {
        Map<String, FairValueGap> updatedFVGsByCompositeId = new HashMap<>();
        List<FairValueGap> recentFVGs = findRecentByTimeframe(timeframe); // existing recent FVGs (to update high-low)
        Map<String, FairValueGap> dbFVGsByCompositeId = fvgRepository.findByTimeframe(timeframe.name()).stream().collect(Collectors.toMap(FairValueGap::compositeId, p -> p));
        Map<String, List<AbstractPrice>> pricesByTicker = pricesService.htfPricesFor(timeframe).stream().collect(Collectors.groupingBy(AbstractPrice::getTicker));

        Map<String, FairValueGap> currentFVGsByCompositeId = recentFVGs.stream().collect(Collectors.toMap(FairValueGap::compositeId, p -> p));
        currentFVGsByCompositeId.forEach((compositeKey, fvg) -> {
            if (dbFVGsByCompositeId.containsKey(compositeKey)) { // check for high-low updates of existing FVGs
                FairValueGap dbFVG = dbFVGsByCompositeId.get(compositeKey);
                boolean differentHighLow = dbFVG.getHigh() != fvg.getHigh() || dbFVG.getLow() != fvg.getLow();
                if (differentHighLow) {
                    dbFVG.setHigh(fvg.getHigh());
                    dbFVG.setLow(fvg.getLow());
                    if (dbFVG.getUnfilledHigh2() == null && dbFVG.getUnfilledLow2() == null) {
                        dbFVG.setUnfilledHigh1(dbFVG.getHigh());
                        dbFVG.setUnfilledLow1(dbFVG.getLow());
                    }
                    updatedFVGsByCompositeId.put(compositeKey, dbFVG);
                }
            }
        });

        updatedFVGsByCompositeId.forEach((compositeKey, fvg) -> {
            if (pricesByTicker.containsKey(fvg.getTicker()) && updateUnfilledGapsHighLowAndStatus(fvg, pricesByTicker.get(fvg.getTicker()))) {
                updatedFVGsByCompositeId.put(compositeKey, fvg); // replace with updated FVG
            }
        });

        dbFVGsByCompositeId.forEach((compositeKey, fvg) -> {
            if (!updatedFVGsByCompositeId.containsKey(compositeKey) && pricesByTicker.containsKey(fvg.getTicker()) && updateUnfilledGapsHighLowAndStatus(fvg, pricesByTicker.get(fvg.getTicker()))) {
                updatedFVGsByCompositeId.put(compositeKey, fvg);
            }
        });

        return new ArrayList<>(updatedFVGsByCompositeId.values());
    }

    public void saveNewFVGsAndUpdateHighLowAndClosedAllTimeframes() {
        for (StockTimeframe timeframe : StockTimeframe.values()) {
            saveNewFVGsAndUpdateHighLowAndClosedFor(timeframe);
            log.warn("saved new FVGs and updated existing FVGs for {} timeframe", timeframe);
        }
    }

    public void saveNewFVGsAndUpdateHighLowAndClosedFor(StockTimeframe timeframe) {
        findNewFVGsAndSaveFor(timeframe);
        updateFVGsHighLowAndClosedFor(timeframe);
    }

    public void updateFVGPricesForStockSplit(String ticker, LocalDate stockSplitDate, double stockSplitMultiplier) {
        int updatedRows = fvgRepository.updateFVGPricesForStockSplit(ticker, stockSplitDate, stockSplitMultiplier);
        log.warn("updated {} FVG rows for {} and stockSplitDate {}", updatedRows, ticker, stockSplitDate);
    }

    public void logFVGsTagged95thPercentile(List<Double> cfdMargins) {
        String cfdMargins54 = cfdMargins.stream().map(cfdMargin -> STR."'\{cfdMargin}'").collect(Collectors.joining(", "));
        for (StockTimeframe timeframe : StockTimeframe.higherTimeframes()) {
            for (PricePerformanceMilestone priceMilestone : milestones95thPercentile()) {
                for (FvgType fvgType : FvgType.values()) {
                    String fvgLabel = fvgLabelFrom(priceMilestone, fvgType, timeframe);
                    log.warn("{}", fvgLabel + fvgTaggedService.findTickersFVGsTaggedFor(timeframe, fvgType, priceMilestone, cfdMargins54));
                }
            }
        }
    }

    public String fvgLabelFrom(PricePerformanceMilestone pricePerformanceMilestone, FvgType fvgType, StockTimeframe stockTimeframe) {
        String highLowTimeframeCorrelation = timeframeFrom(pricePerformanceMilestone);
        boolean isLow95thPercentile = low95thPercentileValues().contains(pricePerformanceMilestone);
        boolean isHigh95thPercentile = high95thPercentileValues().contains(pricePerformanceMilestone);
        if (isLow95thPercentile && fvgType == BEARISH) {
            // price near lower 95th percentile AND Bearish FVG
            return String.join(" ", stockTimeframe.name(), "ANVIL", highLowTimeframeCorrelation, "FVGs:");
        } else if (isHigh95thPercentile && fvgType == BULLISH) {
            // price near upper 95th percentile AND Bullish FVG
            return String.join(" ", stockTimeframe.name(), "ROCKET SHIP", highLowTimeframeCorrelation, "FVGs:");
        }

        String highLowLabel = isLow95thPercentile ? "Low" : "High";
        String mainFvgLabel = switch (pricePerformanceMilestone) {
            case HIGH_52W_95, LOW_52W_95 -> FVG_95TH_PERCENTILE_52W;
            case HIGH_4W_95, LOW_4W_95 -> FVG_95TH_PERCENTILE_4W;
            case HIGH_ALL_TIME_95, LOW_ALL_TIME_95 -> FVG_95TH_PERCENTILE_ALL_TIME;
            case NEW_ALL_TIME_HIGH, NEW_52W_HIGH, NEW_4W_HIGH, NEW_52W_LOW, NEW_4W_LOW, NEW_ALL_TIME_LOW, NONE ->
                    throw new IllegalStateException("Unexpected value " + pricePerformanceMilestone.name());
        };
        return String.join(" ", stockTimeframe.name(), fvgType.name(), mainFvgLabel, highLowLabel, "FVGs:");
    }
}