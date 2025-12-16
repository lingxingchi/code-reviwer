package com.jianxiang.codereviewer.dto.room;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 评审房间详情响应 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomResponse {

    /**
     * 房间ID
     */
    private Long id;

    /**
     * 房间名称
     */
    private String name;

    /**
     * 房间码
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
     * 创建者用户名
     */
    private String ownerUsername;

    /**
     * 房间状态
     */
    private String status;

    /**
     * 仓库信息
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
