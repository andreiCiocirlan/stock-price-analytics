package stock.price.analytics.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import stock.price.analytics.model.BusinessEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static stock.price.analytics.util.LoggingUtil.logTime;

@Slf4j
@Service
public class SyncPersistenceService {

    public static final int BATCH_SIZE = 250;

    public void partitionDataAndSaveNoLogging(List<? extends BusinessEntity> entities, JpaRepository<? extends BusinessEntity, Long> repository) {
        try {
            partitionAndSave(entities, repository);
        } catch (Exception e) {
            log.error("Error saving entities synchronously", e);
        }
    }

    public void partitionDataAndSaveWithLogTime(List<? extends BusinessEntity> entities, JpaRepository<? extends BusinessEntity, Long> repository, String functionName) {
        try {
            logTime(() -> partitionAndSave(entities, repository), functionName);
        } catch (Exception e) {
            log.error("Error saving entities synchronously", e);
        }
    }

    public void partitionDataAndSave(List<? extends BusinessEntity> entities, JpaRepository<? extends BusinessEntity, Long> repository) {
        try {
            partitionAndSave(entities, repository);
            log.info("Saved {} rows of type: {} ", entities.size(), entities.getFirst().getClass().getName());
        } catch (Exception e) {
            log.error("Error saving entities synchronously", e);
        }
    }

    private <T, R extends BusinessEntity> void partitionAndSave(List<T> entities, JpaRepository<R, Long> repository) {
        if (entities.isEmpty()) {
            log.info("entities isEmpty");
            return;
        }
        List<List<T>> partitions = new ArrayList<>();
        for (int i = 0; i < entities.size(); i += BATCH_SIZE) {
            partitions.add(new ArrayList<>(entities.subList(i, Math.min(i + BATCH_SIZE, entities.size()))));
        }

        try (ExecutorService executor = Executors.newFixedThreadPool(10)) {
            for (List<T> partition : partitions) {
                executor.execute(() -> {
                    @SuppressWarnings("unchecked")
                    List<R> entitiesToSave = (List<R>) partition;
                    repository.saveAll(entitiesToSave);
                });
            }
        }
    }
}
