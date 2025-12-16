package com.jianxiang.codereviewer.domain.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

/**
 * 房间成员实体
 *
 * @author jianXiang
 * @date 2026/1/28
 */
@Data
@Table("room_member")
public class RoomMember {

    /**
     * 主键ID
     */
    @Id
    private Long id;

    /**
     * 房间ID
     */
    private Long roomId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 角色：OWNER（房主）/ MEMBER（成员）
     */
    private String role;

    /**
     * 加入时间
     */
    private LocalDateTime joinTime;

    /**
     * 邀请人ID（可为空，房主无邀请人）
     */
    private Long invitedBy;
}
