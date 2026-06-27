package stock.price.analytics.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

public class TickerChangeScanner {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static void main(String[] args) throws IOException {

        Path folder = Paths.get(
                "C:\\Users/andre/IdeaProjects/yahoo-daily-prices");

        Set<TickerChange> changes = new HashSet<>();

        try (Stream<Path> files = Files.list(folder)) {

            files.filter(path -> path.toString().endsWith(".json"))
                    .forEach(path -> scanFile(path, changes));
        }

        changes.stream()
                .sorted(Comparator
                        .comparing(TickerChange::effectiveDate).reversed()
                        .thenComparing(TickerChange::oldTicker))
                .limit(15)
                .forEach(change -> {

                        System.out.printf(
                                "%s will be renamed to %s on %s%n",
                                change.oldTicker(),
                                change.newTicker(),
                                change.effectiveDate()
                        );
                });
    }

    private static void scanFile(
            Path file,
            Set<TickerChange> changes) {

        try {

            JsonNode root = MAPPER.readTree(file.toFile());

            JsonNode results = root.path("quoteResponse")
                    .path("result");

            if (!results.isArray()) {
                System.out.println("No result array found in " + file);
                return;
            }

            for (JsonNode stock : results) {

                String oldTicker = stock.path("symbol").asText();

                JsonNode corporateActions =
                        stock.path("corporateActions");

                if (!corporateActions.isArray()) {
                    continue;
                }

                for (JsonNode action : corporateActions) {

                    JsonNode meta = action.path("meta");

                    String eventType =
                            meta.path("eventType").asText();

                    if (!"TICKER_CHANGE".equals(eventType)) {
                        continue;
                    }

                    String newTicker =
                            meta.path("newTicker").asText();

                    long epochMs =
                            meta.path("dateEpochMs").asLong();

                    LocalDate effectiveDate =
                            Instant.ofEpochMilli(epochMs)
                                    .atZone(ZoneOffset.UTC)
                                    .toLocalDate();

                    changes.add(
                            new TickerChange(
                                    oldTicker,
                                    newTicker,
                                    effectiveDate
                            )
                    );
                }
            }

        } catch (Exception e) {
            System.err.printf(
                    "Failed processing %s: %s%n",
                    file,
                    e.getMessage()
            );
        }
    }

    public record TickerChange(
            String oldTicker,
            String newTicker,
            LocalDate effectiveDate
    ) {
    }
}