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
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

public class TickerChangeScanner {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final LocalDate AFTER_DATE = LocalDate.of(2026, 5, 30);
    private static final DateTimeFormatter FILE_DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    public static void main(String[] args) throws IOException {

        Path folder = Paths.get(
                "C:\\Users/andre/IdeaProjects/yahoo-daily-prices");

        Set<TickerChange> changes = new HashSet<>();

        try (Stream<Path> files = Files.list(folder)) {

            files.filter(path -> path.toString().endsWith(".json"))
                    .filter(path -> isAfterDate(path, AFTER_DATE))
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

    private static boolean isAfterDate(Path path, LocalDate afterDate) {
        String fileName = path.getFileName().toString();

        if (!fileName.endsWith(".json")) {
            return false;
        }

        String datePart = fileName.substring(0, 10);

        try {
            LocalDate fileDate = LocalDate.parse(datePart, FILE_DATE_FORMAT);
            return fileDate.isAfter(afterDate);
        } catch (Exception e) {
            System.err.printf("Skipping file with invalid date name: %s%n", fileName);
            return false;
        }
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