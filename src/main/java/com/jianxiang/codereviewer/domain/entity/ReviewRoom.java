package com.jianxiang.codereviewer.domain.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

/**
 * @className: ReviewRoom
 * @author: jianXiang
 * @description: TODO
 * @date: 2026/1/27 18:30
 */

@Data
@Table("review_room")
public class ReviewRoom {
    @Id
    private Long id;

    /**
     * 房间名称
     */
    private String name;

    /**
     * 房间码（唯一，用于快速加入房间）
     */
    private String roomCode;

    /**
     * 房间描述
     */
    private String description;

    /**
     * 创建者ID
     */
    private Long ownerId;

    /**
     * 房间状态：ONGOING=进行中，CLOSED=已关闭，ARCHIVED=已归档
     */
    private String status;

    /**
     * 仓库信息（Git URL等）
     */
    private String repoInfo;

    /**
     * 代码范围说明
     */
    private String codeScope;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 关闭时间
     */
    private LocalDateTime closeTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
