package com.jianxiang.codereviewer.common.config;

import com.jianxiang.codereviewer.websocket.handler.ReactiveRoomWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;

import java.util.HashMap;
import java.util.Map;

/**
 * WebFlux 原生 WebSocket 配置
 * 使用响应式编程模型,完全基于 Reactor
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class WebFluxWebSocketConfig {

    private final ReactiveRoomWebSocketHandler roomWebSocketHandler;

    /**
     * 配置 WebSocket 路由映射
     * 将 URL 路径映射到对应的 Handler
     */
    @Bean
    public HandlerMapping webSocketHandlerMapping() {
        Map<String, ReactiveRoomWebSocketHandler> map = new HashMap<>();
        // 路径格式: ws://host:port/ws/room/{roomCode}
        map.put("/ws/room/**", roomWebSocketHandler);

        SimpleUrlHandlerMapping handlerMapping = new SimpleUrlHandlerMapping();
        handlerMapping.setOrder(Ordered.HIGHEST_PRECEDENCE);
        handlerMapping.setUrlMap(map);

        log.info("WebSocket 路由已配置: /ws/room/**");
        return handlerMapping;
    }

    /**
     * WebSocket 处理器适配器
     * 用于将 WebSocketHandler 适配到 WebFlux 框架
     */
    @Bean
    public WebSocketHandlerAdapter handlerAdapter() {
        return new WebSocketHandlerAdapter();
    }
}
