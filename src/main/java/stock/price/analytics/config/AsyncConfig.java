package stock.price.analytics.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.VirtualThreadTaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean("virtualThreadTaskExecutor")
    public TaskExecutor virtualThreadTaskExecutor() {
        return new VirtualThreadTaskExecutor();
    }
}
