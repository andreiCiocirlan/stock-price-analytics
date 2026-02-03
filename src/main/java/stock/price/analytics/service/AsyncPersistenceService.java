package stock.price.analytics.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import stock.price.analytics.model.BusinessEntity;

import java.util.List;

@Slf4j
@Service
@Async("virtualThreadTaskExecutor")
public class AsyncPersistenceService extends SyncPersistenceService {

    @Override
    public void partitionDataAndSaveNoLogging(List<? extends BusinessEntity> entities, JpaRepository<? extends BusinessEntity, Long> repository) {
        try {
            super.partitionDataAndSaveNoLogging(entities, repository);
        } catch (Exception e) {
            log.error("Error in async save", e);
        }
    }

    @Override
    public void partitionDataAndSaveWithLogTime(List<? extends BusinessEntity> entities, JpaRepository<? extends BusinessEntity, Long> repository, String functionName) {
        try {
            super.partitionDataAndSaveWithLogTime(entities, repository, functionName);
        } catch (Exception e) {
            log.error("Error in async save", e);
        }
    }

    @Override
    public void partitionDataAndSave(List<? extends BusinessEntity> entities, JpaRepository<? extends BusinessEntity, Long> repository) {
        try {
            super.partitionDataAndSave(entities, repository);
        } catch (Exception e) {
            log.error("Error in async save", e);
        }
    }
}