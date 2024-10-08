package com.kware.common.db;

import java.nio.file.Files;
import java.nio.file.Paths;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class InitDatabase {
	
	@Autowired
    private ApplicationContext applicationContext;
	
	@Value("${hybrid.init-sql-location.ddl}")
    private Resource ddlSql;

    //@Value("${hybrid.init-sql-location.insert-data}")
    //private Resource insertDataResource;
    
    @Value("${hybrid.init-sql-location.insert-data}")
    private String insertSql;

    
    @Transactional // 트랜잭션 관리
    public void initializeDatabase() throws Exception {
        DataSource dataSource = (DataSource) applicationContext.getBean("datasource");
		 
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		boolean schemaExists = checkIfSchemaExists(jdbcTemplate, "k_hybrid");
		
		if (!schemaExists) {
			if(log.isInfoEnabled())
				log.info("Schema does not exist. Creating schema and loading data.");
			
		    executeSqlFromFile(jdbcTemplate, ddlSql);
		    
		    PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources(insertSql);

		    // Insert SQL 파일 실행
            for (Resource sqlFile : resources) {
                executeSqlFromFile(jdbcTemplate, sqlFile);
            }
		} else {
			if(log.isInfoEnabled())
				log.info("Schema exists. Skipping initialization.");
		}
    }

    private boolean checkIfSchemaExists(JdbcTemplate jdbcTemplate, String schemaName) {
        String sql = "SELECT EXISTS (SELECT 1 FROM pg_namespace WHERE nspname = ?)";
        return jdbcTemplate.queryForObject(sql, Boolean.class, schemaName);
    }

    private void executeSqlFromFile(JdbcTemplate jdbcTemplate, Resource resource) {
        try {
            String sql = new String(Files.readAllBytes(Paths.get(resource.getURI())));
            jdbcTemplate.execute(sql);
        } catch (Exception e) {
            throw new RuntimeException("Failed to execute SQL file: " + resource.getFilename(), e);
        }
    }
}
