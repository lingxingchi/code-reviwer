package com.jianxiang.codereviewer.websocket.handler;

import com.jianxiang.codereviewer.service.room.RoomReviewService;
import com.jianxiang.codereviewer.websocket.message.MessageType;
import com.jianxiang.codereviewer.websocket.message.WebSocketMessage;
import com.jianxiang.codereviewer.websocket.message.payload.*;
import com.jianxiang.codereviewer.websocket.session.WebSocketSessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.Map;
import java.util.Set;

/**
 * 房间WebSocket消息处理器
 * 处理房间相关的WebSocket消息
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class RoomWebSocketHandler {

    private final SimpMessagingTemplate messagingTemplate;
    private final WebSocketSessionManager sessionManager;
    private final RoomReviewService roomReviewService;

    /**
     * 处理加入房间消息
     * 客户端发送到: /app/room/{roomCode}/join
     * 广播到: /topic/room/{roomCode}
     */
    @MessageMapping("/room/{roomCode}/join")
    public void handleJoinRoom(@DestinationVariable String roomCode,
                               @Payload JoinRoomPayload payload,
                               SimpMessageHeaderAccessor headerAccessor) {
        // 获取会话属性中的用户信息
        Long userId = (Long) headerAccessor.getSessionAttributes().get("userId");
        String username = (String) headerAccessor.getSessionAttributes().get("username");
        String sessionId = headerAccessor.getSessionId();

        // 保存roomCode到会话属性
        headerAccessor.getSessionAttributes().put("roomCode", roomCode);

        log.info("用户加入房间: roomCode={}, userId={}, username={}, sessionId={}",
                roomCode, userId, username, sessionId);

        // 验证房间是否存在
        roomReviewService.getRoomByCode(roomCode)
                .flatMap(room -> {
                    // 添加会话到SessionManager
                    return sessionManager.addSession(roomCode, userId, sessionId)
                            .flatMap(success -> {
                                if (Boolean.TRUE.equals(success)) {
                                    // 构建加入消息
                                    WebSocketMessage<JoinRoomPayload> message = WebSocketMessage.<JoinRoomPayload>builder()
                                            .type(MessageType.JOIN_ROOM)
                                            .roomCode(roomCode)
                                            .senderId(userId)
                                            .senderUsername(username)
                                            .payload(payload)
                                            .build();

                                    // 广播加入消息到房间所有订阅者
                                    messagingTemplate.convertAndSend(
                                            "/topic/room/" + roomCode,
                                            message
                                    );

                                    // 获取在线用户列表并发送给当前用户
                                    return sessionManager.getOnlineUsers(roomCode)
                                            .doOnNext(onlineUsers -> {
                                                messagingTemplate.convertAndSendToUser(
                                                        username,
                                                        "/queue/online-users",
                                                        Map.of("roomCode", roomCode, "users", onlineUsers)
                                                );
                                            });
                                }
                                return sessionManager.getOnlineUsers(roomCode);
                            });
                })
                .doOnError(e -> {
                    log.error("处理加入房间消息失败: roomCode={}, userId={}", roomCode, userId, e);
                    // 发送错误消息给用户
                    WebSocketMessage<String> errorMessage = WebSocketMessage.<String>builder()
                            .type(MessageType.ERROR)
                            .roomCode(roomCode)
                            .payload("加入房间失败: " + e.getMessage())
                            .build();
                    messagingTemplate.convertAndSendToUser(username, "/queue/errors", errorMessage);
                })
                .subscribe();
    }

    /**
     * 处理离开房间消息
     * 客户端发送到: /app/room/{roomCode}/leave
     */
    @MessageMapping("/room/{roomCode}/leave")
    public void handleLeaveRoom(@DestinationVariable String roomCode,
                                SimpMessageHeaderAccessor headerAccessor) {
        Long userId = (Long) headerAccessor.getSessionAttributes().get("userId");
        String username = (String) headerAccessor.getSessionAttributes().get("username");
        String sessionId = headerAccessor.getSessionId();

        log.info("用户离开房间: roomCode={}, userId={}, username={}",
                roomCode, userId, username);

        // 移除会话
        sessionManager.removeSession(roomCode, sessionId)
                .doOnSuccess(success -> {
                    if (Boolean.TRUE.equals(success)) {
                        // 构建离开消息
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

                        // 广播离开消息
                        messagingTemplate.convertAndSend(
                                "/topic/room/" + roomCode,
                                message
                        );
                    }
                })
                .doOnError(e -> log.error("处理离开房间消息失败: roomCode={}, userId={}",
                        roomCode, userId, e))
                .subscribe();
    }

    /**
     * 处理代码更新消息
     * 客户端发送到: /app/room/{roomCode}/code
     */
    @MessageMapping("/room/{roomCode}/code")
    public void handleCodeUpdate(@DestinationVariable String roomCode,
                                 @Payload CodeUpdatePayload payload,
                                 SimpMessageHeaderAccessor headerAccessor) {
        Long userId = (Long) headerAccessor.getSessionAttributes().get("userId");
        String username = (String) headerAccessor.getSessionAttributes().get("username");

        log.debug("代码更新: roomCode={}, userId={}, filePath={}",
                roomCode, userId, payload.getFilePath());

        // 构建代码更新消息
        WebSocketMessage<CodeUpdatePayload> message = WebSocketMessage.<CodeUpdatePayload>builder()
                .type(MessageType.CODE_UPDATE)
                .roomCode(roomCode)
                .senderId(userId)
                .senderUsername(username)
                .payload(payload)
                .build();

        // 广播到房间所有订阅者(除了发送者)
        messagingTemplate.convertAndSend(
                "/topic/room/" + roomCode,
                message
        );
    }

    /**
     * 处理光标位置消息
     * 客户端发送到: /app/room/{roomCode}/cursor
     */
    @MessageMapping("/room/{roomCode}/cursor")
    public void handleCursorPosition(@DestinationVariable String roomCode,
                                     @Payload CursorPositionPayload payload,
                                     SimpMessageHeaderAccessor headerAccessor) {
        Long userId = (Long) headerAccessor.getSessionAttributes().get("userId");
        String username = (String) headerAccessor.getSessionAttributes().get("username");

        // 构建光标位置消息
        WebSocketMessage<CursorPositionPayload> message = WebSocketMessage.<CursorPositionPayload>builder()
                .type(MessageType.CODE_CURSOR)
                .roomCode(roomCode)
                .senderId(userId)
                .senderUsername(username)
                .payload(payload)
                .build();

        // 广播光标位置
        messagingTemplate.convertAndSend(
                "/topic/room/" + roomCode,
                message
        );
    }

    /**
     * 处理添加评论消息
     * 客户端发送到: /app/room/{roomCode}/comment
     */
    @MessageMapping("/room/{roomCode}/comment")
    public void handleAddComment(@DestinationVariable String roomCode,
                                 @Payload CommentPayload payload,
                                 SimpMessageHeaderAccessor headerAccessor) {
        Long userId = (Long) headerAccessor.getSessionAttributes().get("userId");
        String username = (String) headerAccessor.getSessionAttributes().get("username");

        log.info("添加评论: roomCode={}, userId={}, filePath={}, lineNumber={}",
                roomCode, userId, payload.getFilePath(), payload.getLineNumber());

        // 构建评论消息
        WebSocketMessage<CommentPayload> message = WebSocketMessage.<CommentPayload>builder()
                .type(MessageType.COMMENT_ADD)
                .roomCode(roomCode)
                .senderId(userId)
                .senderUsername(username)
                .payload(payload)
                .build();

        // 广播评论消息
        messagingTemplate.convertAndSend(
                "/topic/room/" + roomCode,
                message
        );
    }
}
