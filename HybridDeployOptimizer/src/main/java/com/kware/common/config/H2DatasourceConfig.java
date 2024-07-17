package com.kware.common.config;

import javax.sql.DataSource;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

@ConditionalOnProperty(name = "hybrid.policy.server.h2.enable", havingValue = "true")
@Configuration("h2-dbconfig")
//@PropertySource("classpath:/application.yml")
//@ImportResource("classpath:/application.yml")
@EnableTransactionManagement
//@MapperScan(basePackages="com.stos.bs21.mng.suggest")
@DependsOn("main")
//@ConditionalOnProperty(name = "hybrid.policy.enable", havingValue = "true")
public class H2DatasourceConfig {
		
	@Autowired
	private ApplicationContext applicationContext;
	
	/*
	@PostConstruct
	public void init(){
		System.out.println("sug_dbConfigure postConctruct");
	}
	*/

	
	@Bean(name = "h2-HikariConfig")
	@ConfigurationProperties(prefix="hybrid.policy.datasource.hikari")
	public HikariConfig hikariConfig() {
		return new HikariConfig();
	}
	
	@Bean(destroyMethod="close", name="h2-datasource")
	public DataSource dataSource() throws Exception {
		
		//Server.createTcpServer("-tcp", "-tcpAllowOthers", "-tcpPort", "9093").start();
		
		DataSource dataSource = new HikariDataSource(hikariConfig());
		return dataSource;
	}
	
	@Bean(name="h2-sqlSessionFactory")
	public SqlSessionFactory sqlSessionFactory(@Qualifier("h2-datasource")DataSource dataSource) throws Exception {
		SqlSessionFactoryBean sqlSessionFactoryBean = new SqlSessionFactoryBean();
		sqlSessionFactoryBean.setDataSource(dataSource);
		sqlSessionFactoryBean.setMapperLocations(applicationContext.getResources("classpath:/com/kware/aaa/**/mapper/*.xml"));
		return sqlSessionFactoryBean.getObject();
	}
	
	@Bean(name="h2-sqlSessionTemplate")
	public SqlSessionTemplate sqlSessionTemplate(@Qualifier("h2-sqlSessionFactory")SqlSessionFactory sqlSessionFactory) {
		return new SqlSessionTemplate(sqlSessionFactory);
	}
	
	
	@Bean(name="h2-transactionManager")
	public PlatformTransactionManager txManager() throws Exception {
		return new DataSourceTransactionManager(dataSource());
	}
	
}

