package com.coders.boardgame.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;

@Configuration
public class CookieConfig {
    @Bean
    public CookieSerializer cookieSerializer() {
        DefaultCookieSerializer serializer = new DefaultCookieSerializer();
        serializer.setCookieName("JSESSIONID");
        serializer.setUseHttpOnlyCookie(true);
        serializer.setDomainName("suhat.kr");
        serializer.setSameSite("None"); // CORS 요청 허용
        serializer.setUseSecureCookie(false);
//        serializer.setUseSecureCookie(true);
        serializer.setCookiePath("/");
        return serializer;
    }
}
