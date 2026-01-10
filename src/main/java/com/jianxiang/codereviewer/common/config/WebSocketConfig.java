package com.jianxiang.codereviewer.common.config;

import com.jianxiang.codereviewer.websocket.interceptor.JwtHandshakeInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.List;

/**
 * WebSocket配置类
 * 配置STOMP协议端点和消息代理
 */
@Slf4j
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtHandshakeInterceptor jwtHandshakeInterceptor;

    @Value("${websocket.endpoint:/ws}")
    private String endpoint;

    @Value("${websocket.allowed-origins}")
    private List<String> allowedOrigins;

    @Value("${websocket.stomp.broker.prefix:/topic}")
    private String brokerPrefix;

    @Value("${websocket.stomp.application.prefix:/app}")
    private String applicationPrefix;

    /**
     * 注册STOMP端点
     * 客户端使用此端点建立WebSocket连接
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint(endpoint)
                .setAllowedOrigins(allowedOrigins.toArray(new String[0]))
                .addInterceptors(jwtHandshakeInterceptor)
                .withSockJS(); // 启用SockJS降级支持

        log.info("WebSocket端点已注册: endpoint={}, allowedOrigins={}",
                endpoint, allowedOrigins);
    }

    /**
     * 配置消息代理
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 启用简单消息代理,用于广播消息
        // /topic: 用于房间广播
        // /user: 用于点对点消息
        registry.enableSimpleBroker(brokerPrefix, "/user");

        // 设置应用前缀,客户端发送消息时使用
        registry.setApplicationDestinationPrefixes(applicationPrefix);

        // 设置用户目的地前缀
        registry.setUserDestinationPrefix("/user");

        log.info("消息代理已配置: brokerPrefix={}, applicationPrefix={}",
                brokerPrefix, applicationPrefix);
    }

    /**
     * 配置客户端入站通道
     * 用于记录连接日志
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new org.springframework.messaging.support.ChannelInterceptor() {
            @Override
            public org.springframework.messaging.Message<?> preSend(
                    org.springframework.messaging.Message<?> message,
                    org.springframework.messaging.MessageChannel channel) {
                log.debug("收到客户端消息: {}", message.getHeaders());
                return message;
            }
        });
    }
}
