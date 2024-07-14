package stock.price.analytics.client.yahoo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import stock.price.analytics.config.TradingDateUtil;
import stock.price.analytics.model.prices.ohlc.CandleOHLC;
import stock.price.analytics.model.prices.ohlc.DailyPriceOHLC;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static java.nio.file.Files.readAllLines;

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
            String path = String.join("", "./yahoo-daily-prices/", TradingDateUtil.tradingDateNow().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")), ".json");
            File jsonFile = new File(path);

            try (OutputStream outputStream = new FileOutputStream(jsonFile)) {
                outputStream.write(jsonData.getBytes(StandardCharsets.UTF_8));
            }
            log.info("saved daily prices file {}", jsonFile.getAbsolutePath());

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
                    if (!TradingDateUtil.tradingDateNow().equals(tradingDate)) {
                        log.warn("Not importing delisted stock daily prices for ticker {} and date {}", ticker, tradingDate);
                        continue;
                    }
                    double percentChange = Math.round(node.get("regularMarketChangePercent").asDouble() * 100.0) / 100.0;
                    intraDayPrices.add(new DailyPriceOHLC(ticker, tradingDate, percentChange,
                            new CandleOHLC(node.get("regularMarketOpen").asDouble(), node.get("regularMarketDayHigh").asDouble(),
                                    node.get("regularMarketDayLow").asDouble(), node.get("regularMarketPrice").asDouble())));
                } catch (IllegalArgumentException e) {
                    log.warn("Unable to import daily prices for ticker {} error: {}", ticker, e.getMessage());
                }
            } else {
                log.warn("Unable to import daily prices for ticker {}. Incomplete JSON data!", ticker);
            }
        }
        return intraDayPrices;
    }
}
