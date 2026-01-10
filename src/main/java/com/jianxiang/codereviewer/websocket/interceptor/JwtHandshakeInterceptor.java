package com.jianxiang.codereviewer.websocket.interceptor;

import com.jianxiang.codereviewer.common.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * WebSocket JWT认证拦截器
 * 在握手阶段验证JWT token
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtUtil jwtUtil;

    /**
     * 握手前的拦截处理
     * 从URL参数或Header中提取JWT token并验证
     */
    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                 ServerHttpResponse response,
                                 WebSocketHandler wsHandler,
                                 Map<String, Object> attributes) throws Exception {
        try {
            // 提取token
            String token = extractTokenFromRequest(request);

            if (token == null || token.isEmpty()) {
                log.warn("WebSocket连接被拒绝: 未提供token");
                return false;
            }

            // 验证token是否过期
            if (jwtUtil.isTokenExpired(token)) {
                log.warn("WebSocket连接被拒绝: token已过期");
                return false;
            }

            // 提取用户信息
            Long userId = jwtUtil.getUserIdFromToken(token);
            String username = jwtUtil.getUsernameFromToken(token);

            // 存储用户信息到WebSocket会话属性中
            attributes.put("userId", userId);
            attributes.put("username", username);
            attributes.put("token", token);

            log.info("WebSocket握手成功: userId={}, username={}", userId, username);
            return true;

        } catch (Exception e) {
            log.error("WebSocket握手失败: JWT验证异常", e);
            return false;
        }
    }

    /**
     * 握手后的处理
     */
    @Override
    public void afterHandshake(ServerHttpRequest request,
                             ServerHttpResponse response,
                             WebSocketHandler wsHandler,
                             Exception exception) {
        if (exception != null) {
            log.error("WebSocket握手后发生异常", exception);
        }
    }

    /**
     * 从请求中提取JWT token
     * 优先从URL参数获取,其次从Header获取
     */
    private String extractTokenFromRequest(ServerHttpRequest request) {
        // 1. 从URL参数提取: ws://host/ws?token=xxx
        String query = request.getURI().getQuery();
        if (query != null && query.contains("token=")) {
            String[] params = query.split("&");
            for (String param : params) {
                if (param.startsWith("token=")) {
                    return param.substring(6); // "token=".length() = 6
                }
            }
        }

        // 2. 从Header提取: Authorization: Bearer xxx
        if (request instanceof ServletServerHttpRequest) {
            ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
            String authHeader = servletRequest.getServletRequest().getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                return authHeader.substring(7);
            }
        }

        return null;
    }
}
