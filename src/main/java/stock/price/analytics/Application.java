package stock.price.analytics;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import stock.price.analytics.repository.stocks.StockRepository;

@RequiredArgsConstructor
@SpringBootApplication
public class Application implements ApplicationRunner {

    private final StockRepository stockRepository;

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        // update ipo/delisted dates at start-up
        stockRepository.updateIpoAndDelistedDates();
    }
}
