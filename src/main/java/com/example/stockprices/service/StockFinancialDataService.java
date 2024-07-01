package com.example.stockprices.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class StockFinancialDataService {

//    private final StockFinancialDataRepository stockFinancialDataRepository;
//    private final FinnhubClient finnhubClient;
//
//    public StockFinancialDataService(StockFinancialDataRepository stockFinancialDataRepository, FinnhubClient finnhubClient) {
//        this.stockFinancialDataRepository = stockFinancialDataRepository;
//        this.finnhubClient = finnhubClient;
//    }
//
//    public ResponseEntity<FinancialData> financialDataFor(String ticker) {
//        return finnhubClient.financialDataFor(ticker);
//    }
//
//    public ResponseEntity<String> financialDataStringFor(String ticker) {
//        return finnhubClient.financialDataStringFor(ticker);
//    }
//
//    @Transactional
//    public void saveFinancialDataFromJSON(File file) {
//        try {
//            FinancialData stockFinancialData = new ObjectMapper().readValue(file, FinancialData.class);
//            Quarterly quarterly = stockFinancialData.getSeries().getQuarterly();
//            log.info("Saving symbol: {} with bookValue: {} ", stockFinancialData.getSymbol(), quarterly != null ? quarterly.getBookValueQ() : null);
//            stockFinancialDataRepository.save(stockFinancialData);
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//    }

}