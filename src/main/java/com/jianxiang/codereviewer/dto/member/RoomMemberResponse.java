package com.jianxiang.codereviewer.dto.member;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 房间成员响应 DTO
 *
 * @author jianXiang
 * @date 2026/1/28
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomMemberResponse {

    /**
     * 成员ID
     */
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
     * 用户名
     */
    private String username;

    /**
     * 角色：OWNER（房主）/ MEMBER（成员）
     */
    private String role;

    /**
     * 角色描述
     */
    private String roleDesc;

    /**
     * 加入时间
     */
    private LocalDateTime joinTime;

    /**
     * 邀请人ID
     */
    private Long invitedBy;

    /**
     * 邀请人用户名
     */
    private String invitedByUsername;
}
