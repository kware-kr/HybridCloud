package com.kware.common.config;

import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.encryption.pbe.config.SimpleStringPBEConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.PropertySource;



/**
 * 
 * @author kljang
 * 여기에서 DB 접근 비번등을 테스트 하고 base64 출력을
 * 각 properties에 ENC[encryptedText]형식으로 처리한다.
 * 
 * 그리고 SpringBoot main 클래스에 
 * @EnableEncryptableProperties 어노테이션 입력한다.
 * 
 * 다음 사이트를 활용해서 처리해도 된다.
 * https://www.devglan.com/online-tools/jasypt-online-encryption-decryption
 */

@SpringBootTest
@PropertySource(name="EncryptedProperties", value = "classpath:dolphin_jaspy.yml")
public class AttributeEncTest {

	@Test
	public void EncTest() throws Exception{
		
		String key = "yjrPOLWm1jwhSXqbFiHeQ7NgqtNRlFj6h7M8jF8OVZMVDheML4AYnGRmFGT1bKdLabPIS4XJ/bqwQ499RcwrjQ==";
		
		StandardPBEStringEncryptor encryptor = new StandardPBEStringEncryptor();
		
		SimpleStringPBEConfig config = new SimpleStringPBEConfig();
        config.setPassword(key);
        config.setAlgorithm("PBEWITHHMACSHA512ANDAES_256");
        config.setKeyObtentionIterations("1000");
        config.setPoolSize("1");
        config.setProviderName("SunJCE");
        config.setSaltGeneratorClassName("org.jasypt.salt.RandomSaltGenerator");
        config.setIvGeneratorClassName("org.jasypt.iv.RandomIvGenerator");
        config.setStringOutputType("base64");
        encryptor.setConfig(config);
		
        
        String encryptedKey      = encryptor.encrypt(key);
		String encryptedUser     = encryptor.encrypt("postgres");
		String encryptedPassword = encryptor.encrypt("postgresql!@");
		
		
		System.out.println("encryptedKeyr: " + encryptedKey);
		System.out.println("encryptedUser: " + encryptedUser);
		System.out.println("encryptedPassword: " + encryptedPassword);
		
		
		
		System.out.println("decryptedUser: " +encryptor.decrypt(encryptedUser));
	}

}
