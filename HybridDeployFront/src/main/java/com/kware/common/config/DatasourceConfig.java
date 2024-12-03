package com.kware.common.config;


import javax.sql.DataSource;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;


//@Value("${baeldung.presentation}") 이런형태를 참고하여 외부에서 설정가능하도록 구성이 가능할 듯

@Configuration("dbconfig")
//@PropertySource("classpath:/application.yml")
//@ImportResource("classpath:/application.yml")
@EnableTransactionManagement
//@MapperScan(basePackages="com.stos.bs21.mng.suggest")
public class DatasourceConfig {
		
	@Autowired
	private ApplicationContext applicationContext;
	
	/*
	@PostConstruct
	public void init(){
		System.out.println("sug_dbConfigure postConctruct");
	}
	*/

	
	@Bean(name = "HikariConfig")
	@ConfigurationProperties(prefix="hybrid.datasource.hikari")
	public HikariConfig hikariConfig() {
		return new HikariConfig();
	}
	
	@Bean(destroyMethod="close", name="datasource")
	public DataSource dataSource() throws Exception {
		DataSource dataSource = new HikariDataSource(hikariConfig());
		return dataSource;
	}
	
	@Bean(name="sqlSessionFactory")
	public SqlSessionFactory sqlSessionFactory(@Qualifier("datasource")DataSource dataSource) throws Exception {
		SqlSessionFactoryBean sqlSessionFactoryBean = new SqlSessionFactoryBean();
		sqlSessionFactoryBean.setDataSource(dataSource);
		sqlSessionFactoryBean.setMapperLocations(applicationContext.getResources("classpath:/com/kware/**/mapper/*_mapper.xml"));
		sqlSessionFactoryBean.setConfigLocation(applicationContext.getResource("classpath:mybatis.xml"));
		return sqlSessionFactoryBean.getObject();
	}
		
	@Bean(name="sqlSessionTemplate")
	public SqlSessionTemplate sqlSessionTemplate(@Qualifier("sqlSessionFactory")SqlSessionFactory sqlSessionFactory) {
		return new SqlSessionTemplate(sqlSessionFactory);
	}
	
	
	@Bean(name="transactionManager")
	public PlatformTransactionManager txManager() throws Exception {
		return new DataSourceTransactionManager(dataSource());
	}
	
}
