package stock.price.analytics.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.ResourceUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static stock.price.analytics.util.Constants.CLASSPATH_TICKERS_XTB_TXT;

@Slf4j
public class FileUtils {

    public static List<String> readTickersXTB() throws IOException {
        return new ArrayList<>(Files.readAllLines(Paths.get(ResourceUtils.getFile(CLASSPATH_TICKERS_XTB_TXT).getAbsolutePath())));
    }

    public static void writeToFile(List<Object> objectList, File outputFile) {
        try (PrintWriter pw = new PrintWriter(outputFile)) {
            objectList.forEach(pw::println);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

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

}
