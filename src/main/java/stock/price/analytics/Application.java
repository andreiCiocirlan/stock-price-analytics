package stock.price.analytics;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import stock.price.analytics.repository.stocks.StockRepository;

@Slf4j
@RequiredArgsConstructor
@SpringBootApplication
public class Application implements ApplicationRunner {

    private final StockRepository stockRepository;

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        // update ipo/delisted dates at start-up
        stockRepository.updateIpoAndDelistedDates();
    }
}
