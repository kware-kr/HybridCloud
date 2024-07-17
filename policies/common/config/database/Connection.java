package kware.common.config.database;

import cetus.config.AbstractDatabaseConnection;
import cetus.config.CetusConfig;
import cetus.log.MyBatisLogInterceptor;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;


@Configuration
public class Connection extends AbstractDatabaseConnection {
    
    public Connection(CetusConfig config) {
        super(config, "default");
    }
    
    @Override
    @Bean
    @Primary
    protected DataSource dataSource() {
        return getLazyConnectionDataSourceProxy(getDataSource());
    }
    
    @Override
    @Bean("sqlSession")
    @Primary
    protected SqlSessionFactory sqlSessionFactory(DataSource datasource,
                                                  MyBatisLogInterceptor interceptor) throws Exception {
        return getSqlSessionFactory(datasource, interceptor, true);
    }
    
    @Override
    @Bean(name="txManager")
    public PlatformTransactionManager transactionManager(DataSource dataSource) {
        return getTransactionManager(dataSource);
    }
}
