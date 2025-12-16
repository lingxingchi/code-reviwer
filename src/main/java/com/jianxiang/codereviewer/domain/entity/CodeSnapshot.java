package com.jianxiang.codereviewer.domain.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

/**
 * @className: CodeSnapshot
 * @author: jianXiang
 * @description: 代码快照实体
 * @date: 2026/1/29
 */
@Data
@Table("code_snapshot")
public class CodeSnapshot {

    /**
     * 主键ID
     */
    @Id
    private Long id;

    /**
     * 所属房间ID
     */
    @Column("room_id")
    private Long roomId;

    /**
     * 版本号（房间内递增）
     */
    @Column("version")
    private Integer version;

    /**
     * 代码内容
     */
    @Column("content")
    private String content;

    /**
     * 编程语言
     */
    @Column("language")
    private String language;

    /**
     * 文件路径
     */
    @Column("file_path")
    private String filePath;

    /**
     * 版本说明
     */
    @Column("description")
    private String description;

    /**
     * 创建者ID
     */
    @Column("created_by")
    private Long createdBy;

    /**
     * 创建时间
     */
    @Column("create_time")
    private LocalDateTime createTime;
}
