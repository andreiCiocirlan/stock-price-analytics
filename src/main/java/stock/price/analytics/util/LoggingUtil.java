package stock.price.analytics.util;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LoggingUtil {

    public static void logElapsedTime(Runnable func, String actionName) {
        long start = System.nanoTime();
        func.run();
        long duration = (System.nanoTime() - start) / 1_000_000;
        log.info("{} in {} ms", actionName, duration);
    }

}
