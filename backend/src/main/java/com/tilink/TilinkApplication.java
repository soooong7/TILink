package com.tilink;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
// @ConfigurationProperties 클래스를 자동으로 등록한다. 설정 클래스가 늘어날 때마다
// @EnableConfigurationProperties 에 하나씩 추가하지 않아도 된다.
@ConfigurationPropertiesScan
public class TilinkApplication {

	public static void main(String[] args) {
		SpringApplication.run(TilinkApplication.class, args);
	}

}
