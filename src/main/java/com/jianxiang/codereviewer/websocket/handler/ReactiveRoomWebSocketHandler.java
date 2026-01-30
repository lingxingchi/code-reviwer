package com.jianxiang.codereviewer.websocket.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jianxiang.codereviewer.common.util.JwtUtil;
import com.jianxiang.codereviewer.service.room.RoomReviewService;
import com.jianxiang.codereviewer.websocket.message.MessageType;
import com.jianxiang.codereviewer.websocket.message.WebSocketMessage;
import com.jianxiang.codereviewer.websocket.message.payload.*;
import com.jianxiang.codereviewer.websocket.session.ReactiveWebSocketSessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage.Type;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.net.URI;
import java.util.Map;

/**
 * 响应式 WebSocket 处理器
 * 使用 WebFlux 原生 WebSocket,完全响应式
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReactiveRoomWebSocketHandler implements WebSocketHandler {

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;
    private final ReactiveWebSocketSessionManager sessionManager;
    private final RoomReviewService roomReviewService;

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        // 1. 从 URL 提取房间代码
        String roomCode = extractRoomCode(session);
        if (roomCode == null) {
            log.warn("无效的房间代码,关闭连接: sessionId={}", session.getId());
            return session.close();
        }

        // 2. JWT 认证
        return authenticateSession(session)
                .flatMap(authInfo -> {
                    if (authInfo == null) {
                        log.warn("JWT 认证失败,关闭连接: sessionId={}", session.getId());
                        return session.close();
                    }

                    Long userId = (Long) authInfo.get("userId");
                    String username = (String) authInfo.get("username");

                    log.info("WebSocket 连接建立: sessionId={}, roomCode={}, userId={}, username={}",
                            session.getId(), roomCode, userId, username);

                    // 3. 验证房间存在性
                    return roomReviewService.getRoomByCode(roomCode)
                            .flatMap(room -> {
                                // 4. 注册会话到 SessionManager
                                return sessionManager.addSession(roomCode, userId, username, session)
                                        .then(Mono.defer(() -> {
                                            // 5. 发送欢迎消息
                                            return sendWelcomeMessage(session, roomCode, userId, username);
                                        }))
                                        .then(Mono.defer(() -> {
                                            // 6. 广播用户加入消息
                                            return broadcastJoinMessage(roomCode, userId, username);
                                        }))
                                        .then(Mono.defer(() -> {
                                            // 7. 处理消息流
                                            return handleMessageFlow(session, roomCode, userId, username);
                                        }));
                            })
                            .doFinally(signalType -> {
                                // 8. 连接关闭时清理
                                log.info("WebSocket 连接关闭: sessionId={}, roomCode={}, userId={}, signal={}",
                                        session.getId(), roomCode, userId, signalType);

                                sessionManager.removeSession(roomCode, session.getId())
                                        .then(broadcastLeaveMessage(roomCode, userId, username))
                                        .subscribe();
                            })
                            .onErrorResume(e -> {
                                log.error("WebSocket 处理异常: sessionId={}, roomCode={}",
                                        session.getId(), roomCode, e);
                                return session.close();
                            });
                })
                .onErrorResume(e -> {
                    log.error("WebSocket 认证异常: sessionId={}", session.getId(), e);
                    return session.close();
                });
    }

    /**
     * 从 URL 提取房间代码
     * URL 格式: ws://host:port/ws/room/{roomCode}?token=xxx
     */
    private String extractRoomCode(WebSocketSession session) {
        URI uri = session.getHandshakeInfo().getUri();
        String path = uri.getPath();

        // 解析路径: /ws/room/{roomCode}
        String[] segments = path.split("/");
        if (segments.length >= 4 && "ws".equals(segments[1]) && "room".equals(segments[2])) {
            return segments[3];
        }

        return null;
    }

    /**
     * JWT 认证
     * 从 URL 参数或 Header 中提取并验证 token
     */
    private Mono<Map<String, Object>> authenticateSession(WebSocketSession session) {
        return Mono.fromCallable(() -> {
            URI uri = session.getHandshakeInfo().getUri();
            String query = uri.getQuery();
            String token = null;

            // 1. 从 URL 参数提取 token
            if (query != null && query.contains("token=")) {
                String[] params = query.split("&");
                for (String param : params) {
                    if (param.startsWith("token=")) {
                        token = param.substring(6);
                        // URL 解码
                        try {
                            token = java.net.URLDecoder.decode(token, java.nio.charset.StandardCharsets.UTF_8);
                            log.debug("提取到 token,长度: {}", token.length());
                        } catch (Exception e) {
                            log.error("Token URL 解码失败", e);
                        }
                        break;
                    }
                }
            }

            // 2. 从 Header 提取 token
            if (token == null) {
                var headers = session.getHandshakeInfo().getHeaders();
                var authHeader = headers.getFirst("Authorization");
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    token = authHeader.substring(7);
                }
            }

            if (token == null || token.isEmpty()) {
                log.warn("未提供 JWT token");
                return null;
            }

            // 3. 验证 token
            if (jwtUtil.isTokenExpired(token)) {
                log.warn("JWT token 已过期");
                return null;
            }

            // 4. 提取用户信息
            Long userId = jwtUtil.getUserIdFromToken(token);
            String username = jwtUtil.getUsernameFromToken(token);

            return Map.<String, Object>of(
                    "userId", userId,
                    "username", username,
                    "token", token
            );
        });
    }

    /**
     * 处理消息流
     * 接收客户端消息并处理
     */
    private Mono<Void> handleMessageFlow(WebSocketSession session, String roomCode,
                                          Long userId, String username) {
        // 接收消息流
        Mono<Void> receiveHandler = session.receive()
                .filter(msg -> msg.getType() == Type.TEXT)
                .flatMap(msg -> {
                    String payload = msg.getPayloadAsText();
                    return handleIncomingMessage(roomCode, userId, username, payload);
                })
                .onErrorResume(e -> {
                    log.error("处理消息异常: roomCode={}, userId={}", roomCode, userId, e);
                    return Mono.empty();
                })
                .then();

        // 发送消息流 (订阅房间的广播消息)
        Flux<org.springframework.web.reactive.socket.WebSocketMessage> sendFlux =
                sessionManager.getMessageFlux(roomCode, session.getId())
                        .map(session::textMessage);

        // 合并接收和发送流
        return session.send(sendFlux)
                .and(receiveHandler);
    }

    /**
     * 处理接收到的消息
     */
    private Mono<Void> handleIncomingMessage(String roomCode, Long userId,
                                              String username, String payload) {
        return Mono.fromCallable(() -> {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> messageMap = objectMapper.readValue(payload, Map.class);
                String type = (String) messageMap.get("type");
                Object payloadObj = messageMap.get("payload");

                log.debug("收到消息: roomCode={}, userId={}, type={}", roomCode, userId, type);

                MessageType messageType = MessageType.valueOf(type);
                return Map.of(
                        "type", messageType,
                        "payload", payloadObj
                );
            } catch (Exception e) {
                log.error("解析消息失败: {}", payload, e);
                return null;
            }
        }).flatMap(parsedMessage -> {
            if (parsedMessage == null) {
                return Mono.empty();
            }

            MessageType type = (MessageType) parsedMessage.get("type");
            Object payloadObj = parsedMessage.get("payload");

            // 根据消息类型处理
            return switch (type) {
                case CODE_UPDATE -> handleCodeUpdate(roomCode, userId, username, payloadObj);
                case CODE_CURSOR -> handleCursorPosition(roomCode, userId, username, payloadObj);
                case COMMENT_ADD -> handleAddComment(roomCode, userId, username, payloadObj);
                case LEAVE_ROOM -> handleLeaveRoom(roomCode, userId, username);
                default -> {
                    log.warn("未知消息类型: {}", type);
                    yield Mono.empty();
                }
            };
        });
    }

    /**
     * 发送欢迎消息
     */
    private Mono<Void> sendWelcomeMessage(WebSocketSession session, String roomCode,
                                           Long userId, String username) {
        return sessionManager.getOnlineUsers(roomCode)
                .flatMap(onlineUsers -> {
                    try {
                        Map<String, Object> welcomeData = Map.of(
                                "type", "WELCOME",
                                "roomCode", roomCode,
                                "onlineUsers", onlineUsers
                        );
                        String json = objectMapper.writeValueAsString(welcomeData);
                        return session.send(Mono.just(session.textMessage(json)));
                    } catch (Exception e) {
                        log.error("发送欢迎消息失败", e);
                        return Mono.empty();
                    }
                });
    }

    /**
     * 广播用户加入消息
     */
    private Mono<Void> broadcastJoinMessage(String roomCode, Long userId, String username) {
        return Mono.fromCallable(() -> {
            try {
                JoinRoomPayload payload = JoinRoomPayload.builder()
                        .userId(userId)
                        .username(username)
                        .build();

                WebSocketMessage<JoinRoomPayload> message = WebSocketMessage.<JoinRoomPayload>builder()
                        .type(MessageType.JOIN_ROOM)
                        .roomCode(roomCode)
                        .senderId(userId)
                        .senderUsername(username)
                        .payload(payload)
                        .build();

                return objectMapper.writeValueAsString(message);
            } catch (Exception e) {
                log.error("构建加入消息失败", e);
                return null;
            }
        }).flatMap(json -> {
            if (json != null) {
                return sessionManager.broadcastToRoom(roomCode, json);
            }
            return Mono.empty();
        });
    }

    /**
     * 广播用户离开消息
     */
    private Mono<Void> broadcastLeaveMessage(String roomCode, Long userId, String username) {
        return Mono.fromCallable(() -> {
            try {
                JoinRoomPayload payload = JoinRoomPayload.builder()
                        .userId(userId)
                        .username(username)
                        .build();

                WebSocketMessage<JoinRoomPayload> message = WebSocketMessage.<JoinRoomPayload>builder()
                        .type(MessageType.LEAVE_ROOM)
                        .roomCode(roomCode)
                        .senderId(userId)
                        .senderUsername(username)
                        .payload(payload)
                        .build();

                return objectMapper.writeValueAsString(message);
            } catch (Exception e) {
                log.error("构建离开消息失败", e);
                return null;
            }
        }).flatMap(json -> {
            if (json != null) {
                return sessionManager.broadcastToRoom(roomCode, json);
            }
            return Mono.empty();
        });
    }

    /**
     * 处理代码更新
     */
    private Mono<Void> handleCodeUpdate(String roomCode, Long userId,
                                         String username, Object payloadObj) {
        return Mono.fromCallable(() -> {
            try {
                CodeUpdatePayload payload = objectMapper.convertValue(payloadObj, CodeUpdatePayload.class);

                WebSocketMessage<CodeUpdatePayload> message = WebSocketMessage.<CodeUpdatePayload>builder()
                        .type(MessageType.CODE_UPDATE)
                        .roomCode(roomCode)
                        .senderId(userId)
                        .senderUsername(username)
                        .payload(payload)
                        .build();

                return objectMapper.writeValueAsString(message);
            } catch (Exception e) {
                log.error("处理代码更新失败", e);
                return null;
            }
        }).flatMap(json -> {
            if (json != null) {
                return sessionManager.broadcastToRoom(roomCode, json);
            }
            return Mono.empty();
        });
    }

    /**
     * 处理光标位置更新
     */
    private Mono<Void> handleCursorPosition(String roomCode, Long userId,
                                             String username, Object payloadObj) {
        return Mono.fromCallable(() -> {
            try {
                CursorPositionPayload payload = objectMapper.convertValue(
                        payloadObj, CursorPositionPayload.class);

                WebSocketMessage<CursorPositionPayload> message =
                        WebSocketMessage.<CursorPositionPayload>builder()
                        .type(MessageType.CODE_CURSOR)
                        .roomCode(roomCode)
                        .senderId(userId)
                        .senderUsername(username)
                        .payload(payload)
                        .build();

                return objectMapper.writeValueAsString(message);
            } catch (Exception e) {
                log.error("处理光标位置失败", e);
                return null;
            }
        }).flatMap(json -> {
            if (json != null) {
                return sessionManager.broadcastToRoom(roomCode, json);
            }
            return Mono.empty();
        });
    }

    /**
     * 处理添加评论
     */
    private Mono<Void> handleAddComment(String roomCode, Long userId,
                                         String username, Object payloadObj) {
        return Mono.fromCallable(() -> {
            try {
                CommentPayload payload = objectMapper.convertValue(payloadObj, CommentPayload.class);

                WebSocketMessage<CommentPayload> message = WebSocketMessage.<CommentPayload>builder()
                        .type(MessageType.COMMENT_ADD)
                        .roomCode(roomCode)
                        .senderId(userId)
                        .senderUsername(username)
                        .payload(payload)
                        .build();

                return objectMapper.writeValueAsString(message);
            } catch (Exception e) {
                log.error("处理评论失败", e);
                return null;
            }
        }).flatMap(json -> {
            if (json != null) {
                return sessionManager.broadcastToRoom(roomCode, json);
            }
            return Mono.empty();
        });
    }

    /**
     * 处理离开房间
     */
    private Mono<Void> handleLeaveRoom(String roomCode, Long userId, String username) {
        log.info("用户主动离开房间: roomCode={}, userId={}", roomCode, userId);
        return broadcastLeaveMessage(roomCode, userId, username);
    }
}
