package com.coders.boardgame.config;

import jakarta.servlet.http.Cookie;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CookieConfig {

    @Bean
    public ServletContextInitializer servletContextInitializer() {
        return servletContext -> {
            Cookie cookie = new Cookie("JSESSIONID", null);
            cookie.setHttpOnly(true);
            cookie.setSecure(false); // HTTP 환경
            cookie.setPath("/");
            cookie.setAttribute("SameSite", "Lax");
        };
    }

}
