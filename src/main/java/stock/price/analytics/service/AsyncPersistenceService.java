package stock.price.analytics.service;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import stock.price.analytics.model.BusinessEntity;

import java.util.List;

@Service
public class AsyncPersistenceService extends SyncPersistenceService {

    @Async
    @Override
    public void partitionDataAndSaveNoLogging(List<? extends BusinessEntity> entities, JpaRepository<? extends BusinessEntity, Long> repository) {
        super.partitionDataAndSaveNoLogging(entities, repository);
    }

    @Async
    @Override
    public void partitionDataAndSaveWithLogTime(List<? extends BusinessEntity> entities, JpaRepository<? extends BusinessEntity, Long> repository, String functionName) {
        super.partitionDataAndSaveWithLogTime(entities, repository, functionName);
    }

    @Async
    @Override
    public void partitionDataAndSave(List<? extends BusinessEntity> entities, JpaRepository<? extends BusinessEntity, Long> repository) {
        super.partitionDataAndSave(entities, repository);
    }
}