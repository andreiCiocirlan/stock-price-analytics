package stock.price.analytics.client;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import stock.price.analytics.model.annual.FinancialData;
import stock.price.analytics.util.Constants;

@Slf4j
@Component
public class FinnhubClient {

    private final RestTemplate restTemplate;
    @Value("${finnhub.apikey}")
    private String apiKey;

    public FinnhubClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

//    public ResponseEntity<StockPrices> stockPricesFor(String ticker) {
//        ResponseEntity<StockPrices> response;
//        try {
//            response = restTemplate.getForEntity(BASE_URL + "/quote?symbol={ticker}&token={apiKey}",
//                    StockPrices.class, ticker, apiKey);
//
//            StockPrices stockPrices = response.getBody();
//            if (stockPrices != null) {
//                stockPrices.setTicker(ticker);
//                stockPrices.setDate(LocalDate.now());
//                if (stockPrices.getCurrentPrice() == 0.00d) {
//                    log.error("Data for ticker {} not found", ticker);
//                    return ResponseEntity.notFound().build();
//                }
//            }
//        } catch (RestClientException e) {
//            log.error("Failed retrieving prices data for ticker {}", ticker);
//            return ResponseEntity.internalServerError().build();
//        }
//        return response;
//    }

    public ResponseEntity<FinancialData> financialDataFor(String ticker) {
        try {
            return restTemplate.getForEntity(Constants.BASE_URL + "/stock/metric?symbol={ticker}&metric=all&token={apiKey}",
                    FinancialData.class, ticker, apiKey);
        } catch (RestClientException e) {
            log.error("Failed retrieving financial data for ticker {}", ticker);
            return ResponseEntity.internalServerError().build();
        }
    }

    public ResponseEntity<String> financialDataStringFor(String ticker) {
        try {
            return restTemplate.getForEntity(Constants.BASE_URL + "/stock/metric?symbol={ticker}&metric=all&token={apiKey}",
                    String.class, ticker, apiKey);
        } catch (RestClientException e) {
            log.error("Failed retrieving financial data String for ticker {}", ticker);
            return ResponseEntity.internalServerError().build();
        }
    }
}