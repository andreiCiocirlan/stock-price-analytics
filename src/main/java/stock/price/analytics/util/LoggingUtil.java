package stock.price.analytics.util;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Callable;

@Slf4j
public final class LoggingUtil {

    public static void logTime(Runnable func, String actionName) {
        long start = System.nanoTime();
        func.run();
        long duration = (System.nanoTime() - start) / 1_000_000;
        log.info("{} in {} ms", actionName, duration);
    }

    public static <T> T logTimeAndReturn(Callable<T> func, String actionName) {
        long start = System.nanoTime();
        T result = null;
        try {
            result = func.call();
        } catch (Exception e) {
            log.error("Error executing {}: {}", actionName, e.getMessage());
        }
        long duration = (System.nanoTime() - start) / 1_000_000;
        log.info("{} in {} ms", actionName, duration);
        return result;
    }

}
