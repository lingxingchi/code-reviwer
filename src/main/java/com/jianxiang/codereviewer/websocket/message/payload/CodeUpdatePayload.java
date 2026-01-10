package com.jianxiang.codereviewer.websocket.message.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 代码更新消息载荷
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodeUpdatePayload {

    /**
     * 文件路径
     */
    private String filePath;

    /**
     * 代码内容
     */
    private String content;

    /**
     * 版本号(用于冲突检测)
     */
    private Long version;

    /**
     * 操作类型: INSERT, DELETE, UPDATE
     */
    private String operation;

    /**
     * 起始行号
     */
    private Integer startLine;

    /**
     * 结束行号
     */
    private Integer endLine;
}
