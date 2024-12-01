package com.coders.boardgame.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestClientConfig {

    @Value("${kakao.api-key}")
    private String kakaoApiKey;

    @Value("${kakao.address-api-url}")
    private String kakaoAddressApiUrl;

    @Bean
    public RestClient SchoolSearchClient(){
        return RestClient.builder()
                .baseUrl(kakaoAddressApiUrl)
                .defaultHeaders(httpHeaders -> {
                    httpHeaders.set("Authorization", "KakaoAK " + kakaoApiKey); // 기본 Authorization 헤더 설정
                })
                .build();
    }

}
