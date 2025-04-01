package stock.price.analytics.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import stock.price.analytics.client.YahooQuotesClient;
import stock.price.analytics.model.prices.enums.StockTimeframe;
import stock.price.analytics.model.prices.highlow.HighLow4w;
import stock.price.analytics.model.prices.highlow.HighLow52Week;
import stock.price.analytics.model.prices.highlow.HighLowForPeriod;
import stock.price.analytics.model.prices.highlow.HighestLowestPrices;
import stock.price.analytics.model.prices.highlow.enums.HighLowPeriod;
import stock.price.analytics.model.prices.ohlc.*;
import stock.price.analytics.repository.prices.highlow.HighLowForPeriodRepository;
import stock.price.analytics.util.TradingDateUtil;

import java.io.IOException;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

import static java.nio.file.Files.readAllLines;
import static stock.price.analytics.util.Constants.USER_AGENT_VALUE;
import static stock.price.analytics.util.FileUtil.writeToFile;
import static stock.price.analytics.util.PricesUtil.htfPricesForTimeframe;
import static stock.price.analytics.util.PricesUtil.pricesWithPerformance;

@Slf4j
@Service
@RequiredArgsConstructor
public class NewTickerService {

    private static final String YAHOO_BASE_URL = "https://query1.finance.yahoo.com/v7/finance";
    private final YahooQuotesClient yahooQuotesClient;
    private final RestTemplate restTemplate;
    private final StockService stockService;
    private final PriceService priceService;
    private final PriceGapService priceGapService;
    private final FairValueGapService fairValueGapService;
    private final AsyncPersistenceService asyncPersistenceService;
    private final HighLowForPeriodRepository highLowForPeriodRepository;
    private String COOKIE;

    public void importFromExistingJSONFor(String tickers) {
        List<String> tickerList = Arrays.stream(tickers.split(",")).toList();
        List<DailyPrice> dailyPricesImported = getDailyPricesFor(tickerList);
        List<AbstractPrice> htfPricesImported = getHigherTimeframePricesFor(dailyPricesImported);
        priceService.savePrices(dailyPricesImported);
        priceService.savePrices(htfPricesImported);
        saveHighLowPricesForPeriodFrom(htfPricesImported);
        saveAndUpdateStocksFor(dailyPricesImported, htfPricesImported);
        priceGapService.saveAllPriceGapsFor(tickerList);
        fairValueGapService.findNewFVGsAndSaveForAllTimeframes(tickerList, true);
    }

    // import all data pertaining to the new tickers and create dailyPrices, htfPrices, stocks, highLowPrices etc.
    public void importAllDataFor(String tickers, Double cfdMargin, Boolean shortSell) {
        List<String> tickerList = Arrays.stream(tickers.split(",")).toList();
        stockService.saveStocks(tickers, Boolean.TRUE, Boolean.TRUE.equals(shortSell), cfdMargin);
        COOKIE = yahooQuotesClient.cookieFromFcYahoo();
        getYahooQuotesAndSaveJSONFileFor(tickers);
        List<DailyPrice> dailyPricesImported = getDailyPricesFor(tickerList);
        priceService.savePrices(dailyPricesImported);
        List<AbstractPrice> htfPricesImported = getHigherTimeframePricesFor(dailyPricesImported);
        priceService.savePrices(htfPricesImported);
        saveHighLowPricesForPeriodFrom(htfPricesImported);
        saveAndUpdateStocksFor(dailyPricesImported, htfPricesImported);
        priceGapService.saveAllPriceGapsFor(tickerList);
        fairValueGapService.findNewFVGsAndSaveForAllTimeframes(tickerList, true);
    }

    private void getYahooQuotesAndSaveJSONFileFor(String tickers) {
        int lowerBound = 100;
        int upperBound = 200;
        int range = (upperBound - lowerBound) + 1;
        try {
            for (String ticker : tickers.split(",")) {
                long currentTime = System.currentTimeMillis();
                ResponseEntity<String> response;
                try {
                    HttpHeaders headers = new HttpHeaders();
                    headers.add("Cookie", COOKIE);
                    headers.add(HttpHeaders.USER_AGENT, USER_AGENT_VALUE);

                    HttpEntity<String> entity = new HttpEntity<>(null, headers);
                    response = restTemplate.exchange(
                            YAHOO_BASE_URL + "/chart/{ticker}?range=60y&interval=" + timeframeToQParam(StockTimeframe.DAILY)
                            + "&indicators=quote&includeTimestamps=true",
                            HttpMethod.GET,
                            entity,
                            String.class,
                            ticker
                    );

                    String responseBody = response.getBody();
                    if (responseBody != null) {
                        String fileName = ticker + ".json";
                        String path = "./all-historical-prices/DAILY/" + fileName;
                        writeToFile(path, responseBody);
                    } else {
                        log.error("response body is null for ticker {}", ticker);
                    }
                } catch (RestClientException e) {
                    log.error("Failed retrieving prices data for ticker {}: {}", ticker, e.getMessage());
                }
                log.info("saving JSON historical prices for {} took {} ms", ticker, (System.currentTimeMillis() - currentTime));
                int sleepTime = (int) (Math.random() * range) + lowerBound;
                Thread.sleep(sleepTime);
                log.info("sleeping for {} ms", sleepTime);
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private List<DailyPrice> getDailyPricesFor(List<String> tickerList) {
        List<DailyPrice> dailyPricesImported = new ArrayList<>();

        try {
            for (String ticker : tickerList) {
                String jsonFilePath = String.join("", "./all-historical-prices/DAILY/", ticker, ".json");
                String dailyPricesFileContent = String.join("", readAllLines(Path.of(jsonFilePath)));
                List<DailyPrice> dailyPricesExtracted = extractDailyPricesFrom(ticker, dailyPricesFileContent).stream().sorted(Comparator.comparing(DailyPrice::getDate)).toList();
                dailyPricesImported.addAll(pricesWithPerformance(dailyPricesExtracted));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return dailyPricesImported;
    }

    private List<AbstractPrice> getHigherTimeframePricesFor(List<DailyPrice> dailyPricesImported) {
        List<AbstractPrice> htfPrices = new ArrayList<>();
        List<WeeklyPrice> weeklyPrices = htfPricesForTimeframe(dailyPricesImported, StockTimeframe.WEEKLY).stream().map(WeeklyPrice.class::cast).toList();
        List<MonthlyPrice> monthlyPrices = htfPricesForTimeframe(dailyPricesImported, StockTimeframe.MONTHLY).stream().map(MonthlyPrice.class::cast).toList();
        List<QuarterlyPrice> quarterlyPrices = htfPricesForTimeframe(dailyPricesImported, StockTimeframe.QUARTERLY).stream().map(QuarterlyPrice.class::cast).toList();
        List<YearlyPrice> yearlyPrices = htfPricesForTimeframe(dailyPricesImported, StockTimeframe.YEARLY).stream().map(YearlyPrice.class::cast).toList();

        htfPrices.addAll(pricesWithPerformance(weeklyPrices.stream().sorted(Comparator.comparing(WeeklyPrice::getStartDate)).toList()));
        htfPrices.addAll(pricesWithPerformance(monthlyPrices.stream().sorted(Comparator.comparing(MonthlyPrice::getStartDate)).toList()));
        htfPrices.addAll(pricesWithPerformance(quarterlyPrices.stream().sorted(Comparator.comparing(QuarterlyPrice::getStartDate)).toList()));
        htfPrices.addAll(pricesWithPerformance(yearlyPrices.stream().sorted(Comparator.comparing(YearlyPrice::getStartDate)).toList()));

        return htfPrices;
    }

    private void saveAndUpdateStocksFor(List<DailyPrice> dailyPricesImported, List<AbstractPrice> htfPricesImported) {
        LocalDate tradingDateNow = TradingDateUtil.tradingDateNow();
        List<DailyPrice> latestDailyPricesImported = dailyPricesImported.stream()
                .filter(dp -> dp.getDate().isEqual(tradingDateNow))
                .toList();

        List<AbstractPrice> latestHTFPrices = htfPricesImported.stream()
                .filter(this::isLatestHTFPrice)
                .toList();

        stockService.updateStocksHighLowsAndOHLCFrom(latestDailyPricesImported, latestHTFPrices);
    }

    private boolean isLatestHTFPrice(AbstractPrice price) {
        LocalDate startDate = price.getStartDate();
        LocalDate tradingDateNow = TradingDateUtil.tradingDateNow();

        return switch (price.getTimeframe()) {
            case WEEKLY -> startDate.isEqual(tradingDateNow.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)));
            case MONTHLY -> startDate.isEqual(tradingDateNow.with(TemporalAdjusters.firstDayOfMonth()));
            case QUARTERLY ->
                    startDate.isEqual(LocalDate.of(tradingDateNow.getYear(), tradingDateNow.getMonth().firstMonthOfQuarter().getValue(), 1));
            case YEARLY -> startDate.isEqual(tradingDateNow.with(TemporalAdjusters.firstDayOfYear()));
            default -> false;
        };
    }

    private void saveHighLowPricesForPeriodFrom(List<AbstractPrice> weeklyPricesImported) {
        List<HighLowForPeriod> highLowForPeriodPrices = new ArrayList<>();
        Map<String, List<AbstractPrice>> weeklyPricesByTicker = weeklyPricesImported.stream()
                .filter(price -> price.getTimeframe() == StockTimeframe.WEEKLY)
                .collect(Collectors.groupingBy(AbstractPrice::getTicker));

        for (HighLowPeriod highLowPeriod : HighLowPeriod.values()) {
            int intervalNrWeeks = switch (highLowPeriod) {
                case HIGH_LOW_4W -> 4;
                case HIGH_LOW_52W -> 52;
                case HIGH_LOW_ALL_TIME -> 1;
            };
            for (Map.Entry<String, List<AbstractPrice>> entry : weeklyPricesByTicker.entrySet()) {
                String ticker = entry.getKey();
                List<AbstractPrice> prices = entry.getValue();
                prices.sort(Comparator.comparing(AbstractPrice::getStartDate).reversed());

                for (int i = 0; i <= prices.size() - intervalNrWeeks; i++) {
                    int toIndex = intervalNrWeeks == 1 ? prices.size() : i + intervalNrWeeks;
                    List<AbstractPrice> currentWeeksForPeriod = prices.subList(i, toIndex);
                    double highestPriceForPeriod = currentWeeksForPeriod.stream()
                            .mapToDouble(AbstractPrice::getHigh)
                            .max()
                            .orElseThrow();

                    double lowestPriceForPeriod = currentWeeksForPeriod.stream()
                            .mapToDouble(AbstractPrice::getLow)
                            .min()
                            .orElseThrow();

                    LocalDate startDate = prices.get(i).getStartDate();
                    LocalDate endDate = startDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.FRIDAY));

                    HighLowForPeriod highLowForPeriod = switch (highLowPeriod) {
                        case HIGH_LOW_4W -> new HighLow4w(ticker, startDate, endDate);
                        case HIGH_LOW_52W -> new HighLow52Week(ticker, startDate, endDate);
                        case HIGH_LOW_ALL_TIME -> new HighestLowestPrices(ticker, startDate, endDate);
                    };
                    highLowForPeriod.setLow(lowestPriceForPeriod);
                    highLowForPeriod.setHigh(highestPriceForPeriod);
                    highLowForPeriodPrices.add(highLowForPeriod);
                }

            }
        }
        asyncPersistenceService.partitionDataAndSaveNoLogging(highLowForPeriodPrices, highLowForPeriodRepository);
    }

    private List<DailyPrice> extractDailyPricesFrom(String ticker, String jsonResponse) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        List<DailyPrice> dailyPrices = new ArrayList<>();
        Map<String, List<LocalDate>> anomalies = new HashMap<>();

        JsonNode rootNode = objectMapper.readTree(jsonResponse);
        JsonNode resultNode = rootNode.path("chart").path("result").get(0);
        JsonNode timestampsNode = resultNode.path("timestamp");
        JsonNode quoteNode = resultNode.path("indicators").path("quote").get(0);

        for (int i = 0; i < timestampsNode.size(); i++) {
            long timestamp = timestampsNode.get(i).asLong();
            LocalDate date = Instant.ofEpochSecond(timestamp).atZone(ZoneId.systemDefault()).toLocalDate();

            double open = quoteNode.path("open").get(i).asDouble();
            double high = quoteNode.path("high").get(i).asDouble();
            double low = quoteNode.path("low").get(i).asDouble();
            double close = quoteNode.path("close").get(i).asDouble();
            boolean anomalyFound = false;
            if (open == 0d) {
                anomalies.computeIfAbsent("open", _ -> new ArrayList<>()).add(date);
                anomalyFound = true;
            }
            if (low == 0d) {
                anomalies.computeIfAbsent("low", _ -> new ArrayList<>()).add(date);
                anomalyFound = true;
            }
            if (high == 0d) {
                anomalies.computeIfAbsent("high", _ -> new ArrayList<>()).add(date);
                anomalyFound = true;
            }
            if (close == 0d) {
                anomalies.computeIfAbsent("close", _ -> new ArrayList<>()).add(date);
                anomalyFound = true;
            }

            if (!anomalyFound) {
                dailyPrices.add(dailyPriceWithRoundedDecimals(new DailyPrice(ticker, date, new CandleOHLC(open, high, low, close))));
            }
        }

        for (Map.Entry<String, List<LocalDate>> entry : anomalies.entrySet()) {
            List<LocalDate> dates = entry.getValue().stream()
                    .filter(d -> d.isAfter(LocalDate.of(2000, 1, 1))) // most anomalies are from 1960-2000
                    .toList();
            if (!dates.isEmpty())
                log.warn("{} price 0 found for ticker {} and dates {}", entry.getKey(), ticker, dates);
        }

        return dailyPrices;
    }

    private DailyPrice dailyPriceWithRoundedDecimals(DailyPrice dailyPrice) {
        dailyPrice.setOpen(Double.parseDouble(String.format("%.4f", dailyPrice.getOpen())));
        dailyPrice.setHigh(Double.parseDouble(String.format("%.4f", dailyPrice.getHigh())));
        dailyPrice.setLow(Double.parseDouble(String.format("%.4f", dailyPrice.getLow())));
        dailyPrice.setClose(Double.parseDouble(String.format("%.4f", dailyPrice.getClose())));
//        if (dailyPrice.getOpen() < 1d) {
//            dailyPrice.setOpen(Math.round(dailyPrice.getOpen() * 100.0) / 100.0);
//            dailyPrice.setHigh(Math.round(dailyPrice.getHigh() * 100.0) / 100.0);
//            dailyPrice.setLow(Math.round(dailyPrice.getLow() * 100.0) / 100.0);
//            dailyPrice.setClose(Math.round(dailyPrice.getClose() * 100.0) / 100.0);
//        } else {
//            dailyPrice.setOpen(Math.round(dailyPrice.getOpen() * 1000.0) / 1000.0);
//            dailyPrice.setHigh(Math.round(dailyPrice.getHigh() * 1000.0) / 1000.0);
//            dailyPrice.setLow(Math.round(dailyPrice.getLow() * 1000.0) / 1000.0);
//            dailyPrice.setClose(Math.round(dailyPrice.getClose() * 1000.0) / 1000.0);
//        }
        return dailyPrice;
    }

    private String timeframeToQParam(StockTimeframe timeframe) {
        return switch (timeframe) {
            case DAILY -> "1d";
            case WEEKLY -> "1wk";
            case MONTHLY -> "1mo";
            case QUARTERLY -> throw new IllegalStateException("Unexpected value QUARTERLY");
            case YEARLY -> throw new IllegalStateException("Unexpected value YEARLY");
        };
    }


}
