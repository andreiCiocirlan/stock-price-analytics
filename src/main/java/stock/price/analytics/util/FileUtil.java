package stock.price.analytics.util;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
public final class FileUtil {


    public static void writeToFile(String filePath, String jsonData) {
        try {
            File jsonFile = new File(filePath);
            try (OutputStream outputStream = new FileOutputStream(jsonFile)) {
                outputStream.write(jsonData.getBytes(StandardCharsets.UTF_8));
            }
            log.info("saved daily prices file {}", jsonFile.getAbsolutePath());
        } catch (IOException e) {
            log.error("Error writing to file: {}", filePath, e);
        }
    }

    public static boolean fileExistsFor(String ticker) {
        String jsonFilePath = String.join("", "./all-historical-prices/DAILY/", ticker, ".json");
        return Files.exists(Path.of(jsonFilePath)) && Files.isRegularFile(Path.of(jsonFilePath));
    }
}
