package com.jianxiang.codereviewer.websocket.message.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 光标位置消息载荷
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CursorPositionPayload {

    /**
     * 文件路径
     */
    private String filePath;

    /**
     * 行号
     */
    private Integer line;

    /**
     * 列号
     */
    private Integer column;

    /**
     * 光标颜色(用于区分不同用户)
     */
    private String color;
}
