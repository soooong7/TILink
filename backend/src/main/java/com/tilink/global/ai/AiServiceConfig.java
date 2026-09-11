package com.tilink.global.ai;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

// 설정 클래스(AiServiceProperties)는 TilinkApplication 의 @ConfigurationPropertiesScan 이 등록한다.
@Configuration
public class AiServiceConfig {

    /**
     * AI 서비스 전용 WebClient. baseUrl 을 여기서 한 번만 박아 두고 호출부는 경로만 쓴다.
     *
     * <p>주입받은 {@code WebClient.Builder} 가 아니라 {@code WebClient.builder()} 로 직접
     * 만든다. 이 프로젝트는 서버를 Web MVC 로 돌리고 WebFlux 는 클라이언트 용도로만 쓰기
     * 때문에 빌더 빈이 자동 구성되지 않는다. HTTP 커넥터는 클래스패스의 reactor-netty 가
     * 자동으로 선택된다.
     */
    @Bean
    public WebClient aiServiceWebClient(AiServiceProperties properties) {
        return WebClient.builder().baseUrl(properties.baseUrl()).build();
    }
}
