package stock.price.analytics.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import stock.price.analytics.model.prices.BusinessEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static stock.price.analytics.util.LoggingUtil.logTime;

@Slf4j
@Component
public class PartitionAndSavePriceEntityUtil {

    public static <T, R extends BusinessEntity> void partitionDataAndSaveWithLogTime(List<T> entities, JpaRepository<R, Long> repository, String functionName) {
        logTime(() -> partitionAndSave(entities, repository), functionName);
    }

    public static <T, R extends BusinessEntity> void partitionDataAndSave(List<T> entities, JpaRepository<R, Long> repository) {
        partitionAndSave(entities, repository);
        log.info("Saved {} rows of type: {} ", entities.size(), entities.getFirst().getClass().getName());
    }

    public static <T, R extends BusinessEntity> void partitionDataAndSaveNoLogging(List<T> entities, JpaRepository<R, Long> repository) {
        partitionAndSave(entities, repository);
    }

    private static <T, R extends BusinessEntity> void partitionAndSave(List<T> entities, JpaRepository<R, Long> repository) {
        if (entities.isEmpty()) {
            log.info("entities isEmpty");
            return;
        }
        List<List<T>> partitions = new ArrayList<>();
        for (int i = 0; i < entities.size(); i += 250) { // default batchSize to 250 like in application.properties
            partitions.add(entities.subList(i, Math.min(i + 250, entities.size())));
        }
        List<CompletableFuture<Void>> futures = partitions.parallelStream()
                .map(partition -> CompletableFuture.runAsync(() -> {
                    @SuppressWarnings("unchecked")
                    List<R> entitiesToSave = (List<R>) partition;
                    repository.saveAll(entitiesToSave);
                }))
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }

}
