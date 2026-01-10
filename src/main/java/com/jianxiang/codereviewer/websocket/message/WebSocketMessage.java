package com.jianxiang.codereviewer.websocket.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * WebSocket消息包装类
 * 统一的消息格式,包含类型、发送者、载荷等信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketMessage<T> {

    /**
     * 消息类型
     */
    private MessageType type;

    /**
     * 房间代码
     */
    private String roomCode;

    /**
     * 发送者ID
     */
    private Long senderId;

    /**
     * 发送者用户名
     */
    private String senderUsername;

    /**
     * 消息载荷(泛型,支持不同类型的消息内容)
     */
    private T payload;

    /**
     * 消息时间戳
     */
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    /**
     * 消息唯一ID
     */
    @Builder.Default
    private String messageId = UUID.randomUUID().toString();
}
