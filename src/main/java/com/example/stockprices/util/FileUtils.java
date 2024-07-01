package com.example.stockprices.util;

import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static com.example.stockprices.util.Constants.CLASSPATH_TICKERS_XTB_TXT;

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

}
