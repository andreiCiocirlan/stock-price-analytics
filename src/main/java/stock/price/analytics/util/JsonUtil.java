package stock.price.analytics.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import stock.price.analytics.model.prices.ohlc.CandleOHLC;
import stock.price.analytics.model.prices.ohlc.DailyPrice;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public final class JsonUtil {

    public static String mergedPricesJSONs(List<String> pricesJSONs) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            ObjectNode mergedQuoteResponse = objectMapper.createObjectNode();
            ArrayNode mergedResults = objectMapper.createArrayNode();
            for (String json : pricesJSONs) {
                JsonNode rootNode = objectMapper.readTree(json);
                JsonNode results = rootNode.path("quoteResponse").path("result");

                if (results.isArray()) {
                    results.forEach(mergedResults::add);
                }
            }
            mergedQuoteResponse.set("result", mergedResults);
            ObjectNode finalResponse = objectMapper.createObjectNode();
            finalResponse.set("quoteResponse", mergedQuoteResponse);

            return objectMapper.writeValueAsString(finalResponse);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<DailyPrice> extractDailyPricesFrom(String ticker, String jsonResponse) throws JsonProcessingException {
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

    private static DailyPrice dailyPriceWithRoundedDecimals(DailyPrice dailyPrice) {
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
}
