package stock.price.analytics.client.finnhub;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import stock.price.analytics.client.finnhub.dto.IntradayPriceDTO;
import stock.price.analytics.config.TradingDateUtil;
import stock.price.analytics.model.annual.FinancialData;
import stock.price.analytics.model.prices.ohlc.CandleOHLC;
import stock.price.analytics.model.prices.ohlc.DailyPriceOHLC;
import stock.price.analytics.util.FileUtils;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static stock.price.analytics.config.TradingDateUtil.tradingDateNow;
import static stock.price.analytics.util.Constants.FINNHUB_BASE_URL;

@Slf4j
@Component
public class FinnhubClient {

    private final RestTemplate restTemplate;
    @Value("${finnhub.apikey}")
    private String apiKey;

    public FinnhubClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public Optional<DailyPriceOHLC> intraDayPricesFor(String ticker) {
        DailyPriceOHLC response = null;
        try {
            ResponseEntity<IntradayPriceDTO> finnHubResponse = restTemplate.getForEntity(FINNHUB_BASE_URL + "/quote?symbol={ticker}&token={apiKey}",
                    IntradayPriceDTO.class, ticker, apiKey);

            IntradayPriceDTO intraDayPrice = finnHubResponse.getBody();
            LocalDate tradingDate = tradingDateNow();
            if (intraDayPrice != null) {
                response = new DailyPriceOHLC(ticker, tradingDate, Math.round(intraDayPrice.getPercentChange() * 100.0) / 100.0,
                        getCandleOHLC(intraDayPrice));
            } else {
                log.error("intraDayPrice null for ticker {}", ticker);
            }
        } catch (RestClientException | IllegalArgumentException e) {
            log.error("Failed retrieving prices data for ticker {}", ticker);
            return Optional.empty();
        }
        return Optional.ofNullable(response);
    }

    private CandleOHLC getCandleOHLC(IntradayPriceDTO intraDayPrice) {
        return new CandleOHLC(
                Math.round(intraDayPrice.getOpen() * 100.0) / 100.0,
                Math.round(intraDayPrice.getHigh() * 100.0) / 100.0,
                Math.round(intraDayPrice.getLow() * 100.0) / 100.0,
                Math.round(intraDayPrice.getClose() * 100.0) / 100.0
        );
    }

    public ResponseEntity<FinancialData> financialDataFor(String ticker) {
        try {
            return restTemplate.getForEntity(STR."\{FINNHUB_BASE_URL}/stock/metric?symbol={ticker}&metric=all&token={apiKey}",
                    FinancialData.class, ticker, apiKey);
        } catch (RestClientException e) {
            log.error("Failed retrieving financial data for ticker {}", ticker);
            return ResponseEntity.internalServerError().build();
        }
    }

    public ResponseEntity<String> financialDataStringFor(String ticker) {
        try {
            return restTemplate.getForEntity(STR."\{FINNHUB_BASE_URL}/stock/metric?symbol={ticker}&metric=all&token={apiKey}",
                    String.class, ticker, apiKey);
        } catch (RestClientException e) {
            log.error("Failed retrieving financial data String for ticker {}", ticker);
            return ResponseEntity.internalServerError().build();
        }
    }

    public List<DailyPriceOHLC> intraDayPricesXTB() {
        try {
            List<String> tickers = FileUtils.readTickersXTB();
            List<DailyPriceOHLC> dailyPriceOHLCs = new ArrayList<>();

            for (String ticker : tickers) {
                Optional<DailyPriceOHLC> intraDayPrice = intraDayPricesFor(ticker);
                intraDayPrice.ifPresent(dailyPrices -> addToListAndLog(dailyPrices, dailyPriceOHLCs));
                Thread.sleep(1005); // rate limit 60 req / min
            }
            return dailyPriceOHLCs;
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void addToListAndLog(DailyPriceOHLC dailyPrices, List<DailyPriceOHLC> dailyPriceOHLCs) {
        log.info("{}", dailyPrices);
        dailyPriceOHLCs.add(dailyPrices);
    }
}