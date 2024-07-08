package stock.price.analytics.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ResourceUtils;
import org.springframework.web.bind.annotation.*;
import stock.price.analytics.client.FinnhubClient;
import stock.price.analytics.service.StockFinancialDataService;
import stock.price.analytics.util.Constants;
import stock.price.analytics.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

@Slf4j
@RequestMapping("/financial-data")
@RestController
public class StockFinancialDataController {

    private final StockFinancialDataService stockFinancialDataService;
    private final FinnhubClient finnhubClient;

    @Value("${stock.financial.data.input.directory}")
    private String stockFinancialDataInputDirectory;

    public StockFinancialDataController(StockFinancialDataService stockFinancialDataService, FinnhubClient finnhubClient) {
        this.stockFinancialDataService = stockFinancialDataService;
        this.finnhubClient = finnhubClient;
    }

    @PostMapping("/tickers-to-json-files")
    @ResponseStatus(HttpStatus.OK)
    public void financialDataToJSONFile() throws IOException, InterruptedException {
        File file = ResourceUtils.getFile(Constants.CLASSPATH_TICKERS_XTB_TXT);

        for (String ticker : FileUtils.readTickersXTB()) {
            Thread.sleep(1100); // do nothing for 1.1 second to prevent rate-limiting

            ResponseEntity<String> responseEntity = finnhubClient.financialDataStringFor(ticker);
            File outputFile = new File(STR."\{stockFinancialDataInputDirectory}\{ticker}.json");
            log.info("Writing data to file {}", outputFile.getAbsolutePath());
            FileUtils.writeToFile(Collections.singletonList(responseEntity.getBody()), outputFile);
        }
    }

    @GetMapping("/{ticker}")
    public ResponseEntity<String> financialData(@PathVariable String ticker) {
        ResponseEntity<String> responseEntity = finnhubClient.financialDataStringFor(ticker);
        File outputFile = new File(STR."C:\\Users/andre/IdeaProjects/stock-price-analytics/src/main/resources/financial_data_\{ticker}.json");
        FileUtils.writeToFile(Collections.singletonList(responseEntity.getBody()), outputFile);

        return responseEntity;
    }

    @PostMapping("/db_save/{ticker}")
    @ResponseStatus(HttpStatus.OK)
    public void financialDataSave(@PathVariable String ticker) {
        File file = new File(STR."\{stockFinancialDataInputDirectory}\{ticker}.json");
//        stockFinancialDataService.saveFinancialDataFromJSON(file);
    }

    @PostMapping("/db_save_all")
    @ResponseStatus(HttpStatus.OK)
    public void financialDataSaveAll() throws IOException {
//        Files.walk(Paths.get(stockFinancialDataInputDirectory), 0).filter(Files::isRegularFile)
//                .parallel()
//                .map(Path::toFile)
//                .forEach(stockFinancialDataService::saveFinancialDataFromJSON);
    }



}