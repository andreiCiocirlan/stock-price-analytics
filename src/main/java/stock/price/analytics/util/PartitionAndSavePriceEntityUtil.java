package stock.price.analytics.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import stock.price.analytics.model.prices.PriceEntity;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class PartitionAndSavePriceEntityUtil {

    public static <T, R extends PriceEntity> void partitionDataAndSave(List<T> entities, JpaRepository<R, Long> repository) {
        partitionDataAndSave(entities, repository, true);
    }

    public static <T, R extends PriceEntity> void partitionDataAndSaveNoLogging(List<T> entities, JpaRepository<R, Long> repository) {
        partitionDataAndSave(entities, repository, false);
    }

    public static <T, R extends PriceEntity> void partitionDataAndSave(List<T> entities, JpaRepository<R, Long> repository, boolean logging) {
        if (entities.isEmpty()) {
            log.info("entities isEmpty");
            return;
        }
        List<List<T>> partitions = new ArrayList<>();
        for (int i = 0; i < entities.size(); i += 250) { // default batchSize to 250 like in application.properties
            partitions.add(entities.subList(i, Math.min(i + 250, entities.size())));
        }
        save(partitions, repository);
        if (logging) {
            log.info("Saved {} rows of type: {} ", entities.size(), entities.getFirst().getClass().getName());
        }
    }

    private static <T, R extends PriceEntity> void save(List<List<T>> partitions, JpaRepository<R, Long> repository) {
        for (List<T> partition : partitions) {
            try {
                List<R> rs = (List<R>) partition;
                repository.saveAllAndFlush(rs);
            } catch (Exception e) {
                log.error("Error saving partition: {}", e.getMessage(), e);
                throw e; // Re-throw to stop execution if save fails
            }
        }
    }

}
