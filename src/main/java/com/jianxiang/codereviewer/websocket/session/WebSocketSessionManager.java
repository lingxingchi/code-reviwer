package com.jianxiang.codereviewer.websocket.session;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Set;

/**
 * WebSocket会话管理器
 * 使用Redis存储会话信息,支持分布式部署
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketSessionManager {

    private final ReactiveRedisTemplate<String, Object> redisTemplate;

    // Redis Key前缀
    private static final String SESSION_KEY_PREFIX = "ws:session:";
    private static final String ROOM_USERS_KEY_PREFIX = "ws:room:users:";

    // 会话过期时间: 24小时
    private static final Duration SESSION_TTL = Duration.ofHours(24);

    /**
     * 添加会话
     *
     * @param roomCode  房间代码
     * @param userId    用户ID
     * @param sessionId WebSocket会话ID
     * @return Mono<Boolean> 添加是否成功
     */
    public Mono<Boolean> addSession(String roomCode, Long userId, String sessionId) {
        String sessionKey = SESSION_KEY_PREFIX + sessionId;

        return redisTemplate.opsForHash()
                .putAll(sessionKey, java.util.Map.of(
                        "userId", userId.toString(),
                        "roomCode", roomCode,
                        "connectTime", System.currentTimeMillis()
                ))
                .flatMap(success -> {
                    if (Boolean.TRUE.equals(success)) {
                        // 设置过期时间
                        return redisTemplate.expire(sessionKey, SESSION_TTL)
                                .flatMap(expired -> {
                                    // 添加用户到房间用户集合
                                    String roomUsersKey = ROOM_USERS_KEY_PREFIX + roomCode;
                                    return redisTemplate.opsForSet()
                                            .add(roomUsersKey, userId.toString())
                                            .doOnSuccess(added ->
                                                    log.info("会话已添加: sessionId={}, roomCode={}, userId={}",
                                                            sessionId, roomCode, userId))
                                            .map(added -> Boolean.TRUE);
                                });
                    }
                    return Mono.just(Boolean.FALSE);
                })
                .doOnError(e -> log.error("添加会话失败: sessionId={}", sessionId, e))
                .onErrorReturn(Boolean.FALSE);
    }

    /**
     * 移除会话
     *
     * @param roomCode  房间代码
     * @param sessionId WebSocket会话ID
     * @return Mono<Boolean> 移除是否成功
     */
    public Mono<Boolean> removeSession(String roomCode, String sessionId) {
        String sessionKey = SESSION_KEY_PREFIX + sessionId;

        // 先获取userId
        return redisTemplate.opsForHash()
                .get(sessionKey, "userId")
                .flatMap(userIdObj -> {
                    String userId = userIdObj.toString();

                    // 删除会话
                    return redisTemplate.delete(sessionKey)
                            .flatMap(deleted -> {
                                // 从房间用户集合中移除
                                String roomUsersKey = ROOM_USERS_KEY_PREFIX + roomCode;
                                return redisTemplate.opsForSet()
                                        .remove(roomUsersKey, userId)
                                        .doOnSuccess(removed ->
                                                log.info("会话已移除: sessionId={}, roomCode={}, userId={}",
                                                        sessionId, roomCode, userId))
                                        .map(removed -> Boolean.TRUE);
                            });
                })
                .doOnError(e -> log.error("移除会话失败: sessionId={}", sessionId, e))
                .onErrorReturn(Boolean.FALSE);
    }

    /**
     * 获取房间在线用户集合
     *
     * @param roomCode 房间代码
     * @return Mono<Set < String>> 用户ID集合
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
     *
     * @param roomCode 房间代码
     * @return Mono<Long> 在线用户数量
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
     *
     * @param roomCode 房间代码
     * @param userId   用户ID
     * @return Mono<Boolean> 是否在房间
     */
    public Mono<Boolean> isUserInRoom(String roomCode, Long userId) {
        String roomUsersKey = ROOM_USERS_KEY_PREFIX + roomCode;
        return redisTemplate.opsForSet()
                .isMember(roomUsersKey, userId.toString())
                .doOnError(e -> log.error("检查用户是否在房间失败: roomCode={}, userId={}",
                        roomCode, userId, e))
                .onErrorReturn(Boolean.FALSE);
    }
}
