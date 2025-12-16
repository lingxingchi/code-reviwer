package com.jianxiang.codereviewer.dto.auth;

import lombok.Data;

/**
 * @className: LoginRequest
 * @author: jianXiang
 * @description: TODO
 * @date: 2026/1/26 15:32
 */
@Data
public class LoginRequest {

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码（明文，传输时建议使用 HTTPS）
     */
    private String password;
}
