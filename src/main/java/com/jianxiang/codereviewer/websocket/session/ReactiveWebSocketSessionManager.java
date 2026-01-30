package com.jianxiang.codereviewer.websocket.session;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 响应式 WebSocket 会话管理器
 * 使用 Reactor Sinks 实现消息广播
 * 使用 Redis 存储用户在线状态(支持分布式)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReactiveWebSocketSessionManager {

    private final ReactiveRedisTemplate<String, Object> redisTemplate;

    // 本地会话存储: roomCode -> (sessionId -> Session信息)
    private final Map<String, Map<String, SessionInfo>> localSessions = new ConcurrentHashMap<>();

    // 房间消息广播器: roomCode -> Sink
    private final Map<String, Sinks.Many<String>> roomSinks = new ConcurrentHashMap<>();

    // Redis Key前缀
    private static final String ROOM_USERS_KEY_PREFIX = "ws:room:users:";
    private static final String USER_INFO_KEY_PREFIX = "ws:user:info:";

    // 会话过期时间: 24小时
    private static final Duration SESSION_TTL = Duration.ofHours(24);

    /**
     * 会话信息
     */
    private record SessionInfo(Long userId, String username, WebSocketSession session) {}

    /**
     * 添加会话
     */
    public Mono<Void> addSession(String roomCode, Long userId, String username,
                                  WebSocketSession session) {
        String sessionId = session.getId();

        // 1. 保存到本地内存
        localSessions.computeIfAbsent(roomCode, k -> new ConcurrentHashMap<>())
                .put(sessionId, new SessionInfo(userId, username, session));

        // 2. 保存到 Redis (用户在线状态)
        String roomUsersKey = ROOM_USERS_KEY_PREFIX + roomCode;
        String userInfoKey = USER_INFO_KEY_PREFIX + roomCode + ":" + userId;

        return redisTemplate.opsForSet()
                .add(roomUsersKey, userId.toString())
                .then(redisTemplate.opsForHash()
                        .putAll(userInfoKey, Map.of(
                                "userId", userId.toString(),
                                "username", username,
                                "sessionId", sessionId,
                                "joinTime", System.currentTimeMillis()
                        )))
                .then(redisTemplate.expire(userInfoKey, SESSION_TTL))
                .doOnSuccess(v -> log.info("会话已添加: roomCode={}, userId={}, username={}, sessionId={}",
                        roomCode, userId, username, sessionId))
                .doOnError(e -> log.error("添加会话失败: roomCode={}, userId={}", roomCode, userId, e))
                .then();
    }

    /**
     * 移除会话
     */
    public Mono<Void> removeSession(String roomCode, String sessionId) {
        // 1. 从本地内存移除
        Map<String, SessionInfo> roomSessions = localSessions.get(roomCode);
        if (roomSessions == null) {
            return Mono.empty();
        }

        SessionInfo sessionInfo = roomSessions.remove(sessionId);
        if (sessionInfo == null) {
            return Mono.empty();
        }

        // 如果房间没有会话了,清理房间
        if (roomSessions.isEmpty()) {
            localSessions.remove(roomCode);
            roomSinks.remove(roomCode);
        }

        // 2. 从 Redis 移除
        Long userId = sessionInfo.userId();
        String roomUsersKey = ROOM_USERS_KEY_PREFIX + roomCode;
        String userInfoKey = USER_INFO_KEY_PREFIX + roomCode + ":" + userId;

        return redisTemplate.opsForSet()
                .remove(roomUsersKey, userId.toString())
                .then(redisTemplate.delete(userInfoKey))
                .doOnSuccess(v -> log.info("会话已移除: roomCode={}, userId={}, sessionId={}",
                        roomCode, userId, sessionId))
                .doOnError(e -> log.error("移除会话失败: roomCode={}, sessionId={}", roomCode, sessionId, e))
                .then();
    }

    /**
     * 获取房间的消息流
     * 每个 WebSocket 连接订阅此流接收广播消息
     */
    public Flux<String> getMessageFlux(String roomCode, String sessionId) {
        Sinks.Many<String> sink = roomSinks.computeIfAbsent(roomCode, k ->
                Sinks.many().multicast().onBackpressureBuffer());

        return sink.asFlux()
                // 过滤掉发送者自己的消息 (可选)
                .filter(message -> !message.contains("\"sessionId\":\"" + sessionId + "\""))
                .doOnError(e -> log.error("消息流异常: roomCode={}, sessionId={}", roomCode, sessionId, e))
                .onErrorResume(e -> Flux.empty());
    }

    /**
     * 广播消息到房间所有连接
     */
    public Mono<Void> broadcastToRoom(String roomCode, String message) {
        Sinks.Many<String> sink = roomSinks.get(roomCode);
        if (sink == null) {
            log.warn("房间不存在或无连接: roomCode={}", roomCode);
            return Mono.empty();
        }

        return Mono.fromRunnable(() -> {
            Sinks.EmitResult result = sink.tryEmitNext(message);
            if (result.isFailure()) {
                log.warn("广播消息失败: roomCode={}, result={}", roomCode, result);
            } else {
                log.debug("消息已广播: roomCode={}", roomCode);
            }
        });
    }

    /**
     * 发送消息给指定用户
     */
    public Mono<Void> sendToUser(String roomCode, Long userId, String message) {
        Map<String, SessionInfo> roomSessions = localSessions.get(roomCode);
        if (roomSessions == null) {
            return Mono.empty();
        }

        return Flux.fromIterable(roomSessions.values())
                .filter(info -> info.userId().equals(userId))
                .flatMap(info -> info.session().send(
                        Mono.just(info.session().textMessage(message))
                ))
                .then()
                .doOnSuccess(v -> log.debug("消息已发送给用户: roomCode={}, userId={}", roomCode, userId))
                .doOnError(e -> log.error("发送消息给用户失败: roomCode={}, userId={}", roomCode, userId, e))
                .onErrorResume(e -> Mono.empty());
    }

    /**
     * 获取房间在线用户列表
     */
    public Mono<Set<String>> getOnlineUsers(String roomCode) {
        String roomUsersKey = ROOM_USERS_KEY_PREFIX + roomCode;
        return redisTemplate.opsForSet()
                .members(roomUsersKey)
                .map(Object::toString)
                .collect(java.util.stream.Collectors.toSet())
                .doOnError(e -> log.error("获取在线用户失败: roomCode={}", roomCode, e))
                .onErrorReturn(Set.of());
    }

    /**
     * 获取房间在线用户数量
     */
    public Mono<Long> getOnlineUserCount(String roomCode) {
        String roomUsersKey = ROOM_USERS_KEY_PREFIX + roomCode;
        return redisTemplate.opsForSet()
                .size(roomUsersKey)
                .doOnError(e -> log.error("获取在线用户数量失败: roomCode={}", roomCode, e))
                .onErrorReturn(0L);
    }

    /**
     * 检查用户是否在房间中
     */
    public Mono<Boolean> isUserInRoom(String roomCode, Long userId) {
        String roomUsersKey = ROOM_USERS_KEY_PREFIX + roomCode;
        return redisTemplate.opsForSet()
                .isMember(roomUsersKey, userId.toString())
                .doOnError(e -> log.error("检查用户是否在房间失败: roomCode={}, userId={}",
                        roomCode, userId, e))
                .onErrorReturn(Boolean.FALSE);
    }

    /**
     * 获取房间的本地会话数量
     */
    public int getLocalSessionCount(String roomCode) {
        Map<String, SessionInfo> roomSessions = localSessions.get(roomCode);
        return roomSessions != null ? roomSessions.size() : 0;
    }
}
