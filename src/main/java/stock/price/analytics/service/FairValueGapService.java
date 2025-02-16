package stock.price.analytics.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import stock.price.analytics.model.fvg.FairValueGap;
import stock.price.analytics.model.prices.enums.FvgStatus;
import stock.price.analytics.model.prices.enums.StockTimeframe;
import stock.price.analytics.model.prices.ohlc.*;
import stock.price.analytics.repository.fvg.FVGRepository;
import stock.price.analytics.repository.prices.*;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static stock.price.analytics.util.Constants.DAILY_FVG_MIN_DATE;
import static stock.price.analytics.util.LoggingUtil.logTime;
import static stock.price.analytics.util.LoggingUtil.logTimeAndReturn;
import static stock.price.analytics.util.PartitionAndSavePriceEntityUtil.partitionDataAndSaveWithLogTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class FairValueGapService {

    private final FVGRepository fvgRepository;
    private final DailyPricesRepository dailyPricesRepository;
    private final WeeklyPricesRepository weeklyPricesRepository;
    private final MonthlyPricesRepository monthlyPricesRepository;
    private final QuarterlyPricesRepository quarterlyPricesRepository;
    private final YearlyPricesRepository yearlyPricesRepository;

    @Transactional
    public void updateUnfilledGapsHighLowAndStatusBy(StockTimeframe timeframe) {
        Map<String, List<AbstractPrice>> pricesByTicker = (switch (timeframe) {
            case DAILY -> dailyPricesRepository.findByDateBetween(DAILY_FVG_MIN_DATE, LocalDate.now())
                    .stream().sorted(Comparator.comparing(DailyPrice::getDate)).toList();
            case WEEKLY -> weeklyPricesRepository.findAll();
            case MONTHLY -> monthlyPricesRepository.findAll().stream()
                    .sorted(Comparator.comparing(MonthlyPrice::getStartDate)).toList();
            case QUARTERLY -> quarterlyPricesRepository.findAll().stream()
                    .sorted(Comparator.comparing(QuarterlyPrice::getStartDate)).toList();
            case YEARLY -> yearlyPricesRepository.findAll().stream()
                    .sorted(Comparator.comparing(YearlyPrice::getStartDate)).toList();
        }).stream().collect(Collectors.groupingBy(AbstractPrice::getTicker));

        List<FairValueGap> fvgsToUpdate = fvgRepository.findByTimeframe(timeframe);
        logTime(() -> fvgsToUpdate.parallelStream().forEachOrdered(fvg -> updateUnfilledGapsHighLowAndStatus(fvg, pricesByTicker.get(fvg.getTicker()))), "updated unfilled gaps high & low & status for " + fvgsToUpdate.size() + " FVGs");
        partitionDataAndSaveWithLogTime(fvgsToUpdate, fvgRepository, "saved " + fvgsToUpdate.size() + " FVGs");
    }

    private void updateUnfilledGapsHighLowAndStatus(FairValueGap fvg, List<AbstractPrice> pricesForTicker) {
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
//                fvgRepository.save(fvg);
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
//                    fvgRepository.save(fvg);
//                    log.info("FVG {} split by gap up. New range2: [{}, {}]", fvg.getId(), fvg.getUnfilledLow2(), fvg.getUnfilledHigh2());
                } else if (price.getClose() < fvg.getUnfilledLow1()) {
                    // Gap Down
                    fvg.setUnfilledLow2(fvg.getUnfilledLow1());
                    fvg.setUnfilledHigh2(price.getOpen());
                    fvg.setUnfilledLow1(price.getOpen());  //reduce the original gap to start at the open
//                    fvgRepository.save(fvg);
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
//                    fvgRepository.save(fvg);
//                    log.info("FVG {} unfilled1 range filled by {}", fvg.getId(), price.getStartDate());
                } else if (price.getHigh() > fvg.getUnfilledHigh1()) {
                    // Intersects top of unfilled1
                    fvg.setUnfilledHigh1(price.getLow());
//                    fvgRepository.save(fvg);
//                    log.info("FVG {} high1 updated to {} by {}", fvg.getId(), price.getLow(), price.getStartDate());
                } else if (price.getLow() < fvg.getUnfilledLow1()) {
                    // Intersects bottom of unfilled1
                    fvg.setUnfilledLow1(price.getHigh());
//                    fvgRepository.save(fvg);
//                    log.info("FVG {} low1 updated to {} by {}", fvg.getId(), price.getHigh(), price.getStartDate());
                } else if (price.getLow() >= fvg.getUnfilledLow1() && price.getHigh() <= fvg.getUnfilledHigh1()) {
                    // Candlestick is completely within the FVG
                    fvg.setUnfilledLow2(fvg.getUnfilledLow1());
                    fvg.setUnfilledHigh2(price.getLow());
                    fvg.setUnfilledLow1(price.getHigh());
//                    fvgRepository.save(fvg);
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
//                    fvgRepository.save(fvg);
//                    log.info("FVG {} unfilled2 range filled by {}", fvg.getId(), price.getStartDate());
                } else if (price.getHigh() > fvg.getUnfilledHigh2()) {
                    // Intersects top of unfilled2
                    fvg.setUnfilledHigh2(price.getLow());
//                    fvgRepository.save(fvg);
//                    log.info("FVG {} high2 updated to {} by {}", fvg.getId(), price.getLow(), price.getStartDate());
                } else if (price.getLow() < fvg.getUnfilledLow2()) {
                    // Intersects bottom of unfilled2
                    fvg.setUnfilledLow2(price.getHigh());
//                    fvgRepository.save(fvg);
//                    log.info("FVG {} low2 updated to {} by {}", fvg.getId(), price.getHigh(), price.getStartDate());
                } else if (price.getLow() >= fvg.getUnfilledLow2() && price.getHigh() <= fvg.getUnfilledHigh2()) {
                    // Candlestick is completely within the FVG
                    fvg.setUnfilledLow2(price.getHigh());
                    fvg.setUnfilledHigh2(price.getLow());
//                    fvgRepository.save(fvg);
//                    log.info("FVG {} split by {} . New FVG {} created.", fvg.getId(), price.getStartDate(), fvg.getHigh());
                }
            }

            // *** Check if both unfilled portions are filled ***
            if (fvg.getUnfilledLow1() == null && fvg.getUnfilledHigh1() == null && fvg.getUnfilledLow2() == null && fvg.getUnfilledHigh2() == null) {
                fvg.setStatus(FvgStatus.CLOSED);
//                fvgRepository.save(fvg);
                log.info("FVG {} completely filled", fvg.getId());
                break; // Move to the next FVG
            }
        }
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