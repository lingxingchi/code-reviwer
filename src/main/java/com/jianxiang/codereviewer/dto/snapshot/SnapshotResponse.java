package com.jianxiang.codereviewer.dto.snapshot;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 代码快照响应 DTO
 *
 * @author jianXiang
 * @date 2026/1/29
 */
@Data
@Builder
public class SnapshotResponse {

    /**
     * 快照ID
     */
    private Long id;

    /**
     * 所属房间ID
     */
    private Long roomId;

    /**
     * 版本号
     */
    private Integer version;

    /**
     * 代码内容
     */
    private String content;

    /**
     * 编程语言
     */
    private String language;

    /**
     * 文件路径
     */
    private String filePath;

    /**
     * 版本说明
     */
    private String description;

    /**
     * 创建者ID
     */
    private Long createdBy;

    /**
     * 创建者用户名
     */
    private String createdByUsername;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
