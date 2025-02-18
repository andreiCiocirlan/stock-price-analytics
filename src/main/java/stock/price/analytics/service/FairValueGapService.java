package stock.price.analytics.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import stock.price.analytics.model.fvg.FairValueGap;
import stock.price.analytics.model.prices.enums.FvgStatus;
import stock.price.analytics.model.prices.enums.StockTimeframe;
import stock.price.analytics.model.prices.ohlc.AbstractPrice;
import stock.price.analytics.repository.fvg.FVGRepository;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static stock.price.analytics.util.LoggingUtil.logTime;
import static stock.price.analytics.util.LoggingUtil.logTimeAndReturn;
import static stock.price.analytics.util.PartitionAndSavePriceEntityUtil.partitionDataAndSaveWithLogTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class FairValueGapService {

    private final FVGRepository fvgRepository;
    private final PricesService pricesService;

    @Transactional
    public void updateUnfilledGapsHighLowAndStatusBy(StockTimeframe timeframe) {
        Map<String, List<AbstractPrice>> pricesByTicker = pricesService.findAllPricesFor(timeframe).stream()
                .sorted(Comparator.comparing(AbstractPrice::getStartDate))
                .collect(Collectors.groupingBy(AbstractPrice::getTicker));

        List<FairValueGap> fvgsToUpdate = fvgRepository.findByTimeframe(timeframe);
        List<FairValueGap> modifiedFvgs = new ArrayList<>();
        logTime(() -> fvgsToUpdate.parallelStream().forEachOrdered(fvg -> {
            if (updateUnfilledGapsHighLowAndStatus(fvg, pricesByTicker.get(fvg.getTicker()))) {
                modifiedFvgs.add(fvg);
            }
        }), "updated unfilled gaps high & low & status for " + fvgsToUpdate.size() + " FVGs");
        partitionDataAndSaveWithLogTime(modifiedFvgs, fvgRepository, "saved " + modifiedFvgs.size() + " FVGs");
    }

    private boolean updateUnfilledGapsHighLowAndStatus(FairValueGap fvg, List<AbstractPrice> pricesForTicker) {
        // copy original state and compare at the end
        FairValueGap originalFVG = new FairValueGap(fvg);

        // Initialize unfilled portions if they are null (first time processing)
        if (fvg.getStatus() == FvgStatus.OPEN && fvg.getUnfilledLow1() == null) {
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
            if (fvg.getStatus() == FvgStatus.CLOSED) {
                break; // Skip further processing for this FVG
            }

            // ***  Engulfing Scenario ***
            if (price.getLow() <= fvg.getLow() && price.getHigh() >= fvg.getHigh()) {
                // Candlestick completely engulfs the original FVG
                fvg.setStatus(FvgStatus.CLOSED);
                fvg.setUnfilledLow1(null);
                fvg.setUnfilledHigh1(null);
                fvg.setUnfilledLow2(null);
                fvg.setUnfilledHigh2(null);
//                log.info("FVG {} filled by {}", fvg.getId(), price.getStartDate());
                break; // Move to the next FVG
            }

            // *** Gap Handling ***
            if (fvg.getUnfilledLow1() != null && price.getOpen() > fvg.getUnfilledLow1() && price.getOpen() < fvg.getUnfilledHigh1()) {
                if (price.getClose() > fvg.getUnfilledHigh1()) {
                    // Gap Up
                    fvg.setUnfilledLow2(price.getOpen());
                    fvg.setUnfilledHigh2(fvg.getUnfilledHigh1());
                    fvg.setUnfilledHigh1(price.getOpen());  //Reduce the original gap to end at the open
//                    log.info("FVG {} split by gap up. New range2: [{}, {}]", fvg.getId(), fvg.getUnfilledLow2(), fvg.getUnfilledHigh2());
                } else if (price.getClose() < fvg.getUnfilledLow1()) {
                    // Gap Down
                    fvg.setUnfilledLow2(fvg.getUnfilledLow1());
                    fvg.setUnfilledHigh2(price.getOpen());
                    fvg.setUnfilledLow1(price.getOpen());  //reduce the original gap to start at the open
//                    log.info("FVG {} split by gap down. New range2: [{}, {}]", fvg.getId(), fvg.getUnfilledLow2(), fvg.getUnfilledHigh2());
                }
            }

            // *** Intersection with Unfilled Portion 1 ***
            if (fvg.getUnfilledLow1() != null && price.getLow() <= fvg.getUnfilledHigh1() && price.getHigh() >= fvg.getUnfilledLow1()) {
                // Price intersects with the first unfilled range

                if (price.getHigh() >= fvg.getUnfilledHigh1() && price.getLow() < fvg.getUnfilledLow1()) {
                    //Candlestick engulfs unfilled1
                    fvg.setUnfilledLow1(null);
                    fvg.setUnfilledHigh1(null);
//                    log.info("FVG {} unfilled1 range filled by {}", fvg.getId(), price.getStartDate());
                } else if (price.getHigh() > fvg.getUnfilledHigh1()) {
                    // Intersects top of unfilled1
                    fvg.setUnfilledHigh1(price.getLow());
//                    log.info("FVG {} high1 updated to {} by {}", fvg.getId(), price.getLow(), price.getStartDate());
                } else if (price.getLow() < fvg.getUnfilledLow1()) {
                    // Intersects bottom of unfilled1
                    fvg.setUnfilledLow1(price.getHigh());
//                    log.info("FVG {} low1 updated to {} by {}", fvg.getId(), price.getHigh(), price.getStartDate());
                } else if (price.getLow() >= fvg.getUnfilledLow1() && price.getHigh() <= fvg.getUnfilledHigh1()) {
                    // Candlestick is completely within the FVG
                    fvg.setUnfilledLow2(fvg.getUnfilledLow1());
                    fvg.setUnfilledHigh2(price.getLow());
                    fvg.setUnfilledLow1(price.getHigh());
//                    log.info("FVG {} split by {} . New FVG {} created.", fvg.getId(), price.getStartDate(), fvg.getHigh());
                }
            }

            // *** Intersection with Unfilled Portion 2 ***
            if (fvg.getUnfilledLow2() != null && price.getLow() <= fvg.getUnfilledHigh2() && price.getHigh() >= fvg.getUnfilledLow2()) {
                // Price intersects with the second unfilled range
                if (price.getHigh() >= fvg.getUnfilledHigh2() && price.getLow() < fvg.getUnfilledLow2()) {
                    //Candlestick engulfs unfilled2
                    fvg.setUnfilledLow2(null);
                    fvg.setUnfilledHigh2(null);
//                    log.info("FVG {} unfilled2 range filled by {}", fvg.getId(), price.getStartDate());
                } else if (price.getHigh() > fvg.getUnfilledHigh2()) {
                    // Intersects top of unfilled2
                    fvg.setUnfilledHigh2(price.getLow());
//                    log.info("FVG {} high2 updated to {} by {}", fvg.getId(), price.getLow(), price.getStartDate());
                } else if (price.getLow() < fvg.getUnfilledLow2()) {
                    // Intersects bottom of unfilled2
                    fvg.setUnfilledLow2(price.getHigh());
//                    log.info("FVG {} low2 updated to {} by {}", fvg.getId(), price.getHigh(), price.getStartDate());
                } else if (price.getLow() >= fvg.getUnfilledLow2() && price.getHigh() <= fvg.getUnfilledHigh2()) {
                    // Candlestick is completely within the FVG
                    fvg.setUnfilledLow2(price.getHigh());
                    fvg.setUnfilledHigh2(price.getLow());
//                    log.info("FVG {} split by {} . New FVG {} created.", fvg.getId(), price.getStartDate(), fvg.getHigh());
                }
            }

            // *** Check if both unfilled portions are filled ***
            if (fvg.getUnfilledLow1() == null && fvg.getUnfilledHigh1() == null && fvg.getUnfilledLow2() == null && fvg.getUnfilledHigh2() == null) {
                fvg.setStatus(FvgStatus.CLOSED);
                log.info("FVG {} {} {} {} completely filled", fvg.getId(), fvg.getTicker(), fvg.getDate(), fvg.getTimeframe());
                break; // Move to the next FVG
            }
        }
        return !originalFVG.equals(fvg);
    }

    public void initUnfilledGapsHighLow1AndSave(StockTimeframe timeframe) {
        List<FairValueGap> fvgsToUpdate = fvgRepository.findByTimeframe(timeframe);
        fvgsToUpdate.parallelStream().forEachOrdered(fvg -> {
            if (fvg.getStatus() == FvgStatus.OPEN && fvg.getUnfilledLow1() == null) {
                fvg.setUnfilledLow1(fvg.getLow());
                fvg.setUnfilledHigh1(fvg.getHigh());
            }
        });
        partitionDataAndSaveWithLogTime(fvgsToUpdate, fvgRepository, "saved " + fvgsToUpdate.size() + " unfilled high-low1 for OPEN FVGs");
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
        List<FairValueGap> recentFVGs = findRecentByTimeframe(timeframe);

        Map<String, FairValueGap> dbFVGsByCompositeId = fvgRepository.findByTimeframe(timeframe.name()).stream().collect(Collectors.toMap(FairValueGap::compositeId, p -> p));
        Map<String, FairValueGap> currentFVGsByCompositeId = recentFVGs.stream().collect(Collectors.toMap(FairValueGap::compositeId, p -> p));
        currentFVGsByCompositeId.forEach((compositeKey, fvg) -> {
            if (!dbFVGsByCompositeId.containsKey(compositeKey)) {
                FairValueGap newFVG = new FairValueGap(fvg.getTicker(), fvg.getTimeframe(), fvg.getDate(), fvg.getType(), fvg.getStatus(), fvg.getLow(), fvg.getHigh());
                newFVG.setUnfilledLow1(fvg.getLow());
                newFVG.setUnfilledHigh1(fvg.getHigh());
                newFVGsFound.add(newFVG);
                newFvgTickers.add(fvg.getTicker());
            }
        });
        log.info("Found {} new {} FVGs for: {}", newFVGsFound.size(), timeframe, newFvgTickers);

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
        Map<String, FairValueGap> updatedFVGsByCompositeId = new HashMap<>();
        List<FairValueGap> recentFVGs = findRecentByTimeframe(timeframe); // existing recent FVGs (to update high-low)
        Set<String> updatedHighLowTickers = new HashSet<>();

        Map<String, FairValueGap> dbFVGsByCompositeId = fvgRepository.findByTimeframe(timeframe.name()).stream().collect(Collectors.toMap(FairValueGap::compositeId, p -> p));
        Map<String, List<AbstractPrice>> pricesByTicker = pricesService.currentCachePricesFor(timeframe).stream().collect(Collectors.groupingBy(AbstractPrice::getTicker));

        Map<String, FairValueGap> currentFVGsByCompositeId = recentFVGs.stream().collect(Collectors.toMap(FairValueGap::compositeId, p -> p));
        currentFVGsByCompositeId.forEach((compositeKey, fvg) -> {
            if (dbFVGsByCompositeId.containsKey(compositeKey)) { // check for high-low updates of existing FVGs
                FairValueGap dbFVG = dbFVGsByCompositeId.get(compositeKey);
                boolean differentHighLow = dbFVG.getHigh() != fvg.getHigh() || dbFVG.getLow() != fvg.getLow();
                if (differentHighLow) {
                    updatedHighLowTickers.add(fvg.getTicker());
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
        log.info("updated high-low for recent {} FVGs : {} ", timeframe, updatedHighLowTickers);

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