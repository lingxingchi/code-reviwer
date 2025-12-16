package com.jianxiang.codereviewer.common.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Spring Security 配置（响应式）
 */
@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;

    /**
     * 白名单路径（无需认证）
     */
    private static final String[] AUTH_WHITELIST = {
        "/api/auth/**",
        "/actuator/**",
        "/ws/**",
        "/v3/api-docs/**",
        "/swagger-ui/**",
        "/swagger-ui.html"
    };

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
            // 禁用CSRF（使用JWT无需CSRF保护）
            .csrf(ServerHttpSecurity.CsrfSpec::disable)

            // 禁用表单登录
            .formLogin(ServerHttpSecurity.FormLoginSpec::disable)

            // 禁用HTTP Basic认证
            .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)

            // 配置认证管理器和上下文存储库
            .authenticationManager(authenticationManager)
            .securityContextRepository(securityContextRepository)

            // 配置异常处理
            .exceptionHandling(exceptionHandling -> exceptionHandling
                .authenticationEntryPoint((exchange, ex) -> {
                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                    return Mono.fromRunnable(() -> exchange.getResponse().setComplete());
                })
                .accessDeniedHandler((exchange, denied) -> {
                    exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                    return Mono.fromRunnable(() -> exchange.getResponse().setComplete());
                })
            )

            // 配置授权规则
            .authorizeExchange(exchanges -> exchanges
                .pathMatchers(AUTH_WHITELIST).permitAll()
                .anyExchange().authenticated()
            )

            .build();
    }

    /**
     * 密码编码器
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
