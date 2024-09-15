package stock.price.analytics.client.yahoo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import stock.price.analytics.model.prices.ohlc.CandleOHLC;
import stock.price.analytics.model.prices.ohlc.DailyPriceOHLC;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static java.nio.file.Files.readAllLines;
import static stock.price.analytics.util.TradingDateUtil.tradingDateNow;

@Slf4j
@Component
@RequiredArgsConstructor
public class YahooFinanceClient {

    public List<DailyPriceOHLC> dailyPricesFromFile(String fileName) {
        try {
            String jsonFilePath = String.join("", "./yahoo-daily-prices/", fileName, ".json");
            String jsonData = String.join("", readAllLines(Path.of(jsonFilePath)));

            return extractDailyPricesFromJSON(jsonData, false);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public List<DailyPriceOHLC> extractDailyPricesFromJSON(String jsonData, boolean preMarketPrices) {
        List<DailyPriceOHLC> intraDayPrices = new ArrayList<>();
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode;
        try {
            jsonNode = objectMapper.readTree(jsonData);
        } catch (JsonProcessingException ex) {
            throw new RuntimeException(ex);
        }

        JsonNode quoteResponse = jsonNode.get("quoteResponse");
        JsonNode resultArray = quoteResponse.get("result");

        for (JsonNode node : resultArray) {
            String ticker = node.get("symbol").asText();
            String marketPrice = preMarketPrices ? "preMarketPrice" : "regularMarketPrice";
            String marketChangePercent = preMarketPrices ? "preMarketChangePercent" : "regularMarketChangePercent";
            if (node.has(marketPrice) && node.has("regularMarketDayHigh") &&
                    node.has("regularMarketDayLow") && node.has("regularMarketOpen") &&
                    node.has(marketChangePercent) && node.has("regularMarketTime")) {
                try {
                    LocalDate tradingDate = Instant.ofEpochSecond(node.get("regularMarketTime").asLong()).atZone(ZoneId.systemDefault()).toLocalDate();
                    LocalDate tradingDateNow = tradingDateNow();
                    if (!tradingDateNow.equals(tradingDate)) {
                        if (tradingDate.plusDays(5).isBefore(tradingDateNow)) {
                            // more than 5 days passed since last intraDay price
                            log.warn("Not extracting delisted stock daily prices for ticker {} and date {}", ticker, tradingDate);
                            continue;
                        } else {
                            // less than 5 days passed since last intraDay price
                            log.warn("Extracting stock daily prices for ticker {} and date {}", ticker, tradingDate);
                        }
                    }
                    double percentChange = Math.round(node.get(marketChangePercent).asDouble() * 100.0) / 100.0;
                    intraDayPrices.add(new DailyPriceOHLC(ticker, tradingDate, percentChange,
                            new CandleOHLC(node.get("regularMarketOpen").asDouble(), node.get("regularMarketDayHigh").asDouble(),
                                    node.get("regularMarketDayLow").asDouble(), node.get(marketPrice).asDouble())));
                } catch (IllegalArgumentException e) {
                    log.warn("Unable to extract daily prices for ticker {} error: {}", ticker, e.getMessage());
                }
            } else {
                log.warn("Unable to extract daily prices for ticker {}. Incomplete JSON data!", ticker);
            }
        }
        return intraDayPrices;
    }
}
