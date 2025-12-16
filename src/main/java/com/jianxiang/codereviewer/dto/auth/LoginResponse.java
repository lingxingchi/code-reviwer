package com.jianxiang.codereviewer.dto.auth;

import com.jianxiang.codereviewer.dto.user.UserDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @className: LoginResponse
 * @author: jianXiang
 * @description: TODO
 * @date: 2026/1/26 15:33
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponse {

    /**
     * JWT 访问令牌
     */
    private String token;

    /**
     * 刷新令牌，用于刷新 access token）
     */
    private String refreshToken;

    /**
     * 用户信息（不含密码）
     */
    private UserDTO user;
}
