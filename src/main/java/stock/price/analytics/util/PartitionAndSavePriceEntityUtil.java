package stock.price.analytics.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import stock.price.analytics.model.prices.PriceEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static stock.price.analytics.util.Constants.NR_THREADS;

@Slf4j
@Component
public class PartitionAndSavePriceEntityUtil {

    public static <T, R extends PriceEntity> void partitionDataAndSave(List<T> entities, JpaRepository<R, Long> repository) {
        if (entities.isEmpty()) {
            log.info("entities isEmpty");
            return;
        }
        List<List<T>> partitions = new ArrayList<>();
        for (int i = 0; i < entities.size(); i += 250) { // default batchSize to 250 like in application.properties
            partitions.add(entities.subList(i, Math.min(i + 250, entities.size())));
        }
        save(partitions, repository);
        log.info("Saved {} rows of type: {} ", entities.size(), entities.getFirst().getClass().getName());
    }

    private static <T, R extends PriceEntity> void save(List<List<T>> partitions, JpaRepository<R, Long> repository) {
        List<Callable<Void>> callables = partitions.stream().map(sublist ->
                (Callable<Void>) () -> {
                    @SuppressWarnings("unchecked")
                    List<R> rs = (List<R>) sublist;
                    repository.saveAll( rs);
                    return null;
                }).collect(Collectors.toList());
        ExecutorService executorService = Executors.newFixedThreadPool(NR_THREADS);
        try {
            executorService.invokeAll(callables);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            executorService.shutdown();
        }
    }

}
