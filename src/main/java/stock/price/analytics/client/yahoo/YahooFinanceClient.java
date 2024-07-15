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
import static stock.price.analytics.config.TradingDateUtil.tradingDateNow;

@Slf4j
@Component
@RequiredArgsConstructor
public class YahooFinanceClient {

    public List<DailyPriceOHLC> dailyPricesFromFile(String fileName) {
        try {
            String jsonFilePath = String.join("", "./yahoo-daily-prices/", fileName, ".json");
            String jsonData = String.join("", readAllLines(Path.of(jsonFilePath)));

            return extractDailyPricesFromJSON(jsonData);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public List<DailyPriceOHLC> yFinDailyPricesFrom(String jsonData) {
        try {
            return extractDailyPricesFromJSON(jsonData);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static List<DailyPriceOHLC> extractDailyPricesFromJSON(String jsonData) throws JsonProcessingException {
        List<DailyPriceOHLC> intraDayPrices = new ArrayList<>();
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(jsonData);
        JsonNode quoteResponse = jsonNode.get("quoteResponse");
        JsonNode resultArray = quoteResponse.get("result");

        for (JsonNode node : resultArray) {
            if ("null".equals(node.get("symbol").asText())) {
                continue;
            }
            String ticker = node.get("symbol").asText();
            if (node.has("regularMarketPrice") && node.has("regularMarketDayHigh") &&
                    node.has("regularMarketDayLow") && node.has("regularMarketOpen") &&
                    node.has("regularMarketChangePercent") && node.has("regularMarketTime")) {
                try {
                    LocalDate tradingDate = Instant.ofEpochSecond(node.get("regularMarketTime").asLong()).atZone(ZoneId.systemDefault()).toLocalDate();
                    if (!tradingDateNow().equals(tradingDate)) {
                        log.warn("Not extracting delisted stock daily prices for ticker {} and date {}", ticker, tradingDate);
                        continue;
                    }
                    double percentChange = Math.round(node.get("regularMarketChangePercent").asDouble() * 100.0) / 100.0;
                    intraDayPrices.add(new DailyPriceOHLC(ticker, tradingDate, percentChange,
                            new CandleOHLC(node.get("regularMarketOpen").asDouble(), node.get("regularMarketDayHigh").asDouble(),
                                    node.get("regularMarketDayLow").asDouble(), node.get("regularMarketPrice").asDouble())));
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
