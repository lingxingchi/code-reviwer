package com.jianxiang.codereviewer.dto.auth;

import lombok.Data;

/**
 * @className: RegisterRequest
 * @author: jianXiang
 * @description: TODO
 * @date: 2026/1/26 15:38
 */
@Data
public class RegisterRequest {
    /**
     * 用户名（唯一）
     */
    private String username;

    /**
     * 密码（明文）
     */
    private String password;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 昵称
     */
    private String nickname;
}
