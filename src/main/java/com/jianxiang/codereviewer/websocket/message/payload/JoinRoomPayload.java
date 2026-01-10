package com.jianxiang.codereviewer.websocket.message.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 加入/离开房间消息载荷
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JoinRoomPayload {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 用户头像
     */
    private String avatar;

    /**
     * 加入时间
     */
    @Builder.Default
    private LocalDateTime joinTime = LocalDateTime.now();
}
