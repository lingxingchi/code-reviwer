package com.jianxiang.codereviewer.websocket.listener;

import com.jianxiang.codereviewer.websocket.message.MessageType;
import com.jianxiang.codereviewer.websocket.message.WebSocketMessage;
import com.jianxiang.codereviewer.websocket.message.payload.JoinRoomPayload;
import com.jianxiang.codereviewer.websocket.session.WebSocketSessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

/**
 * WebSocket事件监听器
 * 监听WebSocket连接和断开事件
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final WebSocketSessionManager sessionManager;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 监听WebSocket连接建立事件
     */
    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();

        log.info("WebSocket连接已建立: sessionId={}", sessionId);
    }

    /**
     * 监听WebSocket断开连接事件
     * 清理会话并通知房间其他成员
     */
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();

        // 从会话属性获取用户信息
        Long userId = (Long) headerAccessor.getSessionAttributes().get("userId");
        String username = (String) headerAccessor.getSessionAttributes().get("username");
        String roomCode = (String) headerAccessor.getSessionAttributes().get("roomCode");

        if (userId == null || username == null) {
            log.warn("WebSocket断开连接: sessionId={}, 但未找到用户信息", sessionId);
            return;
        }

        log.info("WebSocket连接已断开: sessionId={}, userId={}, username={}, roomCode={}",
                sessionId, userId, username, roomCode);

        // 如果用户在某个房间,清理会话并广播离开消息
        if (roomCode != null && !roomCode.isEmpty()) {
            sessionManager.removeSession(roomCode, sessionId)
                    .doOnSuccess(success -> {
                        if (Boolean.TRUE.equals(success)) {
                            // 构建离开消息
                            JoinRoomPayload payload = JoinRoomPayload.builder()
                                    .userId(userId)
                                    .username(username)
                                    .build();

                            WebSocketMessage<JoinRoomPayload> message =
                                    WebSocketMessage.<JoinRoomPayload>builder()
                                            .type(MessageType.LEAVE_ROOM)
                                            .roomCode(roomCode)
                                            .senderId(userId)
                                            .senderUsername(username)
                                            .payload(payload)
                                            .build();

                            // 广播用户离开消息
                            messagingTemplate.convertAndSend(
                                    "/topic/room/" + roomCode,
                                    message
                            );

                            log.info("已广播用户离开消息: roomCode={}, userId={}, username={}",
                                    roomCode, userId, username);
                        }
                    })
                    .doOnError(e -> log.error("清理断开连接的会话失败: sessionId={}, roomCode={}",
                            sessionId, roomCode, e))
                    .subscribe();
        }
    }
}
