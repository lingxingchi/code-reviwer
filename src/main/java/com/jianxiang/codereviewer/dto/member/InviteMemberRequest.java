package com.jianxiang.codereviewer.dto.member;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 邀请成员请求 DTO
 *
 * @author jianXiang
 * @date 2026/1/28
 */
@Data
public class InviteMemberRequest {

    /**
     * 被邀请用户ID
     */
    @NotNull(message = "用户ID不能为空")
    private Long userId;
}
