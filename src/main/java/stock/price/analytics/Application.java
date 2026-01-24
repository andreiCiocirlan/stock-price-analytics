package stock.price.analytics;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import stock.price.analytics.cache.CacheInitializationService;

@RequiredArgsConstructor
@SpringBootApplication
public class Application implements ApplicationRunner {

    private final CacheInitializationService cacheInitializationService;

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Override
    public void run(ApplicationArguments args) {
        cacheInitializationService.initAllCaches();
    }
}
