package com.jianxiang.codereviewer.websocket.message.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 评论消息载荷
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentPayload {

    /**
     * 评论ID
     */
    private Long commentId;

    /**
     * 文件路径
     */
    private String filePath;

    /**
     * 行号
     */
    private Integer lineNumber;

    /**
     * 评论内容
     */
    private String content;

    /**
     * 父评论ID(用于回复)
     */
    private Long parentId;

    /**
     * 创建时间
     */
    @Builder.Default
    private LocalDateTime createTime = LocalDateTime.now();
}
