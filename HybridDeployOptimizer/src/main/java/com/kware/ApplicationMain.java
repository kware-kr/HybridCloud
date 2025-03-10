package com.kware;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.ApplicationPidFileWriter;
import org.springframework.boot.web.embedded.jetty.JettyServletWebServerFactory;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.embedded.undertow.UndertowServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import lombok.extern.slf4j.Slf4j;

/**
 * SpringBoot Application기반으로 일반 Application 프로젝트를 개발한다.
 * 각 Application에서 openApi를 제공하기 위함
 * 1. springboot application 실행
 *    db 설정
 *    환경 설정
 * 2. 일반 application 수행
 *    일반 환경설정: 환경분리
 *    기본 프로퍼티인 application.yml을 제외한 config 파일을 포함할려면 spring.config.import 프로퍼티 파일을 설정(다중파일 ,구분).
 * 
 *  * 다음 사이트를 활용해서 속성의 암호화 처리해도 된다.
 * https://www.devglan.com/online-tools/jasypt-online-encryption-decryption
 * 
 * @author kljang
 *
 *--실행방법
 *java -jar -Dspring.main.web-application-type=none ./SensorUDPCollector.jar WAS 미실행
 *java -jar ./SensorUDPCollector.jar WAS 실행
 */

@Slf4j
@SpringBootApplication(scanBasePackages = {"com.kware"})
@Configuration("main")
public class ApplicationMain {
	//static Logger log = LoggerFactory.getLogger(ApplicationMain.class);
	
	
	@PostConstruct
	public void init(){
	}

		
	@PreDestroy
	public void onExit() {
	}
	
	
	private static String WAS_NAME = null;
	@Bean
    public WebServerFactoryCustomizer webServerFactoryCustomizer() {
        return factory -> {
            if (factory instanceof TomcatServletWebServerFactory) {
            	WAS_NAME = "Tomcat";
            } else if (factory instanceof JettyServletWebServerFactory) {
            	WAS_NAME = "Jetty";
            } else if (factory instanceof UndertowServletWebServerFactory) {
            	WAS_NAME = "Undertow";
            } else {
            	WAS_NAME= "기타";
            }
        };
    }
	
	
	public ApplicationMain(){
		 if(log.isInfoEnabled())
			 log.info("Application START ===========================================!");
	}
	
	//@PostConstruct
		
	public static void main(String[] args) {
		//if(log.isInfoEnabled())
        //	log.info("PID:{}",new ApplicationPid().toString());
		
		SpringApplication application = new SpringApplication(ApplicationMain.class);
        application.addListeners(new ApplicationPidFileWriter()); //pid를 spring.pid.file에 있는 경로에 생성한다.
        //application.setWebApplicationType(WebApplicationType.NONE); //웹서버를 실행하지 못하도록 처리한다.
        //application.yml ==> spring.main.web-application-type: none
        ApplicationContext applicationContext = application.run(args);
        Environment environment = applicationContext.getEnvironment();
    	String serverPort = environment.getProperty("server.port");
    	
    	if(log.isInfoEnabled())
    		log.info("======================= {} Server is running on port: {} ===========================",WAS_NAME, serverPort);
	}
	
}
