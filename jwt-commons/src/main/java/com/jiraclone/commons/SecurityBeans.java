// jwt-commons/src/main/java/com/jiraclone/commons/SecurityBeans.java
package com.jiraclone.commons;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@AutoConfiguration
@ConditionalOnProperty(prefix = "jwt", name = "secret")
public class SecurityBeans {

    @Bean
    @ConditionalOnMissingBean
    JwtTokenProvider jwtTokenProvider(@Value("${jwt.secret}") String jwtSecret,
                                      @Value("${jwt.access-token-expiry-ms}") long accessTokenExpiryMs,
                                      @Value("${jwt.refresh-token-expiry-ms}") long refreshTokenExpiryMs) {
        return new JwtTokenProvider(jwtSecret, accessTokenExpiryMs, refreshTokenExpiryMs);
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @ConditionalOnClass(name = {
            "jakarta.servlet.Filter",
            "org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter"
    })
    static class ServletSecurityBeans {

        @Bean
        @ConditionalOnMissingBean
        JwtAuthenticationFilter jwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
            return new JwtAuthenticationFilter(jwtTokenProvider);
        }
    }
}
