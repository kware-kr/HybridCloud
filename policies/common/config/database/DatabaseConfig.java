package kware.common.config.database;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.transaction.ChainedTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class DatabaseConfig {

    @Bean
    @Primary
    public PlatformTransactionManager transactionManager(
        @Qualifier("txManager") PlatformTransactionManager txManager) {
        return new ChainedTransactionManager(txManager);
    }

}
